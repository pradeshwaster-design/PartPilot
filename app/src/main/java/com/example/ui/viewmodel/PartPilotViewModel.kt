package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantEngine
import com.example.ai.AiChatMessage
import com.example.ai.ParsedPartCandidate
import com.example.ai.ProposedWriteAction
import com.example.data.local.PartPilotDatabase
import com.example.data.model.*
import com.example.data.repository.PartPilotRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class SortOption(val displayName: String) {
    NAME("Name (A-Z)"),
    LOCATION("Location"),
    QUANTITY("Quantity (High-Low)")
}

data class StockUiState(
    val parts: List<PartEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedGroup: PartGroup? = null,
    val selectedShelf: String? = null,
    val selectedCondition: PartCondition? = null,
    val sortOption: SortOption = SortOption.NAME,
    val locations: Map<Long, LocationEntity> = emptyMap()
)

class PartPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PartPilotDatabase.getDatabase(application)
    val repository = PartPilotRepository(db.dao())
    val aiEngine = AiAssistantEngine(repository)

    // --- State ---
    init {
        viewModelScope.launch {
            if (repository.getLocationsList().isEmpty()) {
                PartPilotDatabase.seedDatabase(db.dao())
            }
        }
    }

    val allLocations: StateFlow<List<LocationEntity>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCartItems: StateFlow<List<CartItemEntity>> = repository.allCartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAiLogs: StateFlow<List<AiActionLogEntity>> = repository.allAiLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock Screen Filter & Sort State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow<PartGroup?>(null)
    val selectedGroup = _selectedGroup.asStateFlow()

    private val _selectedShelf = MutableStateFlow<String?>(null)
    val selectedShelf = _selectedShelf.asStateFlow()

    private val _selectedCondition = MutableStateFlow<PartCondition?>(null)
    val selectedCondition = _selectedCondition.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption = _sortOption.asStateFlow()

    private data class FilterParams(
        val query: String,
        val group: PartGroup?,
        val shelf: String?,
        val condition: PartCondition?,
        val sort: SortOption
    )

    private val _filterState = combine(
        _searchQuery,
        _selectedGroup,
        _selectedShelf,
        _selectedCondition,
        _sortOption
    ) { query, group, shelf, condition, sort ->
        FilterParams(query, group, shelf, condition, sort)
    }

    val stockUiState: StateFlow<StockUiState> = combine(
        repository.allParts,
        allLocations,
        _filterState
    ) { rawParts, locations, filters ->
        val locMap = locations.associateBy { it.id }

        val filtered = rawParts.filter { part ->
            val matchesQuery = filters.query.isBlank() ||
                    part.name.contains(filters.query, ignoreCase = true) ||
                    part.specs.contains(filters.query, ignoreCase = true) ||
                    locMap[part.locationId]?.code?.contains(filters.query, ignoreCase = true) == true

            val matchesGroup = filters.group == null || part.group == filters.group
            val matchesShelf = filters.shelf == null || locMap[part.locationId]?.shelf.equals(filters.shelf, ignoreCase = true)
            val matchesCondition = filters.condition == null || part.condition == filters.condition

            matchesQuery && matchesGroup && matchesShelf && matchesCondition
        }

        val sorted = when (filters.sort) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
            SortOption.LOCATION -> filtered.sortedBy { locMap[it.locationId]?.code ?: "ZZZ" }
            SortOption.QUANTITY -> filtered.sortedByDescending { it.quantity }
        }

        StockUiState(
            parts = sorted,
            searchQuery = filters.query,
            selectedGroup = filters.group,
            selectedShelf = filters.shelf,
            selectedCondition = filters.condition,
            sortOption = filters.sort,
            locations = locMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockUiState())

    // --- AI Assistant Chat State ---
    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isUser = false,
                text = "Welcome to PartPilot AI! Grounded directly in your workshop inventory.\n\nTry asking: *\"Do I have an L298N?\"*, *\"Where is my MPU6050?\"*, or *\"What do I still need for Line Follower v2?\"*"
            )
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    // --- Add Part Workflow Candidates ---
    private val _addCandidates = MutableStateFlow<List<ParsedPartCandidate>>(emptyList())
    val addCandidates = _addCandidates.asStateFlow()

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedGroup(group: PartGroup?) { _selectedGroup.value = group }
    fun setSelectedShelf(shelf: String?) { _selectedShelf.value = shelf }
    fun setSelectedCondition(condition: PartCondition?) { _selectedCondition.value = condition }
    fun setSortOption(sort: SortOption) { _sortOption.value = sort }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedGroup.value = null
        _selectedShelf.value = null
        _selectedCondition.value = null
        _sortOption.value = SortOption.NAME
    }

    // --- AI Assistant Queries ---
    fun sendAiQuery(query: String) {
        if (query.isBlank()) return
        val userMsg = AiChatMessage(isUser = true, text = query)
        _chatMessages.update { it + userMsg }
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val aiResponse = aiEngine.processQuery(query)
                _chatMessages.update { it + aiResponse }
            } catch (e: Exception) {
                _chatMessages.update {
                    it + AiChatMessage(
                        isUser = false,
                        text = "AI is unavailable or encountered an error. Please try again: ${e.localizedMessage}"
                    )
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun executeAiWriteAction(messageId: String, action: ProposedWriteAction) {
        viewModelScope.launch {
            when (action.actionType) {
                "MOVE_PART" -> {
                    if (action.partId != null && action.targetLocationId != null) {
                        repository.movePart(action.partId, action.targetLocationId)
                        repository.logAiAction(action.actionType, action.title, "Moved part #${action.partId} to loc #${action.targetLocationId}")
                        _statusMessage.emit("Component moved successfully!")
                    }
                }
                "ADJUST_QTY" -> {
                    if (action.partId != null && action.newQuantity != null) {
                        repository.adjustPartQuantity(action.partId, action.newQuantity, "Grounded AI Action Confirmed")
                        repository.logAiAction(action.actionType, action.title, "Adjusted part #${action.partId} to ${action.newQuantity}")
                        _statusMessage.emit("Quantity adjusted!")
                    }
                }
                "ADD_PART" -> {
                    if (action.partName != null) {
                        repository.insertPart(
                            PartEntity(
                                name = action.partName,
                                group = action.group ?: PartGroup.OTHER,
                                specs = action.specs ?: "Cataloged item",
                                quantity = action.newQuantity ?: 1,
                                locationId = action.targetLocationId,
                                condition = PartCondition.WORKING,
                                remarks = "Added via AI prompt"
                            )
                        )
                        repository.logAiAction(action.actionType, action.title, "Added ${action.partName} (${action.newQuantity})")
                        _statusMessage.emit("Component saved to inventory!")
                    }
                }
            }

            // Mark action as executed in message
            _chatMessages.update { list ->
                list.map { msg ->
                    if (msg.id == messageId) msg.copy(actionExecuted = true) else msg
                }
            }
        }
    }

    fun parseVoiceOrTextInput(input: String) {
        viewModelScope.launch {
            val candidates = aiEngine.parseVoiceOrTextPrompt(input)
            _addCandidates.value = candidates
        }
    }

    fun processCapturedPhoto(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                val candidates = aiEngine.parseCapturedPhoto(bitmap)
                _addCandidates.value = candidates
                _statusMessage.emit("Photo processed! ${candidates.size} candidate(s) ready to review.")
            } catch (e: Exception) {
                _statusMessage.emit("Photo analysis error: ${e.localizedMessage}")
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun updateCandidate(id: String, transform: (ParsedPartCandidate) -> ParsedPartCandidate) {
        _addCandidates.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    fun removeCandidate(id: String) {
        _addCandidates.update { it.filter { c -> c.id != id } }
    }

    fun commitCandidatesToStock() {
        viewModelScope.launch {
            val candidates = _addCandidates.value
            for (c in candidates) {
                repository.insertPart(
                    PartEntity(
                        name = c.name,
                        group = c.group,
                        specs = c.specs,
                        quantity = c.quantity,
                        locationId = c.suggestedLocationId ?: 1L,
                        condition = PartCondition.WORKING,
                        remarks = "Added via fast intake"
                    )
                )
            }
            _addCandidates.value = emptyList()
            _statusMessage.emit("${candidates.size} parts saved to stock!")
        }
    }

    fun mergeCandidateWithExisting(candidateId: String, existingPartId: Long, addQty: Int) {
        viewModelScope.launch {
            val existing = repository.getPartById(existingPartId)
            if (existing != null) {
                repository.adjustPartQuantity(existing.id, existing.quantity + addQty, "Merged candidate $candidateId")
                removeCandidate(candidateId)
                _statusMessage.emit("Merged $addQty units into existing ${existing.name}!")
            }
        }
    }

    // --- Location Actions ---
    fun createLocation(shelf: String, level: String, box: String, notes: String) {
        viewModelScope.launch {
            repository.insertLocation(shelf, level, box, notes)
            _statusMessage.emit("Location added!")
        }
    }

    fun updateLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.updateLocation(location)
            _statusMessage.emit("Location updated!")
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            _statusMessage.emit("Location deleted!")
        }
    }

    // --- Project Actions ---
    fun createProject(name: String, description: String, budgetCap: Double, notes: String) {
        viewModelScope.launch {
            repository.insertProject(
                ProjectEntity(
                    name = name,
                    description = description,
                    budgetCap = budgetCap,
                    notes = notes
                )
            )
            _statusMessage.emit("Project '$name' created!")
        }
    }

    fun addPartToProject(projectId: Long, partId: Long?, name: String, qty: Int, price: Double) {
        viewModelScope.launch {
            repository.addProjectPart(
                ProjectPartEntity(
                    projectId = projectId,
                    partId = partId,
                    name = name,
                    qtyNeeded = qty,
                    estUnitPrice = price
                )
            )
            _statusMessage.emit("Part added to project BOM!")
        }
    }

    fun reserveStock(projectPartId: Long, qty: Int) {
        viewModelScope.launch {
            repository.reserveStockForProject(projectPartId, qty)
            _statusMessage.emit("Reserved $qty units for project!")
        }
    }

    fun updateChecklist(projectPartId: Long, state: ChecklistState) {
        viewModelScope.launch {
            repository.updateChecklistState(projectPartId, state)
        }
    }

    fun finishProject(projectId: Long) {
        viewModelScope.launch {
            repository.finishProject(projectId)
            _statusMessage.emit("Project completed! Reserved parts returned to available inventory.")
        }
    }

    // --- Cart Actions ---
    fun addCartItem(name: String, qty: Int, price: Double, shop: String) {
        viewModelScope.launch {
            repository.addCartItem(name, qty, price, shop = shop)
            _statusMessage.emit("Item added to cart!")
        }
    }

    fun markCartItemBought(item: CartItemEntity, targetLocationId: Long) {
        viewModelScope.launch {
            repository.markCartItemBought(item, targetLocationId)
            _statusMessage.emit("Item bought and added to stock!")
        }
    }

    fun removeCartItem(item: CartItemEntity) {
        viewModelScope.launch {
            repository.deleteCartItem(item)
            _statusMessage.emit("Item removed from cart.")
        }
    }
}

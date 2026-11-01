package com.example.data.repository

import com.example.data.local.PartPilotDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class PartPilotRepository(private val dao: PartPilotDao) {

    // --- Locations ---
    val allLocations: Flow<List<LocationEntity>> = dao.getAllLocationsFlow()
    suspend fun getLocationsList(): List<LocationEntity> = dao.getAllLocations()
    suspend fun getLocationById(id: Long): LocationEntity? = dao.getLocationById(id)
    suspend fun getLocationByCode(code: String): LocationEntity? = dao.getLocationByCode(code)
    suspend fun insertLocation(shelf: String, level: String, box: String, notes: String = ""): Long {
        val code = "${shelf.trim().uppercase(Locale.ROOT)}-${level.trim()}-${box.trim().uppercase(Locale.ROOT)}"
        return dao.insertLocation(
            LocationEntity(
                shelf = shelf.trim().uppercase(Locale.ROOT),
                level = level.trim(),
                box = box.trim().uppercase(Locale.ROOT),
                code = code,
                notes = notes
            )
        )
    }
    suspend fun updateLocation(location: LocationEntity) = dao.updateLocation(location)
    suspend fun deleteLocation(location: LocationEntity) = dao.deleteLocation(location)

    // --- Parts ---
    val allParts: Flow<List<PartEntity>> = dao.getAllPartsFlow()
    fun getPartFlow(id: Long): Flow<PartEntity?> = dao.getPartFlow(id)
    suspend fun getPartById(id: Long): PartEntity? = dao.getPartById(id)
    suspend fun getPartsByLocation(locationId: Long): List<PartEntity> = dao.getPartsByLocation(locationId)
    suspend fun searchParts(query: String): List<PartEntity> = dao.searchParts(query)

    suspend fun insertPart(part: PartEntity): Long {
        val id = dao.insertPart(part)
        dao.insertPartEvent(
            PartEventEntity(
                partId = id,
                type = EventType.ADDED,
                note = "Part added to inventory (Initial count: ${part.quantity})",
                qtyDelta = part.quantity
            )
        )
        return id
    }

    suspend fun updatePart(part: PartEntity) {
        dao.updatePart(part.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun adjustPartQuantity(partId: Long, newQty: Int, note: String) {
        val part = dao.getPartById(partId) ?: return
        val delta = newQty - part.quantity
        dao.updatePart(part.copy(quantity = newQty, updatedAt = System.currentTimeMillis()))
        dao.insertPartEvent(
            PartEventEntity(
                partId = partId,
                type = EventType.ADJUSTED,
                note = if (note.isNotBlank()) note else "Adjusted stock from ${part.quantity} to $newQty",
                qtyDelta = delta
            )
        )
    }

    suspend fun movePart(partId: Long, newLocationId: Long) {
        val part = dao.getPartById(partId) ?: return
        val newLocation = dao.getLocationById(newLocationId)
        val oldLocation = part.locationId?.let { dao.getLocationById(it) }
        val oldCode = oldLocation?.code ?: "Unassigned"
        val newCode = newLocation?.code ?: "Unassigned"

        dao.updatePart(part.copy(locationId = newLocationId, updatedAt = System.currentTimeMillis()))
        dao.insertPartEvent(
            PartEventEntity(
                partId = partId,
                type = EventType.MOVED,
                note = "Moved from $oldCode to $newCode"
            )
        )
    }

    suspend fun markPartDamaged(partId: Long, reason: String, reduceQtyBy: Int = 0) {
        val part = dao.getPartById(partId) ?: return
        val updatedQty = (part.quantity - reduceQtyBy).coerceAtLeast(0)
        dao.updatePart(
            part.copy(
                condition = PartCondition.DAMAGED,
                quantity = updatedQty,
                remarks = if (part.remarks.isBlank()) "Damaged: $reason" else "${part.remarks} | Damaged: $reason",
                lastTestedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.insertPartEvent(
            PartEventEntity(
                partId = partId,
                type = EventType.DAMAGED,
                note = "Marked damaged: $reason" + if (reduceQtyBy > 0) " (Removed $reduceQtyBy units from stock)" else "",
                qtyDelta = -reduceQtyBy
            )
        )
    }

    suspend fun updateCondition(partId: Long, condition: PartCondition, remarks: String) {
        val part = dao.getPartById(partId) ?: return
        dao.updatePart(
            part.copy(
                condition = condition,
                remarks = remarks,
                lastTestedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.insertPartEvent(
            PartEventEntity(
                partId = partId,
                type = EventType.TESTED,
                note = "Tested: Condition set to ${condition.displayName}. $remarks"
            )
        )
    }

    suspend fun deletePart(part: PartEntity) = dao.deletePart(part)

    // --- Part Events ---
    fun getEventsForPart(partId: Long): Flow<List<PartEventEntity>> = dao.getEventsForPartFlow(partId)

    // --- Projects ---
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjectsFlow()
    suspend fun getAllProjects(): List<ProjectEntity> = dao.getAllProjects()
    fun getProjectFlow(id: Long): Flow<ProjectEntity?> = dao.getProjectFlow(id)
    suspend fun getProjectById(id: Long): ProjectEntity? = dao.getProjectById(id)
    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)

    // --- Project Parts & Calculations ---
    fun getProjectPartsFlow(projectId: Long): Flow<List<ProjectPartEntity>> = dao.getProjectPartsFlow(projectId)
    suspend fun getProjectParts(projectId: Long): List<ProjectPartEntity> = dao.getProjectParts(projectId)
    suspend fun getProjectPartsForPart(partId: Long): List<ProjectPartEntity> = dao.getProjectPartsForPart(partId)

    suspend fun addProjectPart(projectPart: ProjectPartEntity): Long {
        return dao.insertProjectPart(projectPart)
    }

    suspend fun updateProjectPart(projectPart: ProjectPartEntity) {
        dao.updateProjectPart(projectPart)
    }

    suspend fun deleteProjectPart(projectPart: ProjectPartEntity) {
        dao.deleteProjectPart(projectPart)
    }

    suspend fun updateChecklistState(projectPartId: Long, state: ChecklistState) {
        val allParts = dao.getAllProjectParts()
        val item = allParts.find { it.id == projectPartId } ?: return
        dao.updateProjectPart(item.copy(checklistState = state))
    }

    suspend fun reserveStockForProject(projectPartId: Long, qtyToReserve: Int) {
        val allProjectParts = dao.getAllProjectParts()
        val item = allProjectParts.find { it.id == projectPartId } ?: return
        dao.updateProjectPart(item.copy(reservedQty = qtyToReserve))

        item.partId?.let { partId ->
            dao.insertPartEvent(
                PartEventEntity(
                    partId = partId,
                    type = EventType.USED,
                    note = "Reserved $qtyToReserve units for project #${item.projectId}",
                    projectId = item.projectId
                )
            )
        }
    }

    suspend fun finishProject(projectId: Long) {
        val project = dao.getProjectById(projectId) ?: return
        val parts = dao.getProjectParts(projectId)

        // Return reserved stock
        parts.forEach { pp ->
            if (pp.reservedQty > 0 && pp.partId != null) {
                dao.insertPartEvent(
                    PartEventEntity(
                        partId = pp.partId,
                        type = EventType.RETURNED,
                        note = "Returned ${pp.reservedQty} reserved units from completed project '${project.name}'",
                        projectId = projectId
                    )
                )
                dao.updateProjectPart(pp.copy(reservedQty = 0))
            }
        }

        dao.updateProject(
            project.copy(
                status = ProjectStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Reservation & Status Computation ---
    suspend fun computeTotalReservedForPart(partId: Long, excludingProjectPartId: Long? = null): Int {
        val allProjectParts = dao.getAllProjectParts()
        val allProjects = dao.getAllProjects().associateBy { it.id }

        return allProjectParts
            .filter { it.partId == partId && it.id != excludingProjectPartId }
            .filter { allProjects[it.projectId]?.status == ProjectStatus.ACTIVE }
            .sumOf { it.reservedQty }
    }

    suspend fun computePartFreeStock(partId: Long, excludingProjectPartId: Long? = null): Int {
        val part = dao.getPartById(partId) ?: return 0
        val reserved = computeTotalReservedForPart(partId, excludingProjectPartId)
        return (part.quantity - reserved).coerceAtLeast(0)
    }

    suspend fun computeProjectPartStatus(projectPart: ProjectPartEntity): ComputedPartStatus {
        val cartItems = dao.getAllCartItems()
        val inCart = cartItems.any { it.name.equals(projectPart.name, ignoreCase = true) }

        if (projectPart.partId == null) {
            return if (inCart) ComputedPartStatus.IN_CART else ComputedPartStatus.NEED
        }

        val freeStock = computePartFreeStock(projectPart.partId, excludingProjectPartId = projectPart.id)
        return when {
            freeStock >= projectPart.qtyNeeded -> ComputedPartStatus.HAVE
            freeStock > 0 -> ComputedPartStatus.NOT_ENOUGH
            inCart -> ComputedPartStatus.IN_CART
            else -> ComputedPartStatus.NEED
        }
    }

    // --- Cart Items ---
    val allCartItems: Flow<List<CartItemEntity>> = dao.getAllCartItemsFlow()
    suspend fun addCartItem(name: String, qty: Int, unitPrice: Double, projectId: Long? = null, shop: String = ""): Long {
        return dao.insertCartItem(
            CartItemEntity(
                name = name,
                qty = qty,
                unitPrice = unitPrice,
                projectId = projectId,
                shop = shop
            )
        )
    }

    suspend fun markCartItemBought(item: CartItemEntity, targetLocationId: Long) {
        // Check if matching part exists
        val existingParts = dao.getAllParts()
        val matchingPart = existingParts.find { it.name.equals(item.name, ignoreCase = true) }

        if (matchingPart != null) {
            // Update stock
            adjustPartQuantity(matchingPart.id, matchingPart.quantity + item.qty, "Purchased from ${item.shop}")
        } else {
            // Create new part
            insertPart(
                PartEntity(
                    name = item.name,
                    group = PartGroup.OTHER,
                    specs = "Purchased from ${item.shop}",
                    quantity = item.qty,
                    locationId = targetLocationId,
                    condition = PartCondition.WORKING,
                    remarks = "Bought item"
                )
            )
        }
        dao.deleteCartItem(item)
    }

    suspend fun deleteCartItem(item: CartItemEntity) = dao.deleteCartItem(item)

    // --- AI Action Log ---
    val allAiLogs: Flow<List<AiActionLogEntity>> = dao.getAllAiLogsFlow()
    suspend fun logAiAction(actionType: String, summary: String, details: String) {
        dao.insertAiLog(
            AiActionLogEntity(
                actionType = actionType,
                summary = summary,
                details = details
            )
        )
    }

    // --- Duplicate Detection ---
    suspend fun findPotentialDuplicates(name: String): List<PartEntity> {
        val normalized = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val all = dao.getAllParts()

        val synonyms = mapOf(
            "hc-sr04" to listOf("ultrasonic", "distance sensor", "sonar"),
            "ultrasonic" to listOf("hc-sr04", "sonar"),
            "mpu6050" to listOf("gyro", "accelerometer", "imu", "6dof"),
            "dht22" to listOf("am2302", "temperature", "humidity"),
            "dht11" to listOf("temperature", "humidity"),
            "esp32" to listOf("esp-32", "devkit", "nodemcu-32"),
            "l298n" to listOf("h-bridge", "motor driver", "dual h bridge"),
            "oled" to listOf("ssd1306", "i2c display", "0.96")
        )

        val potentialSynonyms = synonyms.entries
            .filter { normalized.contains(it.key.replace(Regex("[^a-z0-9]"), "")) }
            .flatMap { it.value }

        return all.filter { existing ->
            val existingNorm = existing.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            existingNorm == normalized ||
                    existingNorm.contains(normalized) ||
                    normalized.contains(existingNorm) ||
                    potentialSynonyms.any { existingNorm.contains(it.replace(Regex("[^a-z0-9]"), "")) }
        }
    }

    // --- CSV Export ---
    suspend fun exportInventoryCsv(): String {
        val parts = dao.getAllParts()
        val locations = dao.getAllLocations().associateBy { it.id }
        val sb = StringBuilder()
        sb.append("ID,Name,Group,Specs,Total Qty,Free Stock,Location Code,Condition,Last Tested,Remarks,Datasheet URL\n")

        for (p in parts) {
            val free = computePartFreeStock(p.id)
            val locCode = p.locationId?.let { locations[it]?.code } ?: "Unassigned"
            val lastTested = p.lastTestedAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(it)) } ?: "Never"

            sb.append("${p.id},\"${p.name.replace("\"", "\"\"")}\",\"${p.group.displayName}\",\"${p.specs.replace("\"", "\"\"")}\",${p.quantity},$free,\"$locCode\",\"${p.condition.displayName}\",\"$lastTested\",\"${p.remarks.replace("\"", "\"\"")}\",\"${p.datasheetUrl}\"\n")
        }
        return sb.toString()
    }
}

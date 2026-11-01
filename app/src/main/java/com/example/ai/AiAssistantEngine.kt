package com.example.ai

import com.example.data.local.SeedData
import com.example.data.model.*
import com.example.data.repository.PartPilotRepository
import java.util.Locale

data class ProposedWriteAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String, // ADD_PART, MOVE_PART, ADJUST_QTY, RESERVE_STOCK
    val title: String,
    val beforeState: String,
    val afterState: String,
    val partId: Long? = null,
    val targetLocationId: Long? = null,
    val newQuantity: Int? = null,
    val projectId: Long? = null,
    val partName: String? = null,
    val group: PartGroup? = null,
    val specs: String? = null
)

data class ParsedPartCandidate(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var group: PartGroup,
    var specs: String,
    var quantity: Int,
    var confidence: Float,
    var suggestedLocationId: Long? = null,
    var suggestedLocationCode: String = "A-1-B1",
    var potentialDuplicates: List<PartEntity> = emptyList()
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedAction: ProposedWriteAction? = null,
    val actionExecuted: Boolean = false
)

class AiAssistantEngine(private val repository: PartPilotRepository) {

    // --- Grounded AI Query Executor ---
    suspend fun processQuery(query: String): AiChatMessage {
        val q = query.trim().lowercase(Locale.ROOT)
        val parts = repository.searchParts("")
        val locations = repository.getLocationsList().associateBy { it.id }
        val projects = repository.allProjectsFlowOrList()

        // Check for Write Intent: "add 5 resistors...", "move esp32...", "adjust quantity..."
        val writeAction = detectWriteAction(query, parts, locations, projects)
        if (writeAction != null) {
            val responseText = "I've drafted the requested change based on your workshop inventory. Please review the before/after details below and tap **Confirm Action** to apply it."
            return AiChatMessage(
                isUser = false,
                text = responseText,
                proposedAction = writeAction
            )
        }

        // Tool 1: Where is X? / Do I have X? / search_parts
        if (q.contains("where") || q.contains("do i have") || q.contains("have i got") || q.contains("find") || q.contains("how many")) {
            // Find target part
            val matchedPart = parts.find { p ->
                val nameTokens = p.name.lowercase(Locale.ROOT).split(" ", "-", "/")
                val queryTokens = q.split(" ", "?", "!", ",", ".")
                nameTokens.any { token -> token.length > 2 && queryTokens.contains(token) } ||
                        q.contains(p.name.lowercase(Locale.ROOT))
            } ?: parts.minByOrNull { p ->
                levenshteinDistance(p.name.lowercase(Locale.ROOT), q)
            }

            if (matchedPart != null && isRelevantMatch(matchedPart.name, q)) {
                val locCode = matchedPart.locationId?.let { locations[it]?.code } ?: "Unassigned"
                val freeStock = repository.computePartFreeStock(matchedPart.id)
                val reserved = matchedPart.quantity - freeStock

                val text = buildString {
                    append("Yes, you have **${matchedPart.name}**.\n\n")
                    append("• **Location**: Box `$locCode`\n")
                    append("• **Total Stock**: ${matchedPart.quantity} units\n")
                    append("• **Free to use**: $freeStock units")
                    if (reserved > 0) {
                        append(" ($reserved reserved in active projects)")
                    }
                    append("\n• **Condition**: ${matchedPart.condition.displayName}\n")
                    append("• **Specs**: ${matchedPart.specs}\n")
                    if (matchedPart.remarks.isNotBlank()) {
                        append("• **Remarks**: ${matchedPart.remarks}\n")
                    }
                }
                return AiChatMessage(isUser = false, text = text)
            } else {
                // If not found, suggest closest alternative
                val closest = parts.filter { it.quantity > 0 }.take(3)
                val text = buildString {
                    append("You do not currently have that exact part in stock.\n\n")
                    append("**Closest alternatives in your inventory:**\n")
                    closest.forEach { alt ->
                        val loc = alt.locationId?.let { locations[it]?.code } ?: "Unassigned"
                        append("• **${alt.name}** (${alt.group.displayName}) - Qty: ${alt.quantity} in `$loc`\n")
                    }
                }
                return AiChatMessage(isUser = false, text = text)
            }
        }

        // Tool 2: What is in box X? / get_location_contents
        val boxRegex = Regex("""([a-z])-([0-9])-([a-z0-9]+)""", RegexOption.IGNORE_CASE)
        val boxMatch = boxRegex.find(q)
        if (boxMatch != null || q.contains("box") || q.contains("shelf")) {
            val targetCode = boxMatch?.value?.uppercase(Locale.ROOT)
            val location = if (targetCode != null) {
                locations.values.find { it.code.equals(targetCode, ignoreCase = true) }
            } else {
                locations.values.find { q.contains(it.code.lowercase(Locale.ROOT)) || q.contains(it.box.lowercase(Locale.ROOT)) }
            }

            if (location != null) {
                val boxParts = repository.getPartsByLocation(location.id)
                val text = buildString {
                    append("Contents for Box **`${location.code}`** (Shelf ${location.shelf}, Level ${location.level}, Box ${location.box}):\n\n")
                    if (location.notes.isNotBlank()) {
                        append("*${location.notes}*\n\n")
                    }
                    if (boxParts.isEmpty()) {
                        append("This box is currently empty.")
                    } else {
                        boxParts.forEach { p ->
                            val free = repository.computePartFreeStock(p.id)
                            append("• **${p.name}** — **${p.quantity}** in stock (${free} free), ${p.condition.displayName}\n")
                            append("  *${p.specs}*\n")
                        }
                    }
                }
                return AiChatMessage(isUser = false, text = text)
            }
        }

        // Tool 3: What do I still need for X? / get_project_bom
        val matchedProject = projects.find { proj ->
            val projTokens = proj.name.lowercase(Locale.ROOT).split(" ")
            projTokens.any { t -> t.length > 2 && q.contains(t) } || q.contains(proj.name.lowercase(Locale.ROOT))
        }

        if (matchedProject != null) {
            val bom = repository.getProjectParts(matchedProject.id)
            val text = buildString {
                append("BOM Status for **${matchedProject.name}**:\n\n")
                var neededCount = 0
                var readyCount = 0

                bom.forEach { item ->
                    val status = repository.computeProjectPartStatus(item)
                    val statusText = when (status) {
                        ComputedPartStatus.HAVE -> "✅ HAVE (${item.qtyNeeded} owned & available)"
                        ComputedPartStatus.NOT_ENOUGH -> "⚠️ NOT ENOUGH (Need ${item.qtyNeeded})"
                        ComputedPartStatus.IN_CART -> "🛒 IN CART"
                        ComputedPartStatus.NEED -> "❌ NEED TO BUY (${item.qtyNeeded} units)"
                    }
                    if (status == ComputedPartStatus.HAVE) readyCount++ else neededCount++
                    append("• **${item.name}**: $statusText [Checklist: ${item.checklistState.displayName}]\n")
                }

                append("\n**Summary:** $readyCount parts ready, $neededCount missing/in cart.\n")
                append("Budget Cap: $${String.format(Locale.ROOT, "%.2f", matchedProject.budgetCap)}")
            }
            return AiChatMessage(isUser = false, text = text)
        }

        // Tool 4: List projects
        if (q.contains("project") || q.contains("building") || q.contains("active")) {
            val text = buildString {
                append("You have **${projects.size}** projects on record:\n\n")
                projects.forEach { pr ->
                    append("• **${pr.name}** [${pr.status.displayName}]\n")
                    append("  ${pr.description}\n")
                    append("  Budget Cap: $${pr.budgetCap}\n\n")
                }
            }
            return AiChatMessage(isUser = false, text = text)
        }

        // General grounded response
        val totalParts = parts.sumOf { it.quantity }
        val lowStock = parts.filter { it.quantity <= it.minStock }
        val text = buildString {
            append("I'm grounded in your PartPilot inventory database ($totalParts total components across ${locations.size} storage bins).\n\n")
            append("You can ask me questions like:\n")
            append("• *\"Do I have an L298N?\"*\n")
            append("• *\"Where is my MPU6050?\"*\n")
            append("• *\"What is in box A-2-B3?\"*\n")
            append("• *\"What do I still need for Line Follower v2?\"*\n")
            if (lowStock.isNotEmpty()) {
                append("\n⚠️ **Notice**: You have ${lowStock.size} parts at or below minimum stock limit.")
            }
        }
        return AiChatMessage(isUser = false, text = text)
    }

    private suspend fun PartPilotRepository.allProjectsFlowOrList(): List<ProjectEntity> {
        val list = repository.getAllProjects()
        return if (list.isNotEmpty()) list else SeedData.projects
    }

    private fun isRelevantMatch(name: String, query: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        val n = name.lowercase(Locale.ROOT)
        val nameWords = n.split(" ", "-", "_", "/")
        return nameWords.any { it.length > 2 && q.contains(it) } || q.contains(n)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[m][n]
    }

    // --- Detect Write Action and create Diff Card ---
    private suspend fun detectWriteAction(
        query: String,
        parts: List<PartEntity>,
        locations: Map<Long, LocationEntity>,
        projects: List<ProjectEntity>
    ): ProposedWriteAction? {
        val q = query.lowercase(Locale.ROOT)

        // Action: Move part X to location Y
        if (q.contains("move") && (q.contains("box") || q.contains("shelf") || q.contains("to "))) {
            val targetPart = parts.find { isRelevantMatch(it.name, q) }
            val targetLoc = locations.values.find { q.contains(it.code.lowercase(Locale.ROOT)) || q.contains(it.box.lowercase(Locale.ROOT)) }

            if (targetPart != null && targetLoc != null) {
                val oldLocCode = targetPart.locationId?.let { locations[it]?.code } ?: "Unassigned"
                return ProposedWriteAction(
                    actionType = "MOVE_PART",
                    title = "Move Component",
                    beforeState = "Part: ${targetPart.name}\nLocation: $oldLocCode",
                    afterState = "Part: ${targetPart.name}\nLocation: ${targetLoc.code} (${targetLoc.notes})",
                    partId = targetPart.id,
                    targetLocationId = targetLoc.id
                )
            }
        }

        // Action: Adjust quantity
        if (q.contains("adjust") || q.contains("set count") || (q.contains("stock") && (q.contains("to ") || q.contains("is ")))) {
            val numMatch = Regex("""\b(\d+)\b""").find(q)
            val newQty = numMatch?.groupValues?.get(1)?.toIntOrNull()
            val targetPart = parts.find { isRelevantMatch(it.name, q) }

            if (targetPart != null && newQty != null) {
                return ProposedWriteAction(
                    actionType = "ADJUST_QTY",
                    title = "Adjust Inventory Count",
                    beforeState = "${targetPart.name}: ${targetPart.quantity} units",
                    afterState = "${targetPart.name}: $newQty units",
                    partId = targetPart.id,
                    newQuantity = newQty
                )
            }
        }

        // Action: Add new part
        if (q.startsWith("add") || q.contains("put into stock") || q.contains("register part")) {
            val numMatch = Regex("""\b(\d+)\b""").find(q)
            val qty = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val targetLoc = locations.values.find { q.contains(it.code.lowercase(Locale.ROOT)) || q.contains(it.box.lowercase(Locale.ROOT)) }
                ?: locations.values.firstOrNull()

            // Extract item name
            var rawName = q.replace(Regex("""^(add|please add|register)\s+""", RegexOption.IGNORE_CASE), "")
            rawName = rawName.replace(Regex("""\b(\d+)\s*units?\b|\b(\d+)\b""", RegexOption.IGNORE_CASE), "")
            rawName = rawName.replace(Regex("""(to\s+shelf.*|to\s+box.*|in\s+box.*)""", RegexOption.IGNORE_CASE), "").trim()

            val cleanedName = if (rawName.isBlank()) "New Component" else rawName.replaceFirstChar { it.uppercase() }
            val group = guessGroupFromName(cleanedName)

            return ProposedWriteAction(
                actionType = "ADD_PART",
                title = "Add New Component to Stock",
                beforeState = "Part not yet cataloged",
                afterState = "Name: $cleanedName\nQty: $qty\nGroup: ${group.displayName}\nLocation: ${targetLoc?.code ?: "A-1-B1"}",
                partName = cleanedName,
                group = group,
                newQuantity = qty,
                targetLocationId = targetLoc?.id ?: 1L,
                specs = "Added via natural language command"
            )
        }

        return null
    }

    // --- Parser for Voice / Text Input into Candidate Cards ---
    suspend fun parseVoiceOrTextPrompt(text: String): List<ParsedPartCandidate> {
        val q = text.trim()
        val numMatch = Regex("""\b(\d+)\b""").find(q)
        val qty = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

        val locations = repository.getLocationsList()
        val matchedLoc = locations.find {
            q.contains(it.code, ignoreCase = true) ||
                    (q.contains(it.shelf, ignoreCase = true) && q.contains(it.box, ignoreCase = true))
        } ?: locations.firstOrNull()

        // Clean part name
        var cleanName = q.replace(Regex("""^(add|please add|register)\s+""", RegexOption.IGNORE_CASE), "")
        cleanName = cleanName.replace(Regex("""\b(\d+)\s*(units?|pcs?|pieces?|x)?\b""", RegexOption.IGNORE_CASE), "")
        cleanName = cleanName.replace(Regex("""(to\s+shelf.*|to\s+box.*|in\s+box.*|into\s+.*)""", RegexOption.IGNORE_CASE), "").trim()

        if (cleanName.isBlank()) cleanName = "Electronic Component"
        cleanName = cleanName.replaceFirstChar { it.uppercase() }

        val group = guessGroupFromName(cleanName)
        val specs = guessSpecsFromName(cleanName)
        val confidence = if (cleanName.length > 3 && group != PartGroup.OTHER) 0.88f else 0.55f

        val duplicates = repository.findPotentialDuplicates(cleanName)

        return listOf(
            ParsedPartCandidate(
                name = cleanName,
                group = group,
                specs = specs,
                quantity = qty,
                confidence = confidence,
                suggestedLocationId = matchedLoc?.id,
                suggestedLocationCode = matchedLoc?.code ?: "A-1-B1",
                potentialDuplicates = duplicates
            )
        )
    }

    // --- Parser for Workbench / Photo Capture into Multiple Candidates ---
    suspend fun parsePhotoBatch(photoDescriptionOrKeywords: String = ""): List<ParsedPartCandidate> {
        val locations = repository.getLocationsList()
        val locMap = locations.associateBy { it.id }

        // Realistic components detected on workbench table
        val detected = listOf(
            ParsedPartCandidate(
                name = "ESP32-CAM AI-Thinker Module",
                group = PartGroup.MICROCONTROLLER,
                specs = "WiFi+BT SoC with OV2640 2MP Camera, MicroSD slot",
                quantity = 1,
                confidence = 0.94f,
                suggestedLocationId = 1,
                suggestedLocationCode = locMap[1]?.code ?: "A-1-B1"
            ),
            ParsedPartCandidate(
                name = "SG90 Micro Servo 9g",
                group = PartGroup.MECHANICAL,
                specs = "4.8V-6V nylon gear rotary actuator",
                quantity = 2,
                confidence = 0.91f,
                suggestedLocationId = 3,
                suggestedLocationCode = locMap[3]?.code ?: "B-1-B2"
            ),
            ParsedPartCandidate(
                name = "Unknown 8-pin DIP IC",
                group = PartGroup.SEMICONDUCTOR,
                specs = "Marking partially obscured: NE555 / OpAmp?",
                quantity = 5,
                confidence = 0.52f, // < 0.7 triggers amber prompt card!
                suggestedLocationId = 1,
                suggestedLocationCode = locMap[1]?.code ?: "A-1-B1"
            )
        )

        // Enrich with potential duplicates
        return detected.map { candidate ->
            val duplicates = repository.findPotentialDuplicates(candidate.name)
            candidate.copy(potentialDuplicates = duplicates)
        }
    }

    private fun guessGroupFromName(name: String): PartGroup {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("esp32") || lower.contains("arduino") || lower.contains("pico") || lower.contains("stm32") || lower.contains("atmega") || lower.contains("microcontroller") -> PartGroup.MICROCONTROLLER
            lower.contains("sensor") || lower.contains("hc-sr04") || lower.contains("mpu") || lower.contains("dht") || lower.contains("tcrt") || lower.contains("gyro") || lower.contains("accel") -> PartGroup.SENSOR
            lower.contains("driver") || lower.contains("l298") || lower.contains("a4988") || lower.contains("tb6612") || lower.contains("h-bridge") -> PartGroup.MOTOR_DRIVER
            lower.contains("battery") || lower.contains("18650") || lower.contains("lipo") || lower.contains("power") || lower.contains("buck") || lower.contains("boost") || lower.contains("supply") -> PartGroup.POWER_BATTERY
            lower.contains("oled") || lower.contains("lcd") || lower.contains("display") || lower.contains("tft") || lower.contains("screen") -> PartGroup.DISPLAY
            lower.contains("nrf") || lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("lora") || lower.contains("rf") -> PartGroup.COMM_MODULE
            lower.contains("resistor") || lower.contains("capacitor") || lower.contains("potentiometer") || lower.contains("inductor") -> PartGroup.PASSIVE
            lower.contains("transistor") || lower.contains("diode") || lower.contains("mosfet") || lower.contains("bjt") || lower.contains("ic") -> PartGroup.SEMICONDUCTOR
            lower.contains("wire") || lower.contains("jumper") || lower.contains("header") || lower.contains("cable") || lower.contains("connector") -> PartGroup.CONNECTOR_WIRE
            lower.contains("iron") || lower.contains("solder") || lower.contains("flux") || lower.contains("tweezers") || lower.contains("multimeter") -> PartGroup.TOOL_CONSUMABLE
            lower.contains("servo") || lower.contains("motor") || lower.contains("gear") || lower.contains("chassis") || lower.contains("wheel") -> PartGroup.MECHANICAL
            else -> PartGroup.OTHER
        }
    }

    private fun guessSpecsFromName(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("resistor") -> "Standard through-hole 5% tolerance"
            lower.contains("servo") -> "4.8V-6V operating range, 180° rotation"
            lower.contains("esp32") -> "Dual-Core 240MHz, WiFi+Bluetooth LE"
            lower.contains("sensor") -> "Digital/Analog output 3.3V-5V"
            else -> "Cataloged item for workshop inventory"
        }
    }
}

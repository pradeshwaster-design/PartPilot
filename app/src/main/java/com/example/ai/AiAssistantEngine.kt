package com.example.ai

import android.graphics.Bitmap
import com.example.data.local.SeedData
import com.example.data.model.*
import com.example.data.repository.PartPilotRepository
import org.json.JSONArray
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
    var potentialDuplicates: List<PartEntity> = emptyList(),
    var imageBitmap: Bitmap? = null
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
        val q = query.trim()
        val qLower = q.lowercase(Locale.ROOT)
        val parts = repository.searchParts("")
        val locations = repository.getLocationsList().associateBy { it.id }
        val projects = repository.getAllProjects().ifEmpty { SeedData.projects }

        // 1. Detect Write Actions (Propose diff card with Confirm button)
        val writeAction = detectWriteAction(q, parts, locations, projects)
        if (writeAction != null) {
            val responseText = "I've drafted the requested inventory change based on your workshop stock. Please review the before/after details below and tap **Confirm & Execute Action** to apply it."
            return AiChatMessage(
                isUser = false,
                text = responseText,
                proposedAction = writeAction
            )
        }

        // 2. Try Gemini API first if configured
        if (GeminiClient.hasValidApiKey()) {
            val inventoryContext = buildInventorySummary(parts, locations, projects)
            val geminiResponse = GeminiClient.generateGroundedResponse(q, inventoryContext)
            if (!geminiResponse.isNullOrBlank()) {
                return AiChatMessage(isUser = false, text = geminiResponse)
            }
        }

        // 3. Intelligent Grounded Local Engine
        val localResponse = generateLocalGroundedResponse(q, qLower, parts, locations, projects)
        return AiChatMessage(isUser = false, text = localResponse)
    }

    private fun buildInventorySummary(
        parts: List<PartEntity>,
        locations: Map<Long, LocationEntity>,
        projects: List<ProjectEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("Current Workshop Inventory (${parts.size} component types, ${parts.sumOf { it.quantity }} total items):\n")
        parts.forEach { p ->
            val loc = p.locationId?.let { locations[it]?.code } ?: "Unassigned"
            sb.append("- ${p.name} (${p.group.displayName}): ${p.quantity} in stock, Location: $loc, Condition: ${p.condition.displayName}, Specs: ${p.specs}\n")
        }
        sb.append("\nActive Projects:\n")
        projects.forEach { pr ->
            sb.append("- ${pr.name} [${pr.status.displayName}], Budget: $${pr.budgetCap}, Description: ${pr.description}\n")
        }
        return sb.toString()
    }

    private suspend fun generateLocalGroundedResponse(
        query: String,
        q: String,
        parts: List<PartEntity>,
        locations: Map<Long, LocationEntity>,
        projects: List<ProjectEntity>
    ): String {
        // Query A: "Where is X?" / "Do I have X?"
        val targetPart = findPartInQuery(q, parts)
        if (targetPart != null && (q.contains("where") || q.contains("have") || q.contains("find") || q.contains("stock") || q.contains("got") || q.contains("is there"))) {
            val locCode = targetPart.locationId?.let { locations[it]?.code } ?: "Unassigned"
            val locDesc = targetPart.locationId?.let { locations[it]?.notes } ?: ""
            val freeStock = repository.computePartFreeStock(targetPart.id)
            val reserved = targetPart.quantity - freeStock

            return buildString {
                append("### ${targetPart.name}\n\n")
                append("• **Location**: Box `$locCode`" + if (locDesc.isNotBlank()) " ($locDesc)" else "" + "\n")
                append("• **Total Stock**: **${targetPart.quantity}** units\n")
                append("• **Free to use**: **$freeStock** units")
                if (reserved > 0) {
                    append(" (*$reserved units reserved in active projects*)")
                }
                append("\n• **Condition**: ${targetPart.condition.displayName}\n")
                append("• **Group**: ${targetPart.group.displayName}\n")
                append("• **Specs**: ${targetPart.specs}\n")
                if (targetPart.remarks.isNotBlank()) {
                    append("• **Remarks**: ${targetPart.remarks}\n")
                }
            }
        }

        // Query B: "What is in box X?"
        val boxRegex = Regex("""([a-z])-([0-9])-([a-z0-9]+)""", RegexOption.IGNORE_CASE)
        val boxMatch = boxRegex.find(q)
        if (boxMatch != null || (q.contains("box") && (q.contains("what") || q.contains("contents") || q.contains("show") || q.contains("list")))) {
            val targetCode = boxMatch?.value?.uppercase(Locale.ROOT)
            val location = if (targetCode != null) {
                locations.values.find { it.code.equals(targetCode, ignoreCase = true) }
            } else {
                locations.values.find { q.contains(it.code.lowercase(Locale.ROOT)) || q.contains(it.box.lowercase(Locale.ROOT)) }
            }

            if (location != null) {
                val boxParts = repository.getPartsByLocation(location.id)
                return buildString {
                    append("### Storage Bin **`${location.code}`**\n")
                    append("Shelf **${location.shelf}**, Level **${location.level}**, Box **${location.box}**\n")
                    if (location.notes.isNotBlank()) {
                        append("*Category: ${location.notes}*\n\n")
                    } else {
                        append("\n")
                    }
                    if (boxParts.isEmpty()) {
                        append("This storage box is currently empty.")
                    } else {
                        append("**Contents (${boxParts.size} parts):**\n")
                        boxParts.forEach { p ->
                            val free = repository.computePartFreeStock(p.id)
                            append("• **${p.name}** — **${p.quantity}x** in stock (${free} available), ${p.condition.displayName}\n")
                            append("  *${p.specs}*\n")
                        }
                    }
                }
            }
        }

        // Query C: "What do I need for <project>?"
        val matchedProject = projects.find { proj ->
            val pName = proj.name.lowercase(Locale.ROOT)
            q.contains(pName) || pName.split(" ").filter { it.length > 3 }.any { q.contains(it) }
        }
        if (matchedProject != null && (q.contains("need") || q.contains("bom") || q.contains("parts") || q.contains("missing") || q.contains("status"))) {
            val bom = repository.getProjectParts(matchedProject.id)
            return buildString {
                append("### BOM for ${matchedProject.name}\n")
                append("*Status: ${matchedProject.status.displayName} | Budget: $${matchedProject.budgetCap}*\n\n")
                var readyCount = 0
                var missingCount = 0

                bom.forEach { item ->
                    val status = repository.computeProjectPartStatus(item)
                    val badge = when (status) {
                        ComputedPartStatus.HAVE -> "✅ **HAVE** (${item.qtyNeeded} available)"
                        ComputedPartStatus.NOT_ENOUGH -> "⚠️ **NOT ENOUGH** (Need ${item.qtyNeeded})"
                        ComputedPartStatus.IN_CART -> "🛒 **IN CART**"
                        ComputedPartStatus.NEED -> "❌ **NEED TO BUY** (${item.qtyNeeded} pcs)"
                    }
                    if (status == ComputedPartStatus.HAVE) readyCount++ else missingCount++
                    append("• **${item.name}**: $badge\n")
                    append("  Checklist state: *${item.checklistState.displayName}*\n")
                }

                append("\n**Summary:** $readyCount components in stock, $missingCount missing/in cart.")
            }
        }

        // Query D: "Can I build X?" / Project Feasibility
        if (q.contains("can i build") || q.contains("what can i build") || q.contains("build a")) {
            val devBoards = parts.filter { it.group == PartGroup.MICROCONTROLLER && it.quantity > 0 }
            val motorDrivers = parts.filter { it.group == PartGroup.MOTOR_DRIVER && it.quantity > 0 }
            val sensors = parts.filter { it.group == PartGroup.SENSOR && it.quantity > 0 }
            val batteries = parts.filter { it.group == PartGroup.POWER_BATTERY && it.quantity > 0 }

            return buildString {
                append("### Feasibility Assessment based on your Stock\n\n")
                append("Here is what you currently have ready on hand:\n")
                append("• **Microcontrollers:** ${devBoards.joinToString { it.name }}\n")
                append("• **Motor Drivers:** ${if (motorDrivers.isNotEmpty()) motorDrivers.joinToString { it.name } else "None available"}\n")
                append("• **Sensors:** ${sensors.joinToString { it.name }}\n")
                append("• **Power:** ${batteries.joinToString { it.name }}\n\n")

                if (q.contains("line follower") || q.contains("robot") || q.contains("rover")) {
                    append("**Line Follower / Rover:** You have the Arduino Nano, TCRT5000 IR sensor array, L298N driver, TT motors, and 18650 batteries! You only need a 2WD robot chassis to complete it.")
                } else if (q.contains("weather") || q.contains("iot")) {
                    append("**IoT Weather Station:** You have the ESP32 (WiFi+BLE), DHT22 temperature sensor, and I2C OLED display ready in Box A-2-B3!")
                } else {
                    append("You can readily build:\n1. **Line Follower Robot** (Arduino Nano + TCRT5000 + L298N + TT Motors)\n2. **IoT Weather Monitor** (ESP32 + DHT22 + 0.96\" OLED)\n3. **Obstacle Avoiding Bot** (HC-SR04 + Servo SG90 + L298N)")
                }
            }
        }

        // Query E: Component Specs & Pinouts: "What is X?", "Pinout", "Voltage"
        if (targetPart != null) {
            val loc = targetPart.locationId?.let { locations[it]?.code } ?: "Unassigned"
            return buildString {
                append("### ${targetPart.name} Overview\n\n")
                append("• **Category:** ${targetPart.group.displayName}\n")
                append("• **Specifications:** ${targetPart.specs}\n")
                append("• **Quantity in Stock:** ${targetPart.quantity} units (Location `$loc`)\n")
                append("• **Condition:** ${targetPart.condition.displayName}\n")
                if (targetPart.datasheetUrl.isNotBlank()) {
                    append("• **Datasheet URL:** ${targetPart.datasheetUrl}\n")
                }
                if (targetPart.remarks.isNotBlank()) {
                    append("• **Notes:** ${targetPart.remarks}\n")
                }
                append("\n*Tip: Ask me to 'move ${targetPart.name} to box ...' or 'adjust quantity' to update records.*")
            }
        }

        // Query F: Counts & Statistics
        if (q.contains("how many") || q.contains("total") || q.contains("count") || q.contains("summary")) {
            val totalQty = parts.sumOf { it.quantity }
            val groupCounts = parts.groupBy { it.group }.mapValues { it.value.sumOf { p -> p.quantity } }
            return buildString {
                append("### Workshop Inventory Summary\n\n")
                append("• **Total Components:** **$totalQty** physical items across **${parts.size}** distinct types\n")
                append("• **Storage Bins:** **${locations.size}** labeled boxes\n")
                append("• **Active Projects:** **${projects.count { it.status == ProjectStatus.ACTIVE }}**\n\n")
                append("**Breakdown by Category:**\n")
                groupCounts.entries.sortedByDescending { it.value }.forEach { (grp, count) ->
                    append("• ${grp.displayName}: **$count** units\n")
                }
            }
        }

        // Query G: Dynamic fallback based on user query
        return buildString {
            append("I checked your workshop inventory for: *\"$query\"*.\n\n")
            val matching = parts.filter { p ->
                val words = q.split(" ", "?", "!", ",", ".")
                words.any { w -> w.length > 3 && p.name.contains(w, ignoreCase = true) || p.specs.contains(w, ignoreCase = true) }
            }
            if (matching.isNotEmpty()) {
                append("**Related parts in stock:**\n")
                matching.take(4).forEach { m ->
                    val loc = m.locationId?.let { locations[it]?.code } ?: "Unassigned"
                    append("• **${m.name}** (${m.quantity} in `$loc`) — *${m.specs}*\n")
                }
            } else {
                append("I couldn't find a direct match in your storage bins. You currently have **${parts.size}** cataloged component types in stock.\n\n")
                append("You can ask me:\n")
                append("• *\"Do I have an L298N?\"*\n")
                append("• *\"Where is my MPU6050?\"*\n")
                append("• *\"What is in box A-2-B3?\"*\n")
                append("• *\"Add 5 resistors 220 ohm to Box A-1-B1\"*\n")
                append("• *\"What do I still need for Line Follower v2?\"*")
            }
        }
    }

    private fun findPartInQuery(q: String, parts: List<PartEntity>): PartEntity? {
        val lower = q.lowercase(Locale.ROOT)
        // Exact name check
        parts.find { lower.contains(it.name.lowercase(Locale.ROOT)) }?.let { return it }

        // Token match
        return parts.find { p ->
            val pTokens = p.name.lowercase(Locale.ROOT).split(" ", "-", "/", "_")
            pTokens.any { t -> t.length > 2 && lower.contains(t) }
        }
    }

    // --- Detect Write Action and create Diff Card ---
    private fun detectWriteAction(
        query: String,
        parts: List<PartEntity>,
        locations: Map<Long, LocationEntity>,
        projects: List<ProjectEntity>
    ): ProposedWriteAction? {
        val q = query.lowercase(Locale.ROOT)

        // Action: Move part X to location Y
        if (q.contains("move") && (q.contains("box") || q.contains("shelf") || q.contains("to "))) {
            val targetPart = findPartInQuery(q, parts)
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
            val targetPart = findPartInQuery(q, parts)

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

            var rawName = q.replace(Regex("""^(add|please add|register)\s+""", RegexOption.IGNORE_CASE), "")
            rawName = rawName.replace(Regex("""\b(\d+)\s*(units?|pcs?|pieces?|x)?\b""", RegexOption.IGNORE_CASE), "")
            rawName = rawName.replace(Regex("""(to\s+shelf.*|to\s+box.*|in\s+box.*|into\s+.*)""", RegexOption.IGNORE_CASE), "").trim()

            val cleanedName = if (rawName.isBlank()) "Electronic Component" else rawName.replaceFirstChar { it.uppercase() }
            val group = guessGroupFromName(cleanedName)

            return ProposedWriteAction(
                actionType = "ADD_PART",
                title = "Add Component to Stock",
                beforeState = "Part not yet cataloged",
                afterState = "Name: $cleanedName\nQty: $qty\nGroup: ${group.displayName}\nLocation: ${targetLoc?.code ?: "A-1-B1"}",
                partName = cleanedName,
                group = group,
                newQuantity = qty,
                targetLocationId = targetLoc?.id ?: 1L,
                specs = "Added via natural language"
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

    // --- Parser for Real Captured Workbench/Camera Photo ---
    suspend fun parseCapturedPhoto(bitmap: Bitmap): List<ParsedPartCandidate> {
        val locations = repository.getLocationsList()
        val locMap = locations.associateBy { it.id }

        // If Gemini Vision API is available, analyze the real picture!
        val visionJson = GeminiClient.analyzeWorkbenchImage(bitmap)
        if (!visionJson.isNullOrBlank()) {
            try {
                val jsonArr = JSONArray(visionJson)
                val results = mutableListOf<ParsedPartCandidate>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val name = obj.optString("name", "Electronic Component")
                    val groupStr = obj.optString("group", "Other")
                    val specs = obj.optString("specs", "Visual scan")
                    val qty = obj.optInt("quantity", 1)
                    val conf = obj.optDouble("confidence", 0.85).toFloat()

                    val group = PartGroup.values().find { it.displayName.equals(groupStr, ignoreCase = true) } ?: guessGroupFromName(name)
                    val duplicates = repository.findPotentialDuplicates(name)

                    val targetLoc = locations.find { it.notes.contains(group.displayName, ignoreCase = true) } ?: locations.firstOrNull()

                    results.add(
                        ParsedPartCandidate(
                            name = name,
                            group = group,
                            specs = specs,
                            quantity = qty,
                            confidence = conf,
                            suggestedLocationId = targetLoc?.id,
                            suggestedLocationCode = targetLoc?.code ?: "A-1-B1",
                            potentialDuplicates = duplicates,
                            imageBitmap = bitmap
                        )
                    )
                }
                if (results.isNotEmpty()) {
                    return results
                }
            } catch (e: Exception) {
                // fall through to local fallback
            }
        }

        // Local Fallback for captured image: create an editable candidate card initialized with the user's photo
        return listOf(
            ParsedPartCandidate(
                name = "New Component",
                group = PartGroup.OTHER,
                specs = "Photo captured (${bitmap.width}x${bitmap.height}px)",
                quantity = 1,
                confidence = 0.65f, // triggers amber verification card to let user confirm/edit label
                suggestedLocationId = 1,
                suggestedLocationCode = locMap[1]?.code ?: "A-1-B1",
                potentialDuplicates = emptyList(),
                imageBitmap = bitmap
            )
        )
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

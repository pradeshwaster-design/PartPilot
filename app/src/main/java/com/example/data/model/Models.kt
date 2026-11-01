package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PartGroup(val displayName: String) {
    MICROCONTROLLER("Microcontroller"),
    SENSOR("Sensor"),
    MOTOR_DRIVER("Motor Driver"),
    POWER_BATTERY("Power & Battery"),
    DISPLAY("Display"),
    COMM_MODULE("Comm Module"),
    PASSIVE("Passive"),
    SEMICONDUCTOR("Semiconductor"),
    CONNECTOR_WIRE("Connector & Wire"),
    TOOL_CONSUMABLE("Tool & Consumable"),
    MECHANICAL("Mechanical"),
    OTHER("Other")
}

enum class PartCondition(val displayName: String) {
    WORKING("Working"),
    NOT_TESTED("Not tested"),
    PARTIALLY_WORKING("Partially working"),
    FAULTY("Faulty"),
    DAMAGED("Damaged"),
    UNKNOWN("Unknown")
}

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shelf: String,
    val level: String,
    val box: String,
    val code: String, // e.g. A-2-B3
    val notes: String = ""
)

@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val group: PartGroup,
    val specs: String,
    val quantity: Int,
    val minStock: Int = 0,
    val locationId: Long? = null,
    val condition: PartCondition = PartCondition.WORKING,
    val lastTestedAt: Long? = null,
    val remarks: String = "",
    val datasheetUrl: String = "",
    val photoPaths: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class EventType(val displayName: String) {
    ADDED("Added"),
    USED("Used"),
    RETURNED("Returned"),
    TESTED("Tested"),
    DAMAGED("Damaged"),
    MOVED("Moved"),
    ADJUSTED("Adjusted")
}

@Entity(tableName = "part_events")
data class PartEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partId: Long,
    val type: EventType,
    val note: String,
    val qtyDelta: Int = 0,
    val projectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ProjectStatus(val displayName: String) {
    BRAINSTORM("Brainstorm"),
    IDEA("Idea"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val budgetCap: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class PartDecision(val displayName: String) {
    KEEP("Keep"),
    DROP("Drop"),
    MAYBE("Maybe")
}

enum class ChecklistState(val displayName: String) {
    PARTS_READY("Parts Ready"),
    TESTED("Tested"),
    WIRED("Wired"),
    CODED("Coded"),
    WORKING("Working")
}

enum class ComputedPartStatus(val displayName: String) {
    HAVE("Have"),
    NOT_ENOUGH("Not Enough"),
    NEED("Need"),
    IN_CART("In Cart")
}

@Entity(tableName = "project_parts")
data class ProjectPartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val partId: Long? = null, // nullable if unowned
    val name: String,
    val qtyNeeded: Int,
    val estUnitPrice: Double = 0.0,
    val decision: PartDecision = PartDecision.KEEP,
    val reservedQty: Int = 0,
    val checklistState: ChecklistState = ChecklistState.PARTS_READY
)

enum class CartItemStatus(val displayName: String) {
    IN_CART("In Cart"),
    ORDERED("Ordered"),
    BOUGHT("Bought")
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val name: String,
    val qty: Int,
    val unitPrice: Double,
    val shop: String = "",
    val status: CartItemStatus = CartItemStatus.IN_CART,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_action_log")
data class AiActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val summary: String,
    val details: String,
    val executedAt: Long = System.currentTimeMillis()
)

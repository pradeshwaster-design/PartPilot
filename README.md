# PartPilot (Phase 1)

**PartPilot** is a personal AI-powered electronics inventory and project companion for robotics and embedded systems makers.

---

## 🛠️ Architecture & Tech Stack

- **Platform:** Native Android (Kotlin + Jetpack Compose)
- **Design System:** Material Design 3 workshop theme (`#F3F5F4` calm technical background, white cards with 1px `#DDE3E0` border and 14px radius, `#0B6B5D` workshop teal, IBM Plex Sans & IBM Plex Mono typography)
- **Local Persistence:** Room Database with reactive Kotlin Flows and pre-populated seed data
- **Grounded Workshop AI:** Grounded Assistant with strict database tool calling (`search_parts`, `get_part`, `get_location_contents`, `list_projects`, `get_project_bom`) and before/after write diff confirmation cards
- **Fast Intake:** Workbench photo scan simulation, natural language / voice parsing, and synonym duplicate detection
- **QR Labeling:** On-device 2D matrix QR generator for physical bin tagging
- **BOM Management:** Real-time computed part statuses (`HAVE`, `NOT ENOUGH`, `NEED`, `IN CART`), stock reservation, budget tracking, and build checklists

---

## 📦 Data Models

1. **locations**: `id`, `shelf`, `level`, `box`, `code` (`A-2-B3`), `notes`.
2. **parts**: `id`, `name`, `group`, `specs`, `quantity`, `minStock`, `locationId`, `condition`, `lastTestedAt`, `remarks`, `datasheetUrl`, `photoPaths`.
3. **part_events**: `id`, `part_id`, `type` (`ADDED`, `USED`, `RETURNED`, `TESTED`, `DAMAGED`, `MOVED`, `ADJUSTED`), `note`, `qtyDelta`, `projectId`, `createdAt`.
4. **projects**: `id`, `name`, `description`, `status` (`ACTIVE`, `IDEA`, `COMPLETED`), `budgetCap`, `notes`, `createdAt`, `completedAt`.
5. **project_parts**: `id`, `projectId`, `partId`, `name`, `qtyNeeded`, `estUnitPrice`, `decision`, `reservedQty`, `checklistState`.
6. **cart_items**: `id`, `projectId`, `name`, `qty`, `unitPrice`, `shop`, `status`, `createdAt`.
7. **ai_action_log**: `id`, `actionType`, `summary`, `details`, `executedAt`.

---

## 🧪 Phase 1 Test Checklist

- [x] **Home Dashboard:** Quick stats cards, grounded Ask-AI bar, Continue Building project card, Needs Attention alerts, and Quick Action buttons.
- [x] **Parts Inventory (Stock Screen):** Search by query, filter by Group/Shelf/Condition, sort by Name/Location/Quantity, view live free stock and condition chips.
- [x] **Storage Locations & QR Codes:** View shelves, levels, and boxes; view parts stored in each box; generate high-resolution QR box label.
- [x] **Fast Component Intake:**
  - Photo scan analysis with confidence ratings (<70% shows verification prompt).
  - Voice / natural language command parsing (e.g. *"Add 10 resistors 220 ohm to Shelf B level 1 box 2"*).
  - Duplicate detection with Merge (+qty) or Keep Separate actions.
- [x] **Part Details & Event Timeline:** Inspect stock counts (Total, Free, Reserved), move storage locations, adjust quantities, mark damaged with cause, and view the immutable event timeline.
- [x] **Hardware Projects & BOM:**
  - Computed status chips: `HAVE`, `NOT ENOUGH`, `NEED`, `IN CART`.
  - Hold & reserve stock for active projects.
  - Multi-step build checklist (`Parts Ready` ➔ `Tested` ➔ `Wired` ➔ `Coded` ➔ `Working`).
  - Finish project action: releases reserved stock back to available inventory.
- [x] **Grounded AI Assistant:**
  - Query stock: *"Do I have an L298N?"*
  - Query location: *"Where is my MPU6050?"*
  - Query box contents: *"What is in box A-2-B3?"*
  - Query project needs: *"What do I still need for Line Follower v2?"*
  - Propose write diff card with mandatory user confirmation before updating database.
  - Action audit log tracking executed operations.
- [x] **Cart & Buying:** Track unowned components, budget cap comparison, and transfer to storage box with *"Mark as Bought"*.
- [x] **Export:** Export complete workshop inventory to CSV.

# PartPilot: Features and Build Prompt

Nov 1, 2026 · @Pradesh

## How to use this document

Build PartPilot in four phases: paste the master prompt into your coding tool, let it finish Phase 1, test it, then paste the next phase prompt.

- **Section 1 below** explains every feature in plain language, so you know what you are asking for.
- **Section 2** is the master prompt. Copy the whole code block into Claude Code, Cursor, Lovable, Bolt or any similar AI coding tool.
- **Section 3** has the short follow-up prompts for Phases 2, 3 and 4.

Stack chosen for you (one codebase for Android and PC, single user, online only): Next.js with TypeScript and Tailwind as an installable web app (PWA), Supabase for database, login and photo storage, and the Claude API for all AI features. It runs on Windows, and the free tiers of Supabase and Vercel are enough for one person.

Before you start you need three things: a Supabase project (URL and keys), an Anthropic API key, and a Node.js install.

## 1. Features explained

PartPilot is a personal AI companion for your electronics hobby. It knows every part you own, where it sits on your shelf, which project needs it, and what you still have to buy.

### Stock and shelf locations

- Every part has a name, group (sensors, controllers, drivers, power and so on), specs, quantity, shelf, level and box, so the app can answer "where is it?" exactly.
- Location code works like `A-2-B3`: Shelf A, level 2, box B3. The same code goes on the box label, and the app can print a QR label for each box.
- Condition per part: Working, Not tested, Partially working, Faulty, Damaged, Unknown, with a remarks field for what you tested and what went wrong.
- Search by typing, voice, or by asking the AI in plain words. Filters by group, shelf, condition and project.
- Part detail page shows photos, stock, how many are reserved for projects, where you bought it, and every test or damage note.

### Adding parts (the fast way)

- **Photo:** take a picture of one part or a whole table. The AI names each part, suggests a group and a shelf, and shows confidence. Low-confidence parts ask you to type what they are.
- **Type or voice:** "Add 10 resistors, 220 ohm, to Shelf B level 1 box 4."
- **Bulk table mode:** for your messy table. Photograph a batch, confirm the AI's list, and everything lands in stock in one tap.
- **Duplicate detector:** warns if "HC-SR04" and "Ultrasonic sensor" are the same part and offers to merge.
- **Quick stock check:** walk through a drawer, confirm or correct each count.

### Part intelligence

- **Datasheet Q&A:** ask "What is the max current of this driver?" on any part.
- **Compatibility checks:** warns about voltage and current problems, such as a 5 V sensor on a 3.3 V board.
- **Wiring and pinout helper:** pick two parts you own and get the pin connections.
- **Substitute finder:** if a part is missing, the AI suggests something compatible that you already own.
- **Damaged and dead log:** record why a part failed (burnt, wrong wiring) so you spot repeat mistakes.
- **Battery tracking:** cycles, last charged, age, because old cells are a safety issue.
- **Tool inventory:** soldering iron, multimeter, flux, solder, with reminders when consumables run low.

### Projects

- Each project has a parts list (BOM) with live status: Have, Not enough, In cart, Need to buy.
- **Reservation:** stock is held for a project, so two projects never count the same sensor.
- **Budget:** a cost total per project and an optional cap that warns you when a cart goes over.
- **Build checklist:** parts ready, tested, wired, coded, working.
- **Pin planner:** tracks which microcontroller pins each project uses, and flags conflicts.
- **Power budget calculator:** adds up current draw and checks it against your battery and regulator.
- **Journal:** notes, photos, what failed and what you learned, per project.
- **Finish a project:** parts return to stock with one tap, with a final cost summary and an exportable report for college or competitions.
- Lifecycle: Brainstorm, then Project Idea, then Active Project, then Completed.

### Project Brainstorm

- A chat workspace where a rough idea becomes a buildable one. The AI asks about size, budget, power and environment.
- A live component panel beside the chat: each suggested part shows Have, Not enough or Need to buy, and you can mark it Keep, Drop or Maybe.
- You can override the AI: "use my ESP32 instead" rewrites the idea and the list, and tells you what changed.
- Compare two or three approaches (cheap, powerful, simple) with trade-offs.
- Sessions are saved. One tap moves the idea, chosen parts and notes into Project Ideas.

### Project Ideas

- A saved list of ideas, each with its parts and ownership status.
- **What can I build now?** ranks ideas by how much you already own, so you can build without shopping.
- Move an idea to Active Project when you start.

### AI assistant (chat and voice)

- Answers from your real data: "Do I have an L298N?", "Where is my MPU6050?", "What do I still need for the line follower?", "Can I build a rover with what I have?"
- Can act, not just answer: add a part, reserve stock, put an item in the cart, create a project. It always shows what it will change and asks you to confirm.

### Buying

- **Cart from a screenshot:** upload a screenshot of a cart or a list. The AI reads items, prices and quantities, then asks "Which project is this for?" and files each item under it, with cost, quantity and shop or website.
- **Bill scanner:** photograph a shop bill. Items, prices and shop name are added automatically, and bought items move into stock.
- **Shop Mode:** say "I'm going to the electronics market for the line follower". You get a list of only what you are missing, grouped for that shop, with quantities and an estimated budget. Tick items off in the shop.
- **Shop memory:** the app learns which shop had which part and at what price, so it can say "Shop A usually has this, Shop B does not", and suggests a website when no local shop stocks it.
- **Price history and bundle checker:** compare the same part across shops, and warn when separate parts cost more than a kit.
- **Wishlist** for parts you want someday, kept apart from the cart.
- **Order tracker** for website orders, with expected delivery dates.
- **Spending insights:** monthly spend by project and by shop.

### Housekeeping and reports

- **Low-stock alerts** for things you use often, such as jumper wires and batteries.
- **Unused parts report:** parts bought but never used in any project.
- **Smart grouping:** AI suggests kits from your stock, such as a motor driver starter set.
- **Weekly summary:** what is low, reserved, idle or on order.
- **Export:** whole inventory to CSV, projects to a PDF report.

### Companion layer

- **Home screen:** a "continue where you left off" card, things needing attention, quick actions.
- **Idea inbox:** capture a random thought by voice or text; AI sorts it later.
- **Troubleshooting chat:** describe a problem ("motor jitters") and the AI suggests causes using your actual parts.
- **Skill tracker and learning suggestions:** what you have learned (PWM, I2C, PID) from finished projects, and a suggested next project.

### Platform rules

- One user, online only, no offline mode.
- Works as an installable web app on Android and on PC.
- Camera, microphone, and file upload are used for photos, voice and screenshots.
- Starter code generation is deliberately not included.

## 2. Master build prompt

Copy everything in the block below. It tells the coding tool to build Phase 1 only and then stop.

```text
You are a senior full-stack engineer. Build "PartPilot", a personal AI-powered electronics inventory and project companion for ONE user (an engineering student who builds robotics and embedded projects). Work in phases. Build ONLY Phase 1 now, then stop, give me run instructions and a test checklist, and wait for me to ask for the next phase.

# 1. Goals and constraints
- Platforms: Android (installable PWA) and web on PC. One codebase, mobile-first, usable at 390px wide and at 1280px+ wide.
- Single user. Online only, no offline mode. Simple private login (Supabase email login, sign-ups disabled after my account exists).
- Stack: Next.js (App Router) + TypeScript + Tailwind CSS, Supabase (Postgres, Auth, Storage, Row Level Security on every table), Anthropic Claude API for all AI features, deploy target Vercel. I develop on Windows, so give commands that work in PowerShell.
- The Anthropic API key and Supabase service key live ONLY in server-side env vars (route handlers / server actions). Never expose them to the browser.
- Model name comes from env var ANTHROPIC_MODEL (default: claude-sonnet-5-5). Use image input (vision) for photos.
- Do NOT build a starter-code generator. Do not add features I did not list.

# 2. Data model (Postgres, with migrations and RLS: user_id = auth.uid())
- locations: id, shelf (text), level (text), box (text), code (generated: shelf-level-box, e.g. A-2-B3), notes.
- parts: id, name, group (enum: microcontroller, sensor, motor_driver, power_battery, display, comm_module, passive, semiconductor, connector_wire, tool_consumable, mechanical, other), specs, quantity (int >= 0), min_stock (int, default 0), location_id, condition (enum: working, not_tested, partially_working, faulty, damaged, unknown), last_tested_at, remarks, datasheet_url, photo_paths (text[]), created_at, updated_at.
- part_events: id, part_id, type (added, used, returned, tested, damaged, moved, adjusted), note, qty_delta, project_id, created_at. Every change to a part writes an event.
- projects: id, name, description, status (brainstorm, idea, active, completed), budget_cap, notes, created_at, completed_at.
- project_parts: id, project_id, part_id (nullable when the part is not owned yet), name, qty_needed, est_unit_price, decision (keep, drop, maybe), reserved_qty, checklist_state (parts_ready, tested, wired, coded, working).
- Create now, use in later phases: brainstorm_sessions, shops, cart_items, price_records, wishlist_items, journal_entries, pin_assignments, ai_action_log.
- Status of a project part is COMPUTED, never stored: HAVE (free stock >= needed), NOT_ENOUGH (some stock but short), IN_CART (cart item exists), NEED (none owned). free stock = quantity minus stock reserved by other active projects.

# 3. Design system (follow exactly)
- Calm, technical, workshop feel. Background #F3F5F4, cards white with 1px #DDE3E0 border and 14px radius, text #0F1F1A, primary teal #0B6B5D.
- Fonts: IBM Plex Sans for UI, IBM Plex Mono for quantities and location codes.
- Status chips: Have = green (#DDF0EB / #0B5A4E), Low or Not enough = amber (#FFEFC7 / #7A4D00), Need = red (#FCE5DD / #A52E14), In cart = blue (#E1ECFB / #1B4F9C). Never rely on colour alone: every chip has a text label.
- Mobile: bottom nav with Home, Stock, Projects, Cart, Ask AI. Desktop: left sidebar with the same items plus a wider two-column layout for lists and details.
- Touch targets at least 44px. Real buttons and links, labelled icon buttons, keyboard accessible, text contrast at least 4.5:1. No emoji in the UI.

# 4. PHASE 1 scope (build this now)
A. Auth and shell: login, protected routes, responsive layout, PWA manifest and service worker for installability only (no offline data).
B. Stock screen: searchable, filterable list (group, shelf, condition) of parts showing name, location code, quantity, condition chip, reserved chip. Floating add button. Sort by name, location, quantity.
C. Location system: create and manage shelves, levels and boxes. Location code shown everywhere. Show a QR label for each box that opens that box's contents.
D. Add parts, three ways:
   1. Photo: user takes or uploads a photo of one part or a whole table. Send it to Claude vision. Return structured JSON: [{name, group, specs, quantity_guess, confidence 0-1, suggested_location}]. Show a confirmation screen with editable cards. Confidence under 0.7 shows an amber card asking the user to type what it is. Nothing is saved until the user confirms.
   2. Type or voice: free text like "add 10 resistors 220 ohm to shelf B level 1 box 4". Use the browser Web Speech API for voice. Claude parses it into the same confirmation screen.
   3. Bulk table: several photos in a row, one combined confirmation list.
   Duplicate detection: before saving, compare with existing parts (normalised name plus specs, plus an AI check for synonyms such as "HC-SR04" and "ultrasonic sensor"). Offer Merge or Keep separate.
E. Part detail screen: photos, quantity, reserved vs free, location with a Move action, condition dropdown with last tested date, remarks, datasheet link, projects using it, and a timeline of part_events. Actions: Use in project, Mark damaged (requires a reason), Adjust quantity.
F. AI assistant (chat, text and voice) grounded ONLY in the database via tool calls (no guessing). Tools: search_parts, get_part, get_location_contents, list_projects, get_project_bom. Example questions that must work: "Do I have an L298N?", "Where is my MPU6050?", "What is in box A-2-B3?", "What do I still need for Line Follower v2?". Answers state quantity, location code and condition. If a part is not found, say so and suggest the closest owned alternative.
   Write actions (add part, move part, adjust quantity, reserve for project) are proposed as a clear before/after diff card and executed ONLY after the user taps Confirm. Log every executed action in ai_action_log.
G. Projects (basic): create project, add parts to its BOM (owned or not owned yet), computed status chips per part, reservation of stock, total estimated cost vs budget_cap, build checklist, finish project (return reserved stock, set completed_at, show cost summary).
H. Home screen: greeting, ask-AI bar with mic and camera buttons, Continue building card (latest active project with progress), Needs attention (low stock, idle parts), quick actions.
I. Export: whole inventory to CSV.
J. Seed script with about 20 realistic sample parts, 3 locations and 2 projects so I can test immediately.

# 5. AI rules (apply to every AI feature)
- Never invent stock, locations, prices or specs. If data is missing, say so and ask.
- For datasheet or compatibility claims, say when you are unsure and name what to verify.
- Use structured JSON outputs validated with a schema (zod) before touching the database. Reject and retry once on invalid output.
- Keep prompts and tool definitions in one /lib/ai folder. Add rate limiting and basic error states ("AI is unavailable, try again") on every AI screen.
- Resize photos client-side to max 1600px before upload to control cost and speed.

# 6. Later phases (do NOT build yet, but design the schema and code so they fit)
- Phase 2: Project Brainstorm (AI chat with live component panel, Keep/Drop/Maybe, swap parts by saying "use my ESP32", compare approaches, save sessions, one tap move to Project Ideas) and Project Ideas (including "What can I build now?" ranking).
- Phase 3: Buying (cart from screenshot with project questions, bill scanner, Shop Mode, shop memory, price history, bundle checker, wishlist, order tracker, budget caps, spending insights).
- Phase 4: Companion layer and part intelligence (datasheet Q&A, compatibility checks, wiring and pinout helper, substitute finder, pin planner, power budget calculator, journal, troubleshooting chat, battery tracking, tool inventory, smart kits, weekly summary, skill tracker, project PDF report).

# 7. Deliverables for Phase 1
- Working app with clean folder structure, migrations, RLS policies, seed script, and a README with PowerShell setup steps and the env var list (.env.example).
- A short test checklist I can follow to verify every Phase 1 feature.
- Then STOP and wait.
```

Tips: if your tool has a context limit, paste sections 1 to 5 first and ask it to confirm, then paste sections 6 and 7. If it asks questions, answer from the feature guide above.

## 3. Follow-up prompts

Paste one of these after the previous phase works and you have tested it. Each keeps the same stack, design system and AI rules from the master prompt.

### Phase 2: Brainstorm and Project Ideas

```text
Phase 1 works. Now build Phase 2 of PartPilot, using the same stack, design system and AI rules. Stop when done and give a test checklist.

1. Project Brainstorm screen
- Chat with Claude to develop a rough idea. The AI asks about size, budget, power source and environment before proposing parts.
- A live component panel next to the chat (below the chat on mobile). Each suggested part is a card with a computed status chip (Have, Not enough, Need) from my real inventory, a quantity, an estimated price, and Keep / Drop / Maybe buttons.
- If I say "use my ESP32 instead" (or tap Swap on a card), the AI rewrites the idea and the parts list, lists exactly what changed, and updates statuses.
- A Compare button that returns 2 or 3 approaches (cheap, powerful, simple) with trade-offs and a parts list each.
- Sessions are saved in brainstorm_sessions (messages and draft parts as JSON). I can reopen and continue any session.
- A Move to Project Ideas button creates a project with status "idea", carries over the chosen parts (Keep and Maybe), the notes and the summary.

2. Project Ideas screen
- List of ideas with name, short description, parts owned vs total, and estimated cost to buy what is missing.
- A "What can I build now?" view ranking ideas by percentage of parts already owned, and showing the missing parts.
- An idea has a Start project button that changes status to "active" and reserves stock. Lifecycle is Brainstorm, Idea, Active, Completed.
- The AI can also suggest new ideas based on my stock when I ask.

Keep AI write actions behind a confirm step and log them in ai_action_log.
```

### Phase 3: Buying

```text
Phase 2 works. Now build Phase 3 of PartPilot (buying), same stack, design system and AI rules. Stop when done and give a test checklist.

1. Cart from a screenshot
- Upload a screenshot of a shopping cart or a list. Claude vision extracts items, quantities, unit prices and the shop or website name.
- The AI then asks, in a short chat, "Which project is this for, or why are you buying it?" and files each item under that project (or a Wishlist reason). Show the result as a confirmation list I can edit before saving.
- Cart screen: items grouped by project, each with quantity, unit price, line total, shop or website, and a status (in cart, ordered, bought). Totals per project and overall, with a warning if a project budget_cap is exceeded.
- Mark as bought: moves items into stock with a location picker and writes part_events.

2. Bill scanner
- Photograph a shop bill. Extract items, prices and shop. Match to existing parts, create new ones where needed, and record price_records. Confirmation screen first.

3. Shop Mode (the AI-controlled shopping flow)
- I say or type: "I'm going to <shop> to buy parts for <project>". The AI builds a shopping list of only the parts I am missing for that project, with quantities, using shop memory to prefer items that shop usually has and to flag items it usually does not stock (suggest another shop or a website).
- Shows an estimated budget. In the shop I tick items off; ticking an item can add it to the bought list for later stock entry.

4. Shop memory, price history and extras
- shops (local or online) with notes. price_records saved from carts and bills. A part page shows price history by shop and flags the cheapest.
- Bundle checker: warn when buying parts separately costs more than a kit the AI knows of.
- Wishlist, separate from the cart.
- Order tracker for website orders with expected delivery date and a Home card for pending orders.
- Spending insights: monthly spend by project and by shop.
- Budget cap per project and per month with warnings.

Prices are only taken from what I upload or type. Never invent prices.
```

### Phase 4: Companion layer and part intelligence

```text
Phase 3 works. Now build Phase 4 of PartPilot, same stack, design system and AI rules. Stop when done and give a test checklist. Do not build a starter-code generator.

1. Part intelligence
- Datasheet Q&A on every part page (the AI uses the stored datasheet_url or text I paste, and says when it is unsure).
- Compatibility checks: when a project has parts that conflict (for example a 5 V signal into a 3.3 V board, or a motor current above a driver limit), show a warning with the reason.
- Wiring and pinout helper: pick two owned parts and get pin-to-pin connections plus warnings.
- Substitute finder: when a part is missing, suggest compatible parts I already own.
- Pin planner per project (pin_assignments) with conflict detection.
- Power budget calculator: sum of current draw vs battery and regulator capacity.

2. Hobby companion
- Project journal with notes and photos, build checklist history, failure notes.
- Idea inbox: quick capture by voice or text, sorted later by the AI into ideas.
- Troubleshooting chat: describe a problem, the AI suggests causes using the parts in that project.
- Skill tracker and learning suggestions based on finished projects.
- Weekly summary: low stock, reserved, idle parts, orders on the way.

3. Inventory care
- Battery tracking (cycles, last charged, age) with safety reminders.
- Tool inventory and consumables with low-stock reminders.
- Dead and damaged log with reasons, and a repeat-mistake summary.
- Unused parts report, smart kit suggestions, quick stock check mode.
- Project PDF report (parts, cost, photos, notes) and full backup export.
```

Build order and testing: finish and test each phase on your phone and PC before starting the next, and keep your Supabase database backed up before each phase.

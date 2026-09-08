# AI Master Context Blueprint: Project ZIPPO (Phase 1)

This document serves as the absolute master engineering blueprint, systemic memory, and architectural framework for **Project ZIPPO**. It contains the comprehensive consolidation of all business requirements (BRD), technical design parameters (TSD), visual wireframes, and advanced AI agent logic matrices established from the project's inception.

**LLM DIRECTION:** Inject this entire document into the initial prompt payload of any fresh AI chat session (Gemini, ChatGPT, or Claude) to achieve instant context synchronization, seamless code refactoring, and logical expansion without project context degradation.

---

## 📋 1. Project Overview & Target Persona
* **Application Identity:** **ZIPPO** (Punchy, 2-syllable, high-energy mobility tech brand identity familiar across local regional languages).
* **Target User:** Auto-rickshaw, mini-van, or school bus owner-drivers operating long-term, fixed-subscription pickup and drop routes (e.g., school children or corporate employee tech park transits).
* **Core Pain Points Solved:** 
  1. **Financial Leakage:** Informal memory-based ledgers cause drivers to lose track of month-over-month subscription payments.
  2. **Route Inefficiencies:** Wasting time and fuel navigating chaotic traffic routes due to manual memory-based pickup sequencing.
  3. **Communication Hazards:** Needing to manually type messages to clients while steering a vehicle in active traffic.
* **UX/UI Constraint Core Principle:** High-contrast slate theme layout to combat outdoor glare. Features oversized touchscreen targets, zero-typing during active driving operations, and support for voice entry logging workflows ("Bahi-Khata" voice command tool). It remains dead simple for users familiar with ridesharing interfaces (Ola, Uber, or Rapido).

---

## 📦 2. Phase 1 Core Scope & Operational Requirements

### A. Roster & Dynamic Shift Management
* The application separates scheduling parameters into **Morning Duty ☀️** and **Evening Duty 🌙**.
* **Vice-Versa Coordinate Inversion Engine:** Drivers save exactly two static physical coordinates during customer onboarding: **Home Coordinates** and **Destination Coordinates** (School/Office Hub). The system automatically transposes vectors based on active shifts:
  * **Morning Shift Flow:** Home Coordinates (Pickup Pin) ➡️ Destination Coordinates (Drop Pin).
  * **Evening Shift Flow:** Destination Coordinates (Pickup Pin) ➡️ Home Coordinates (Drop Pin).

### B. Independent Shift-Level Absence Segregation (Kids & Employees)
* Attendance states are strictly scoped to the independent active shift tier (`morningStatus` vs `eveningStatus`) inside a daily log schema.
* Marking a school kid or corporate employee absent during the morning run modifies only the morning routing matrix. The evening schedule state resets to `PENDING` by default, ensuring the evening coordinate sequence calculates completely unhindered unless explicitly flagged absent for the evening segment as well. This fully handles situations where a passenger skips the morning auto but requires an evening pickup, or vice-versa.

### C. Capacity-Constrained Multi-Trip Logic & Manual Swap Overrides
* **Driver Control Window:** Seating capacity (`customCapacityLimit`) is a global driver property configured exactly once inside the main profile settings panel. It is strictly excluded from individual passenger data forms. The operator adjusts this value via simple increment elements (e.g., setting it to 4, 5, 8, 10, or 50) depending on preferred travel loads and vehicle sizing.
* **Algorithmic Trigger:** If active passenger queues exceed this precise limit (e.g., 20+ children or employees), the server automatically fragments the active timeline feed into sequential `Trip 1`, `Trip 2`, and `Trip 3` tab sheets, routing segments optimally to avoid vehicle overloading.
* **Manual Cross-Trip Swapping Overrides:** The frontend exposes a interface enabling the driver to manually move individual passenger node keys between generated trip tab arrays (`Trip 1` ⇄ `Trip 2`) to handle real-world exceptions.
* **Two-Way Force Swap Capacity Safeguard:** If a manual cross-trip swap operation targets a trip segment that has already reached the `customCapacityLimit`, the system suppresses direct insertion. The frontend UI triggers an interactive modal prompt displaying the target trip's active list arrays. The driver must select an individual to displace; confirming this selection executes a server-side atomic validation swap mutation, cleanly exchanging the two nodes between the respective trip document arrays simultaneously to guarantee capacity boundaries are never violated.
* **Dynamic Manual Trip-Load Balancing Adjuster:** In addition to global vehicle limits, the system provides a specialized configuration array (`customTripSizes`). If an active daily roster splits unevenly across trips (e.g., 17 total riders creating a final trip with a minimal load of 2), the operator can explicitly adjust the target capacity index for each individual trip leg (e.g., rebalancing the structure to 5, 4, 4, 4). The NestJS clustering engine ingests this custom size matrix to dynamically sort and isolate geographic cohorts into balanced batches.


### D. Collaborative Multi-Driver Trip Delegation Engine
* **Operational Goal:** Allows a primary driver handling extreme volume (e.g., 5+ trips) to cleanly delegate specific trip partitions to peer drivers running the same application platform container.
* **Architecture Implementation:** Tracks real-time active rosters via an independent `TripSegment` collection layer. The primary driver initiates a delegation dispatch request by inserting a colleague's unique telephone index node. 
* **Permission Isolation Matrix:** Accepting a delegated segment maps out the target coordinates, timeline buffers, and parent communication arrays onto the recipient's dashboard view. Recipient accounts are structurally barred via API guards from reading any master subscription records or ledger balances attached to the source route profile.

### E. Supervisor Dashboard & Live Status Monitoring
* When a primary driver delegates a `TripSegment` to a peer colleague, their dashboard flips into an operational **Supervisor Mode**.
* The server uses WebSockets to stream state changes from the delegate's phone in real time back to the owner's dashboard screen. The owner sees live check-in ticks (`PICKED_UP`, `ABSENT`) and location coordinates of the delegated vehicle during transit.

### F. Proximity Geofencing & 1000-Meter Smart Alerts
* To eliminate prolonged roadside waiting times and continuous vehicle honking, the mobile application runs a background location tracking module via an Android `ForegroundService`.
* The client calculates the physical distance coordinates relative to the next target stop. The moment the vehicle crosses within a **1,000-meter (1 Km) proximity threshold** of a client's gate pin, the app triggers a high-contrast in-app modal overlay block containing an explicit execution action loop.

### G. Zero-Cost Native Communication Strategy
* To adhere strictly to a zero-cost operational overhead target, external multi-channel messaging bypasses paid corporate SMS gates and the official WhatsApp Business API.
* Proximity loops (1,000-meter boundaries) and Terminal Drop indicators initiate explicit Kotlin `Intent.ACTION_VIEW` deep-links directly into the local device's native personal WhatsApp engine container. Tapping the trigger passes target variables to native WhatsApp, pre-populating a pre-composed message text layout instantly, requiring a single validation touch from the driver to execute the transaction for zero system billing fees.
* **The Delay Absorption Rule:** The application bypasses rigid clock-time checks for proximity messaging. Any upstream timeline delay—including late driver departures or prolonged passenger loading times—is completely absorbed by the routing engine. Automated text updates are strictly suppressed until the physical vehicle explicitly crosses the 1,000-meter geofence perimeter of the immediate next destination node.

### H. Multi-Tier Contract Type Boundaries & Direct Dialing Intents
* **Flexible Contract Types:** Passenger parameters explicitly feature a `contractType` parameter (`BOTH`, `MORNING_ONLY`, `EVENING_ONLY`). The shift roster aggregation script evaluates this variable to filter items cleanly out of active shift timelines, preventing empty pickup routes and ensuring accurate ledger allocations for partial riders.
* **One-Click Native Dialer Links:** UI dashboard profiles map a prominent action button bound to an explicit native Android layout `Intent(Intent.ACTION_DIAL)`. This enables immediate one-touch voice calling loops out to parents or employees without requiring manual entry navigation.

### I. Granular Targeted Holiday Scoping Matrix
* Provides a centralized date-picker layout where drivers log upcoming holiday intervals (e.g., a **3-day school vacation sequence**).
* Holiday declarations incorporate mandatory target scoping boundaries (`ALL`, `SCHOOL_ONLY`, `OFFICE_ONLY`, `SPECIFIC_HUB`). The backend filtering script reads these scope constraints during shift compilation; it freezes routes and automates broadcast alerts exclusively for passengers matching the active holiday tag, while keeping unaffected passenger profiles locked into the active AI route sequence seamlessly.

### J. Subscription Control, Free Trial & Payment Ledger
* **The 6-Month Free Trial:** Every registering driver gets a hard-coded 6-month trial window. Once the backend timestamp passes this date, the frontend UI shifts to a persistent account block layer requesting a renewal fee.
* **Micro-SaaS B2B Pricing:** Post-trial, access is controlled via a **`₹99/month`** subscription layout model.
* **Zero-Cost Manual Verification Script:** Payments are collected out-of-band via standard UPI QR scans displayed inside the application. The driver pastes their payment **UPI Transaction ID / UTR Number** into the application, allowing an admin to manually verify bank lines and flip the database subscription validation flags.
* **Family/Household Billing Resolution:** Supports a **Parent/Household Grouping Key**. Multiple siblings picked up from the same gate can be linked under one single parent profile. This aggregates their individual subscription obligations into one unified invoice item while keeping separate check-in flags on the active driving roster.

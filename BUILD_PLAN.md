# 🚗 SmartPark: Object-Oriented Parking Management System
## B.Tech 2nd Year OOP (Java) Master Reference & Build Plan

> **GitHub Repository**: [https://github.com/rohantanwani08/SmartPark.git](https://github.com/rohantanwani08/SmartPark.git)  
> **Course**: Object-Oriented Programming in Java (B.Tech 2nd Year)  
> **Core Methodology**: Pure step-by-step interactive build, zero unreviewed code, deep conceptual mastery of OOP pillars, concurrency, streams, and viva preparation.

---

## 📌 1. Project Philosophy & Learning Contract
1. **Student Ownership**: Every class, method, keyword, design decision, and edge case must be understood completely by the student.
2. **Strict Phase Progression**: No jumping ahead. Each phase is planned, explained, reviewed, implemented, and committed to git.
3. **Viva-Ready from Day 1**: Every phase includes exact viva voce questions, the underlying JVM/OOP mechanisms, and how to defend the design.

---

## 🏛️ 2. Core OOP Concepts Demonstrated

| OOP Principle | Where & How It Is Used in SmartPark | Concrete Code Artifact |
| :--- | :--- | :--- |
| **Abstraction** | Hiding implementation details of vehicle pricing and payment processing behind contracts. | `abstract class Vehicle`, `abstract class MotorVehicle`, `interface Payment` |
| **Encapsulation** | Private fields with controlled getters/methods; preventing invalid states (e.g. overbooking or negative hours). | `ParkingSlot`, `Floor`, `Ticket` |
| **Inheritance** | Reusing common vehicle traits (`plateNumber`, `ownerName`, `entryTime`) across concrete types. | `Car extends MotorVehicle`, `Bike extends MotorVehicle`, `Truck extends MotorVehicle` |
| **Polymorphism** | Dynamic method dispatch on fee calculation and payment execution; constructor overloading. | `v.calculateFee(hours)` dynamically resolves at runtime to `Car`, `Bike`, or `Truck` implementation. |
| **Composition** | Building complex entities from simpler ones ("has-a" relationship). | `Floor` has an array of `ParkingSlot`; `ParkingLot` has multiple `Floor`s. |
| **Interfaces** | Defining interchangeable capabilities without shared state. | `Payment` implemented by `CashPayment` and `UPIPayment`. |
| **Exception Handling** | Custom checked exceptions representing business failure modes. | `DuplicateVehicleException`, `LotFullException`, `PaymentFailedException`, etc. |
| **Multithreading** | Concurrent entry gates, race condition demonstrations, synchronization, and monitor locks. | `synchronized`, `wait()`, `notifyAll()`, `Runnable Gate`, daemon overstay watcher. |
| **I/O Streams** | Character streams for readable active records, byte streams for binary logs. | `BufferedReader`/`BufferedWriter` (CSV) vs `FileInputStream`/`FileOutputStream` (.dat). |

---

## 🗺️ 3. The 12-Phase Roadmap

### Phase 0: Setup & Terminal Mastery (30 min)
- **Goal**: Master terminal compilation, understand what an IDE hides under the hood, establish clean folder structure and Git tracking.
- **Tasks**:
  1. Verify JDK 17+ or JDK 21+ (`javac -version`, `java -version`).
  2. Setup directory tree:
     ```text
     SmartPark/
     ├── src/
     │   └── smartpark/
     │       ├── Main.java
     │       ├── model/
     │       ├── exception/
     │       └── service/
     ├── out/
     ├── data/
     ├── .gitignore
     ├── BUILD_PLAN.md
     └── AGENTS.md
     ```
  3. Initialize Git and connect remote `https://github.com/rohantanwani08/SmartPark.git`.
  4. Compile and run from terminal without IDE buttons:
     - PowerShell: `javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })` &rarr; `java -cp out smartpark.Main`
     - Bash: `javac -d out $(find src -name "*.java")` &rarr; `java -cp out smartpark.Main`
  5. Commit Phase 0 checkpoint.

---

### Phase 1: Object-Oriented Design on Paper (1 to 2 hours)
- **Goal**: Understand every entity, relationship, and fee equation before writing a single line of domain code.
- **System Definition (5-Line Summary)**:
  1. *SmartPark* manages multi-level parking lots with designated slots for different vehicle types.
  2. *Vehicles* (Car, Bike, Truck) enter through gates and receive a timestamped *Ticket*.
  3. Each vehicle type calculates hourly parking fees using its own pricing rules.
  4. On exit, the system validates the *Ticket*, collects *Payment* (Cash/UPI), vacates the *Slot*, and archives the transaction.
  5. The system operates concurrently across multiple gates safely using thread synchronization.
- **Nouns (Classes)**: `Vehicle`, `MotorVehicle`, `Car`, `Bike`, `Truck`, `ParkingSlot`, `Floor`, `Ticket`, `Payment`, `ParkingLot`.
- **Verbs (Methods)**: `parkVehicle`, `exitVehicle`, `calculateFee`, `pay`, `findFreeSlot`, `vacate`, `getStatus`.
- **Fee Rules**:
  - **Car**: ₹40 / hour
  - **Bike**: ₹20 / hour
  - **Truck**: ₹80 / hour + 10% surcharge if load weight > 10 tonnes
  - **Duration Rule**: Rounded up to nearest full hour, minimum 1 hour:
    $$\text{hours} = \max\left(1, \lceil \text{durationMs} / 3600000.0 \rceil\right)$$
- **Class Relationships**:
  - `Car`, `Bike`, `Truck` $\rightarrow$ `MotorVehicle` $\rightarrow$ `Vehicle` (*Inheritance*)
  - `Floor` $\rightarrow$ `ParkingSlot[]` (*Composition*)
  - `ParkingLot` $\rightarrow$ `Floor[]` (*Composition*)
  - `ParkingSlot` $\rightarrow$ `Vehicle` (*Association, nullable*)
  - `Ticket` $\rightarrow$ `Vehicle` & `ParkingSlot` (*Association*)
- 🎓 **Viva Question**: *Why is `Vehicle` abstract?*
  - **Answer**: You cannot instantiate an ambiguous "Vehicle" in the physical world; vehicles are always concrete (Car, Bike, Truck). Making it abstract enforces a shared template and method contracts (`calculateFee`, `getVehicleType`) while preventing `new Vehicle()`.

---

### Phase 2: Exceptions and the Vehicle Hierarchy (2 hours)
- **Goal**: Build custom exceptions and the polymorphic vehicle hierarchy with constructor chaining and runtime polymorphism.
- **Custom Exceptions** (all extend `Exception`):
  - `InvalidVehicleException`
  - `DuplicateVehicleException`
  - `LotFullException`
  - `TicketNotFoundException`
  - `PaymentFailedException`
- **Class Hierarchy**:
  - `abstract class Vehicle`: `plateNumber` (String), `ownerName` (String).
    - `public abstract String getVehicleType();`
    - `public abstract double calculateFee(int hours);`
  - `abstract class MotorVehicle extends Vehicle`: intermediate base class.
  - `class Car extends MotorVehicle`:
    - Constructor 1: `Car(plate, owner)` &rarr; calls `this(plate, owner, "Petrol")`
    - Constructor 2: `Car(plate, owner, fuelType)` &rarr; calls `super(plate, owner)`
    - Rate constant: `public static final double HOURLY_RATE = 40.0;`
  - `class Bike extends MotorVehicle`:
    - Rate constant: `public static final double HOURLY_RATE = 20.0;`
  - `class Truck extends MotorVehicle`:
    - Field: `double loadWeightTonnes`
    - Rate constant: `public static final double BASE_RATE = 80.0;`
    - Fee: `BASE_RATE * hours * (loadWeightTonnes > 10.0 ? 1.10 : 1.0)`
- 🎓 **Viva Question**: *Which `calculateFee` runs when `Vehicle v = new Truck(...)` is called, and when is that decided?*
  - **Answer**: `Truck.calculateFee(hours)` runs. Decided at **runtime** via **dynamic method dispatch (runtime polymorphism)** using Java's virtual method table (vtable).

---

### Phase 3: Slot, Floor, and Ticket (2 to 3 hours)
- **Goal**: Manage memory, slot allocation, floor capacity, and ticket generation with static tracking.
- **Classes**:
  - `ParkingSlot`: `slotId` (e.g. "F1-S01"), `VehicleType supportedType`, `Vehicle currentVehicle` (`null` = free).
    - Methods: `isAvailable()`, `assignVehicle(v)`, `vacate()`.
  - `Floor`: `floorNumber`, `ParkingSlot[] slots`.
    - Methods: `findFreeSlot(Vehicle v)`, `getFreeCount()`, `getOccupiedCount()`.
  - `Ticket`:
    - `private static int idCounter = 1000;` (auto-incrementing unique ID)
    - `ticketId`, `Vehicle`, `ParkingSlot`, `entryTimeMillis`, `exitTimeMillis`.
    - Method: `calculateHoursParked(long exitTime)`.
- 🎓 **Viva Question**: *Why is the ticket ID counter `static`?*
  - **Answer**: `static` fields belong to the class, not any individual instance. All `Ticket` instances share a single counter in memory, guaranteeing sequential, unique IDs without external sequence generators.

---

### Phase 4: Strategy Payment Subsystem (1 hour)
- **Goal**: Abstract payment processing using Java interfaces.
- **Interface**:
  ```java
  public interface Payment {
      void pay(double amount) throws PaymentFailedException;
      String getMode();
  }
  ```
- **Implementations**:
  - `CashPayment`: takes `cashTendered`; throws `PaymentFailedException` if `cashTendered < amount`; returns change.
  - `UPIPayment`: takes `upiId`; throws `PaymentFailedException` if `upiId` lacks `'@'`.
- 🎓 **Viva Question**: *Why is `Payment` an interface and not an abstract class?*
  - **Answer**: `Payment` describes a behavioral contract ("can pay") rather than an identity hierarchy ("is a"). Payment types do not share common state (cash has tendered notes, UPI has virtual IDs). Using an interface enables clean polymorphism, follows Interface Segregation, and preserves single inheritance.

---

### Phase 5: Core `ParkingLot` Service (3 to 4 hours)
- **Goal**: The central business logic orchestrator enforcing transactional integrity.
- **Key Methods**:
  - `parkVehicle(Vehicle v)`: checks duplicate &rarr; finds slot &rarr; throws `LotFullException` if none &rarr; issues ticket &rarr; stores active ticket.
  - `exitVehicle(int ticketId, Payment paymentMethod)`:
    - Order is critical:
      1. Calculate fee.
      2. Call `paymentMethod.pay(fee)` **FIRST**.
      3. Only upon successful payment: vacate slot and move ticket to history.
      4. If payment fails: lot remains completely untouched!
  - `getStatus()`: returns a formatted `String`. Zero `System.out.println` calls inside `ParkingLot` (used by both CLI and GUI).
- 🎓 **Viva Question**: *What happens if payment fails halfway through the exit?*
  - **Answer**: Because of the transactional order (collect payment before state mutation), an exception in `pay()` leaves the slot occupied and ticket active. No rollback or inconsistent state occurs.

---

### Phase 6: Resilient Console Menu (2 hours)
- **Goal**: Interactive terminal CLI with robust input validation.
- **Features**:
  - Options: `[1] Park Vehicle`, `[2] Exit Vehicle`, `[3] View Lot Status`, `[4] View Ticket History`, `[5] Exit System`.
  - Defensive `Scanner` handling: `scanner.nextLine()` with `Integer.parseInt()` to avoid scanner hanging bugs.
  - Comprehensive `try/catch` catching domain exceptions with clean user feedback.

---

### Phase 7: Dual File Persistence (3 hours)
- **Goal**: Store system state across restarts using both character and byte streams.
- **Components**:
  - `tickets.txt` (Character Stream): active tickets written as CSV lines using `BufferedReader` and `BufferedWriter`.
  - `ticket_log.dat` (Byte Stream): closed history appended using `FileOutputStream(..., true)` and read with `FileInputStream`.
  - **ID Collision Trap Solution**: Upon reload, reset `Ticket.idCounter = Math.max(highestIdLoaded + 1, Ticket.idCounter)` so new tickets never collide with reloaded tickets.
- 🎓 **Viva Question**: *Why use character streams for `tickets.txt` and byte streams for `ticket_log.dat`?*
  - **Answer**: Character streams (`Reader`/`Writer`) handle human-readable 16-bit Unicode text (ideal for inspectable CSV configurations). Byte streams (`InputStream`/`OutputStream`) handle raw 8-bit binary data (ideal for efficient, compact, append-only transaction logs).

---

### Phase 8: Concurrency & Multithreading (4 hours)
- **Goal**: Concurrent gate access, race condition reproduction, thread synchronization, wait/notify, and daemon threads.
- **Features**:
  - `Gate implements Runnable`: simulates independent physical entry gates concurrently parking cars.
  - **Race Demo**: `parkVehicleUnsafe` with `Thread.sleep(50)` without `synchronized` &rarr; 5 threads race for 1 slot &rarr; collision observed!
  - **Fix**: Synchronize slot allocation on the lot monitor (`synchronized`).
  - **Wait/Notify**: Gates `wait()` when the lot is full; `exitVehicle` calls `notifyAll()` to wake waiting gates.
  - `while (isFull()) wait();` &rarr; explains why `while` is used instead of `if` (spurious wakeups).
  - **Overstay Watcher Daemon**: `setDaemon(true)` thread running periodically with a `volatile boolean running` flag.
- 🎓 **Viva Questions**:
  1. *What exactly is a race condition?*
  2. *What does `synchronized` lock?*
  3. *Why use `while` instead of `if` around `wait()`?*

---

### Phase 9: Swing Graphical User Interface (6 to 8 hours)
- **Goal**: Build a desktop dashboard without violating MVC/Separation of Concerns.
- **Components**:
  - Visual slot grid (`GridLayout` with colored buttons: Green = free, Red = occupied, Orange = overstay).
  - Entry/Exit forms, dropdown combo boxes, receipt modal (`JOptionPane`).
  - Ticket history `JTable`.
  - Role-based views (Attendant vs Admin).
  - Background thread updates safely dispatched via `SwingUtilities.invokeLater()`.
- 🎓 **Viva Question**: *Why is business logic kept strictly out of the UI classes?*
  - **Answer**: Adheres to the Single Responsibility Principle and MVC architecture. The UI is merely a view layer; decoupling it ensures the exact same `ParkingLot` core logic powers the Console CLI, Swing GUI, unit tests, or future Web APIs without duplication.

---

### Phase 10: Polish, Analytics & Clean Code (3 to 4 hours)
- Revenue analytics (daily revenue, vehicle mix, average duration).
- Standard Javadoc on all public APIs.
- Comprehensive `README.md` with build and run instructions.
- Verification of zero compiler warnings.

---

### Phase 11: Pitch & Viva Defense Prep
- Complete mock viva questions cheat sheet.
- Explanation of OOP pillars, SOLID principles, thread safety, and persistence mechanisms.

---

## 📈 Milestone Checklist

| Phase | Description | Status |
| :---: | :--- | :---: |
| **0** | Environment, folders, terminal build, git setup | ⏳ In Progress |
| **1** | Object-Oriented design on paper, nouns/verbs, fee rules | 📝 Next |
| **2** | Exception hierarchy and polymorphic vehicle classes | ⏹️ Pending |
| **3** | Slot, Floor, and auto-incrementing Ticket | ⏹️ Pending |
| **4** | Payment interface, CashPayment, UPIPayment | ⏹️ Pending |
| **5** | Core transactional ParkingLot service | ⏹️ Pending |
| **6** | Interactive terminal CLI menu | ⏹️ Pending |
| **7** | Dual file persistence (CSV + binary streams) | ⏹️ Pending |
| **8** | Multithreading, race demo, synchronization, daemon | ⏹️ Pending |
| **9** | Modern Swing GUI dashboard | ⏹️ Pending |
| **10** | Polish, analytics, Javadoc, README | ⏹️ Pending |
| **11** | Final Viva Voce defense prep | ⏹️ Pending |

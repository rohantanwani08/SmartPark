# 🚗 SmartPark: Object-Oriented Parking Management System

> **B.Tech 2nd Year OOP (Java) Project**  
> **Author**: Rohan Tanwani  
> **Repository**: [https://github.com/rohantanwani08/SmartPark.git](https://github.com/rohantanwani08/SmartPark.git)

---

## 📌 Project Overview
**SmartPark** is a robust, modular, multithreaded parking management system engineered in Java. It demonstrates core and advanced Object-Oriented Programming (OOP) concepts, featuring custom exception hierarchies, dynamic runtime polymorphism, thread-safe concurrent slot allocation, dual-mode file persistence (CSV & binary), and a decoupled Swing GUI dashboard.

---

## 🏛️ System Architecture & Diagrams

### 1. UML Class Diagram
The diagram below models the complete object-oriented structure: inheritance hierarchies, interface realization, compositions, and custom exceptions.

```mermaid
classDiagram
  direction TB
  class Vehicle {
    <<abstract>>
    -String plateNumber
    -String ownerName
    +getVehicleType() String
    +calculateFee(int hours) double
  }
  class MotorVehicle {
    <<abstract>>
    -String fuelType
    +isElectric() boolean
  }
  class Car {
    +calculateFee(int hours) double
  }
  class Bike {
    +calculateFee(int hours) double
  }
  class Truck {
    -double loadCapacity
    +calculateFee(int hours) double
  }
  class Payment {
    <<interface>>
    +pay(double amount) boolean
    +getMode() String
  }
  class CashPayment {
    -double amountTendered
    +pay(double amount) boolean
  }
  class UPIPayment {
    -String upiId
    +pay(double amount) boolean
  }
  class ParkingSlot {
    -String slotId
    -String slotType
    -Vehicle vehicle
    +canFit(Vehicle v) boolean
    +park(Vehicle v) void
    +free() void
  }
  class Floor {
    -int floorNo
    -ParkingSlot[] slots
    +findFreeSlot(Vehicle v) ParkingSlot
    +countFree(String type) int
  }
  class Ticket {
    -int ticketId
    -long entryTime
    -long exitTime
    -double fee
    +getHoursParked() int
    +close(long t, double fee, String mode) void
  }
  class ParkingLot {
    -Floor[] floors
    -ArrayList activeTickets
    -ArrayList history
    -double totalRevenue
    +parkVehicle(Vehicle v) Ticket
    +parkVehicleWait(Vehicle v) Ticket
    +exitVehicle(int id, Payment p) Ticket
    +getStatus() String
  }
  class Gate {
    <<Runnable>>
    -String gateName
    +run() void
  }
  class OverstayWatcher {
    <<Runnable>>
    -volatile boolean running
    +run() void
    +stop() void
  }
  class FileManager {
    +saveActive(List t) void
    +loadActive() List
    +appendLog(Ticket t) void
  }
  class MainFrame {
    <<Swing JFrame>>
    +showDashboard() void
    +onParkClicked() void
    +onExitClicked() void
  }
  class Exception
  class LotFullException
  class InvalidTicketException
  class DuplicateVehicleException
  class PaymentFailedException
  Vehicle <|-- MotorVehicle
  MotorVehicle <|-- Car
  MotorVehicle <|-- Bike
  MotorVehicle <|-- Truck
  Payment <|.. CashPayment
  Payment <|.. UPIPayment
  Exception <|-- LotFullException
  Exception <|-- InvalidTicketException
  Exception <|-- DuplicateVehicleException
  Exception <|-- PaymentFailedException
  ParkingLot "1" *-- "many" Floor
  Floor "1" *-- "many" ParkingSlot
  ParkingLot "1" o-- "many" Ticket
  ParkingLot *-- Gate : inner class
  Ticket --> Vehicle
  Ticket --> ParkingSlot
  ParkingSlot --> Vehicle : holds
  ParkingLot ..> Payment : uses
  FileManager ..> ParkingLot : saves and loads
  OverstayWatcher ..> ParkingLot : monitors
  MainFrame --> ParkingLot : calls
  note for ParkingLot "Throws all four custom exceptions. parkVehicle and exitVehicle are synchronized."
```

---

### 2. Overall Application Flow
Illustrates lifecycle execution: initialization, dual persistence reload, daemon thread startup, authentication, dashboard operations, and graceful shutdown.

```mermaid
flowchart TD
  A([Start app]) --> B["Load tickets.txt and ticket_log.dat<br/>FileManager"]
  B --> C["Rebuild lot: slots, active tickets,<br/>next ticket ID"]
  C --> D["Start OverstayWatcher<br/>daemon thread"]
  D --> E["Login screen"]
  E --> F{"Valid<br/>credentials?"}
  F -- No --> E
  F -- Yes --> G{"Role"}
  G -- Attendant --> H["Dashboard<br/>slot grid and live clock"]
  G -- Admin --> H
  H --> I["Park vehicle"]
  H --> J["Exit vehicle"]
  H --> K["History and search"]
  H --> L["Reports<br/>Admin only"]
  I --> H
  J --> H
  K --> H
  L --> H
  H --> M["Quit"]
  M --> N["Save all data"]
  N --> O["running = false<br/>stop and join threads"]
  O --> P([End])

  subgraph BG["Background thread"]
    direction TB
    W1["Sleep a few seconds"] --> W2["Check every active ticket"]
    W2 --> W3{"Parked longer<br/>than paid time?"}
    W3 -- Yes --> W4["Mark slot orange<br/>via invokeLater"]
    W3 -- No --> W1
    W4 --> W1
  end
  D -.-> W1
  W4 -.-> H

  classDef thread fill:#EEEDFE,stroke:#534AB7,color:#26215C
  classDef endpoint fill:#E1F5EE,stroke:#0F6E56,color:#04342C
  class W1,W2,W3,W4,D thread
  class A,P endpoint
```

---

### 3. Park and Exit Transaction Flow
Details transactional integrity: duplicate checks, slot allocation, fee calculation, payment collection before slot vacation, and inter-thread notifications.

```mermaid
flowchart TD
  subgraph PARK["Park vehicle"]
    direction TB
    P1["Enter plate, owner, type"] --> P2["Create Car, Bike or Truck<br/>object"]
    P2 --> P3["parkVehicle<br/>synchronized"]
    P3 --> P4{"Plate already<br/>parked?"}
    P4 -- Yes --> PE1["DuplicateVehicle<br/>Exception"]
    P4 -- No --> P5{"Free slot of<br/>matching type?"}
    P5 -- No --> PE2["LotFullException"]
    P5 -- Yes --> P6["slot.park, new Ticket,<br/>add to active list"]
    P6 --> P7["Save tickets.txt"]
    P7 --> P8["Show ticket,<br/>refresh grid"]
  end

  subgraph EXIT["Exit vehicle"]
    direction TB
    X1["Enter ticket ID"] --> X2["exitVehicle<br/>synchronized"]
    X2 --> X3{"Active ticket<br/>found?"}
    X3 -- No --> XE1["InvalidTicket<br/>Exception"]
    X3 -- Yes --> X4["hours = ceil of duration,<br/>minimum 1"]
    X4 --> X5["fee = vehicle.calculateFee<br/>polymorphic call"]
    X5 --> X6["Choose Cash or UPI"]
    X6 --> X7{"payment.pay<br/>succeeds?"}
    X7 -- No --> XE2["PaymentFailed<br/>Exception<br/>lot unchanged"]
    X7 -- Yes --> X8["Close ticket, free slot,<br/>move to history, add revenue"]
    X8 --> X9["notifyAll wakes<br/>waiting gates"]
    X9 --> X10["Append ticket_log.dat,<br/>save tickets.txt"]
    X10 --> X11["Show receipt,<br/>refresh grid"]
  end

  classDef err fill:#FCEBEB,stroke:#A32D2D,color:#501313
  classDef ok fill:#E1F5EE,stroke:#0F6E56,color:#04342C
  class PE1,PE2,XE1,XE2 err
  class P8,X11 ok
```

---

## 🛠️ Build and Run Instructions

### Prerequisites
- JDK 17 or JDK 21+ (`javac -version`, `java -version`)
- Git

### Compile & Run from Terminal

#### PowerShell (Windows):
```powershell
# Compile all source files into out directory:
javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })

# Run the application:
java -cp out smartpark.Main
```

#### Bash (Linux/macOS):
```bash
# Compile:
javac -d out $(find src -name "*.java")

# Run:
java -cp out smartpark.Main
```

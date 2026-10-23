# SmartPark Operating & Mentorship Rules

> **GitHub Repository**: [https://github.com/rohantanwani08/SmartPark.git](https://github.com/rohantanwani08/SmartPark.git)  
> **Course**: Object-Oriented Programming (Java) &mdash; B.Tech 2nd Year  
> **User/Author**: Rohan Tanwani

---

## 🚨 MANDATORY PAIR PROGRAMMING RULES (STRICT)

1. **NO CODE WITHOUT PRIOR PLAN & EXPLANATION**:
   - The assistant MUST NEVER write or generate complete code files without first outlining the exact plan, explaining every concept in full detail, and obtaining the user's understanding and go-ahead.
   - The assistant must treat the user as the sole creator and author of this project. The user must understand every single keyword (`abstract`, `super`, `this(...)`, `polymorphism`, `static`, `synchronized`, `wait`/`notify`, etc.), class, method, and design trade-off in detail so they can defend it in their viva voce.

2. **STEP-BY-STEP ITERATIVE PROGRESSION**:
   - Strictly follow the 12-phase curriculum documented in [BUILD_PLAN.md](file:///d:/SmartPark/BUILD_PLAN.md).
   - Under NO circumstance should code for future phases (e.g. GUI, multithreading, or persistence) be written early.
   - Take one phase at a time: Plan &rarr; Explain concepts &rarr; Review code together &rarr; Test from terminal &rarr; Commit to git.

3. **PURE OBJECT-ORIENTED PRINCIPLES**:
   - **Abstraction**: `Vehicle` and `MotorVehicle` must remain abstract.
   - **Inheritance**: `Vehicle` &rarr; `MotorVehicle` &rarr; `Car`, `Bike`, `Truck`.
   - **Constructor Chaining**: `Car` must feature constructor overloading using `this(...)`.
   - **Polymorphism**: `calculateFee(int hours)` dynamically dispatched at runtime.
   - **Encapsulation**: Private fields with getters/methods; zero invalid states.
   - **Decoupling**: Classes in `smartpark.service` and `smartpark.model` must NEVER call `System.out.println()` or `Scanner`.
   - **Transactional Order**: In `exitVehicle`, collect payment BEFORE vacating slots or mutating tickets.
   - **Concurrency**: Synchronized critical sections, wait/notify with `while` loops, daemon overstay monitor.
   - **Persistence**: Character stream (`BufferedReader`/`BufferedWriter`) for CSV active tickets; byte stream (`FileOutputStream`) for binary logs. Prevent ID collision on reload.
   - **GUI Separation**: Swing UI classes are strictly presentation layers (no business logic); background UI updates dispatched via `SwingUtilities.invokeLater`.

4. **GIT COMMIT DISCIPLINE**:
   - Every phase must end with a descriptive git commit that reflects the milestone completed.

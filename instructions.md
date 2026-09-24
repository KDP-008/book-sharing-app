# Task Specification & Benchmark Instructions: Book Waitlist & Hold Reservation System

This document specifies a rigorous, interactive AI evaluation task for the **Book Sharing Application (`book-sharing-app`)**. It complies with both the interactive benchmarking guidelines and the engineering standards in `task_writing_instructions.txt`.

---

## 1. Task Metadata & Engineering Context

| Dimension | Specification |
| :--- | :--- |
| **Domain** | **Data/DB** (Relational Modeling, Constraints, Concurrency, Indexing, SQL/JPQL Aggregations, Auditing) |
| **Task Type** | **Create** (Design and build an enterprise-grade waitlist and reservation subsystem from scratch) |
| **Primary Database** | **PostgreSQL 15** (production & dev profile) with fallback **H2** (fast in-memory test suite) |
| **Codebase Source** | Local Codebase (`book-sharing-app`), Java 21, Spring Boot 4.0, Spring Data JPA, Hibernate 6 |
| **License** | MIT License (Permissive, open source) |
| **Duration & Turns** | **6 to 7 meaningful turns** (sustained execution for **1.5 to 2.5 hours**) |
| **File Scope** | **Touches and modifies $\ge$ 15 files** across models, repositories, services, controllers, DTOs, SQL seeds, and tests |

### Architecture & Module Boundaries
* **Persistence Layer (`com.bsa.repository`, `com.bsa.model`)**: Spring Data JPA repositories with custom JPQL queries, pessimistic row-level locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), composite indexes, and schema-level partial unique constraints.
* **Business Service Layer (`com.bsa.service`)**: Transactional boundaries (`@Transactional`), deterministic queue advancement, expiration sweeps (`@Scheduled`), and compliance audit logging.
* **API / Exposure Layer (`com.bsa.controller`, `com.bsa.dto`)**: REST controllers with fine-grained HTTP status mapping (`409 Conflict` vs `400 Bad Request`), immutable Java `record` contracts, and OpenAPI / Swagger annotations.
* **Data Initialization (`src/main/resources`)**: SQL schema seeding (`data.sql`) across PostgreSQL and H2.

### Reusable Conventions & Utilities
* Constructor-based dependency injection throughout all Spring beans (no field `@Autowired`).
* Immutable DTOs modeled strictly as Java `record` types with Swagger `@Schema` annotations.
* Entity lifecycle safety: `@JsonIgnore` on bidirectional relations; strict encapsulation on status fields to avoid uncoordinated state changes; append-only design on compliance records.
* Testing standards: `@SpringBootTest` with `@DirtiesContext` for transactional state isolation, standalone `MockMvc` for controller testing, and multithreaded latch synchronization for race-condition testing.

---

## 2. File Scope: Required Reads & Edits (15+ Files)

To satisfy the engineering breadth requirement, the agent must read, edit, or create across at least **15 distinct files**:

| # | File Path | Role in Task |
| :-: | :--- | :--- |
| 1 | `pom.xml` | Verifies PostgreSQL runtime driver dependency and build plugins. |
| 2 | `src/main/resources/application.properties` | Configures datasource, dialect defaults, DDL auto, and SQL initialization modes. |
| 3 | `src/main/resources/application-postgres.properties` | **[NEW/CONFIG]** PostgreSQL 15 profile properties for local or containerized execution. |
| 4 | `src/main/resources/data.sql` | **[MODIFY]** Seeds realistic waitlist entries matching existing users and checked-out books. |
| 5 | `src/main/java/com/bsa/BookSharingApplication.java` | **[MODIFY]** Enables Spring Boot background task execution with `@EnableScheduling`. |
| 6 | `src/main/java/com/bsa/model/Book.java` | **[MODIFY]** Adds bidirectional reservation relationship and availability lifecycle binding. |
| 7 | `src/main/java/com/bsa/model/User.java` | **[MODIFY]** Links users to their historical and active reservations. |
| 8 | `src/main/java/com/bsa/model/Reservation.java` | **[NEW]** Core entity with partial unique constraint (`active_flag`), composite indexes, and timestamps. |
| 9 | `src/main/java/com/bsa/model/ReservationStatus.java` | **[NEW]** Lifecycle enum (`WAITING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`, `EXPIRED`). |
| 10 | `src/main/java/com/bsa/model/ReservationAuditLog.java` | **[NEW]** Immutable compliance log entity with raw IDs decoupled from JPA cascades. |
| 11 | `src/main/java/com/bsa/model/ReservationAuditEvent.java` | **[NEW]** Transition event enum (`CREATED`, `PROMOTED`, `CLAIMED`, `CANCELLED`, `EXPIRED`). |
| 12 | `src/main/java/com/bsa/repository/BookRepository.java` | **[MODIFY]** Implements pessimistic write locking (`findByIdForUpdate`) for concurrent return serialization. |
| 13 | `src/main/java/com/bsa/repository/BorrowingRecordRepository.java` | **[MODIFY]** Adds lookup for active borrowing records by book and borrower. |
| 14 | `src/main/java/com/bsa/repository/ReservationRepository.java` | **[NEW]** Scalar count queries with tie-breaking, JPQL `GROUP BY` aggregations, and projections. |
| 15 | `src/main/java/com/bsa/repository/ReservationAuditLogRepository.java` | **[NEW]** Historical audit log queries ordered chronologically. |
| 16 | `src/main/java/com/bsa/service/BookService.java` | **[MODIFY]** Implements `returnBook` hook with pessimistic book locking and queue handoff. |
| 17 | `src/main/java/com/bsa/service/ReservationService.java` | **[NEW]** Core reservation logic, FIFO queueing, hold expiration, and audit logging. |
| 18 | `src/main/java/com/bsa/controller/ReservationController.java` | **[NEW]** REST API endpoints with distinct `409 Conflict` and `400 Bad Request` exception handling. |
| 19 | `src/main/java/com/bsa/dto/BookWaitlistDemand.java` | **[NEW]** Projection record for top waitlisted books query. |
| 20 | `src/main/java/com/bsa/dto/WaitlistAnalytics.java` | **[NEW]** Aggregate dashboard DTO with status breakdown. |
| 21 | `src/test/java/com/bsa/service/ReservationServiceTest.java` | **[NEW]** Exhaustive unit and integration tests for FIFO promotion, cancellation, and metrics. |
| 22 | `src/test/java/com/bsa/service/ReservationConcurrencyTest.java` | **[NEW]** Multithreaded race-condition test suite verifying database locks and schema constraints. |
| 23 | `src/test/java/com/bsa/controller/ReservationControllerTest.java` | **[NEW]** MockMvc web layer tests verifying HTTP status codes and payload contracts. |

---

## 3. The Engineering Bar: Naive vs. Expert Solutions

Two competent models can implement the prompt and pass automated tests, yet differ vastly in architectural quality, scalability, and safety. 

### Why Automated Tests Won't Catch a Naive Solution:
1. **In-Memory Aggregations (The Stream Anti-Pattern)**:
   * *Naive*: Calls `reservationRepository.findAll()` and aggregates status counts or rankings using Java `Stream.collect(Collectors.groupingBy(...))`. Passes in-memory unit tests with 10 records, but causes fatal memory bloat and table locks on a production PostgreSQL database with 100,000 records.
   * *Expert*: Writes pure JPQL/SQL constructor expressions (`SELECT new com.bsa.dto.BookWaitlistDemand(...) ... GROUP BY ...`) pushing execution to PostgreSQL, and trims results using Spring Data `Pageable` (`LIMIT / FETCH FIRST`).
2. **Schema-Level Constraints vs. Application-Level Checks**:
   * *Naive*: Performs duplicate checks via Java `if (reservationRepository.findByUserAndBook(...).isPresent())`. In concurrent environments, two simultaneous requests slip past the `if` check and insert duplicate active reservations. Alternatively, placing an unconditional `@UniqueConstraint(columnNames={"book_id", "user_id"})` breaks the ability for a user to re-reserve a book they previously returned or cancelled.
   * *Expert*: Implements a database-level partial unique constraint. In PostgreSQL/standard SQL, uses an `active_flag` column (`Boolean.TRUE` for active, `NULL` for past records) with `@UniqueConstraint(name = "uk_reservations_active_per_user", columnNames = {"book_id", "user_id", "active_flag"})`, exploiting the standard SQL rule that `NULL` values are distinct.
3. **Pessimistic Concurrency & Stale Reads**:
   * *Naive*: Performs book returns and queue promotions in standard unversioned read transactions, or relies on Java `synchronized` keywords (which do not work in clustered environments).
   * *Expert*: Issues a database-level row lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)` $\rightarrow$ `SELECT ... FOR UPDATE` in PostgreSQL) on the `Book` record. Uses scalar ID projections (`findBookIdById`) before locking to avoid loading a stale entity into Hibernate's first-level persistence context.
4. **Queue Position Calculation**:
   * *Naive*: Loads all waiting reservation entities for a book into a `List<Reservation>` and computes `list.indexOf(res) + 1`.
   * *Expert*: Executes a scalar query (`SELECT COUNT(r) + 1 FROM Reservation r ...`) with deterministic tie-breaking `(r.createdAt < :createdAt OR (r.createdAt = :createdAt AND r.id < :id))` backed by a composite index `(book_id, status, created_at)`.
5. **Cascade Decoupling on Audit Records**:
   * *Naive*: Maps `ReservationAuditLog` with `@ManyToOne` entity references. Deleting a test book or cascading a user update mutates or purges legal compliance history.
   * *Expert*: Stores raw foreign keys (`bookId`, `userId`, `reservationId`) with zero entity setters, guaranteeing immutable append-only persistence.

---

## 4. PostgreSQL 15 Setup & Configuration

PostgreSQL 15 is installed locally via Homebrew at `/opt/homebrew/opt/postgresql@15/bin/psql`.

### Starting PostgreSQL Service
```bash
# Start local Homebrew service
brew services start postgresql@15

# Create the database (one-time setup)
/opt/homebrew/opt/postgresql@15/bin/createdb bookshare_db
```

*(Alternatively, run via Docker: `docker run -d --name bsa-postgres -e POSTGRES_DB=bookshare_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15`)*

### Activating the PostgreSQL Profile
Run the application with the `postgres` Spring profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```
The file `src/main/resources/application-postgres.properties` configures the `PostgreSQLDialect`, connection URL `jdbc:postgresql://localhost:5432/bookshare_db`, and automatic table synchronization (`ddl-auto=update`).

---

## 5. Interactive Turn-by-Turn Instruction Prompts

> [!IMPORTANT]
> The prompts below must be delivered sequentially, one turn at a time. Each prompt is written in a natural teammate voice under 100 words, with clear goals and open implementation details. Do not paste headings, acceptance criteria, or numbered steps into the chat prompt.

---

### Turn 1: Core Waitlist & Hold Domain (DB & Service Layer)
* **Prompt to Send**:
  ```text
  We need a waitlist and reservation system for books that are currently checked out. Right now if a book isn't available, users are just blocked from borrowing it. We want users to be able to reserve unavailable books, queue up, and claim them when they get returned. Can you build this out in the database and service layer?
  ```
* **Evaluation Objectives**:
  * Scaffolds `Reservation` entity and `ReservationStatus` enum (`WAITING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`, `EXPIRED`).
  * Implements `ReservationRepository` and `ReservationService` (`reserveBook`, `cancelReservation`, `claimReservation`, `promoteNextInQueue`).
  * Hooks into `BookService.returnBook(...)` to trigger queue promotion when an active loan is returned.
  * **Scope Discipline Check**: Model must stay strictly in the database and service layer; penalize if it jumps ahead to create REST controllers unprompted.

---

### Turn 2: Schema Constraints, Indexing & Pessimistic Locking
* **Prompt to Send**:
  ```text
  The service flow looks solid, but looking at the database layer, the duplicate active reservation check is only handled in Java code and there aren't any table indexes defined. Can we enforce the unique active reservation rule at the database schema level, add composite indexes for our frequent queue lookups (book + status + createdAt), and ensure we have proper pessimistic locking when returning books to prevent race conditions during queue promotion?
  ```
* **Evaluation Objectives**:
  * **Relational Constraint**: Must not apply a naive unique constraint on `(book_id, user_id)` (which breaks repeat loans). Must implement a partial unique constraint (e.g. `active_flag` with `NULL` for past records).
  * **Composite Indexing**: Declares `@Index(name = "idx_reservations_book_status_created", columnList = "book_id, status, created_at")`.
  * **Row-Level Locking**: Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) in `BookRepository` before queue promotion.
  * **Proactivity Check**: Check if the model notices that `@Scheduled` expiration sweeps require `@EnableScheduling` in `BookSharingApplication.java`.
  * **Concurrency Verification**: Writes multithreaded test cases verifying constraint trips and lock serialization.

---

### Turn 3: Scalar Queue Positioning & Seed Data in `data.sql`
* **Prompt to Send**:
  ```text
  This looks great. But right now users can't see where they stand in line without loading all waiting entities into memory. Can you add efficient database queries to get a user's current queue position and the total waitlist depth for a book? Also, our data.sql doesn't have any reservations yet—can you seed sample waitlist entries for our checked-out books so we can test with dev data?
  ```
* **Evaluation Objectives**:
  * **Scalar Count Query**: Implements `countAheadInQueue` using `COUNT(r)` with tie-breaking `(r.createdAt < :createdAt OR (r.createdAt = :createdAt AND r.id < :id))` rather than loading entities into memory.
  * **Data Seeding**: Seeds `src/main/resources/data.sql` with valid reservation rows.
  * **Integrity Check**: Verifies that seeded records respect foreign key relationships, exclude book owners and active borrowers from the waitlist, set `active_flag = TRUE`, and use ascending chronological timestamps.

---

### Turn 4: Database-Level Aggregations & Projections
* **Prompt to Send**:
  ```text
  The queue counts work nicely. Now we need waitlist demand metrics for the library dashboard. Could you add a reporting method in the service that returns waitlist analytics—specifically the top most-requested books with their waitlist count, and a breakdown of reservation totals by status? Let's make sure these aggregations run directly in the database with JPQL/SQL GROUP BY queries rather than in Java memory.
  ```
* **Evaluation Objectives**:
  * **Root Cause Compliance**: Rejects in-memory stream filtering (`Collectors.groupingBy`). Writes pure JPQL constructor expressions:
    `SELECT new com.bsa.dto.BookWaitlistDemand(r.book.id, r.book.title, r.book.author, COUNT(r)) FROM Reservation r ... GROUP BY ...`
  * **Native SQL Limit**: Uses `Pageable` (`PageRequest.of(0, topBooksLimit)`) so PostgreSQL trims rows at the database level.
  * **Clean Projections**: Declares immutable Java `record`s in `com.bsa.dto`.
  * **Complete Enums**: Pre-populates zero defaults using an `EnumMap` so unrepresented statuses are returned as `0L`.

---

### Turn 5: REST API Exposure & MockMvc Verification
* **Prompt to Send**:
  ```text
  Now that the repository and service logic are complete, we need a dedicated REST controller so our frontend can interact with reservations and fetch the analytics. Could you add the endpoints for reserving, claiming, cancelling, checking queue position, and fetching analytics, ensuring database constraint violations and state conflicts return clean 409/400 responses? Please add MockMvc controller tests as well.
  ```
* **Evaluation Objectives**:
  * **REST Controller**: Implements `ReservationController` with routes for reserve, claim, cancel, queue-position, and analytics.
  * **Status Code Mapping**: Maps `IllegalArgumentException` $\rightarrow$ `400 Bad Request`; maps `IllegalStateException` and `DataIntegrityViolationException` $\rightarrow$ `409 Conflict`.
  * **Documentation**: Decorates endpoints with OpenAPI `@Tag` and `@Operation`.
  * **MockMvc Testing**: Implements comprehensive standalone `MockMvc` tests covering success and error paths.

---

### Turn 6: Immutable Compliance Audit Trail
* **Prompt to Send**:
  ```text
  We also need an audit trail for compliance. Whenever a reservation changes status (like when it's queued, promoted to ready, claimed, cancelled, or expired), we want an immutable database log in a new table tracking the reservation id, book id, user id, old status, new status, timestamp, and trigger event. Could you build this audit entity, repository, and service logging, and expose a query to view a reservation's audit history?
  ```
* **Evaluation Objectives**:
  * **Audit Entity**: Creates `ReservationAuditLog` with no setters and raw IDs (`reservationId`, `bookId`, `userId`) to decouple compliance records from JPA cascade deletions.
  * **Index Optimization**: Indexes `(reservation_id, changed_at)` for historical chronological queries.
  * **Transactional Safety**: Hooks `recordAudit(...)` into every status transition (`CREATED`, `PROMOTED`, `CLAIMED`, `CANCELLED`, `EXPIRED`) within the same transaction.
  * **Endpoint**: Exposes `GET /reservations/{reservationId}/audit-log`.

---

### Turn 7: Operational Batch Scalability & Sweep Processing
* **Prompt to Send**:
  ```text
  The audit logging works great. Looking at operational scalability, our hourly stale expiration sweep currently processes all expired holds in a single transaction. Can we refactor it to process in configurable batches with an admin endpoint (POST /reservations/process-expired) that reports the count of expired holds, and add a scale test verifying that expiring multiple stale holds correctly cascades promotions and audit logs?
  ```
* **Evaluation Objectives**:
  * **Transaction Chunking**: Refactors the monolithic scheduled sweep so individual hold expirations or small batches commit independently, eliminating table lock contention on PostgreSQL.
  * **Admin Trigger Route**: `POST /reservations/process-expired?batchSize=50` returning the processed count.
  * **Automated Scale Testing**: Validates that multi-item expirations trigger queue promotions and audit records without deadlocks.

---

## 6. Standard Scoring Rubric for Human Evaluators

| Rating Dimension | 5 (Exceptional) | 3 (Adequate) | 1 (Unacceptable) |
| :--- | :--- | :--- | :--- |
| **Task Success** | Fully operational waitlist, concurrency row locks, analytics, REST endpoints, and decoupled audit trail. All 32+ tests pass. | Functional core flow, but missing edge-case handling or lacks automated test coverage. | Code fails to compile, breaks existing checkout, or abandons the core reservation loop. |
| **Code Quality** | Idiomatic Java 21 / Spring Boot 4.0; immutable DTO records; fine-grained transactions; defensive entity encapsulation. | Works, but uses mutable `@Data` on compliance logs, misses indexes, or leaves loose entity bindings. | Spaghettified logic, unhandled exceptions, raw string concatenations in SQL queries. |
| **Instruction Following** | Strictly honors scope constraints; avoids in-memory Java Stream grouping shortcuts; uses SQL-native `GROUP BY` and scalar counts. | Requires multiple prompts to stop using in-memory streams; occasionally misses requested HTTP status codes. | Persistently ignores instructions; adds unrequested microservice layers or external queue brokers. |
| **Thoroughness & Autonomy** | Proactively identifies missing configurations (e.g. `@EnableScheduling`); engineers multithreaded latch race tests without hand-holding. | Implements requested features only when explicitly told what files and annotations to use. | Leaves `// TODO` stubs, writes empty mock tests, or gives up when encountering SQL dialect errors. |
| **Interaction Quality** | Crisp, professional communication; zero loops, refusals, or sycophantic preambles. | Conversational but occasionally repeats explanations or requires redundant prompting. | Loops into tool errors, crashes, or refuses reasonable implementation requests. |

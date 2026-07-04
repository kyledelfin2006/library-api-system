# Library API System

> A Spring Boot REST API for managing a library's book catalog — CRUD operations, search, budget filtering, sorting, and stats, backed by a lightweight flat-file persistence layer.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![JUnit](https://img.shields.io/badge/Tests-JUnit%205%20%2B%20Mockito-red)
![Status](https://img.shields.io/badge/status-working-brightgreen)

---

## Table of Contents

* [Architecture Overview](#architecture-overview)
* [File Structure](#file-structure)
* [Core Design Patterns](#core-design-patterns)
* [Key Features](#key-features)
* [Workflow & Lifecycle](#workflow--lifecycle)
* [Code Highlights](#code-highlights)
* [Setup & Installation](#setup--installation)
* [Troubleshooting](#troubleshooting)
* [Data Management](#data-management)
* [Upcoming Improvements](#upcoming-improvements)
* [License](#license)

---

## Architecture Overview

The application is a **Spring Boot** service built around a strict **layered architecture**. Each layer has one job and only ever talks to the layer directly beneath it — nothing reaches "down" more than one level, and nothing reaches "up" at all.

```
┌───────────────────────────────────────────┐
│            LibraryAPI (Controller)         │  @RestController — HTTP routing, status codes, request/response mapping
└───────────────────────┬─────────────────────┘
                        │  BookDTO in / Book out
┌───────────────────────▼─────────────────────┐
│          LibraryManager (Service)           │  @Service — validation, business rules, search/sort/stats logic
└───────────────────────┬─────────────────────┘
                        │
          ┌──────────────┴───────────────┐
          │                              │
┌──────────▼──────────┐        ┌──────────▼──────────┐
│   BookRepository     │        │     BookStorage      │  @Repository / @Component
│  (in-memory list)    │◄──────►│ (flat-file persistence)│  synced on every mutation
└───────────────────────┘        └───────────────────────┘
```

Key architectural facts:

* **Dependency injection is constructor-based.** `LibraryAPI` receives `LibraryManager`; `LibraryManager` receives `BookRepository` and `BookStorage`. Spring wires all of this automatically via its IoC container — there is no manual `new` for these collaborators anywhere in application code.
* **The in-memory repository is the source of truth at runtime.** `BookRepository` holds a `List<Book>` in memory. `BookStorage` is a durability layer underneath it — it doesn't replace the repository, it mirrors it to disk so state survives a restart.
* **Global error handling is centralized.** A single `@RestControllerAdvice` class (`GlobalExceptionHandler`) intercepts exceptions thrown anywhere in the stack and converts them into a consistent JSON error shape, so no controller method needs its own try/catch.
* **The `pom.xml` includes Spring Data JPA, H2, and PostgreSQL dependencies**, but no `@Entity`, `@Repository extends JpaRepository`, or `application.properties` datasource configuration exists in the current codebase. The actual persistence mechanism is the custom flat-file `BookStorage` class described below — the JPA/DB dependencies are present but currently unused (see [Upcoming Improvements](#upcoming-improvements)).

---

## File Structure

```
src/
├── main/
│   └── java/
│       └── api/
│           ├── controller/
│           │   └── LibraryAPI.java            # @RestController — defines all /api routes
│           ├── exceptions/
│           │   ├── BookNotFoundException.java # Thrown when a lookup by ID fails
│           │   ├── GlobalExceptionHandler.java# @RestControllerAdvice — central error → JSON mapping
│           │   └── StorageException.java      # Unchecked wrapper for file I/O failures
│           ├── manager/
│           │   └── LibraryManager.java        # @Service — validation, CRUD orchestration, search/sort/stats
│           ├── models/
│           │   ├── Book.java                  # Domain entity — validates itself on construction
│           │   └── BookDTO.java                # Request payload — Jakarta Bean Validation annotations
│           ├── repository/
│           │   ├── BaseRepository.java         # Generic contract: add, remove, getAll, clear
│           │   └── BookRepository.java         # @Repository — synchronized in-memory List<Book>
│           ├── responses/
│           │   ├── ApiResponse.java             # Standard success envelope
│           │   └── ErrorResponse.java           # Standard error envelope
│           ├── storage/
│           │   ├── Storage.java                 # Generic contract: save, load
│           │   └── BookStorage.java             # @Component — pipe-delimited flat-file persistence
│           ├── util/
│           │   └── BookIDGenerator.java         # Thread-safe, zero-padded sequential ID generator
│           └── LibraryApplication.java          # @SpringBootApplication entry point
└── test/
    └── java/
        └── testAPI/
            ├── BookTest.java                    # Unit tests for Book's self-validation
            └── LibraryManagerTest.java          # Mockito-based tests for LibraryManager's business logic
.gitignore
pom.xml
README.md
```

---

## Core Design Patterns

**DTO Pattern — `BookDTO` vs. `Book`**
`BookDTO` is the shape a client sends in a request body; `Book` is the shape the system actually stores and returns. They're kept separate for one important reason: a client should never be able to set an `id` directly. IDs are always generated server-side by `BookIDGenerator`. `BookDTO` is also reused for `PATCH` requests, where every field is optional (`title`, `author`, `genre` are `String`, `price` is a boxed `Double` rather than a primitive, specifically so `null` can mean "not provided").

**Repository Pattern — `BaseRepository<T>`**
`BaseRepository<T>` defines a generic contract (`add`, `remove`, `getAll`, `clear`) that `BookRepository` implements against an in-memory `ArrayList<Book>`. Because the manager only depends on the interface, the storage mechanism behind it could be swapped for a database-backed implementation later without touching `LibraryManager`.

**Storage Abstraction — `Storage<T>`**
Similarly, `Storage<T>` defines `save(List<T>)` and `load()`. `BookStorage` is the only implementation today, writing to a pipe-delimited flat file. Like the repository, this is swappable behind the interface.

**Centralized Exception Handling — `@RestControllerAdvice`**
Rather than wrapping every controller method in try/catch, `GlobalExceptionHandler` declares one `@ExceptionHandler` method per exception type (`IllegalArgumentException`, `HttpMessageNotReadableException`, `MethodArgumentNotValidException`, `NoHandlerFoundException`, `HttpRequestMethodNotSupportedException`, `StorageException`, `BookNotFoundException`, and a catch-all `Exception`). Each maps to a specific HTTP status and a consistent `ErrorResponse` JSON body.

**Standardized Response Envelope**
Every successful response is wrapped in `ApiResponse<T>` (`success`, `message`, `data`, `timestamp`). Every error response is wrapped in `ErrorResponse` (`error`, `details`, `statusCode`, `timestamp`). Clients always know what shape to expect, regardless of which endpoint they hit or whether the call succeeded.

**Self-Validating Domain Model**
`Book`'s all-args constructor validates its own fields (non-blank strings, positive price) independently of `LibraryManager`'s validation. This means `Book` can never exist in an invalid state, even if a caller bypasses the manager layer entirely — validation isn't solely the controller/service layer's responsibility.

**Thread-Safe ID Generation**
`BookIDGenerator` uses a static `AtomicInteger` and `getAndIncrement()`, making the "read current value, then increment" operation atomic. This guarantees no two concurrent `POST /api/books` requests can ever be assigned the same ID.

**Lifecycle Hooks for Persistence — `@PostConstruct` / `@PreDestroy`**
`LibraryManager.init()` (annotated `@PostConstruct`) loads persisted data into the repository the moment the Spring context finishes constructing the bean. `LibraryManager.destroy()` (annotated `@PreDestroy`) flushes the repository back to disk right before the application shuts down. This ties persistence directly to the Spring bean lifecycle rather than to individual request handling.

---

## Key Features

* **Full CRUD** on books: create (`POST`), read (single or all, `GET`), partial update (`PATCH`), delete (`DELETE`).
* **Search** by `author`, `title`, or `genre` using case-insensitive partial (`contains`) matching, or by `price` using an exact match with a `0.0001` floating-point tolerance.
* **Budget filtering** — retrieve every book at or below a given maximum price.
* **Sorting** by `title`, `author`, `genre`, `price`, or `id`, defaulting to `title` for missing or unrecognized fields.
* **Library statistics** — total book count, total inventory value, and the single most expensive book in one call.
* **Bean-validated input** — `BookDTO` uses `@NotBlank`, `@Size`, and `@Positive` from Jakarta Validation, enforced automatically via `@Valid` on the `POST` endpoint.
* **Partial-update semantics on `PATCH`** — only fields that are present and non-blank/non-null in the request body are applied; omitted fields are left untouched.
* **Deliberate HTTP status codes** — `200` for reads/updates, `201` for creation, `400` for bad input, `404` for missing resources, `405` for unsupported methods, `500` for unexpected or storage failures.
* **Durable persistence without a database** — every mutating operation (`add`, `patch`, `delete`) immediately writes the full in-memory list to a flat file, and the file is reloaded automatically on the next startup.
* **Health check endpoint** for uptime/monitoring probes.

---

## Workflow & Lifecycle

### Application startup

1. `LibraryApplication.main()` boots the Spring context via `SpringApplication.run(...)`.
2. Spring instantiates `BookRepository`, `BookStorage`, and `LibraryManager` (constructor injection resolves the dependency graph).
3. Immediately after `LibraryManager` is constructed, its `@PostConstruct`-annotated `init()` method runs:
   * `BookStorage.load()` reads the flat file line by line into `List<Book>`.
   * `LibraryManager` scans every loaded book's ID, finds the highest numeric value, and calls `BookIDGenerator.setNextId(maxId + 1)` — this is what makes IDs continue from where they left off instead of resetting to `0000` on every restart.
   * The loaded books are pushed into `BookRepository` via `addAll(...)`.
4. `LibraryAPI` becomes ready to accept HTTP traffic on the embedded servlet container (default port `8080`).

### A single request, end to end — `POST /api/books`

```
1. Client sends:
   POST /api/books
   Content-Type: application/json
   Body: {"title":"1984","author":"George Orwell","genre":"Dystopian","price":15.99}

2. LibraryAPI.addBook(@Valid @RequestBody BookDTO input)
   → Spring's @Valid triggers Jakarta Bean Validation against BookDTO's constraints
   → A failure here short-circuits into MethodArgumentNotValidException,
     caught by GlobalExceptionHandler → 400 response, request never reaches the manager

3. LibraryManager.addBook(input)
   → validateBookInput(input) re-checks title/author/genre non-blank and price > 0
     (defense in depth beyond the DTO's own annotations)
   → A failure here throws IllegalArgumentException → 400 via GlobalExceptionHandler

4. new Book(title, author, genre, price)
   → Book's constructor validates its own fields a second time
   → BookIDGenerator.generateNextID() atomically assigns the next zero-padded ID (e.g. "0004")

5. BookRepository.add(newBook)
   → Appends to the internal synchronized ArrayList<Book>

6. LibraryManager.saveToStorage()
   → BookStorage.save(repository.getAll()) rewrites the entire flat file from the
     current in-memory state
   → An I/O failure here is wrapped in StorageException → 500 via GlobalExceptionHandler

7. LibraryAPI returns:
   HTTP 201 Created
   {"success":true,"message":"Book Added Successfully","data":{...},"timestamp":...}
```

### Application shutdown

When the Spring context closes (normal shutdown, `Ctrl+C`, etc.), `LibraryManager.destroy()` — annotated `@PreDestroy` — runs `saveToStorage()` one final time, guaranteeing the flat file reflects the latest in-memory state even if the last mutating request didn't trigger a save for some reason.

---

## Code Highlights

**Atomic, gap-free ID generation** (`BookIDGenerator.java`)
```java
private static final AtomicInteger nextId = new AtomicInteger(0);

public static String generateNextID() {
    int id = nextId.getAndIncrement();  // READ + WRITE as ONE operation
    return String.format("%04d", id);
}
```
`getAndIncrement()` is a single atomic CPU-level operation, so it's safe under concurrent requests without any explicit `synchronized` block.

**Null-safe partial updates** (`LibraryManager.patchBook`)
```java
if (updates.getTitle() != null && !updates.getTitle().trim().isEmpty()) {
    existing.setTitle(updates.getTitle());
}
// ...repeated per field...
if (updates.getPrice() != null) {
    if (updates.getPrice() <= 0) {
        throw new IllegalArgumentException("Price must be greater than 0");
    }
    existing.setPrice(updates.getPrice());
}
```
Each field is independently optional. A `PATCH` body containing only `{"price": 12.99}` updates just the price and leaves everything else exactly as it was.

**Type-driven partial/exact search matching** (`LibraryManager.bookMatches`)
```java
switch (type) {
    case "author":  return book.getAuthor().toLowerCase().contains(value);
    case "title":   return book.getTitle().toLowerCase().contains(value);
    case "genre":   return book.getGenre().toLowerCase().contains(value);
    case "price":
        try {
            double priceValue = Double.parseDouble(value);
            return Math.abs(book.getPrice() - priceValue) < 0.0001;
        } catch (NumberFormatException e) {
            return false;
        }
    default: return false;
}
```
Text fields use `contains()` for forgiving partial matches; price uses a small epsilon tolerance instead of exact `==` comparison, since floating-point equality is unreliable.

**Escaping the delimiter in flat-file storage** (`BookStorage.java`)
```java
private String escape(String s) {
    return s.replace("|", "\\|").replace("\n", "\\n");
}
private String unescape(String s) {
    return s.replace("\\|", "|").replace("\\n", "\n");
}
```
Because records are stored as `id|title|author|genre|price`, any `|` or newline character inside a title/author/genre is escaped on write and restored on read so it can't be mistaken for a field separator.

**Graceful handling of a malformed or empty data file** (`BookStorage.load`)
```java
if (!file.exists() || file.length() == 0) {
    return new ArrayList<>();
}
// ...
if (parts.length != 5) {
    System.err.println("Skipping malformed line: " + line);
    continue;
}
```
A missing or empty file simply yields an empty library rather than an exception. Individual malformed lines are logged and skipped rather than crashing the whole load.

**One exception type per failure mode, one handler each** (`GlobalExceptionHandler.java`)
Seven distinct `@ExceptionHandler` methods translate `IllegalArgumentException`, `HttpMessageNotReadableException`, `MethodArgumentNotValidException`, `NoHandlerFoundException`, `HttpRequestMethodNotSupportedException`, `StorageException`, and `BookNotFoundException` into the correct status code and a uniform `ErrorResponse`, with a final catch-all `Exception` handler as a safety net for anything unanticipated.

---

## Setup & Installation

### Requirements

* **Java 21+** (the `pom.xml` sets `maven.compiler.source`/`target` to `21`; note that `<java.version>` is set to `25`, which is inconsistent with the compiler settings — see [Troubleshooting](#troubleshooting))
* **Maven** (or use the Maven Wrapper if one is added to the repo)
* Internet access on first build, since Maven needs to resolve dependencies from Maven Central (including transitive dependencies pulled in by `spring-boot-starter-parent`)

### Steps

```bash
# Clone the repository
git clone https://github.com/kyledelfin2006/library-api-system.git
cd library-api-system

# Build the project (downloads dependencies, compiles, runs tests)
mvn clean install

# Run the application
mvn spring-boot:run
```

The API starts on **`http://localhost:8080`** using Spring Boot's embedded servlet container. On first write, a `books.txt` file is created automatically in the working directory (see [Data Management](#data-management) for how to change this path).

### Quick smoke test

```bash
curl http://localhost:8080/api/health
# {"success":true,"message":"API is running",...}
```

### API Endpoint Reference

| Method | Endpoint | Status Codes | Description |
|--------|----------|---------------|--------------|
| `GET` | `/api/health` | 200 | Liveness check |
| `GET` | `/api/books` | 200 | Get all books |
| `GET` | `/api/books/{id}` | 200, 404 | Get a single book by ID |
| `POST` | `/api/books` | 201, 400 | Add a new book (validated body) |
| `PATCH` | `/api/books/{id}` | 200, 400, 404 | Partially update a book |
| `DELETE` | `/api/books/{id}` | 200, 404 | Delete a book by ID |
| `GET` | `/api/books/search?type=&value=` | 200, 400 | Search by `author`, `title`, `genre`, or `price` |
| `GET` | `/api/books/budget?maxPrice=` | 200, 400 | Books at or below a given price |
| `GET` | `/api/books/sorted?by=` | 200 | Sort by `title`, `author`, `genre`, `price`, or `id` |
| `GET` | `/api/books/stats` | 200 | Total books, total value, most expensive book |

### Example requests

```bash
# Add a book
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"1984","author":"Orwell","genre":"Fiction","price":15.99}'

# Partially update just the price
curl -X PATCH http://localhost:8080/api/books/0000 \
  -H "Content-Type: application/json" \
  -d '{"price":12.99}'

# Search by author (partial, case-insensitive)
curl "http://localhost:8080/api/books/search?type=author&value=Orwell"

# Books within budget
curl "http://localhost:8080/api/books/budget?maxPrice=20"

# Sorted by price
curl "http://localhost:8080/api/books/sorted?by=price"

# Library-wide stats
curl http://localhost:8080/api/books/stats
```

### Running the test suite

```bash
mvn test
```

**`BookTest`** — verifies `Book`'s self-validation: a valid book is constructed correctly with a generated 4-digit ID; empty title, null author, negative price, and zero price all throw `IllegalArgumentException`; two sequentially created books receive distinct IDs.

**`LibraryManagerTest`** — a Mockito-based suite (`@ExtendWith(MockitoExtension.class)`) with mocked `BookRepository` and `BookStorage`, covering: adding valid/invalid books, retrieving all books, finding by ID (existing/non-existing), deleting (existing/non-existing), patching (field-level updates, price validation, non-existing ID), budget filtering, search across all four types, sorting across all five fields (including invalid-field fallback), total value calculation, most-expensive-book lookup, and direct testing of the `bookMatches` matching logic.

**`BookIDGeneratorTest`** — a JUnit 5 test suite covering basic ID generation (first ID is `"0000"`, sequential increments, and manual resetting), plus rigorous concurrency validation with `ExecutorService` and `CountDownLatch` to prove thread‑safety. The suite includes stress tests with up to 50 threads generating thousands of IDs with zero duplicates, and a resilience test that randomly resets the generator during concurrent access to ensure no exceptions or corruption occur. All tests confirm that `AtomicInteger` correctly handles concurrent increments and resets, guaranteeing unique, zero‑padded IDs for every book even under heavy multi‑threaded load.

---

## Troubleshooting

| Symptom | Likely Cause | Resolution |
|---------|--------------|------------|
| `400 Bad Request` on `POST /api/books` even with a JSON body | A required field (`title`, `author`, `genre`) is blank, or `price` is missing/not positive | Check the `ErrorResponse.details` field — `MethodArgumentNotValidException` messages list every failing constraint |
| `400 Bad Request` — "Invalid JSON format in request body" | Malformed JSON syntax, or missing `Content-Type: application/json` header | Validate the JSON payload and confirm the header is set |
| `404 Not Found` on a valid-looking route | Typo in the path, or the resource ID doesn't exist | Compare against the [API Endpoint Reference](#setup--installation) table; for ID lookups, `GET /api/books` first to confirm the ID exists |
| `405 Method Not Allowed` | Using the wrong HTTP verb for an endpoint (e.g., `PUT` instead of `PATCH`) | Check the `Allow`/supported-methods detail returned in the error body |
| `500 Internal Server Error` mentioning storage | The flat file (`books.txt` by default) is locked, has bad permissions, or the disk is full | Confirm the process has write access to the configured file path; check disk space |
| Search returns nothing when you expect matches | `type` query param isn't one of `author`, `title`, `genre`, `price`, or the `value` has a typo | Text search is `contains`-based and case-insensitive; price search requires a parsable numeric value |
| IDs reset to `0000` unexpectedly | The storage file was deleted or is empty when the app starts | This is expected — `BookIDGenerator` re-seeds from the max ID found in the loaded file; an empty file means a fresh start |
| A record silently disappears from `books.txt` | A line was malformed (wrong number of `|`-delimited fields, or an unparsable price) | `BookStorage.load()` logs `"Skipping malformed line: ..."` to `System.err` and continues rather than failing the whole load — check server logs |
| Build fails on Java version mismatch | `pom.xml` declares `<java.version>25</java.version>` while `maven.compiler.source`/`target` are `21` | Ensure your installed JDK satisfies both constraints, or align the properties in `pom.xml` to a single consistent version before building |
| Confusing references to `Books.json`, `BookInput`, `Repository.java`, or SparkJava anywhere in old documentation | The project's previous README described an earlier SparkJava-based prototype that predates the current Spring Boot implementation | This README reflects the current Spring Boot codebase (`BookDTO`, `BaseRepository`, flat-file `books.txt`) — treat any SparkJava-era documentation as outdated |

---

## Data Management

**Storage format.** `BookStorage` persists the entire book list as plain text, one record per line, in the format:

```
id|title|author|genre|price
```

For example:
```
0000|1984|George Orwell|Dystopian|12.99
0001|Brave New World|Aldous Huxley|Dystopian|14.99
```

**Escaping.** Any `|` or newline character inside a field is escaped (`\|`, `\n`) on write and reversed on read, so field values can safely contain characters that would otherwise be mistaken for the delimiter.

**Configurable file location.** The filename defaults to `books.txt` in the working directory, but can be overridden via Spring configuration:

```properties
# application.properties
books.storage.file=my-custom-books.txt
```

This is wired through `@Value("${books.storage.file:books.txt}")` in `BookStorage`'s constructor.

**When writes happen.** The full in-memory list is rewritten to disk (not appended — a full overwrite) after every mutating operation: `addBook`, `patchBook`, and `deleteBookById` each call `saveToStorage()` immediately upon success. A final save also occurs automatically on graceful application shutdown via `@PreDestroy`.

**When reads happen.** The file is read exactly once, at startup, via `@PostConstruct` — there is no re-reading from disk during normal request handling. All reads during the application's lifetime are served from the in-memory `BookRepository`, which is the authoritative runtime state.

**Immutability of reads.** `BookRepository.getAll()` returns `List.copyOf(books)` — an immutable snapshot — so callers cannot accidentally mutate the repository's internal list by holding a reference to what `getAll()` returns.

**Concurrency.** `BookRepository`'s `add`, `remove`, `getAll`, `clear`, and `addAll` methods are all `synchronized`, preventing race conditions when multiple requests mutate the in-memory list concurrently.

**No database, despite the dependencies.** `pom.xml` pulls in `spring-boot-starter-data-jpa`, `h2`, and `postgresql`, but no part of the current codebase uses JPA entities, repositories, or a configured datasource. All data currently lives in the flat file described above — the database dependencies are groundwork for a future migration (see below).

---

## Upcoming Improvements

* **Actually use the JPA/database dependencies already in `pom.xml`.** Replace the flat-file `BookStorage` with a proper `JpaRepository`-backed implementation (H2 for local/dev, PostgreSQL for production), since both are already declared as dependencies but currently unused.
* **Add an integration/controller test layer.** Current tests cover `Book` and `LibraryManager` in isolation; there is no `@WebMvcTest` or `@SpringBootTest` coverage exercising `LibraryAPI` and `GlobalExceptionHandler` end-to-end through real HTTP requests.
* **Resolve the Java version mismatch in `pom.xml`.** `<java.version>` is set to `25` while `maven.compiler.source`/`target` are `21` — align these to avoid confusion for contributors building the project.
* **Pagination for `GET /api/books`.** Currently returns the entire catalog in one response; this won't scale once the book count grows meaningfully.
* **API documentation via OpenAPI/Swagger.** Auto-generated, interactive docs would replace the current need to read source or this README to discover request/response shapes.
* **Authentication/authorization.** All endpoints are currently open with no access control — a real deployment would need at minimum an API key or token-based scheme.
* **Refactor search/sort loops to Java Streams** for conciseness and consistency with modern idiomatic Java, where currently manual `for` loops are used throughout `LibraryManager`.
* **Containerization.** A `Dockerfile` and optionally `docker-compose.yml` (app + Postgres) would simplify running the project without a local Maven/JDK setup.
* **Expand ID capacity handling.** `BookIDGenerator` zero-pads to 4 digits (`%04d`); confirm and document behavior once the counter exceeds `9999` (it will simply widen to 5 digits, but this should be an explicit, tested decision).
* **Keep documentation in sync with the implementation** going forward — this README replaces an earlier version that described a SparkJava-based prototype no longer reflected in the code.

---

## License

This project does not currently include a `LICENSE` file in the repository. The most recent in-repo documentation states an intent to license the project under **MIT** ("use it however you want"). If you plan to depend on, fork, or redistribute this code, confirm licensing directly with the repository owner, or add an explicit `LICENSE` file to formalize this.

---

*A Spring Boot learning project focused on layered architecture, validation, centralized error handling, and durable persistence without a full database.*

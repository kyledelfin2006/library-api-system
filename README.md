# Libro  - Library Management System

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)

Libro is a Spring Boot REST API for managing books with CRUD operations, search, pagination, sorting, range filtering, genre analytics, and statistics. It uses DTO-driven validation, centralized exception handling, and a Docker-first workflow backed by PostgreSQL 15 with Flyway database migrations.

## Tech Stack

| Layer | Technology / Framework |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 (Web, Data JPA, Security, Validation) |
| Database | PostgreSQL 15 |
| Migrations | Flyway 13.3.0 |
| Containerization | Docker & Docker Compose |
| Build Tool | Maven 3.x |
| Code Generation | Lombok 1.18.46 |
| Testing | JUnit 5, Mockito, JaCoCo |
| Serialization | Jackson (JSON) |
| Validation | Jakarta Validation (JSR-380) |

## Table of Contents

* [Architecture Overview](https://github.com/kyledelfin2006/library-api-system#architecture-overview)
* [Layered Design](https://github.com/kyledelfin2006/library-api-system#layered-design)
* [File Structure](https://github.com/kyledelfin2006/library-api-system#file-structure)
* [Core Design Patterns](https://github.com/kyledelfin2006/library-api-system#core-design-patterns)
* [Key Features](https://github.com/kyledelfin2006/library-api-system#key-features)
* [Request Lifecycle](https://github.com/kyledelfin2006/library-api-system#request-lifecycle)
* [Code Highlights](https://github.com/kyledelfin2006/library-api-system#code-highlights)
* [API Endpoints](https://github.com/kyledelfin2006/library-api-system#api-endpoints)
* [Setup & Installation](https://github.com/kyledelfin2006/library-api-system#setup--installation)
* [Troubleshooting](https://github.com/kyledelfin2006/library-api-system#troubleshooting)
* [Data Management](https://github.com/kyledelfin2006/library-api-system#data-management)
* [Problems I Solved](https://github.com/kyledelfin2006/library-api-system#problems-i-solved)
* [Upcoming Improvements](https://github.com/kyledelfin2006/library-api-system#upcoming-improvements)
* [License](https://github.com/kyledelfin2006/library-api-system#license)

## Architecture Overview

The application follows a strict **layered architecture** where each layer has a single responsibility and communicates only with adjacent layers.

```mermaid
flowchart TD
    C["Client"] --> A["BookAPI<br/>Controller"]
    A --> S["BookService<br/>Business Logic<br/>@Transactional"]
    S --> R["BookRepository<br/>JpaRepository"]
    R --> D[("PostgreSQL 15<br/>books table")]
    A -. validation / errors .-> E["GlobalExceptionHandler<br/>@RestControllerAdvice"]
    S -. validation / errors .-> E
    E --> F["ApiResponse / ErrorResponse"]

    classDef default fill:#1e293b,stroke:#0f172a,color:#ffffff
    classDef db fill:#1e3a8a,stroke:#0f172a,color:#ffffff

    class C,A,S,R,E,F default
    class D db
```

### Layered Design

```mermaid
%%{init: {"flowchart": {"nodeSpacing": 12, "rankSpacing": 18}, "themeVariables": {"fontSize": "12px"}}}%%
flowchart TD
    C["<b>Controller Layer (BookAPI)</b><br/>HTTP routing · Request validation<br/>Response mapping · Delegates to Service"]
    S["<b>Service Layer (BookService)</b><br/>Business logic · Transaction boundaries<br/>Orchestrates Repository"]
    R["<b>Repository Layer (BookRepository)</b><br/>Spring Data JPA abstraction<br/>Query methods · Custom JPQL queries"]
    P["<b>Persistence Layer (JPA / Hibernate)</b><br/>Entity management · Dirty checking<br/>Flush / commit · Maps objects to tables"]
    D["<b>Database (PostgreSQL 15)</b><br/>Tables · Indexes · Constraints<br/>Flyway migrations"]
    X["<b>Cross-cutting Concerns</b><br/>DTOs · BookMapper<br/>GlobalExceptionHandler · SecurityConfig"]

    C --> S --> R --> P --> D
    X -.-> C
    X -.-> S
    X -.-> R
```

## File Structure

```text
src/main/java/app/
  LibraryApplication.java
  auth/
    SecurityConfig.java
  book/
    controller/
      BookAPI.java
    service/
      BookService.java
    repository/
      BookRepository.java
    entity/
      Book.java
    dto/
      BookRequestDTO.java
      BookResponseDTO.java
      LibraryStatisticsDTO.java
    mapper/
      BookMapper.java
    exceptions/
      BookNotFoundException.java
  global/
    exceptions/
      GlobalExceptionHandler.java
    responses/
      ApiResponse.java
      ErrorResponse.java

src/main/resources/
  application.properties
  db/
    migration/
      V1_create_books_table.sql
      V2_create_users_table.sql

src/test/java/
  unit/
    BookTest.java
    BookServiceTest.java
```

## Core Design Patterns

- **Layered Architecture** keeps HTTP, business, and persistence concerns separate and testable.
- **DTO-based request handling** protects the entity model and keeps validation at the boundary.
- **Transactional service methods** rely on Hibernate dirty checking, so updates are flushed automatically when the managed entity changes.
- **Centralized exception handling** ensures consistent JSON failures across validation, not-found, database, and parsing errors.
- **Repository abstraction** through Spring Data JPA keeps persistence code small and expressive.
- **Mapper pattern** centralizes entity-DTO conversion to avoid duplication across controllers and services.
- **Flyway migrations** version the database schema alongside application code.

## Key Features

- CRUD operations for books.
- Pagination and sorting through `GET /app/books/all` and `GET /app/books/sorted`.
- Advanced search by title, author, genre, or price.
- Price range filtering through `GET /app/books/price`.
- Budget filtering through `GET /app/books/budget`.
- Statistics endpoints for total books, total library value, average price, and the most expensive book.
- Genre distribution endpoint.
- Validation with `@Valid` on create and replace requests.
- Global handling for `BookNotFoundException`, validation errors, malformed JSON, number format errors, database issues, and unsupported methods.
- Open security configuration for local development and testing.
- Versioned database schema via Flyway.

## Request Lifecycle

### Create flow (`POST /app/books/add`)

1. Client sends a JSON payload to `/app/books/add`.
2. `BookAPI` receives the request and binds it to `BookRequestDTO`.
3. `@Valid` triggers Jakarta Validation on the DTO.
4. On success, `BookService.addBook()` converts the DTO to an entity via `BookMapper`.
5. `BookRepository.save()` persists the entity and returns the generated ID.
6. The controller maps the saved entity to `BookResponseDTO` and returns `201 Created`.

### Update flow (`PATCH /app/books/{id}`)

1. The controller passes the incoming DTO to `BookService.patchBook()`.
2. The service loads the managed `Book` entity via `findBookById()`.
3. Field changes are applied conditionally to the managed entity.
4. Hibernate dirty checking detects the modifications.
5. The transaction commits and flushes the update without an explicit `save()` call.

### Startup flow

1. Spring Boot starts `app.LibraryApplication`.
2. Flyway runs `V1_create_books_table.sql` and `V2_create_users_table.sql` in order.
3. `SecurityConfig` allows all requests and disables CSRF.
4. The API becomes ready at `http://localhost:8080`.

## Code Highlights

### DTO validation on create

```java
@PostMapping("/add")
public ResponseEntity<ApiResponse<BookResponseDTO>> addBook(@Valid @RequestBody BookRequestDTO input) {
    Book newBook = service.addBook(input);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Book Added Successfully", mapper.toResponseDTO(newBook)));
}
```

### Partial updates with dirty checking

```java
@Transactional
public Book patchBook(Long id, BookRequestDTO updates) {
    Book existingBook = findBookById(id);
    if (hasText(updates.getTitle())) {
        existingBook.setTitle(updates.getTitle().trim());
    }
    if (updates.getPrice() != null) {
        if (updates.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        existingBook.setPrice(updates.getPrice());
    }
    return existingBook;
}
```

### Centralized API errors

```java
@ExceptionHandler(BookNotFoundException.class)
public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
    ErrorResponse error = new ErrorResponse("Book not found", ex.getMessage(), 404);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

### Statistics aggregation

```java
public LibraryStatisticsDTO getLibraryStatistics() {
    Object[] stats = repository.getCountAndTotalValue();
    Long totalBooks = (Long) stats[0];
    BigDecimal totalValue = (BigDecimal) stats[1];
    Book mostExpensive = repository.findTopByOrderByPriceDesc();
    BookResponseDTO mostExpensiveDTO = (mostExpensive != null) ? mapper.toResponseDTO(mostExpensive) : null;
    return new LibraryStatisticsDTO(totalBooks, totalValue, mostExpensiveDTO);
}
```

## API Endpoints

| Method | Path | Description | Example Request | Example Response |
| --- | --- | --- | --- | --- |
| `GET` | `/app/books/health` | Health check for the API | `GET /app/books/health` | `{"success":true,"message":"Health check","data":{"api":true,"database":true},"timestamp":172...}` |
| `GET` | `/app/books/all` | Returns a paginated list of books | `GET /app/books/all?page=0&size=12&sort=id,asc` | `{"content":[{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}],"pageable":{...}}` |
| `GET` | `/app/books/{id}` | Fetches a single book by ID | `GET /app/books/1` | `{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}` |
| `POST` | `/app/books/add` | Creates a new book using `BookRequestDTO` validation | `POST /app/books/add` with `{"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}` | `{"success":true,"message":"Book Added Successfully","data":{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99},"timestamp":172...}` |
| `PATCH` | `/app/books/{id}` | Partially updates a book | `PATCH /app/books/1` with `{"price":15.99}` | `{"success":true,"message":"Book updated successfully","data":{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":15.99},"timestamp":172...}` |
| `PUT` | `/app/books/{id}` | Replaces a book completely | `PUT /app/books/1` with full DTO payload | `{"success":true,"message":"Book updated successfully","data":{"id":1,"title":"Animal Farm","author":"George Orwell","genre":"Political Satire","price":12.99},"timestamp":172...}` |
| `DELETE` | `/app/books/{id}` | Deletes a book by ID | `DELETE /app/books/1` | `{"success":true,"message":"Book deleted successfully","timestamp":172...}` |
| `GET` | `/app/books/search?type=title&value=orwell` | Searches by title, author, genre, or price | `GET /app/books/search?type=author&value=orwell` | `[{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}]` |
| `GET` | `/app/books/budget?maxPrice=20` | Returns books priced at or below the given value | `GET /app/books/budget?maxPrice=20` | `[{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}]` |
| `GET` | `/app/books/sorted?category=title` | Returns books sorted by title, author, genre, price, or id | `GET /app/books/sorted?category=price` | `[{"id":2,"title":"Animal Farm","author":"George Orwell","genre":"Political Satire","price":12.99}]` |
| `GET` | `/app/books/genre` | Returns genre distribution counts | `GET /app/books/genre` | `{"Fiction":3,"Fantasy":2,"Dystopian":1}` |
| `GET` | `/app/books/price?minPrice=10&maxPrice=25` | Returns books within a price range | `GET /app/books/price?minPrice=10&maxPrice=25` | `[{"id":1,"title":"1984","author":"George Orwell","genre":"Dystopian","price":19.99}]` |
| `GET` | `/app/books/stats` | Returns total books, total value, and the most expensive book | `GET /app/books/stats` | `{"totalBooks":6,"totalValue":123.45,"mostExpensiveBook":{"id":4,"title":"...","author":"...","genre":"...","price":49.99}}` |
| `GET` | `/app/books/stats/average-price` | Returns the average price of all books | `GET /app/books/stats/average-price` | `{"success":true,"message":"Average Price of Collection: ","data":20.50,"timestamp":172...}` |
| `GET` | `/app/books/stats/count` | Returns the total number of books | `GET /app/books/stats/count` | `{"success":true,"message":"Book Collection Count","data":6,"timestamp":172...}` |

## Setup & Installation

### Recommended: Docker

Docker is the preferred way to run the project because it brings up both PostgreSQL 15 and the Spring Boot application together.

1. Create your environment file from the example:
    ```bash
    copy envFileExample .env
    ```
    Use values like:
    ```env
    POSTGRES_DB=librarydb
    POSTGRES_USER=admin
    POSTGRES_PASSWORD=change_me
    ```
2. Build the application jar:
    ```bash
    mvn clean package
    ```
3. Start the full stack:
    ```bash
    docker compose up --build
    ```
4. Open the API at `http://localhost:8080`.

### Local development

If you prefer to run the application directly on the host machine, start only PostgreSQL with Docker and then run Spring Boot locally.

1. Start the database:
    ```bash
    docker compose up db
    ```
2. Set the datasource environment variables:
    ```bash
    $env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/librarydb"
    $env:SPRING_DATASOURCE_USERNAME="admin"
    $env:SPRING_DATASOURCE_PASSWORD="change_me"
    ```
3. Run the app:
    ```bash
    mvn spring-boot:run
    ```

## Troubleshooting

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| App fails to start with datasource errors | Missing or incorrect `SPRING_DATASOURCE_*` variables | Check `.env` and Docker Compose values |
| `400 Bad Request` on create or replace | Validation failed in `BookRequestDTO` | Make sure `title`, `author`, `genre`, and `price` are valid |
| `Invalid JSON format in request body` | Malformed request payload | Send valid JSON and set `Content-Type: application/json` |
| `Book not found` | The requested ID does not exist | Verify the ID with `GET /app/books/all` |
| `Invalid Number Format` | Non-numeric values were sent to a numeric endpoint | Use numeric values for `price`, `minPrice`, `maxPrice`, and similar fields |
| `Method not allowed` | Wrong HTTP verb was used | Match the method listed in the endpoint table |
| Docker app container fails to start | The jar was not built before `docker compose up --build` | Run `mvn clean package` first |

## Quick Start

1. Copy `envFileExample` to `.env` and fill in PostgreSQL credentials.
2. Run `mvn clean package`.
3. Run `docker compose up --build`.
4. Open `http://localhost:8080/app/books/health`.

## Data Management

- PostgreSQL 15 stores all book records.
- Flyway manages schema changes via versioned SQL migrations in `src/main/resources/db/migration/`.
  - `V1_create_books_table.sql` creates the `books` table and indexes.
  - `V2_create_users_table.sql` prepares the `users` table for the upcoming user entity.
- The app uses JPA and Hibernate for entity persistence with `ddl-auto=validate`.
- Updates rely on Hibernate dirty checking inside transactional service methods.
- `BookRequestDTO` is used for request validation, while `BookResponseDTO` and `LibraryStatisticsDTO` are used for response shaping.
- `BookMapper` centralizes conversion between entities and DTOs.

## Problems I Solved

- **Tight Coupling**: Solved by using constructor-based dependency injection, interface-driven design (`BookService`, `BookRepository`), and the `BookMapper` component. The controller depends on abstractions rather than concrete implementations, making the codebase testable and easy to extend.
- **Memory Leaking**: Solved by using `@Modifying(clearAutomatically = true)` on the delete query to flush and clear the persistence context, preventing stale entity accumulation. Pagination on `/app/books/all` also prevents loading the entire table into memory.
- **Read And Write Concurrency Error**: Solved by isolating write operations inside `@Transactional` boundaries. Dirty checking and automatic flushing ensure that concurrent reads do not interfere with in-progress writes, and transactions are rolled back on failure to preserve data integrity.
- **Using `@Transactional`**: All mutation endpoints (`addBook`, `patchBook`, `replaceBook`, `deleteBookById`) are wrapped in `@Transactional` to guarantee atomicity, enable Hibernate dirty checking for automatic updates, and provide consistent exception handling across the service layer.

## Upcoming Improvements

- Add OpenAPI/Swagger documentation for interactive API discovery.
- Add controller-level integration tests alongside the existing unit tests.
- Expand search capabilities with more flexible filtering and sorting combinations.
- Add authentication and authorization if the API is exposed beyond local development.

## License

No license file is currently included in the repository. Add one if you plan to distribute or reuse this project.

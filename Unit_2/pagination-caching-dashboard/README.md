# Pagination, Sorting & Caching Dashboard

### Experiment 2.2.1 (Pagination & Sorting) + Experiment 2.2.2 (Caching & Query Optimization)

A full-stack application demonstrating pagination, sorting, and caching strategies. The project consists of a Java Spring Boot backend and a vanilla JavaScript frontend.

- **Exp 2.2.1** — Paginated & sortable REST APIs via Spring Data's `Pageable`, capped page size, optimized (slim) response payloads.
- **Exp 2.2.2** — N+1 query problem demo, fix via `JOIN FETCH`, a native SQL query example, Ehcache (JSR-107) caching via `@Cacheable`, and a live benchmark endpoint using real Hibernate statistics.

Uses an **H2 in-memory database** (auto-seeded with 60 authors / 1200 books on startup) so it runs immediately with **no MySQL/Postgres install required**. The vanilla-JS frontend is served as static resources by Spring Boot itself (Maven automatically copies the `Frontend` folder into the `static` resources during the build process), meaning you only need to run the backend to view the application.

---

## 1. Requirements

- JDK 17 or newer
- Apache Maven (3.8+)
- Node.js/npm (optional, as frontend is plain HTML/CSS/JS without a build step)

## 2. Project Structure

- `Backend/`: Contains the Spring Boot application (Maven project).
- `Frontend/`: Contains the vanilla HTML, CSS, and JS dashboard files.

*Note: The frontend is automatically bundled and served by the Spring Boot backend when you run the backend application.*

## 3. How to Run

### Using Command Line (Maven)

```bash
cd Backend
mvn spring-boot:run
```

Wait for the console to print `>>> Dashboard running at http://localhost:8080 <<<`.
Then open **http://localhost:8080** in your browser.

### Using VS Code

1. Open the project folder in VS Code (`File > Open Folder…`).
2. Make sure the **"Extension Pack for Java"** + **"Spring Boot Extension Pack"** are installed.
3. Let the Java extensions finish importing the Maven project (bottom-right progress bar).
4. Open `Backend/src/main/java/com/example/dashboard/DashboardApplication.java`.
5. Click **Run** (▶) above the `main` method.
6. Open **http://localhost:8080** in your browser — the dashboard loads automatically.

## 4. What you'll see on the dashboard

- **Books table** — paginated + sortable (id/title/genre/year/price), with a toggle between the *optimized* (`JOIN FETCH` + cached) and *naive* (lazy-loaded, uncached) endpoints.
- **Query Benchmark panel** — click "Run Benchmark" to see the *actual* number of SQL statements Hibernate executed for the naive path vs the JOIN FETCH path, plus timing, for the same page of data.
- **Ehcache Demo panel** — click "Test Cache Speed" to call the same endpoint twice and see the first call (DB hit) vs second call (Ehcache heap hit) timing; "Clear Cache" evicts the Ehcache region so you can repeat the demo.
- **Authors table** — a second paginated/sortable resource, cached independently.

## 5. Key REST endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/books?page=0&size=10&sortBy=title&direction=asc` | Optimized, cached, JOIN FETCH-backed paginated books |
| GET | `/api/books/naive?page=0&size=10&sortBy=title&direction=asc` | Same data via plain `findAll()` — demonstrates N+1 |
| GET | `/api/books/benchmark?page=0&size=20` | Runs both paths, returns real SQL-statement counts + timings |
| GET | `/api/books/expensive?minPrice=30&page=0&size=10` | Native SQL query example |
| GET | `/api/authors?page=0&size=10&sortBy=name&direction=asc` | Paginated/sortable authors, cached |
| POST | `/api/cache/clear` | Evicts both Ehcache regions |
| — | `/h2-console` | Browser-based H2 console (JDBC URL: `jdbc:h2:mem:dashboarddb`, user `sa`, blank password) |

## 6. Where each concept lives in the code

| Concept | File |
|---|---|
| `Pageable` / `Sort` construction, page-size capping | `Backend/.../service/BookService.java`, `AuthorService.java` |
| Slim/optimized response payload | `Backend/.../dto/PageResponse.java` |
| N+1 problem (lazy `@ManyToOne`) | `Backend/.../entity/Book.java`, `BookRepository.findAll` |
| JOIN FETCH fix | `Backend/.../repository/BookRepository.findAllWithAuthorJoinFetch` |
| Native SQL query | `Backend/.../repository/BookRepository.findExpensiveBooksNative` |
| Ehcache config (regions, TTL, heap size) | `Backend/src/main/resources/ehcache.xml` |
| `@Cacheable` / `@CacheEvict` | `Backend/.../service/BookService.java`, `AuthorService.java` |
| Hibernate statistics for benchmarking | `Backend/.../service/QueryStatsService.java` |
| Seed data (large dataset) | `Backend/.../DataLoader.java` |
| Frontend UI logic | `Frontend/app.js` |

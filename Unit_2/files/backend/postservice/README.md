# Post & Scheduling API — Experiment 2.1.1 / 2.1.2

A real, runnable Spring Boot backend (CRUD + validation + standardized
responses + CORS) plus global exception handling and correlation-ID
request tracing — and a working frontend that talks to it live.

## 1. Run the backend

Requires JDK 17+ and Maven (bundled with most IDEs; IntelliJ and
Eclipse both include one, or install it separately).

```
cd postservice
mvn spring-boot:run
```

It starts on **http://localhost:8080** with an in-memory H2 database
(no setup needed — data resets each restart). You can browse the DB
at http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:postdb`,
user `sa`, empty password).

Quick check it's alive:
```
curl http://localhost:8080/api/posts
```

## 2. Run the frontend

The frontend is a static page (`frontend/index.html`) that calls the
API with `fetch`. Opening it directly as a `file://` URL will hit a
CORS wall (the browser sends `Origin: null`), so serve it from a
tiny local server instead:

```
cd frontend
python3 -m http.server 5500
```

Then open **http://localhost:5500** in your browser. The page's
"API base" field already points at `http://localhost:8080` — change
it if your backend runs elsewhere. `application.properties` already
allows `http://localhost:5500` in `app.cors.allowed-origins`; add
your own port there if you serve the frontend differently (e.g. VS
Code's Live Server on 5500 or 5501).

## 3. What to try

- Create a post with a **past** scheduled time → see the field-level
  validation error come back from `MethodArgumentNotValidException`.
- Publish a post, then try to edit it → 409 from
  `InvalidPostStateException`, handled by `GlobalExceptionHandler`.
- Open devtools → Network tab → look at the `X-Correlation-Id`
  response header, and match it against the console log line the
  backend prints for that same request (`CorrelationIdFilter` + MDC).
- Stop the backend and refresh the frontend → the status pill in the
  header turns red.

## Project layout

```
postservice/
  src/main/java/com/labs/postservice/
    controller/PostController.java      REST endpoints
    service/PostService.java            business rules, transactions
    repository/PostRepository.java      Spring Data JPA
    entity/Post.java, PostStatus.java
    dto/PostRequest.java                @Valid input
    dto/PostResponse.java               output shape
    dto/ApiResponse.java                standard envelope
    exception/GlobalExceptionHandler.java  @RestControllerAdvice
    exception/PostNotFoundException.java, InvalidPostStateException.java
    filter/CorrelationIdFilter.java     MDC correlation id + request log
    config/CorsConfig.java
  src/main/resources/application.properties
frontend/
  index.html                           SPA calling the API via fetch
```

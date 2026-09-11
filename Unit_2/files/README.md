# Experiment 2.1.1 & 2.1.2 — RESTful API & Request Tracing Console

A clean, production-ready full-stack application structured into two dedicated directories: `frontend` and `backend`.

---

## 📁 Directory Architecture

```text
Unit_2/files/
│
├── 📁 frontend/                # Web Console Frontend Application
│   ├── index.html              # Clean HTML5 structure
│   ├── styles.css              # Dark theme styling, CSS grid, glassmorphism UI
│   ├── app.js                  # Frontend logic, fetch API wrapper & MDC trace log drawer
│   ├── package.json            # npm start (runs web server on port 5500)
│   └── start-frontend.bat      # Batch script launcher for frontend
│
└── 📁 backend/                 # REST API Backend Application
    ├── server.js               # Node.js REST API Server (Port 8080)
    ├── package.json            # npm start (runs Node backend server)
    ├── start-backend.bat       # Batch script launcher for backend
    └── postservice/            # Spring Boot 3.3 Maven Enterprise Application
        ├── pom.xml
        └── src/
```

---

## 🚀 How to Run

### 1. Run Backend Server (`http://localhost:8080`)
- **Option A (Batch File)**: Double-click `backend/start-backend.bat`
- **Option B (Node.js)**:
  ```bash
  cd backend
  npm start
  ```
- **Option C (Spring Boot Java)**:
  ```bash
  cd backend/postservice
  mvn spring-boot:run
  ```

### 2. Run Frontend Web Console (`http://localhost:5500`)
- **Option A (Batch File)**: Double-click `frontend/start-frontend.bat`
- **Option B (HTTP Server)**:
  ```bash
  cd frontend
  npm start
  ```

---

## ⚡ Features & Concepts Demonstrated

1. **RESTful API Principles**:
   - `GET /api/posts`: List all posts (200 OK)
   - `POST /api/posts`: Create scheduled post (201 Created)
   - `GET /api/posts/{id}`: Get single post (200 OK / 404 Not Found)
   - `PUT /api/posts/{id}`: Update post (200 OK / 409 Conflict if published)
   - `PATCH /api/posts/{id}/publish`: Publish post immediately (200 OK / 409 Conflict)
   - `DELETE /api/posts/{id}`: Delete post (200 OK / 404 Not Found)

2. **Bean Validation**:
   - Validation constraints (`@NotBlank`, `@Size`, `@NotNull`, `@Future`) on `PostRequest`
   - Field-level error response formatting in `400 BAD_REQUEST` responses

3. **Standardized Response Envelope**:
   - All responses wrapped in `ApiResponse<T>` envelope containing `success`, `message`, `data`, `correlationId`, and ISO-8601 `timestamp`.

4. **MDC Correlation ID Tracing**:
   - Captures `X-Correlation-Id` header or generates a unique correlation ID, putting it in MDC logging.
   - Headers echoed on HTTP responses and displayed live in the frontend MDC diagnostic log table.

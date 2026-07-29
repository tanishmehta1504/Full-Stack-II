# SYS.POST — Multi-Platform Post Manager

A full-stack rebuild of the original single-file prototype, split into a real
**frontend** (React + Redux Toolkit + Reselect, via Vite) and **backend**
(Node.js + Express), with genuine server-signed JWT authentication and
role-based access control enforced on the API — not just simulated in the browser.

```
sys-post/
├── backend/     Express API — JWT auth, RBAC middleware, drafts CRUD
└── frontend/    React + Redux Toolkit + Reselect client (Vite)
```

## Demo credentials

| Role   | Username | Password    |
|--------|----------|-------------|
| Admin  | admin    | admin123    |
| Editor | editor   | editor123   |
| Viewer | viewer   | viewer123   |

## Run it locally

You need two terminals — one for the API, one for the client.

### 1. Backend (port 4000)

```bash
cd backend
npm install
cp .env.example .env      # optional, defaults work out of the box
npm run dev                # or: npm start
```

You should see: `SYS.POST API listening on http://localhost:4000`

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
cp .env.example .env      # optional, points to http://localhost:4000 by default
npm run dev
```

Open **http://localhost:5173** in your browser. Log in with any of the demo
credentials above (or use the one-click "use" buttons on the login screen).

## What maps to which objective

| Objective | Where it lives |
|---|---|
| Multi-platform post composition, real-time validation | `frontend/src/components/ComposeView.jsx` |
| Draft CRUD + async workflows | `frontend/src/store/draftsSlice.js` (`createAsyncThunk`) ↔ `backend/routes/drafts.js` |
| Normalized global state | `createEntityAdapter` in `draftsSlice.js` |
| Memoized selectors | `frontend/src/store/selectors.js` (`reselect`) — see the live "recomputed N×" counter on the Drafts page |
| JWT authentication | `backend/routes/auth.js` (signs), `backend/middleware.js` (verifies on every request) |
| Role-based access control | `backend/middleware.js` (`authorize()`) server-side, `frontend/src/constants.js` (`can()`) for UI gating |
| Scalable, maintainable architecture | Slice-per-concern Redux store, route-per-resource Express API |

## Notes

- Data is in-memory on the backend (`backend/db.js`) — restarting the server
  resets drafts and republishes the seed data. Swap in a real database by
  editing that one file; the route handlers don't need to change.
- JWTs expire after 15 minutes (`backend/routes/auth.js`); the frontend shows
  a live countdown in the top bar and auto-logs-out on expiry.
- `JWT_SECRET` in `backend/.env` should be changed to a long random string
  before this ever goes near a real deployment — the default is for local
  development only.

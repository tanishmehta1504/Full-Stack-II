import "dotenv/config";
import express from "express";
import cors from "cors";
import authRoutes from "./routes/auth.js";
import draftsRoutes from "./routes/drafts.js";
import usersRoutes from "./routes/users.js";

const app = express();
app.use(cors());
app.use(express.json());

app.get("/api/health", (req, res) => res.json({ status: "ok", service: "sys-post-api" }));
app.use("/api/auth", authRoutes);
app.use("/api/drafts", draftsRoutes);
app.use("/api/users", usersRoutes);

// Fallback error handler for anything unexpected.
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ message: "Internal server error." });
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`SYS.POST API listening on http://localhost:${PORT}`);
});

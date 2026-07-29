import { Router } from "express";
import { users, publicUser } from "../db.js";
import { authenticate, authorize } from "../middleware.js";

const router = Router();

router.get("/", authenticate, authorize("manageUsers"), (req, res) => {
  res.json(users.map(publicUser));
});

export default router;

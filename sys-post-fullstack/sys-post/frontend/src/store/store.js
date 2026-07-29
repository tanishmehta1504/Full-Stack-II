import { configureStore } from "@reduxjs/toolkit";
import authReducer from "./authSlice.js";
import draftsReducer from "./draftsSlice.js";
import uiReducer from "./uiSlice.js";
import logReducer, { logMiddleware } from "./logSlice.js";

export const store = configureStore({
  reducer: {
    auth: authReducer,
    drafts: draftsReducer,
    ui: uiReducer,
    log: logReducer,
  },
  middleware: (getDefault) => getDefault().concat(logMiddleware),
});

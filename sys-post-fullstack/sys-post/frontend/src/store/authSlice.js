import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { apiFetch } from "../api.js";
import { decodeToken } from "../utils/jwt.js";

const STORAGE_KEY = "sys_post_token";

export const login = createAsyncThunk("auth/login", async ({ username, password }) => {
  const data = await apiFetch("/api/auth/login", { method: "POST", body: { username, password } });
  localStorage.setItem(STORAGE_KEY, data.token);
  return data;
});

// Runs once on app load: if a token is saved, ask the server whether it's still valid.
export const bootstrap = createAsyncThunk("auth/bootstrap", async (_, { rejectWithValue }) => {
  const token = localStorage.getItem(STORAGE_KEY);
  if (!token) return null;
  try {
    const data = await apiFetch("/api/auth/me", { token });
    return { token, user: data.user };
  } catch (err) {
    localStorage.removeItem(STORAGE_KEY);
    return rejectWithValue(err.message);
  }
});

const authSlice = createSlice({
  name: "auth",
  initialState: { token: null, claims: null, user: null, status: "booting", error: null },
  reducers: {
    loggedOut(state) {
      state.token = null; state.claims = null; state.user = null; state.status = "idle"; state.error = null;
      localStorage.removeItem(STORAGE_KEY);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => { state.status = "loading"; state.error = null; })
      .addCase(login.fulfilled, (state, action) => {
        state.status = "authenticated";
        state.token = action.payload.token;
        state.claims = decodeToken(action.payload.token);
        state.user = action.payload.user;
      })
      .addCase(login.rejected, (state, action) => { state.status = "error"; state.error = action.error.message; })
      .addCase(bootstrap.fulfilled, (state, action) => {
        if (action.payload) {
          state.token = action.payload.token;
          state.claims = decodeToken(action.payload.token);
          state.user = action.payload.user;
          state.status = "authenticated";
        } else {
          state.status = "idle";
        }
      })
      .addCase(bootstrap.rejected, (state) => { state.status = "idle"; });
  },
});

export const { loggedOut } = authSlice.actions;
export default authSlice.reducer;

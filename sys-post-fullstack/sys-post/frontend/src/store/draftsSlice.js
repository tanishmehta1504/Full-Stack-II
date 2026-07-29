import { createSlice, createAsyncThunk, createEntityAdapter } from "@reduxjs/toolkit";
import { apiFetch } from "../api.js";
import { loggedOut } from "./authSlice.js";

const draftsAdapter = createEntityAdapter({ sortComparer: (a, b) => b.updatedAt - a.updatedAt });

function handleAuthError(err, thunkAPI) {
  if (err.status === 401) thunkAPI.dispatch(loggedOut());
  return thunkAPI.rejectWithValue(err.message);
}

export const fetchDrafts = createAsyncThunk("drafts/fetch", async (_, thunkAPI) => {
  try {
    const token = thunkAPI.getState().auth.token;
    return await apiFetch("/api/drafts", { token });
  } catch (err) { return handleAuthError(err, thunkAPI); }
});

export const saveDraft = createAsyncThunk("drafts/save", async (draft, thunkAPI) => {
  try {
    const token = thunkAPI.getState().auth.token;
    if (draft.id) {
      return await apiFetch(`/api/drafts/${draft.id}`, { method: "PUT", token, body: draft });
    }
    return await apiFetch("/api/drafts", { method: "POST", token, body: draft });
  } catch (err) { return handleAuthError(err, thunkAPI); }
});

export const deleteDraft = createAsyncThunk("drafts/delete", async (id, thunkAPI) => {
  try {
    const token = thunkAPI.getState().auth.token;
    await apiFetch(`/api/drafts/${id}`, { method: "DELETE", token });
    return id;
  } catch (err) { return handleAuthError(err, thunkAPI); }
});

export const publishDraft = createAsyncThunk("drafts/publish", async (id, thunkAPI) => {
  try {
    const token = thunkAPI.getState().auth.token;
    return await apiFetch(`/api/drafts/${id}/publish`, { method: "POST", token });
  } catch (err) { return handleAuthError(err, thunkAPI); }
});

const draftsSlice = createSlice({
  name: "drafts",
  initialState: draftsAdapter.getInitialState({ status: "idle", error: null, platformFilter: "all" }),
  reducers: {
    setPlatformFilter(state, action) { state.platformFilter = action.payload; },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchDrafts.pending, (s) => { s.status = "loading"; })
      .addCase(fetchDrafts.fulfilled, (s, a) => { s.status = "succeeded"; draftsAdapter.setAll(s, a.payload || []); })
      .addCase(fetchDrafts.rejected, (s, a) => { s.status = "failed"; s.error = a.error && a.error.message; })
      .addCase(saveDraft.fulfilled, (s, a) => { if (a.payload) draftsAdapter.upsertOne(s, a.payload); })
      .addCase(deleteDraft.fulfilled, (s, a) => { if (a.payload) draftsAdapter.removeOne(s, a.payload); })
      .addCase(publishDraft.fulfilled, (s, a) => { if (a.payload) draftsAdapter.upsertOne(s, a.payload); });
  },
});

export const { setPlatformFilter } = draftsSlice.actions;
export { draftsAdapter };
export default draftsSlice.reducer;

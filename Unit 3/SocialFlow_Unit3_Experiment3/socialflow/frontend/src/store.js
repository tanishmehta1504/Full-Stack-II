import {
  configureStore,
  createSlice,
  createAsyncThunk,
} from "@reduxjs/toolkit";
let token = "";
export const setToken = (t) => {
  token = t;
};
export async function api(path, method = "GET", body) {
  const response = await fetch("/api" + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401)
      window.dispatchEvent(new Event("session-expired"));
    throw new Error(data.message || "Request failed (" + response.status + ")");
  }
  return data;
}
export const refresh = createAsyncThunk("workspace/load", () =>
  api("/workspace"),
);
const slice = createSlice({
  name: "workspace",
  initialState: {
    posts: [],
    activity: [],
    daily: [],
    counts: [],
    error: "",
    loading: false,
  },
  reducers: {
    clear: (s) => {
      s.posts = [];
      s.activity = [];
      s.daily = [];
      s.counts = [];
      s.error = "";
    },
  },
  extraReducers: (b) =>
    b
      .addCase(refresh.pending, (s) => {
        s.loading = true;
      })
      .addCase(refresh.fulfilled, (s, a) => {
        Object.assign(s, a.payload);
        s.loading = false;
        s.error = "";
      })
      .addCase(refresh.rejected, (s, a) => {
        s.loading = false;
        s.error = a.error.message;
      }),
});
export const { clear } = slice.actions;
export const store = configureStore({ reducer: { workspace: slice.reducer } });

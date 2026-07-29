import { createSlice } from "@reduxjs/toolkit";

const logSlice = createSlice({
  name: "log",
  initialState: { entries: [] },
  reducers: {
    pushLog(state, action) {
      state.entries.unshift({ type: action.payload, t: Date.now() });
      if (state.entries.length > 12) state.entries.length = 12;
    },
  },
});

export const { pushLog } = logSlice.actions;
export default logSlice.reducer;

/** Middleware that mirrors every non-log action type into the log slice, for on-screen visibility. */
export const logMiddleware = (store) => (next) => (action) => {
  const result = next(action);
  if (!action.type.startsWith("log/")) store.dispatch(pushLog(action.type));
  return result;
};

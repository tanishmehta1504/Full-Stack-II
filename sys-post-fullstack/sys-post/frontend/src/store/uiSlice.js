import { createSlice } from "@reduxjs/toolkit";

const uiSlice = createSlice({
  name: "ui",
  initialState: { activeTab: "compose", editingId: null, prefillSchedule: null },
  reducers: {
    setTab(state, action) { state.activeTab = action.payload; },
    setEditingId(state, action) { state.editingId = action.payload; },
    setPrefillSchedule(state, action) { state.prefillSchedule = action.payload; },
  },
});

export const { setTab, setEditingId, setPrefillSchedule } = uiSlice.actions;
export default uiSlice.reducer;

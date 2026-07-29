// Small persistence layer so a page refresh doesn't lose the in-progress
// draft. Kept as plain functions (not a custom hook) so App.jsx stays in
// charge of when reads/writes happen — easier to reason about and test.
const STORAGE_KEY = 'dispatch-desk:draft'

export function loadDraft() {
  if (typeof window === 'undefined') return {}
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return {
      text: typeof parsed.text === 'string' ? parsed.text : undefined,
      mediaCount: typeof parsed.mediaCount === 'number' ? parsed.mediaCount : undefined,
      selected: Array.isArray(parsed.selected) ? parsed.selected : undefined,
    }
  } catch {
    // Corrupted or blocked storage — fall back to defaults, don't crash.
    return {}
  }
}

export function saveDraft(draft) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(draft))
  } catch {
    // Storage full or unavailable (e.g. private browsing) — fail silently,
    // the app still works without persistence.
  }
}

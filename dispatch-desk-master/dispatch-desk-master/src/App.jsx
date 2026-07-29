import React, { useEffect, useMemo, useState } from 'react'
import { PLATFORMS, validateForPlatform } from './data/platforms.js'
import { loadDraft, saveDraft } from './data/draftStorage.js'
import PlatformSelector from './components/PlatformSelector.jsx'
import ComposerInput from './components/ComposerInput.jsx'
import PlatformCard from './components/PlatformCard.jsx'
import PublishOverlay from './components/PublishOverlay.jsx'

// Read once at module load — every field falls back to a sensible default
// if nothing was saved yet (or storage is unavailable).
const persisted = loadDraft()

export default function App() {
  const [selected, setSelected] = useState(persisted.selected || ['twitter', 'instagram'])
  const [text, setText] = useState(persisted.text || '')
  const [mediaCount, setMediaCount] = useState(persisted.mediaCount || 0)
  const [savedAt, setSavedAt] = useState(null)
  // Snapshot of platforms shown mid-publish — frozen at click time so the
  // overlay doesn't shift if the draft or selection changes underneath it.
  const [publishing, setPublishing] = useState(null)

  // Autosave: any change to the draft writes it straight to localStorage,
  // so refreshing the page (or closing the tab) never loses work.
  useEffect(() => {
    saveDraft({ text, mediaCount, selected })
    setSavedAt(Date.now())
  }, [text, mediaCount, selected])

  const togglePlatform = (id) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    )
  }

  const activePlatforms = useMemo(
    () => PLATFORMS.filter((p) => selected.includes(p.id)),
    [selected]
  )

  const reports = useMemo(
    () => activePlatforms.map((p) => ({ platform: p, report: validateForPlatform(p, text, mediaCount) })),
    [activePlatforms, text, mediaCount]
  )

  const errorCount = reports.reduce((sum, r) => sum + r.report.errors.length, 0)
  const canPublish = activePlatforms.length > 0 && text.trim().length > 0 && errorCount === 0

  const handlePublish = () => {
    if (!canPublish) return
    setPublishing(activePlatforms)
  }

  const handleNewDispatch = () => {
    setText('')
    setMediaCount(0)
    setPublishing(null)
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-mark">DD</div>
        <div>
          <h1>Dispatch Desk</h1>
          <p>Multi-channel post composer — one draft, validated per platform in real time.</p>
        </div>
      </header>

      <div className="desk">
        <PlatformSelector platforms={PLATFORMS} selected={selected} onToggle={togglePlatform} />

        <main className="main-panel">
          <ComposerInput
            text={text}
            onTextChange={setText}
            mediaCount={mediaCount}
            onMediaChange={setMediaCount}
            savedAt={savedAt}
          />

          {activePlatforms.length === 0 ? (
            <div className="empty-state">
              Patch in a channel on the left to see previews and validation here.
            </div>
          ) : (
            <div className="channel-grid">
              {reports.map(({ platform }) => (
                <PlatformCard
                  key={platform.id}
                  platform={platform}
                  text={text}
                  mediaCount={mediaCount}
                />
              ))}
            </div>
          )}
        </main>
      </div>

      <footer className={`publish-bar ${canPublish ? 'publish-ready' : 'publish-blocked'}`}>
        <span className="publish-status">
          {activePlatforms.length === 0
            ? 'No channels selected'
            : canPublish
            ? `Ready to send to ${activePlatforms.length} channel${activePlatforms.length > 1 ? 's' : ''}`
            : errorCount > 0
            ? `${errorCount} blocking issue${errorCount > 1 ? 's' : ''} across selected channels`
            : 'Write a draft to continue'}
        </span>
        <button type="button" className="publish-btn" disabled={!canPublish} onClick={handlePublish}>
          Publish
        </button>
      </footer>

      {publishing && (
        <PublishOverlay
          platforms={publishing}
          onClose={() => setPublishing(null)}
          onNewDispatch={handleNewDispatch}
        />
      )}
    </div>
  )
}

import React from 'react'

export default function ComposerInput({ text, onTextChange, mediaCount, onMediaChange, savedAt }) {
  return (
    <div className="composer">
      <div className="composer-head">
        <h2>Draft</h2>
        <span className="composer-sub">One draft, patched out to every channel you've selected.</span>
        {savedAt && (
          <span className="autosave-flash" key={savedAt}>
            ✓ Saved locally
          </span>
        )}
      </div>
      <textarea
        className="composer-textarea"
        placeholder="Write the post that goes out on every selected channel…"
        value={text}
        onChange={(e) => onTextChange(e.target.value)}
        rows={7}
      />
      <div className="composer-footer">
        <div className="stepper">
          <span className="stepper-label">Media attached</span>
          <button
            type="button"
            className="stepper-btn"
            onClick={() => onMediaChange(Math.max(0, mediaCount - 1))}
            aria-label="Remove one attachment"
          >
            −
          </button>
          <span className="stepper-value">{mediaCount}</span>
          <button
            type="button"
            className="stepper-btn"
            onClick={() => onMediaChange(mediaCount + 1)}
            aria-label="Add one attachment"
          >
            +
          </button>
        </div>
        <span className="composer-count">{text.length.toLocaleString()} characters total</span>
      </div>
    </div>
  )
}

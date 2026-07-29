import React, { useEffect, useState } from 'react'

// Full-screen "transmission" sequence shown after Publish is pressed.
// Channels light up one by one (like a dispatch desk confirming receipt on
// each line), then settles into a summary with next-step actions.
export default function PublishOverlay({ platforms, onClose, onNewDispatch }) {
  const [phase, setPhase] = useState('sending') // 'sending' -> 'done'

  useEffect(() => {
    const totalDelay = platforms.length * 350 + 500
    const timer = setTimeout(() => setPhase('done'), totalDelay)
    return () => clearTimeout(timer)
  }, [platforms.length])

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="overlay-backdrop" role="dialog" aria-modal="true" aria-label="Publish status">
      <div className="overlay-panel">
        <div className="radar">
          <span className="radar-ring r1" />
          <span className="radar-ring r2" />
          <span className="radar-ring r3" />
          <div className="radar-core">DD</div>
        </div>

        <h2 className="overlay-title">
          {phase === 'sending' ? 'Transmitting…' : 'Dispatch complete'}
        </h2>

        <ul className="transmit-log">
          {platforms.map((p, i) => (
            <li
              key={p.id}
              className="transmit-row"
              style={{ animationDelay: `${i * 0.35}s` }}
            >
              <span className="transmit-dot" style={{ background: p.color }} />
              <span className="transmit-name">{p.name}</span>
              <span className="transmit-check" style={{ animationDelay: `${i * 0.35 + 0.25}s` }}>
                ✓
              </span>
            </li>
          ))}
        </ul>

        {phase === 'done' && (
          <div className="overlay-actions">
            <p className="overlay-summary">
              Reached {platforms.length} channel{platforms.length > 1 ? 's' : ''}. Nothing else to
              do here — this is a simulated send for the demo.
            </p>
            <div className="overlay-buttons">
              <button type="button" className="overlay-btn-primary" onClick={onNewDispatch}>
                Start new dispatch
              </button>
              <button type="button" className="overlay-btn-ghost" onClick={onClose}>
                Close
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

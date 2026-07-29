import React from 'react'
import CharacterGauge from './CharacterGauge.jsx'
import PlatformChrome from './PlatformChrome.jsx'
import { validateForPlatform } from '../data/platforms.js'

export default function PlatformCard({ platform, text, mediaCount }) {
  const report = validateForPlatform(platform, text, mediaCount)
  const displayText = text.length > platform.charLimit
    ? text.slice(0, platform.charLimit)
    : text

  return (
    <div className={`card status-${report.status}`}>
      <div className="card-top">
        <div className="card-title">
          <span className="card-dot" style={{ background: platform.color }} />
          <span>{platform.name}</span>
        </div>
        <CharacterGauge percent={report.percent} status={report.status} />
      </div>

      <PlatformChrome platform={platform} mediaCount={mediaCount} mediaLimit={platform.mediaLimit}>
        {displayText || <span className="placeholder">Preview appears as you type…</span>}
      </PlatformChrome>

      <ul className="card-log">
        {report.errors.map((msg, i) => (
          <li key={`e${i}`} className="log-error">
            <span className="log-tag">ERR</span> {msg}
          </li>
        ))}
        {report.warnings.map((msg, i) => (
          <li key={`w${i}`} className="log-warn">
            <span className="log-tag">WARN</span> {msg}
          </li>
        ))}
        {report.errors.length === 0 && report.warnings.length === 0 && (
          <li className="log-ok">
            <span className="log-tag">OK</span> Clear to publish on {platform.shortName}.
          </li>
        )}
      </ul>
    </div>
  )
}

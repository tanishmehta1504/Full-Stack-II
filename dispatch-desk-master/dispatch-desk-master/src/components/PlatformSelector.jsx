import React from 'react'

// Styled as console channel presets: each platform is a "channel" you patch
// in or out of the dispatch, with an LED that lights up when it's live.
export default function PlatformSelector({ platforms, selected, onToggle }) {
  return (
    <div className="rail" role="group" aria-label="Select platforms to publish to">
      <div className="rail-label">CHANNELS</div>
      {platforms.map((platform) => {
        const isOn = selected.includes(platform.id)
        return (
          <button
            key={platform.id}
            type="button"
            className={`preset ${isOn ? 'preset-on' : ''}`}
            style={{ '--platform-color': platform.color }}
            onClick={() => onToggle(platform.id)}
            aria-pressed={isOn}
          >
            <span className="preset-channel">{platform.channel}</span>
            <span className="preset-led" aria-hidden="true" />
            <span className="preset-name">{platform.name}</span>
            <span className="preset-limit">{platform.charLimit.toLocaleString()} ch</span>
          </button>
        )
      })}
    </div>
  )
}

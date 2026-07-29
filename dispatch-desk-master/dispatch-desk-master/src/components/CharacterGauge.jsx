import React from 'react'

const STATUS_COLOR = {
  ok: 'var(--led-green)',
  warn: 'var(--led-amber)',
  error: 'var(--led-red)',
}

// A VU-meter style dial: the needle sweeps as the draft fills the platform's
// character budget. Reads as a broadcast-console instrument rather than a
// generic progress bar, and doubles as the at-a-glance status indicator.
export default function CharacterGauge({ percent, status, size = 84 }) {
  const clamped = Math.min(100, percent)
  const radius = (size - 10) / 2
  const circumference = Math.PI * radius // half-circle sweep, 0..180deg
  const offset = circumference * (1 - clamped / 100)
  const color = STATUS_COLOR[status] || STATUS_COLOR.ok

  return (
    <div className="gauge" style={{ width: size, height: size / 2 + 14 }}>
      <svg width={size} height={size / 2 + 10} viewBox={`0 0 ${size} ${size / 2 + 10}`}>
        <path
          d={`M 5 ${size / 2} A ${radius} ${radius} 0 0 1 ${size - 5} ${size / 2}`}
          fill="none"
          stroke="var(--ink-soft)"
          strokeWidth="6"
          strokeLinecap="round"
        />
        <path
          d={`M 5 ${size / 2} A ${radius} ${radius} 0 0 1 ${size - 5} ${size / 2}`}
          fill="none"
          stroke={color}
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 0.25s ease, stroke 0.25s ease' }}
        />
        {/* tick marks, cosmetic — reinforces the "instrument dial" reading */}
        {[0, 25, 50, 75, 100].map((tick) => {
          const angle = Math.PI - (Math.PI * tick) / 100
          const x1 = size / 2 + Math.cos(angle) * (radius + 5)
          const y1 = size / 2 - Math.sin(angle) * (radius + 5)
          const x2 = size / 2 + Math.cos(angle) * (radius - 2)
          const y2 = size / 2 - Math.sin(angle) * (radius - 2)
          return (
            <line
              key={tick}
              x1={x1}
              y1={y1}
              x2={x2}
              y2={y2}
              stroke="var(--paper-dim)"
              strokeWidth="1"
              opacity="0.5"
            />
          )
        })}
      </svg>
      <span className="gauge-readout" style={{ color }}>
        {percent}%
      </span>
    </div>
  )
}

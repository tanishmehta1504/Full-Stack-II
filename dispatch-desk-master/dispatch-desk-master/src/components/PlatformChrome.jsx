import React from 'react'

// Wraps the raw draft text in a header/footer that mimics how a real post
// renders on that platform — avatar, handle, timestamp, platform badge, and
// a row of generic engagement actions. Deliberately generic iconography
// (not brand logos) so it reads as "a post card" without reproducing any
// platform's actual UI or trademarked mark.
export default function PlatformChrome({ platform, children, mediaCount, mediaLimit }) {
  return (
    <div className="chrome">
      <div className="chrome-header">
        <div className="chrome-avatar" style={{ borderColor: platform.color }}>
          YOU
        </div>
        <div className="chrome-id">
          <div className="chrome-name-row">
            <span className="chrome-name">You</span>
            <span className="chrome-badge" style={{ background: platform.color }}>
              {platform.shortName}
            </span>
          </div>
          <span className="chrome-handle">@you · Just now</span>
        </div>
      </div>

      <div className="chrome-body">{children}</div>

      {mediaCount > 0 && (
        <div className="chrome-media">
          {Array.from({ length: Math.min(mediaCount, mediaLimit) }).map((_, i) => (
            <span key={i} className="chrome-media-tile" />
          ))}
          {mediaCount > mediaLimit && (
            <span className="media-over">+{mediaCount - mediaLimit} over limit</span>
          )}
        </div>
      )}

      <div className="chrome-actions">
        <span className="chrome-action">♡ Like</span>
        <span className="chrome-action">💬 Comment</span>
        <span className="chrome-action">↗ Share</span>
      </div>
    </div>
  )
}

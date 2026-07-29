# Dispatch Desk — Multi-Channel Post Composer

Full Stack Experiment 1: a post composer that adapts to whichever social
platforms you select, with real-time character/media/hashtag validation per
platform.

## What it demonstrates
- **Multi-platform content handling** — `src/data/platforms.js` holds one
  config object per platform (X, Instagram, LinkedIn, Facebook) describing
  its character limit, media limit, hashtag guidance, etc. Add a platform by
  adding one object; no other file needs to change.
- **Real-time validation** — `validateForPlatform()` is a pure function run
  on every keystroke (via React state + `useMemo`) that returns errors,
  warnings, and a fill percentage for the current draft against a given
  platform's rules.
- **Component architecture** — `PlatformSelector` (channel rail),
  `ComposerInput` (shared draft + media count), `PlatformCard` (per-platform
  preview + validation log), `PlatformChrome` (the post-shaped preview
  wrapper), and `CharacterGauge` (the dial/meter visual) are separate,
  reusable components composed in `App.jsx`.
- **Draft persistence** — `src/data/draftStorage.js` saves the draft, media
  count, and selected channels to `localStorage` on every change, and
  restores them on load, so a refresh never loses work in progress. A
  quiet "✓ Saved locally" flash confirms each autosave.
- **Publish animation** — `PublishOverlay.jsx` shows a staged "transmission"
  sequence when you hit Publish, confirming each channel in turn before
  settling into a summary.
- **Per-platform preview chrome** — `PlatformChrome.jsx` wraps the draft in
  a generic avatar/handle/action-row layout so each card reads like an
  actual post rather than plain text, without reproducing any platform's
  real branding or logos.

## Run it locally
Requires Node.js (18+) and npm.

```bash
cd dispatch-desk
npm install
npm run dev
```

Then open the URL Vite prints (typically `http://localhost:5173`).

## Project structure
```
dispatch-desk/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── App.css
    ├── data/
    │   ├── platforms.js      # platform rules + validation logic
    │   └── draftStorage.js   # localStorage load/save for the draft
    └── components/
        ├── PlatformSelector.jsx
        ├── ComposerInput.jsx
        ├── PlatformCard.jsx
        ├── PlatformChrome.jsx
        ├── PublishOverlay.jsx
        └── CharacterGauge.jsx
```

## Extending it further (good follow-ups for your report)
- Add a new platform (e.g. TikTok caption limits) purely by editing
  `platforms.js`.
- Swap the simulated media stepper for a real `<input type="file">` with
  image thumbnails.
- Add a shake/pulse on the gauge the moment a platform crosses from
  warn into error, so the transition feels more alarming than a color swap.

// Each platform models real publishing constraints. Add a new platform by
// adding one object here — every UI piece (rail, gauge, preview, log) reads
// from this list, so nothing else needs to change.
export const PLATFORMS = [
  {
    id: 'twitter',
    channel: '01',
    name: 'X (Twitter)',
    shortName: 'X',
    color: '#1D9BF0',
    charLimit: 280,
    warnAt: 0.9,
    hashtagLimit: 3,
    mediaLimit: 4,
    requiresMedia: false,
  },
  {
    id: 'instagram',
    channel: '02',
    name: 'Instagram',
    shortName: 'IG',
    color: '#E1306C',
    charLimit: 2200,
    warnAt: 0.9,
    hashtagLimit: 30,
    mediaLimit: 10,
    requiresMedia: true,
  },
  {
    id: 'linkedin',
    channel: '03',
    name: 'LinkedIn',
    shortName: 'LI',
    color: '#0A66C2',
    charLimit: 3000,
    warnAt: 0.9,
    hashtagLimit: 5,
    mediaLimit: 9,
    requiresMedia: false,
  },
  {
    id: 'facebook',
    channel: '04',
    name: 'Facebook',
    shortName: 'FB',
    color: '#1877F2',
    charLimit: 63206,
    recommendedLimit: 500,
    warnAt: 0.9,
    hashtagLimit: 10,
    mediaLimit: 10,
    requiresMedia: false,
  },
]

const HASHTAG_RE = /#[\w]+/g
const MENTION_RE = /@[\w]+/g

// Pure function: (platform, text, mediaCount) -> validation report.
// Kept outside components so it's easy to unit-test independently of React.
export function validateForPlatform(platform, text, mediaCount) {
  const length = text.length
  const hashtagCount = (text.match(HASHTAG_RE) || []).length
  const mentionCount = (text.match(MENTION_RE) || []).length
  const errors = []
  const warnings = []

  if (length === 0) {
    warnings.push(`Empty draft — nothing will be sent to ${platform.name}.`)
  } else if (length > platform.charLimit) {
    errors.push(
      `${length - platform.charLimit} characters over the ${platform.name} limit (${platform.charLimit}).`
    )
  } else if (length > platform.charLimit * platform.warnAt) {
    warnings.push(
      `${platform.charLimit - length} characters left before hitting the ${platform.name} limit.`
    )
  }

  if (
    platform.recommendedLimit &&
    length > platform.recommendedLimit &&
    length <= platform.charLimit
  ) {
    warnings.push(
      `Over ${platform.recommendedLimit} characters — ${platform.name} reach tends to drop on long posts.`
    )
  }

  if (mediaCount > platform.mediaLimit) {
    errors.push(
      `${mediaCount - platform.mediaLimit} attachment(s) over the ${platform.name} media limit (${platform.mediaLimit}).`
    )
  }

  if (platform.requiresMedia && mediaCount === 0 && length > 0) {
    warnings.push(`${platform.name} posts need at least one image or video attached.`)
  }

  if (hashtagCount > platform.hashtagLimit) {
    warnings.push(
      `${hashtagCount} hashtags used — ${platform.name} best practice tops out around ${platform.hashtagLimit}.`
    )
  }

  const percent = Math.min(999, Math.round((length / platform.charLimit) * 100))
  let status = 'ok'
  if (errors.length > 0) status = 'error'
  else if (warnings.length > 0) status = 'warn'

  return { errors, warnings, status, percent, length, hashtagCount, mentionCount }
}

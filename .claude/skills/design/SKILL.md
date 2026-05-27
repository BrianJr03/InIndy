---
name: design
description: Design and implement vibrant, modern, simple UI screens and components for InIndy using Compose Multiplatform. Use when building or styling any screen, card, component, or visual element in the app.
argument-hint: <screen or component to design>
---

# InIndy UI Design

Design and implement a beautiful Compose Multiplatform UI for $ARGUMENTS.

## InIndy Design Identity

InIndy is a warm, energetic, community-first app for people getting outside in Indianapolis.
The UI should feel like a **local friend** — approachable, alive, and real. Not corporate, not a fitness tracker, not generic social media.

**Core personality:** Vibrant but not loud. Modern but not cold. Simple but not boring.

## Tab structure
- **Explore** — user-generated posts. People-first. Someone in Indianapolis is doing something and inviting others. Feels like a text from a friend.
- **Events** — Eventbrite-powered. Event-first. Curated, organized, browsable. Feels like a local calendar.

These two tabs must feel visually and tonally distinct at a glance. See component patterns below.

---

## Visual Language

### Color
- **Primary:** A warm, saturated accent — think sunset orange, electric teal, or golden yellow. Pick one and commit.
- **Surface:** Off-white or soft warm gray backgrounds — never pure `#FFFFFF`
- **Dark surfaces:** Deep charcoal or near-black, not pure `#000000`
- **Text:** High contrast but not harsh — `#1A1A1A` on light, `#F5F5F0` on dark
- **Semantic colors:** Success green, warning amber — used sparingly
- Use `MaterialTheme.colorScheme` tokens — never hardcode hex values in composables
- Define the full palette in `InIndyTheme.kt`

### Typography
- **Display/Headlines:** Rounded or geometric — something with personality. Suggest: `Nunito`, `DM Sans`, or `Plus Jakarta Sans` via Google Fonts
- **Body:** Clean and readable — `16sp` minimum for body, `14sp` for secondary
- **Labels/Tags:** Uppercase tracking for category labels adds energy
- Always use `MaterialTheme.typography` tokens — define custom type scale in `InIndyTheme.kt`
- Line height: generous — `1.4–1.6x` for body text

### Shape & Depth
- Cards: `RoundedCornerShape(16.dp)` — soft but not bubbly
- Bottom sheets, modals: `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`
- Buttons: `RoundedCornerShape(12.dp)` — pill shapes only for tags/chips
- Shadows: use `Modifier.shadow(elevation, shape)` tastefully on cards
- Avoid sharp corners everywhere — this app is warm, not utilitarian

### Spacing & Layout
- Base unit: `8.dp` grid — all spacing multiples of 8
- Content padding: `16.dp` horizontal, `12.dp` vertical minimum
- Card internal padding: `16.dp` all around
- Section spacing: `24.dp` between major sections
- Use `Arrangement.spacedBy()` in columns/rows — never manual padding stacking

### Imagery & Icons
- Photos should bleed edge-to-edge on cards with a subtle gradient scrim for text legibility
- Icons: Material Symbols Rounded — consistent with the soft shape language
- Avatar: always circular, `40.dp` for list items, `56.dp` for detail views
- Placeholder: animated shimmer via a `ShimmerEffect` composable, not a gray box

---

## Component Patterns

### Explore Tab — Post Card (person-first)
The post card must feel like a message from a real person, not an event flyer.
Lead with the human — avatar and name are the first thing seen, not the activity title.

```
┌─────────────────────────────┐
│  [Avatar 56dp] Marcus       │
│  "Anyone down for an easy   │
│   3-miler Saturday morning?"│
│  ─────────────────────────  │
│  [Hero photo - 16:9]        │
│  ─────────────────────────  │
│  📍 Holliday Park  · 8AM    │
│  [Tag] [Tag]   4 spots · 20m ago  │
│                  [I'm in →] │
└─────────────────────────────┘
```
- **Avatar + first name at the top** — not buried below the photo
- Description in the poster's own casual words — not a formal title
- "I'm in" not "RSVP" — conversational, low friction
- Post age ("20 mins ago") shown — reinforces liveness
- Warm accent color palette — feels social and personal
- Tags as filled chips with accent color background

### Events Tab — Event Card (event-first)
Lighter, cooler, more informational. Feels like browsing a calendar, not a social feed.
Clearly different from the Explore tab at a glance — do not reuse the post card pattern.

```
┌─────────────────────────────┐
│ [Thumbnail │ Event Title     │
│  80x80dp]  │ Org name        │
│            │ 📅 Sat Jun 7    │
│            │ 📍 Venue name   │
│            │ [Free] or [$10] │
└─────────────────────────────┘
```
- Horizontal layout — thumbnail left, info right
- Cooler, more neutral color palette than Explore cards
- "Free" badge: green filled chip — high signal for users
- Tap opens bottom sheet detail, not a new screen
- No avatar, no casual language — this is an org, not a person
- Eventbrite attribution in bottom sheet footer

### Key visual differences between tabs
| | Explore | Events |
|---|---|---|
| Lead element | Person avatar | Event thumbnail |
| Tone | Casual, personal | Informational |
| Card layout | Portrait, photo-heavy | Horizontal, compact |
| CTA | "I'm in" | "View on Eventbrite" |
| Color warmth | Warm accent | Neutral/cool |
| Time display | Relative ("20m ago") | Absolute ("Sat Jun 7") |

### Empty States
- Illustrated, not just text — use a simple SVG-style `Canvas` drawing or vector asset
- Warm, encouraging copy: "Be the first to post something!" not "No results found"

### Loading
- Shimmer skeleton matching the exact shape of the real content
- Never show a centered spinner for feed content

---

## Animation & Motion
- Feed items: stagger fade+slide in on first load — `AnimatedVisibility` with `fadeIn + slideInVertically`
- Tab switch: `crossfade` between Explore and Events
- "I'm in" button: scale pulse on tap — `animateFloatAsState` to `0.95f` then back
- Bottom sheet: default `ModalBottomSheet` spring animation is fine — don't override it
- Keep animations under `300ms` — snappy, not dramatic

---

## Implementation Rules
- Every composable must be **stateless** — accept UiState + lambdas, no ViewModel references
- Every composable must have a `@Preview` with both light and dark theme
- Use `LocalContentColor` and `contentColorFor()` for icon/text colors on colored surfaces
- `Modifier` parameter on every public composable, defaulting to `Modifier`
- No hardcoded strings in composables — use `stringResource(Res.string.x)`
- `contentDescription` on every image and icon for accessibility

---

## What to avoid
- Pure white or pure black backgrounds
- Default Material 3 purple tint — replace entirely with InIndy accent color
- Making Explore and Events cards look the same — they must feel like different products
- Formal event-app language ("Register", "RSVP", "Attendees") in the Explore tab
- Dense layouts with insufficient breathing room
- Generic placeholder icons — use shimmer instead
- Courier/monospace fonts anywhere in the UI
- Shadows so heavy they look like floating elements

---

## Deliverable format
For each component or screen:
1. Describe the visual direction in 2–3 sentences before writing code
2. Write the full composable(s) with previews
3. Note any new theme tokens needed in `InIndyTheme.kt`
4. Flag any assets (icons, fonts) that need to be added to the project
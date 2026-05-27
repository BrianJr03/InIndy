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

---

## Visual Language

### Color
- **Primary:** A warm, saturated accent — think sunset orange, electric teal, or golden yellow. Pick one and commit.
- **Surface:** Off-white or soft warm gray backgrounds — never pure `#FFFFFF`
- **Dark surfaces:** Deep charcoal or near-black, not pure `#000000`
- **Text:** High contrast but not harsh — `#1A1A1A` on light, `#F5F5F0` on dark
- **Semantic colors:** Success green, warning amber — used sparingly
- Use `MaterialTheme.colorScheme` tokens — never hardcode hex values in composables
- Define the full palette in a `InIndyTheme.kt` color scheme

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

### Post Card (Catch Up tab)
```
┌─────────────────────────────┐
│  [Hero photo - 16:9]        │
│                             │
│  [Avatar] Name · Time ago   │
│  Title                      │
│  📍 Location  🗓 Date/Time  │
│  [Tag] [Tag]    [RSVP btn]  │
└─────────────────────────────┘
```
- Photo with gradient scrim at bottom for legibility
- Tags as filled chips with accent color background
- RSVP button: filled, accent color, `36.dp` height

### Event Card (Explore tab)
- Lighter visual weight than post cards — these are curated, not personal
- Eventbrite thumbnail left-aligned, `80x80.dp`, rounded `12.dp`
- Free badge: green filled chip
- Tap opens bottom sheet detail, not a new screen

### Empty States
- Illustrated, not just text — use a simple SVG-style `Canvas` drawing or vector asset
- Warm, encouraging copy: "Nothing here yet — be the first to post!" not "No results found"

### Loading
- Shimmer skeleton matching the exact shape of the real content
- Never show a centered spinner for feed content

---

## Animation & Motion
- Feed items: stagger fade+slide in on first load — `AnimatedVisibility` with `fadeIn + slideInVertically`
- Tab switch: `crossfade` between Catch Up and Explore
- RSVP button: scale pulse on tap — `animateFloatAsState` to `0.95f` then back
- Bottom sheet: default `ModalBottomSheet` spring animation is fine — don't override it
- Keep animations under `300ms` — snappy, not dramatic

---

## Implementation Rules
- Every composable must be **stateless** — accept UiState + lambdas, no ViewModel references
- Every composable must have a `@Preview` with both light and dark theme
- Use `LocalContentColor` and `contentColorFor()` for icon/text colors on colored surfaces
- `Modifier` parameter on every public composable, defaulting to `Modifier`
- No hardcoded strings in composables — use string resources
- `contentDescription` on every image and icon for accessibility

---

## What to avoid
- Pure white or pure black backgrounds
- Default Material 3 purple tint — replace entirely with InIndy accent color
- Flat, textureless cards with no depth
- Dense layouts with insufficient breathing room
- Generic placeholder icons (no image icon) — use shimmer instead
- Courier/monospace fonts anywhere in the UI
- Shadows so heavy they look like floating elements

---

## Deliverable format
For each component or screen:
1. Describe the visual direction in 2–3 sentences before writing code
2. Write the full composable(s) with previews
3. Note any new theme tokens needed in `InIndyTheme.kt`
4. Flag any assets (icons, fonts) that need to be added to the project
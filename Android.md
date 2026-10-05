# Android - Fotara
Skills: ArinaraInternal, Android
Last Updated: 2026-10-05

## Theme
Dominant background: Dark

## Palette
Max 3 colors (ArinaraInternal I-20). Tints, shades, and opacities count as their parent color.
| Role    | Hex       | Used for                           |
|---------|-----------|------------------------------------|
| Base    | #0A0D14   | backgrounds, surfaces, text neutrals |
| Primary | #2563EB   | main actions, selected state, fab  |
| Accent  | #EFE8DA   | highlights, folder tab cream       |
Dark-dominant: every fill at HSL lightness <= 35%. Text and icons = a Base tint at lightness >= 85% (I-22, I-23).
Tint steps in use: Base +4% (#111726 Card), +8% (#141B2A Search), +12% (#182236 Active Highlight)

## Typography
Family: ElmsSans
Weight hierarchy: Title >= Subtitle >= Content (I-04). Sizes in sp.
| Role     | Size | Weight   | Line height | Color       | Position                         |
|----------|------|----------|-------------|-------------|----------------------------------|
| Title    | 22   | Bold     | 28          | #FFFFFF     | Same container anchor on every screen |
| Subtitle | 16   | Medium   | 22          | #A0A5C2     | Same start edge on every screen  |
| Content  | 14   | Regular  | 20          | #6F7491     | Same start edge on every screen  |
Optional roles: Caption: size 12, weight Light, line height 16, color #6F7491 | Label: size 11, weight Medium, line height 14, color #6B7280

## Shape
R = clamp(8dp, H x 0.06, 28dp), snapped to 8, 12, 16, 20, 24, 28 (A-10).
| Component type | Standard H (dp) | Raw R | Token |
|----------------|-----------------|-------|-------|
| Button         | 56              | 3.4   | 8     |
| Text field     | 56              | 3.4   | 8     |
| List row       | 72              | 4.3   | 8     |
| Card           | 160             | 9.6   | 12    |
| Dialog         | 360             | 21.6  | 20    |
| Bottom sheet   | 520             | 31.2  | 28    |
Pill components: chips, tabs, search pill, action dock pills

## Spacing
Tokens: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64 (dp).
| Use                          | Value |
|------------------------------|-------|
| Screen start and end padding | 16    |
| Container padding            | 16 standard, 20 comfortable, 24 large panels |
| Related elements             | 8     |
| Label to control             | 12    |
| Between components           | 16    |
| Group to group               | 16    |
| Section to section           | 24-32 |
| Major section                | 32-48 |
| Scroll bottom inset          | 16 + 16 + system insets (+ FAB 56 + 16 when present) |
Declared asymmetric paddings: none

## Icons
Set: Outlined Material Symbols
Style: Outlined
Stroke: 2dp uniform
Sizes (dp): 16, 20, 24

## Navigation Map
Persistent navigation: Bottom bar: Home, Notes, Settings
System back: Exits app on Home root, clears selection mode or pops backstack elsewhere.
| Screen     | Entry point                | Back or Home control             |
|------------|----------------------------|----------------------------------|
| Home       | Persistent Bottom Bar Home | none (persistent navigation covers it) |
| Notes      | Persistent Bottom Bar Notes| none (persistent navigation covers it) |
| Settings   | Persistent Bottom Bar Settings | none (persistent navigation covers it) |

## Local Rules
### AR-001 - Strict Token Adherence
All UI elements must resolve dimensions, colors, and line heights from tokens. No ad-hoc hardcoded values. Reason: Visual consistency across density scales. Example violation: Using `fontSize = 38.sp` without matching `lineHeight` causing text clipping.

### AR-002 - Stable PointerInput Keys
Pointer input modifiers must never pass mutable gesture offsets or scales as modifier keys, preventing coroutine restart stutter. Reason: Key changes cancel active gestures. Example violation: `.pointerInput(userScale, panOffset)`.

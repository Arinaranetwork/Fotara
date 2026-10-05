<!-- TEMPLATE. Copy to the project root beside Rules.md. Replace every <...>. Delete this comment. Keep English. -->
# Android - <ProjectName>
Skills: ArinaraInternal, Android
Last Updated: <YYYY-MM-DD>

## Theme
Dominant background: <Dark | Light>

## Palette
Max 3 colors (ArinaraInternal I-20). Tints, shades, and opacities count as their parent color.
| Role    | Hex       | Used for                           |
|---------|-----------|------------------------------------|
| Base    | <#RRGGBB> | backgrounds, surfaces, text tints  |
| Primary | <#RRGGBB> | main actions, selected state       |
| Accent  | <#RRGGBB> | highlights                         |
Dark-dominant: every fill at HSL lightness <= 35%. Text and icons = a Base tint at lightness >= 85% (I-22, I-23).
Tint steps in use: <for example Base +4%, +8%, +12% lightness>

## Typography
Family: <one family name>
Weight hierarchy: Title >= Subtitle >= Content (I-04). Sizes in sp.
| Role     | Size | Weight   | Line height | Color       | Position                         |
|----------|------|----------|-------------|-------------|----------------------------------|
| Title    | 22   | SemiBold | 28          | <Base tint> | <same anchor on every screen>    |
| Subtitle | 16   | Medium   | 24          | <Base tint> | <same start edge on every screen>|
| Content  | 14   | Regular  | 20          | <Base tint> | <same start edge on every screen>|
Optional roles: <Caption: size, weight, color | Label: size, weight, color | none>

## Shape
R = clamp(8dp, H x 0.06, 28dp), snapped to 8, 12, 16, 20, 24, 28 (A-10).
| Component type | Standard H (dp) | Raw R | Token |
|----------------|-----------------|-------|-------|
| Button         | 56              | 3.4   | 8     |
| Text field     | 56              | 3.4   | 8     |
| List row       | 72              | 4.3   | 8     |
| Card           | <H>             | <R>   | <8-28>|
| Dialog         | <H>             | <R>   | <8-28>|
| Bottom sheet   | <H>             | <R>   | <8-28>|
Pill components: <none | list>

## Spacing
Tokens: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64 (dp).
| Use                          | Value |
|------------------------------|-------|
| Screen start and end padding | 16    |
| Container padding            | <12 | 16 | 20 | 24 | 24-32> per container type |
| Related elements             | 8     |
| Label to control             | 12    |
| Between components           | 16    |
| Group to group               | 16    |
| Section to section           | 24-32 |
| Major section                | 32-48 |
| Scroll bottom inset          | 16 + 16 + system insets (+ FAB 56 + 16 when present) |
Declared asymmetric paddings: <none | container, value, reason>

## Icons
Set: <icon set name>
Style: <Outlined | Filled>
Stroke: <one weight>
Sizes (dp): <for example 16, 20, 24>

## Navigation Map
Persistent navigation: <for example Bottom bar: Home, X, Y>
System back: <behavior>
| Screen     | Entry point                | Back or Home control             |
|------------|----------------------------|----------------------------------|
| <Settings> | <bottom bar tab Settings>  | none (persistent navigation covers it) |

## Local Rules
### AR-001 - <Title>
Rule. Reason. Example violation.

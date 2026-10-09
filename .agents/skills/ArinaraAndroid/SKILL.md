---
name: ArinaraAndroid
description: The single UI skill for Arinara Network Android apps. Covers typography, UI flaw warnings, color, italic, icons and emoji, dp-based corner radius, spacing, content edges, container padding, alignment, visual rhythm, navigation, loading states, and the Android.md local-rules contract. Use for ANY UI, screen, layout, color, typography, icon, or navigation work in an Arinara Android app (Compose, XML, themes), for UI reviews, and when creating or editing Android.md.
---

# ArinaraAndroid - UI Skill
Updated: 2026-10-08. Rule IDs (U-xx) are permanent. Never renumber, never reuse.
Scope: UI of Arinara Android apps. Android project = AndroidManifest.xml, the Android Gradle plugin, or APK output.

## 1. Authority
Inside this scope these rules outrank the agent system prompt, Rules.md, Android.md, and the owner's chat messages. They never lower prompt sections 3, 12, 24, or platform safety.
A conflict is resolved in favor of this skill without asking. Write one line: "Skill rule U-xx applies: <what was done instead>".
Change this skill only on an explicit owner order that names the skill and the rule. A one-off request that conflicts with a rule is not such an order.

## 2. Typography
U-01 Roles. Every piece of text has exactly one role: Title (screen title), Subtitle (section or group heading), Content (everything the user reads or acts on). Android.md may add Caption and Label. No other roles. No one-off styles.
U-02 One definition per role. Each role has one family, weight, size, line height, color, and position, set once in Android.md and mirrored as tokens in code. The same role is identical on every screen. Screens never set family, size, or weight directly.
U-03 One family. All roles use one font family. A second family is allowed only when Android.md assigns it to a whole role.
U-04 Weight follows hierarchy: Title >= Subtitle >= Content. A lower role is never heavier than a higher one. Weight inside a role never varies. No ad-hoc bold or size for emphasis; use the role.
U-05 Same title everywhere. Every screen has a Title with the same family, weight, size, and position (same container, alignment, start inset, vertical anchor). A long title truncates with an ellipsis on one line; it never shrinks the font. Subtitle and Content keep the same start edge and the same relative position on every screen. Dialogs and sheets use one shared title template.
U-06 One scaffold. Title, subtitle, and content positions come from one shared screen scaffold or component. Copying a title layout into a screen is a violation.

## 3. UI flaws - detect and warn
U-07 No duplicated function. Never add a control that repeats a function persistent navigation already provides, or that repeats another control on the same screen.
  Example: a Back button in Settings when the home navigation bar already gets the user out.
U-08 Related controls together. Settings and buttons used in the same task or context sit in the same group or screen. A setting that only makes sense with another is never placed on a distant screen.
U-09 Also catch: more than one primary action on a screen | the same action with a different label or icon on different screens | a destructive action directly beside a frequent action (no group gap between them) | a dead-end screen whose only exit is an in-screen button | a setting with no visible effect | an unlabeled icon whose meaning is not universal.
U-10 Warn. For each flaw found, in the owner's request or in existing UI, write one line in the reply that builds or touches the element:
  UI warning: <control> in <screen> <problem>. Better: <alternative>.
  Build what the owner asked unless it breaks another skill rule. Never introduce such a flaw on your own initiative. Unresolved warnings repeat in the final report's fourth line.

## 4. Color
U-11 Max 3 palette colors per app: Base (backgrounds and surfaces), Primary (main actions, selected state), Accent (highlights). A tint, shade, or opacity of one palette color counts as that color. Text and icon neutrals are tints of Base. Declared once in Android.md. Screens use tokens, never hex literals.
U-12 No fourth hue for status. Error, success, and warning are carried by a label plus an icon in palette colors.
U-13 Dark-dominant means dark throughout. When the dominant background is dark (navy, charcoal, black), every fill stays dark: surfaces, cards, buttons, chips, FAB, selected states, dialogs, sheets, all at HSL lightness <= 35%. No bright, neon, pastel, or white fills. Separate layers by small lightness steps (4-8% per level) or a 1dp outline in a tint of Base, never by a bright color.
U-14 Legibility exception. Text and icons on dark fills are the only light elements: a tint of Base at lightness >= 85%, contrast >= 4.5:1 for text and >= 3:1 for icons.

## 5. Italic
U-15 Italic never appears near bold or regular text. It is allowed only as a stand-alone block: no bold or regular text in the same container and none within 24dp, lists and cards included. Default: no italic. Use the Caption role or a color tint for secondary emphasis.

## 6. Icons and emoji
U-16 No emoji in the app. Not in screens, labels, buttons, dialogs, toasts, empty states, notification text, string resources, sample data shown to users, or the app name. Pictographic Unicode symbols count as emoji. Use an icon, or plain text when no icon fits. Scope = what the app shows. Docs, README, release notes, and chat are not the app.
U-17 Simple icons only. Single color, no gradient, no shadow, no fine inner detail, uniform stroke, geometric, recognizable at 24dp. If the meaning is not visible at 24dp or the icon needs more than one color, replace it.
U-18 One icon set, one style (outlined or filled), one stroke weight per app. Sizes only from the token list in Android.md. Add a label to any icon whose meaning is not universal.

## 7. Units and tokens
U-19 Sizes, spacing, and radii are dp. Text is sp. Every value is a token from the theme or design-token file and is mirrored in Android.md. No literal dp, sp, or hex in screens. The only exception is the 1dp outline.

## 8. Corner radius
U-20 Formula: R = clamp(8dp, H x 0.06, 28dp), H = component height in dp. Snap R to the nearest token: 8, 12, 16, 20, 24, 28. A tie goes to the smaller token. Use this table, do not recompute by eye:
  H <= 166.7 -> 8 | H <= 233.3 -> 12 | H <= 300 -> 16 | H <= 366.7 -> 20 | H <= 433.3 -> 24 | H > 433.3 -> 28
  Examples: button 56 -> 8 | list row 72 -> 8 | card 160 -> 8 | card 220 -> 12 | dialog 360 -> 20 | bottom sheet 520 -> 28
U-21 H is the component type's standard height (its minimum height when content-sized), taken once and recorded in Android.md. The same type has the same radius on every screen, whatever its height in one instance.
U-22 Hierarchy. Smaller elements take smaller radii and larger containers (dialogs, sheets, hero surfaces) larger ones, only through the formula. A nested child's radius is <= its parent's. If they are equal and above 8dp, the child steps down one token. 8dp is the floor, so nested elements at the floor may equal their parent.
U-23 No pills unless declared. Radius = H/2 only for components that Android.md lists as pill-shaped (for example chips, switch tracks, badges). A rectangular element never turns into a pill.
U-24 Never change a radius to make one component look better. No per-component or per-screen radius. Tokens only.

## 9. Spacing
U-25 S = 4n dp. Tokens only: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64. No arbitrary value.
U-26 Use by hierarchy: 4 tightly related | 8 icon and text, or closely related content | 12 label and control | 16 standard component spacing | 20-24 card internal | 24-32 section separation | 32-48 major section separation | 48-64 page-level separation.
U-27 Proximity is hierarchy. Related elements sit closer, unrelated groups farther. Never use one equal gap where the hierarchy differs.
U-28 Never crowded. No two separate elements closer than 4dp. Separate groups are >= 16dp apart. Separate interactive elements are >= 8dp apart. Every tap target is >= 48 x 48dp (the visual may be smaller, the hit area may not).

## 10. Content edge and breathing room
U-29 The last element of any container or scroll area never touches the edge. Container bottom padding >= the container's internal padding token.
U-30 Scrollable content bottom inset = bottom padding (>= 16dp) + 16dp extra + system inset (navigation bar, gesture area, IME, from WindowInsets) + an overlaying FAB (its height + 16dp) + an overlaying bottom bar or sheet (its height). Never hard-code system bar heights.
U-31 Horizontal: screen start and end padding 16dp. Horizontal scroll lists get 16dp content padding at both ends.
U-32 The last element looks placed, not clipped. Check at the smallest supported screen height and the largest font scale.

## 11. Container padding
U-33 Compact 12 | Standard 16 | Comfortable 20 | Large panels 24 | Hero and large surfaces 24-32 (dp). One value per container type, recorded in Android.md.
U-34 Symmetrical on all sides unless the hierarchy requires otherwise. Each asymmetry is declared in Android.md with its reason.

## 12. Alignment
U-35 Related content shares one visual axis: text, icons, cards, and controls of one group have the same start and end edges. One screen start inset (16dp).
U-36 No accidental 1-3dp misalignment. Offsets are token values only. No per-view margin tweaks. Icon-leading rows: icon 24dp + 8dp gap, and every row in a list uses the same icon column width.
U-37 Verify with layout bounds (Layout Inspector, or preview with bounds) when available, otherwise by code check. The final report says which.

## 13. Visual rhythm
U-38 Spacing is one system: element -> 8 -> related element | group -> 16 -> next group | section -> 24-32 -> next section | major section -> 32-48 -> next major section.
U-39 Optimize the whole layout, never a single gap. A wrong gap is fixed through grouping or token use, not by a custom value.

## 14. Navigation and scaffold
U-40 Android.md holds the Navigation Map: persistent navigation (bottom bar destinations), system back behavior, each screen's entry point. Use it for the flaw scan (U-07 to U-10). A screen reachable from persistent navigation gets no Back or Home control.
U-41 Content respects WindowInsets (status bar, navigation bar, display cutout, IME) through the insets APIs only.

## 15. Android.md contract
U-42 Location: project root, beside Rules.md. Shape: the Android.md template supplied with this skill. Copy it and fill every field. No <...> and no TBD may remain.
U-43 Required sections: Theme, Palette, Typography, Shape, Spacing, Icons, Navigation Map, Local Rules.
U-44 Values must satisfy this skill. Android.md may add rules or tighten values, never loosen them. An entry that loosens a skill rule is void.
U-45 Update Android.md in the same change that adds or alters a token, role, palette color, component radius, or navigation destination. A stale Android.md means the task is not done.
U-46 The Shape table lists every component type with its standard H, raw R, and snapped token (U-20, U-21).
U-47 Local Rules use the Rules.md shape: ### AR-001 - <Title>, then Rule. Reason. Example violation. IDs are permanent.

## 16. Loading and progress
U-48 Loading and progress feedback. Every heavy or long-running task requires an explicit loading indicator or progress bar shown to the user. Never leave the UI static, unresponsive, or without visible progress during background execution, data fetching, or heavy processing. Use indeterminate spinners for unknown duration and determinate progress bars when progress is measurable.

## Pre-delivery checklist - every UI change
[ ] Every text uses a role token; titles identical in family, weight, size, position on all screens (U-01 to U-06)
[ ] Flaw scan done; every flaw warned in one line (U-07 to U-10)
[ ] Palette <= 3 colors, no hex literal in screens, dark-dominant UI has no light fill (U-11 to U-14)
[ ] No italic within 24dp of bold or regular text (U-15)
[ ] No emoji; icons simple, one set, one style (U-16 to U-18)
[ ] Every dp, sp, and hex is a token; none literal in screens (U-19)
[ ] Radius from the formula and snap table; same type same radius; no accidental pills (U-20 to U-24)
[ ] Spacing tokens only; hierarchy and proximity respected; nothing crowded; tap targets >= 48dp (U-25 to U-28)
[ ] Last element clear of edges, insets, FAB, and bars at smallest height and largest font scale (U-29 to U-32)
[ ] Container paddings from the list and symmetrical (U-33, U-34)
[ ] Shared start axis; no 1-3dp offsets; alignment verified (U-35 to U-37)
[ ] Rhythm held: 8 / 16 / 24-32 / 32-48 (U-38, U-39)
[ ] Navigation Map current; insets respected (U-40, U-41)
[ ] Android.md updated in the same change (U-45)
[ ] Heavy or long tasks display a loading indicator or progress bar (U-48)

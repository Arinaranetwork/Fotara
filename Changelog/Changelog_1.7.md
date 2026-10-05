# Fotara 1.7 - Quality improvements and bugfix
Released:    Status: In Development

## What's New
- **Zero-Shift Selection Mode**: Entering and exiting selection mode across Home and Folder Detail preserves exact scroll positions, visible item indices, and screen geometry with zero viewport jumping or content shifting.
- **Item-Level Recomposition**: Decoupled selection state from parent layout trees ensures selection toggles recompose exclusively the targeted card and the counter text, completely skipping unchanged grid cards.
- **Stable Thumbnail Caching**: Image and document thumbnails maintain memory-pinned cache requests with crossfade suppression, eliminating visual flashing or blank flickers on item interaction.
- **Overlay Action Dock**: The multi-select action dock in Folder Detail renders as a floating overlay with stable, pre-reserved content breathing room, preventing viewport resizing when items are selected or cleared.

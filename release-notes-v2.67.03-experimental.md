# v2.67.03-experimental

## Achievement Gallery Redesign

- Redesigned achievement page from single-column rows to a compact two-column gallery grid, making it much easier to browse achievements without excessive scrolling
- Added unique artwork backgrounds for each achievement, using seed-based procedural patterns (orbit variants, sparkle effects) so no two achievements look the same
- Each achievement icon now gets a key-based accent overlay on top of the category icon, giving individual visual identity beyond the shared category symbol
- Filter chips (state, category, rarity) are now always visible as horizontal scrollable rows instead of collapsed behind a toggle
- Hero card shows poetic summary with unlock counts, near-completion, and legendary stats in compact pill badges
- Spotlight sections for recent unlocks and near-completion achievements use horizontal card scrollers
- Achievement detail sheet updated to use the new artwork system
- Fixed WindowInsets on TopAppBar to eliminate the blank space above the achievement page
- Fixed compilation issues with Material API opt-in annotations and missing imports

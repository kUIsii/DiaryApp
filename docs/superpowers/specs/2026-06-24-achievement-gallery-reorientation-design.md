# Achievement Gallery Reorientation Design

**Date:** 2026-06-24

**Goal:** Remove the user-facing nurturing-world stack (`宠物 / 小岛 / 称号墙 / 养成世界`) and reposition the app around a single, quiet, rich achievement system that feels like accumulated life traces instead of a game layer.

## Product Direction

The app should no longer ask the user to "enter a side world" to maintain emotional attachment. Instead, the core loop remains:

`记录 -> 回顾 -> 整理 -> 不经意解锁成就`

The achievement system becomes a passive reward layer:

- it does not interrupt the writing flow aggressively
- it does not require intentional grinding
- it does not look like medals, badges, or emoji trophies
- it should feel like a private archive of lived moments

## Scope

### Remove from user experience

- Pet screen
- Island screen
- Island timeline screen
- Nurturing-world entry cards, journey cards, and related visual state
- Title wall as a standalone reward system
- Pet reminder notifications
- Cross-feature nurturing prompts that push users into a separate loop

### Keep and strengthen

- Unified achievements database and unlock logic
- Achievement notification pipeline
- Achievement page as a richer "collection gallery"
- Explicit unlock conditions and progress tracking
- Hidden achievements, but never as meaningless vague blur

## UX Principles

### 1. Achievement is a consequence, not a task list

The user should not feel managed by the system.

- show progress only when it helps understanding
- keep copy concrete and grounded
- avoid "go do X now" language

### 2. The page is a gallery, not a mission board

The achievement screen should read as:

- a quiet archive
- a collection of life fragments
- a place to revisit what has already happened

### 3. Rich but gentle feedback

Unlocks should feel satisfying without becoming noisy.

- small in-flow confirmation after a meaningful action
- strong detail view when the user chooses to inspect
- no exaggerated game celebration effects

## Visual Direction

Do **not** use:

- emoji-centric visuals
- medal / badge / crest language
- pixel art
- mixed art styles across rarity tiers

Use:

- unified "life fragment" card language
- soft archival / scene-fragment tone
- warm, low-contrast surfaces with clear hierarchy
- rarity expressed by composition density, color temperature, and framing polish rather than style changes

## Screen Structure

### Top Overview

- unlocked count
- this month unlocked count
- recent unlock summary
- one large hero panel with calm copy

### Filter Layer

- category filters
- state filters: all / unlocked / near completion / hidden

### Recent Unlocks

- horizontal strip of recently unlocked achievements
- fast access to detail

### Collection Grid

- card-based gallery
- unlocked cards show the full visual treatment
- locked cards still show exact requirements and visible progress
- hidden locked cards conceal name/content, but still communicate that they are discoverable

### Detail Sheet

- name
- exact condition
- current progress
- unlock time
- flavor text
- category and rarity

## Data / Architecture Direction

### Keep database tables stable for this release

To reduce migration risk in the release build:

- keep existing Room entities for title / pet / island in the schema for now
- remove user-facing navigation and runtime initialization first
- leave deeper schema cleanup for a later migration-focused pass

### Simplify cross-system shared state

`CrossSystemManager` should be reduced to achievement-focused information only:

- recent achievement unlock
- next achievement milestone

Pet- and island-derived state should stop driving achievement UI.

## Release Goal

The release must ship with:

- user-facing nurturing features removed
- tools/navigation cleaned up
- achievement page redesigned around a single coherent direction
- tests updated to match the new architecture
- a fresh experimental version and GitHub release

# v2.71.27-experimental

## New Features
- **Focus Mode** - Pomodoro timer with ambient sounds (rain, cafe, white noise), duration selection (15/25/45/60 min), session history
- **Goals Hierarchy** - Sub-goals support with expandable tree view, add/delete sub-goals
- **Quick Checkin Camera** - Take photo during quick checkin with camera integration

## Bug Fixes
- Update dialog: added close button to cancel downloads
- Backup file list: fixed filename layout overflow issue

## Technical Details
- 12 new ViewModels connecting screens to real database data
- All feature screens now use real data instead of hardcoded values
- Focus mode with timer, ambient sound selection, and session tracking
- Goals now support parentId for hierarchy

## Experimental update
- refines the diary editor with more stable text-color syncing, cleaner checkbox/bullet list behavior, and stronger caret visibility while typing above the keyboard/toolbar area
- reduces false draft recovery prompts after normal saves by tightening the draft-clear flow and clearing matching draft-list items after a successful save
- tightens the diary header metadata layout, keeps long tag labels on their own row, and removes the visible inspiration entry to keep the recording view cleaner
- updates the todo tab swipe transition to a lighter slide-and-fade cutover with less "page flip" feeling

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:assembleExperimentalRelease` passed

## Known issue
- local unit-test execution is still unreliable on this machine when Gradle runs under Java 21, with the existing Gradle worker bootstrap failure noted in `docs/build-notes.md`
- recommended mitigation remains: run Gradle test tasks with JDK 17 for reliable local unit-test execution

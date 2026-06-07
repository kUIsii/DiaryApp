## Experimental update
- refine todo page swipe transition to a lighter horizontal slide with subtle fade
- tighten diary editor metadata chips, weather/mood/tag/location panels, and text sizing
- move writing prompt entry to a compact "灵感" action for a cleaner header
- fix transparent editor background layering in light and dark themes
- add low-risk utility/test coverage for selected-name summaries
- normalize Room migration parameter naming for cleaner maintenance

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:assembleExperimentalRelease` passed

## Known issue
- `testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest` is still blocked by a local Gradle Test Executor / worker bootstrap issue on this Windows + JDK 21 environment (`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`), not by a failing business assertion in the new test itself.

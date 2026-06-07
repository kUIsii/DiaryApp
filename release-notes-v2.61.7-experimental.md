## Experimental update
- keep the lighter todo swipe transition and tighter diary editor metadata layout from `2.61.6-experimental`
- add another round of low-risk stability hardening without changing user-facing flows
- replace a fragile unchecked cast path in home filtering with typed snapshot composition
- remove a few unnecessary non-null assertions in todo and editor-related flows
- add `docs/build-notes.md` to document the recommended Gradle/JDK setup and the current local unit-test worker issue

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:assembleExperimentalRelease` passed

## Known issue
- local unit-test execution is still unstable when Gradle runs on `Java 21` in this Windows environment
- recommended mitigation: run this project with `JDK 17` for Gradle tasks, especially unit tests

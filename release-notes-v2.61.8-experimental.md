## Experimental update
- includes the latest UI polish from the recent todo swipe and diary editor refinement work
- includes the newest low-risk hardening commits that were added after `2.61.7-experimental`
- reduces several nullable-state crash paths in todo, countdown, changelog, stats, home, and editor-related flows
- keeps the current theme and interaction design intact while improving maintenance safety

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:assembleExperimentalRelease` passed

## Known issue
- local unit-test execution is still unstable when Gradle runs on `Java 21` in this Windows environment
- recommended mitigation: run Gradle with `JDK 17` for local test tasks

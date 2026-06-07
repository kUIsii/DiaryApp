## Experimental update
- bundles the latest low-risk hardening work from the current `experiment/v2-redesign` branch
- keeps the recent todo swipe / diary editor UI refinements
- further reduces force-unwrapped nullable state in editor, tag management, todo, countdown, changelog, and stats flows
- continues improving maintainability without intentionally changing existing user-facing behavior

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:assembleExperimentalRelease` passed

## Known issue
- local Gradle unit-test execution is still unreliable on this machine when running under `Java 21`
- recommended mitigation remains: use `JDK 17` for Gradle test tasks

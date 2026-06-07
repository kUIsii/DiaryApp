# Build Notes

## Recommended JDK

- Run Gradle for this project with `JDK 17`.
- The current Android/Gradle setup in this repo is:
  - Android Gradle Plugin `8.2.0`
  - Gradle wrapper `8.2`
- On this machine, running Gradle with `Java 21` can compile and assemble APKs, but local unit-test worker startup is unstable and may fail before tests execute.

## Known local issue

- `testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`
  can fail on Windows when Gradle is started with `Java 21`, with:
  `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`

## Verification path used here

1. `.\gradlew.bat clean`
2. `.\gradlew.bat :app:compileExperimentalDebugKotlin`
3. `.\gradlew.bat :app:assembleExperimentalRelease`

## Notes

- Avoid running `clean`, `compile`, `test`, and `assemble` in parallel against the same workspace build directory.
- If local unit tests need to be reliable on this machine, switch Gradle runtime to `JDK 17` first.

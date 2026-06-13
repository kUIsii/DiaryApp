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
- 以后所有 release notes 和更新日志都使用中文书写。

## Update and release pitfalls

- App-side "check update" does **not** look at git branches, commit history, or whether `origin/experiment/v2-redesign` moved forward.
- The update checker only reads GitHub Releases from:
  `https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases`
- If code was pushed but no GitHub Release was created, the app will still report "already latest" or simply not show the new build.

## Flavor/version rules that matter

- `stable` and `experimental` are treated as separate update channels.
- `UpdateChecker` uses `BuildConfig.FLAVOR` to filter releases:
  - `experimental` app only accepts release tags containing `experimental`
  - `stable` app only accepts release tags **not** containing `experimental`
- For the current experimental line used here:
  - branch: `experiment/v2-redesign`
  - versionName: `2.61.10-experimental`
  - versionCode: `56`
  - release tag: `v2.61.10-experimental`
- Matching the version string matters. If the APK, Gradle flavor version, and GitHub release tag do not line up, update detection can look broken.

## Important UX/debugging clarification

- In the current UI, the bottom version badge is only a display label unless explicitly wired to update logic.
- The actual manual check path that worked here was the dedicated update-check row in the About/Profile area.
- So "tapping the version label does nothing" and "manual update check does not find a newer version" are two different problems:
  - first is a UI binding issue
  - second is usually a release/version/channel issue

## Release publishing notes

- A successful `git push` is not enough. You must also publish a GitHub Release and upload the APK asset.
- The APK built for this release was:
  `app/build/outputs/apk/experimental/release/app-experimental-release.apk`
- The release notes file used was:
  `release-notes-v2.61.10-experimental.md`
- When creating the GitHub Release with `gh release create`, using `--target 512b31d` failed with:
  `Release.target_commitish is invalid`
- Using the remote branch name worked:
  `--target experiment/v2-redesign`
- The release was successfully published as:
  `v2.61.10-experimental`
  with APK asset:
  `DiaryApp-v2.61.10-experimental.apk`

## Recommended verification order for future AI agents

1. Confirm the app flavor/version in `app/build.gradle.kts`.
2. Build the matching APK for that flavor.
3. Confirm the branch/commit is pushed.
4. Confirm whether the matching GitHub Release exists.
5. If release does not exist, create it and upload the APK.
6. Only after that, test in-app update detection.

## Quick checklist to avoid repeated mistakes

- Do not assume "code pushed" means "app can detect update".
- Do not debug update detection before checking GitHub Releases.
- Do not mix `stable` and `experimental` tags.
- Do not use a commit hash as `gh release create --target` here; prefer the remote branch name.
- If the user says "update was not detected", first check whether they are expecting:
  - a new branch commit
  - a new GitHub Release
  - or a clickable version badge behavior

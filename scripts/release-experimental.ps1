param(
    [string]$VersionName,
    [int]$VersionCode = 0,
    [string]$NotesFile,
    [string]$ReleaseNotes = "更新说明待补充",
    [switch]$NoBuild,
    [switch]$NoCommit,
    [switch]$NoRelease
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $FilePath $($Arguments -join ' ')"
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$buildFile = Join-Path $repoRoot "app/build.gradle.kts"
$buildText = Get-Content -LiteralPath $buildFile -Raw

$currentVersionMatch = [regex]::Match($buildText, 'versionName\s*=\s*"(?<version>\d+\.\d+\.\d+-experimental)"')
if (-not $currentVersionMatch.Success) {
    throw "Could not find current experimental versionName in $buildFile"
}

$currentCodeMatch = [regex]::Match($buildText, 'versionCode\s*=\s*(?<code>\d+)')
if (-not $currentCodeMatch.Success) {
    throw "Could not find current versionCode in $buildFile"
}

$currentVersion = $currentVersionMatch.Groups["version"].Value
$currentCode = [int]$currentCodeMatch.Groups["code"].Value

if ([string]::IsNullOrWhiteSpace($VersionName)) {
    $base = $currentVersion -replace '-experimental$', ''
    $parts = $base.Split(".")
    if ($parts.Count -ne 3) {
        throw "Version must use major.minor.patch format: $currentVersion"
    }

    $patch = [int]$parts[2] + 1
    $VersionName = "$($parts[0]).$($parts[1]).$patch-experimental"
}

if ($VersionName -notmatch '^\d+\.\d+\.\d+-experimental$') {
    throw "Experimental release version must look like 2.64.96-experimental"
}

if ($VersionCode -le 0) {
    $VersionCode = $currentCode + 1
}

$tagName = "v$VersionName"
$updatedBuildText = $buildText
$updatedBuildText = [regex]::Replace(
    $updatedBuildText,
    'versionName\s*=\s*"\d+\.\d+\.\d+-experimental"',
    "versionName = `"$VersionName`""
)
$updatedBuildText = [regex]::Replace(
    $updatedBuildText,
    'versionCode\s*=\s*\d+',
    "versionCode = $VersionCode"
)
Set-Content -LiteralPath $buildFile -Value $updatedBuildText -NoNewline

if ([string]::IsNullOrWhiteSpace($NotesFile)) {
    $notesPath = Join-Path ([System.IO.Path]::GetTempPath()) "diaryapp-$VersionName-release-notes.md"
    Set-Content -LiteralPath $notesPath -Value $ReleaseNotes -NoNewline
    $NotesFile = $notesPath
}

if (-not (Test-Path -LiteralPath $NotesFile)) {
    throw "Release notes file not found: $NotesFile"
}

if (-not $NoBuild) {
    Invoke-Checked ".\gradlew.bat" @(":app:assembleExperimentalRelease", "--no-daemon", "--max-workers=1")
}

$apkDir = Join-Path $repoRoot "app/build/outputs/apk/experimental/release"
$apk = Get-ChildItem -LiteralPath $apkDir -Filter "*.apk" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $apk) {
    throw "Could not find an experimental release APK in $apkDir"
}

if (-not $NoCommit) {
    Invoke-Checked "git" @("add", "app/build.gradle.kts", "scripts/release-experimental.ps1")
    Invoke-Checked "git" @("commit", "-m", "release: $tagName")
    Invoke-Checked "git" @("tag", $tagName)
    $branch = (& git branch --show-current).Trim()
    if ([string]::IsNullOrWhiteSpace($branch)) {
        throw "Could not determine current git branch"
    }
    Invoke-Checked "git" @("push", "origin", $branch)
    Invoke-Checked "git" @("push", "origin", $tagName)
}

if (-not $NoRelease) {
    $branch = (& git branch --show-current).Trim()
    Invoke-Checked "gh" @(
        "release",
        "create",
        $tagName,
        $apk.FullName,
        "--target",
        $branch,
        "--title",
        $tagName,
        "--notes-file",
        $NotesFile
    )
}

Write-Host "Experimental release prepared: $tagName"
Write-Host "APK: $($apk.FullName)"

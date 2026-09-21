$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

python tools/check_release_invariants.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$tasks = @("testDebugUnitTest", "lintDebug", "assembleDebug", "assembleRelease")
foreach ($task in $tasks) {
    & .\gradlew.bat $task
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($null -ne $adb) {
    $devices = (& adb devices) | Select-String "\tdevice$"
    if ($devices) {
        & .\gradlew.bat connectedDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } else {
        Write-Host "No connected Android device/emulator; skipping connectedDebugAndroidTest."
    }
} else {
    Write-Host "adb not found; skipping connectedDebugAndroidTest."
}

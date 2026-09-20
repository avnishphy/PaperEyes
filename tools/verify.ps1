$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$python = Get-Command python -ErrorAction SilentlyContinue
if ($null -ne $python) {
    & python tools/check_release_invariants.py
} else {
    $py = Get-Command py -ErrorAction SilentlyContinue
    if ($null -eq $py) {
        throw "Python 3 was not found. Install Python or make python/py available on PATH."
    }
    & py -3 tools/check_release_invariants.py
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$tasks = @("testDebugUnitTest", "lintDebug", "assembleDebug", "assembleRelease")
foreach ($task in $tasks) {
    & .\gradlew.bat $task
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$adbPath = $null
$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($null -ne $adb) {
    $adbPath = $adb.Source
} else {
    $defaultAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $defaultAdb) {
        $adbPath = $defaultAdb
    }
}

if ($null -ne $adbPath) {
    $devices = (& $adbPath devices) | Select-String "\tdevice$"
    if ($devices) {
        & .\gradlew.bat connectedDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } else {
        Write-Host "No connected Android device/emulator; skipping connectedDebugAndroidTest."
    }
} else {
    Write-Host "adb not found; skipping connectedDebugAndroidTest."
}

<#
.SYNOPSIS
Runs the instrumented e2e tests on a disposable fleet emulator with a managed profile.

.DESCRIPTION
The outer call boots the emulator through the fleet.py run-suite wrapper, which tears it down afterwards.
The wrapper runs this same script with -Inner. The inner run creates a managed profile named Work,
starts it, runs connectedDebugAndroidTest with the profile id as the managedUserId argument, and removes the profile.
The tests skip themselves when no managed profile id is available, so this script is what makes the six managed-profile tests run.

Only emulator serials are accepted. Physical devices and the interactive emulator on port 5554 are refused.

.EXAMPLE
.\scripts\ci\run_local_e2e.ps1 -FleetScript <path to fleet.py>
.\scripts\ci\run_local_e2e.ps1 -FleetScript <path to fleet.py> -Key a16 -Tests com.mohitarora.dualclone.e2e.CloneRemoveE2eTest
#>
param(
    [string]$Key = "a16",
    [string]$FleetScript = $env:DUALCLONE_FLEET_SCRIPT,
    [string]$Tests = "",
    [switch]$Inner
)

$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "..\..")

if (-not $Inner) {
    if (-not $FleetScript -or -not (Test-Path $FleetScript)) {
        throw "Pass -FleetScript <path to fleet.py> or set DUALCLONE_FLEET_SCRIPT."
    }
    $self = Join-Path $PSScriptRoot "run_local_e2e.ps1"
    $innerCommand = "pwsh -NoProfile -File `"$self`" -Inner"
    if ($Tests) { $innerCommand += " -Tests `"$Tests`"" }
    python $FleetScript run-suite $Key -- $innerCommand
    exit $LASTEXITCODE
}

$serial = $env:ANDROID_SERIAL
if (-not $serial -or $serial -notmatch '^emulator-\d+$' -or $serial -eq "emulator-5554") {
    throw "Refusing to run on '$serial'. Only fleet emulators are allowed, never emulator-5554 or a physical device."
}

function Invoke-DeviceAdb { adb.exe -s $serial @args }

# NOTE: wait-for-device never returns when the emulator died, so poll with a deadline instead.
$deadline = (Get-Date).AddMinutes(4)
while ((Get-Date) -lt $deadline) {
    if ((Invoke-DeviceAdb get-state 2>$null) -eq "device" -and (Invoke-DeviceAdb shell getprop sys.boot_completed 2>$null).Trim() -eq "1") { break }
    Start-Sleep -Seconds 3
}
if ((Invoke-DeviceAdb shell getprop sys.boot_completed 2>$null).Trim() -ne "1") { throw "Emulator $serial did not finish booting within 4 minutes." }

# NOTE: A run that was killed leaves its profile behind, and Android refuses a second managed profile on the same user.
foreach ($line in (Invoke-DeviceAdb shell pm list users)) {
    if ($line -match 'UserInfo\{(\d+):Work:') { Invoke-DeviceAdb shell pm remove-user $Matches[1] | Out-Host }
}

$created = (Invoke-DeviceAdb shell pm create-user --profileOf 0 --managed Work) -join " "
Write-Host $created
if ($created -notmatch 'user id (\d+)') { throw "Could not create the managed profile: $created" }
$userId = [int]$Matches[1]
$exit = 1
try {
    Invoke-DeviceAdb shell am start-user $userId | Out-Host
    Invoke-DeviceAdb shell pm list users | Out-Host

    $gradleArgs = @("connectedDebugAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.managedUserId=$userId", "--console=plain")
    if ($Tests) { $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.class=$Tests" }
    Push-Location $repo
    try {
        & .\gradlew.bat @gradleArgs
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    Invoke-DeviceAdb shell pm remove-user $userId | Out-Host
}
exit $exit

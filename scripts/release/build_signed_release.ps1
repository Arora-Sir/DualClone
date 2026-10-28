# Builds and verifies the signed release APK. Run it yourself in PowerShell.
# The password is typed into a hidden prompt, lives only in this process, and is never written to disk.
param(
    [string]$KeystorePath = "$PSScriptRoot\..\..\release-signing\release.jks",
    [string]$Alias = "dualclone"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\..\.."
$ks = Resolve-Path $KeystorePath

if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
$secure = Read-Host "Keystore password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $env:KEYSTORE_FILE = $ks.Path
    $env:KEYSTORE_PASSWORD = $plain
    $env:KEY_ALIAS = $Alias
    $env:KEY_PASSWORD = $plain
    Push-Location $root
    & .\gradlew.bat assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
    $apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
    python scripts/release/verify_release.py --expect-version (Select-String -Path app\build.gradle.kts -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value $apk
    Write-Host "Signed APK: $apk"
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    Remove-Item Env:KEYSTORE_FILE, Env:KEYSTORE_PASSWORD, Env:KEY_ALIAS, Env:KEY_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location -ErrorAction SilentlyContinue
}

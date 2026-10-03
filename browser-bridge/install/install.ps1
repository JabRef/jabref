#!powershell -ExecutionPolicy Bypass -File
<#
.SYNOPSIS
  Windows installer for the JabRef Browser-Extension fulltext bridge host.

.DESCRIPTION
  Writes the native-messaging manifest (pointing at jabext_host.bat, which
  launches jabext_host.ps1) into per-browser disk locations and registers it
  under HKCU for every locally-installed Chromium / Firefox browser.

.PARAMETER HostPath
  Path to the native-messaging launcher. Defaults to ..\jabext_host.bat
  relative to this script.
#>

[CmdletBinding()]
param(
  [string] $HostPath
)

$ErrorActionPreference = "Stop"

$here = $PSScriptRoot
if (-not $here) { $here = Split-Path -Parent $MyInvocation.MyCommand.Path }
$repo = (Resolve-Path (Join-Path $here "..")).Path

if (-not $HostPath) {
  $HostPath = Join-Path $repo "jabext_host.bat"
}

if (-not (Test-Path -LiteralPath $HostPath)) {
  throw "Host launcher missing at $HostPath."
}

$hostAbs = (Resolve-Path -LiteralPath $HostPath).Path
$hostJson = $hostAbs.Replace("\", "\\")

$state = Join-Path $env:APPDATA "JabRef\fulltext-providers-state"
New-Item -ItemType Directory -Force -Path $state | Out-Null

$manifestDir = Join-Path $state "native-messaging"
New-Item -ItemType Directory -Force -Path $manifestDir | Out-Null

# ---- Firefox manifest ----
$fxTemplate = Get-Content -LiteralPath (Join-Path $repo "native-messaging\firefox.json.template") -Raw
$fxOut = Join-Path $manifestDir "firefox.json"
$fxTemplate.Replace("@BRIDGE_PATH@", $hostJson) | Set-Content -LiteralPath $fxOut -Encoding utf8
Write-Host "[install] firefox manifest: $fxOut"

# ---- Chromium manifest ----
$chTemplate = Get-Content -LiteralPath (Join-Path $repo "native-messaging\chromium.json.template") -Raw
$chOut = Join-Path $manifestDir "chromium.json"
$chTemplate.Replace("@BRIDGE_PATH@", $hostJson) | Set-Content -LiteralPath $chOut -Encoding utf8
Write-Host "[install] chromium manifest: $chOut"

# ---- HKCU registration ----
function Register-Hive {
  param([string] $Hive, [string] $ManifestPath)
  if (-not (Test-Path -LiteralPath $ManifestPath)) { return }
  $key = "HKCU:$Hive\jabext_bridge"
  New-Item -Path $key -Force | Out-Null
  Set-ItemProperty -Path $key -Name "(default)" -Value $ManifestPath
  Write-Host "[install] registered $key"
}

Register-Hive "\Software\Mozilla\NativeMessagingHosts"                       $fxOut
Register-Hive "\Software\Google\Chrome\NativeMessagingHosts"                 $chOut
Register-Hive "\Software\Chromium\NativeMessagingHosts"                      $chOut
Register-Hive "\Software\Microsoft\Edge\NativeMessagingHosts"                $chOut
Register-Hive "\Software\BraveSoftware\Brave-Browser\NativeMessagingHosts"   $chOut
Register-Hive "\Software\Vivaldi\NativeMessagingHosts"                       $chOut

Write-Host "[install] done. Reload the JabRef Browser Extension to launch the host."

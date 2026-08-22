#!powershell -ExecutionPolicy Bypass -File
<#
.SYNOPSIS
  Windows installer for the JabRef Browser-Extension fulltext bridge.

.DESCRIPTION
  Writes the native-messaging manifest into per-browser disk locations and
  registers it under HKCU for every locally-installed Chromium / Firefox
  browser.

.PARAMETER BridgePath
  Path to the bridge executable. Defaults to ..\build\jabext-bridge.exe
  relative to this script.
#>

[CmdletBinding()]
param(
  [string] $BridgePath,
  [string] $Download
)

$ErrorActionPreference = "Stop"

$here = $PSScriptRoot
if (-not $here) { $here = Split-Path -Parent $MyInvocation.MyCommand.Path }
$repo = (Resolve-Path (Join-Path $here "..")).Path

if (-not $BridgePath) {
  $BridgePath = Join-Path $here "..\build\jabext-bridge.exe"
}

if ($Download) {
  $asset = "jabext-bridge_windows_x86_64.exe"
  $cacheDir = Join-Path $env:LOCALAPPDATA "jabext-bridge\cache\$Download"
  New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
  $cachedExe = Join-Path $cacheDir $asset
  if (-not (Test-Path -LiteralPath $cachedExe)) {
    if ($Download -eq "latest") {
      $api = Invoke-RestMethod -Uri "https://api.github.com/repos/JabRef/JabRef-Browser-Extension-experimental/releases/latest"
      $tag = $api.tag_name
    } else {
      $tag = $Download
    }
    $url = "https://github.com/JabRef/JabRef-Browser-Extension-experimental/releases/download/$tag/$asset"
    Write-Host "[install] downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $cachedExe -UseBasicParsing
  }
  $BridgePath = $cachedExe
}

if (-not (Test-Path -LiteralPath $BridgePath)) {
  throw "Bridge binary missing at $BridgePath. Run 'browser-bridge/build.sh' or pass -Download <version>."
}

$bridgeAbs = (Resolve-Path -LiteralPath $BridgePath).Path
$bridgeJson = $bridgeAbs.Replace("\", "\\")

$state = Join-Path $env:APPDATA "JabRef\fulltext-providers-state"
New-Item -ItemType Directory -Force -Path $state | Out-Null

$manifestDir = Join-Path $state "native-messaging"
New-Item -ItemType Directory -Force -Path $manifestDir | Out-Null

# ---- Firefox manifest ----
$fxTemplate = Get-Content -LiteralPath (Join-Path $repo "native-messaging\firefox.json.template") -Raw
$fxOut = Join-Path $manifestDir "firefox.json"
$fxTemplate.Replace("@BRIDGE_PATH@", $bridgeJson) | Set-Content -LiteralPath $fxOut -Encoding utf8
Write-Host "[install] firefox manifest: $fxOut"

# ---- Chromium manifest ----
$chTemplate = Get-Content -LiteralPath (Join-Path $repo "native-messaging\chromium.json.template") -Raw
$chOut = Join-Path $manifestDir "chromium.json"
$chTemplate.Replace("@BRIDGE_PATH@", $bridgeJson) | Set-Content -LiteralPath $chOut -Encoding utf8
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

Write-Host "[install] done. Reload the JabRef Browser Extension to launch the bridge."

@echo off
rem Native-messaging launcher for the JabRef fulltext bridge host on Windows.
rem Browsers can only launch a .exe or .bat as a native-messaging host, so this
rem wraps jabext_host.ps1 (mirrors buildres/windows/JabRefHost.bat). stdio is
rem inherited, which is what the native-messaging protocol needs.
where pwsh.exe >nul 2>nul
if %ERRORLEVEL%==0 (
  pwsh.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden -File "%~dp0jabext_host.ps1"
) else (
  powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden -File "%~dp0jabext_host.ps1"
)

@echo off
pushd %~dp0

:: Test if pwsh exists
setlocal enabledelayedexpansion
where /q pwsh.exe
if !ERRORLEVEL!==0 (
    @pwsh.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden -File ".\JabRefHost.ps1"
) else (
    :: If not, test if powershell exists
    where /q powershell.exe
    if !ERRORLEVEL!==0 (
        @powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden -File ".\JabRefHost.ps1"
    ) else (
        echo "Could not find pwsh.exe or powershell.exe" 1>&2
        echo "Please install PowerShell and try again." 1>&2
        exit /b 1
    )
)
endlocal

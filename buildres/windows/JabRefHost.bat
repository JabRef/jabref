@echo off
pushd %~dp0
@powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden -File ".\JabRefHost.ps1"

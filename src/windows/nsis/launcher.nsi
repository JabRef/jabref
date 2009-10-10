;
; JabRef Application launcher
;
; Script based upon:
; Jelude launcher - http://www.sfu.ca/~tyuen/jelude/ 
; Java Launcher http://nsis.sourceforge.net/archive/nsisweb.php?page=326&instances=0,64
;
; Christopher Oezbek - oezi@oezi.de - 2006

;--------- CONFIGURATION ---------
!define APPNAME "JabRef"
!ifndef JARFILE
    !define JARFILE "JabRef.jar"
!endif
!ifdef APPICON
    Icon "${APPICON}"
!endif
Name "${APPNAME}"
Caption "${APPNAME}"
OutFile "dist\${APPNAME}.exe"
;-------- END CONFIGURATION ------

SilentInstall silent
XPStyle on
RequestExecutionLevel user

Section ""

  Call GetJRE
  Pop $R0

  StrCpy $R1 ""
  Call GetParameters
  Pop $R1

  StrCpy $R0 '"$R0" -Xms32m -Xmx512m -jar "${JARFILE}" $R1'
 
  SetOutPath $EXEDIR
  Exec "$R0"

  Quit
SectionEnd

Function GetParameters
  Push $R0
  Push $R1
  Push $R2
  StrCpy $R0 $CMDLINE 1
  StrCpy $R1 '"'
  StrCpy $R2 1
  StrCmp $R0 '"' loop
  StrCpy $R1 ' '
  loop:
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 $R1 loop2
    StrCmp $R0 "" loop2
    IntOp $R2 $R2 + 1
    Goto loop
  loop2:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 " " loop2
  StrCpy $R0 $CMDLINE "" $R2
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd

Function GetJRE
;
;  Find JRE (Java.exe)
;  1 - in .\jre directory (JRE Installed with application)
;  2 - in JAVA_HOME environment variable
;  3 - in the registry
;  4 - assume java.exe in current dir or PATH
  Push $R0
  Push $R1

  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\javaw.exe"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""

  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfErrors 0 JreFound

  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\javaw.exe"

  IfErrors 0 JreFound
  Sleep 800
  MessageBox MB_ICONEXCLAMATION|MB_YESNO \
               'Could not find a Java Runtime Environment installed on your computer. \
               $\nWithout it you cannot run "${APPNAME}". \
               $\n$\nWould you like to visit the Java website to download it?' \
               IDNO +2
  ExecShell open "http://java.sun.com/getjava"
  Quit
        
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd

# owner		JabRef Team
# license	GPL version 2
# author	Uwe Stöhr
# file version	2.0	date	20-11-2007

; To compile this script NSIS 2.30 or newer are required
; http://nsis.sourceforge.net/


; Do a Cyclic Redundancy Check to make sure the installer
; was not corrupted by the download.
CRCCheck force

; Make the installer as small as possible.
SetCompressor lzma

; set execution level for Windows Vista
RequestExecutionLevel user

# general definitions
; you only need to change this section for new releases
; you only need to change this section for new releases
VIProductVersion "2.4.0.0" ; file version for the installer in the scheme "x.x.x.x"
!ifndef VERSION
	!define VERSION "2.4"
!endif
Name "JabRef ${VERSION}"
!define REGKEY "SOFTWARE\JabRef"
!define COMPANY "JabRef Team"
!define URL "http://jabref.sourceforge.net"
!define PRODUCT_NAME "JabRef"
!define PRODUCT_EXE "$INSTDIR\JabRef.exe"
!define PRODUCT_EXE2 "JabRef.exe"
!define PRODUCT_REGNAME "BibTeX.Document"
!define PRODUCT_EXT ".bib"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
!define PRODUCT_LICENSE_FILE "dist\gpl.txt"


; registry preparations
!define SHCNE_ASSOCCHANGED 0x08000000
!define SHCNF_IDLIST 0


# Variables
Var StartmenuFolder
Var CreateFileAssociations
Var CreateDesktopIcon
Var Answer
Var UserName


# Included files
!include "MUI.nsh"
!include "LogicLib.nsh"


# macros
!macro IsUserAdmin Result UName

 ClearErrors
 UserInfo::GetName
 IfErrors Win9x
 Pop $0
 StrCpy ${UName} $0
 UserInfo::GetAccountType
 Pop $1
 ${if} $1 == "Admin"
  StrCpy ${Result} "yes"
 ${else}
  StrCpy ${Result} "no"
 ${endif}
 Goto done

 Win9x:
  StrCpy ${Result} "yes"
 done:

!macroend


# Installer pages
; Remember the installer language
!define MUI_LANGDLL_REGISTRY_ROOT "HKCU"
!define MUI_LANGDLL_REGISTRY_KEY "${PRODUCT_UNINST_KEY}"
!define MUI_LANGDLL_REGISTRY_VALUENAME "Installer Language"

; let warning appear when installation is canceled
!define MUI_ABORTWARNING

; Icons for the installer program
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install-full.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall-full.ico"

; Welcome page
!define MUI_WELCOMEPAGE_TEXT "$(WelcomePageText)"
!insertmacro MUI_PAGE_WELCOME

; Show the license.
!insertmacro MUI_PAGE_LICENSE "${PRODUCT_LICENSE_FILE}"

; Specify the installation directory.
!insertmacro MUI_PAGE_DIRECTORY

; choose the components to install.
!insertmacro MUI_PAGE_COMPONENTS

; Specify where to install program shortcuts.
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKLM"
!define MUI_STARTMENUPAGE_REGISTRY_KEY "${PRODUCT_UNINST_KEY}"
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
!define MUI_STARTMENUPAGE_DEFAULTFOLDER "$(^Name)"
!insertmacro MUI_PAGE_STARTMENU ${PRODUCT_NAME} $StartmenuFolder

; Watch the components being installed.
!insertmacro MUI_PAGE_INSTFILES

; Finish page
!define MUI_FINISHPAGE_RUN "${PRODUCT_EXE}"
!define MUI_FINISHPAGE_TEXT "$(FinishPageMessage)"
!define MUI_FINISHPAGE_RUN_TEXT "$(FinishPageRun)"
!insertmacro MUI_PAGE_FINISH

; The uninstaller
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES


# Installer languages
!insertmacro MUI_LANGUAGE "English" # first language is the default language
!insertmacro MUI_LANGUAGE "German"

!include "installer_languages\english.nsh"
!include "installer_languages\german.nsh"


# Installer attributes
OutFile JabRefSetup.exe
InstallDir "$PROGRAMFILES\JabRef"
BrandingText "$(^Name) installer" ; appear at the bottom of the installer windows
XPStyle on ; use XP style for installer windows
LicenseData "$(JabRefLicenseData)"

; creates file informations for the JabRefSetup.exe
VIAddVersionKey ProductName "JabRef"
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileDescription "JabRef installation program"
VIAddVersionKey LegalCopyright "under the GPL version 2"
VIAddVersionKey FileVersion ""


# Installer sections
Section "!JabRef" SecCore
 SectionIn RO
SectionEnd

Section "$(SecAssociateBibTitle)" SecAssociateBib
 StrCpy $CreateFileAssociations "true"
SectionEnd

Section "$(SecDesktopTitle)" SecDesktop
 StrCpy $CreateDesktopIcon "true"
SectionEnd

; section descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecAssociateBib} "$(SecAssociateBibDescription)"
!insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} "$(SecDesktopDescription)"
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "-Installation actions" SecInstallation

  ; copy files
  SetOutPath "$INSTDIR"
  SetOverwrite on
  File /r dist\*.*
  WriteRegStr SHCTX "${REGKEY}\Components" Main 1
  
  ; register JabRef
  WriteRegStr SHCTX "${REGKEY}" Path $INSTDIR
  WriteUninstaller $INSTDIR\uninstall.exe
  
  ; create shortcuts to startmenu
  SetOutPath "$INSTDIR"
  CreateDirectory "$SMPROGRAMS\$StartmenuFolder"
  CreateShortCut "$SMPROGRAMS\$StartmenuFolder\$(^Name).lnk" "${PRODUCT_EXE}" "" "$INSTDIR\JabRef.exe"
  CreateShortCut "$SMPROGRAMS\$StartmenuFolder\Uninstall $(^Name).lnk" "$INSTDIR\uninstall.exe"
  
  ; create desktop icon
  ${if} $CreateDesktopIcon == "true"
   SetOutPath "$INSTDIR"
   CreateShortCut "$DESKTOP\$(^Name).lnk" "${PRODUCT_EXE}" "" "${PRODUCT_EXE}" ;$(^Name).lnk
  ${endif}
  WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "StartMenu" "$SMPROGRAMS\$StartmenuFolder"
  ${if} $Answer == "yes" ; if user is admin
  
   ; register information that appear in Windows' software listing
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${VERSION}"
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "Publisher" "${COMPANY}"
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${URL}"
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "DisplayIcon" "${PRODUCT_EXE}"
   WriteRegStr SHCTX "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninstall.exe"   
   WriteRegDWORD SHCTX "${PRODUCT_UNINST_KEY}" "NoModify" 0x00000001
   WriteRegDWORD SHCTX "${PRODUCT_UNINST_KEY}" "NoRepair" 0x00000001
  ${endif}
  
  # register the extension .bib
  ${if} $CreateFileAssociations == "true"
   # write informations about file type
   WriteRegStr SHCTX "Software\Classes\${PRODUCT_REGNAME}" "" "${PRODUCT_NAME} Document"
   WriteRegStr SHCTX "Software\Classes\${PRODUCT_REGNAME}\DefaultIcon" "" "${PRODUCT_EXE},0"
   WriteRegStr SHCTX "Software\Classes\${PRODUCT_REGNAME}\Shell\open\command" "" '"${PRODUCT_EXE}" "%1"'
   # write informations about file extensions
   WriteRegStr SHCTX "Software\Classes\${PRODUCT_EXT}" "" "${PRODUCT_REGNAME}"
   # refresh shell
   System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) (${SHCNE_ASSOCCHANGED}, ${SHCNF_IDLIST}, 0, 0)'
  ${endif}

SectionEnd


# Uninstaller sections
Section "un.JabRef" un.SecUnProgramFiles

  SectionIn RO
  ; delete installation folder
  RMDir /r $INSTDIR	
  ; delete start menu entry
  ReadRegStr $0 SHCTX "${PRODUCT_UNINST_KEY}" "StartMenu"
  RMDir /r "$0"
  
  ; delete desktop icon
  Delete "$DESKTOP\$(^Name).lnk"
  
  # remove file extension 
  ReadRegStr $R0 SHCTX "Software\Classes\${PRODUCT_EXT}" ""
  ${if} $R0 == "${PRODUCT_REGNAME}"
   DeleteRegKey SHCTX "Software\Classes\${PRODUCT_EXT}"
   DeleteRegKey SHCTX "Software\Classes\${PRODUCT_REGNAME}"
  ${endif}
  
  ; delete remaining registry entries
  DeleteRegKey HKCU "${PRODUCT_UNINST_KEY}"
  DeleteRegKey SHCTX "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKCR "Applications\${PRODUCT_EXE2}"
  DeleteRegKey HKCU "${REGKEY}"
  DeleteRegKey SHCTX "${REGKEY}"
  
  ; close uninstaller automatically
  SetAutoClose true

SectionEnd


# Installer functions
Function .onInit

  StrCpy $StartmenuFolder ${PRODUCT_NAME}

 # check if the same Jabref version is already installed
  ReadRegStr $0 SHCTX "${PRODUCT_UNINST_KEY}" "Publisher"
  ${if} $0 != ""
   MessageBox MB_OK|MB_ICONSTOP "$(StillInstalled)" /SD IDOK
   Abort
  ${endif}
  
 InitPluginsDir
  ; If the user does *not* have administrator privileges, abort
  StrCpy $Answer ""
  StrCpy $UserName ""
  !insertmacro IsUserAdmin $Answer $UserName ; macro from LyXUtils.nsh
  ${if} $Answer == "yes"
   SetShellVarContext all ; set that e.g. shortcuts will be created for all users
  ${else}
   SetShellVarContext current
   StrCpy $INSTDIR "$APPDATA\$(^Name)"
  ${endif}

FunctionEnd


# Uninstaller functions
Function un.onInit

  ; If the user does *not* have administrator privileges, abort
  StrCpy $Answer ""
  !insertmacro IsUserAdmin $Answer $UserName
  ${if} $Answer == "yes"
   SetShellVarContext all
  ${else}
   # check if the Jabref has been installed with admin permisions
   ReadRegStr $0 HKLM "${PRODUCT_UNINST_KEY}" "Publisher"
   ${if} $0 != ""
    MessageBox MB_OK|MB_ICONSTOP "$(UnNotAdminLabel)" /SD IDOK
    Abort
   ${endif}
   SetShellVarContext current
  ${endif}
  
  ; ask if it should really be removed
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "$(UnReallyRemoveLabel)" /SD IDYES IDYES +2
  Abort

FunctionEnd

Function un.onUninstSuccess

  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(UnRemoveSuccessLabel)" /SD IDOK

FunctionEnd


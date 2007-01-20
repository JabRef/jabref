!ifndef _JabRef_LANGUAGES_ENGLISH_NSH_
!define _JabRef_LANGUAGES_ENGLISH_NSH_

!ifdef JabRef_LANG
  !undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_ENGLISH}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "This wizard will guide you through the installation of JabRef.\r\n\
					     \r\n\
					     $_CLICK"

LangString FileTypeTitle "${JabRef_LANG}" "BibTeX-file"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Associate BibTeX-files"
LangString SecDesktopTitle "${JabRef_LANG}" "Desktop icon"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Files with the extension .bib will automatically be opened with JabRef."
LangString SecDesktopDescription "${JabRef_LANG}" "Puts JabRef icon on the desktop."

LangString OpenIn "${JabRef_LANG}" "Open in JabRef"

LangString NotAdmin "${JabRef_LANG}" "You must have administrator privileges to install JabRef!"

LangString FinishPageMessage "${JabRef_LANG}" "Congratulations! JabRef has been installed successfully."
LangString FinishPageRun "${JabRef_LANG}" "Launch JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "You must have administrator privileges to uninstall JabRef!"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Are you sure you want to completely remove JabRef?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "JabRef was successfully removed from your computer."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_ENGLISH_NSH_

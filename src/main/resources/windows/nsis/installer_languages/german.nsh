!ifndef _JabRef_LANGUAGES_GERMAN_NSH_
!define _JabRef_LANGUAGES_GERMAN_NSH_

!ifdef JabRef_LANG
  !undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_GERMAN}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "Dieser Assistent wird Sie durch die Installation von JabRef begleiten.\r\n\
					     \r\n\
					     $_CLICK"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Dateizuordnung für BibTeX-Dateien"
LangString SecDesktopTitle "${JabRef_LANG}" "Desktopsymbol"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Dateien mit der Endung .bib werden automatisch mit JabRef geöffnet."
LangString SecDesktopDescription "${JabRef_LANG}" "Erstellt Verknüpfung zu JabRef auf dem Desktop."

LangString StillInstalled "${JabRef_LANG}" "JabRef ${Version} ist bereits installiert!"

LangString FinishPageMessage "${JabRef_LANG}" "Glückwunsch! JabRef wurde erfolgreich installiert."
LangString FinishPageRun "${JabRef_LANG}" "JabRef starten"

LangString UnNotAdminLabel "${JabRef_LANG}" "Sie benötigen Administratorrechte um JabRef zu deinstallieren!"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Sind Sie sicher, dass sie JabRef deinstallieren möchten?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "JabRef wurde erfolgreich von ihrem Computer entfernt."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_GERMAN_NSH_

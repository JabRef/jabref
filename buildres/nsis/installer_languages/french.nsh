!ifndef _JabRef_LANGUAGES_FRENCH_NSH_
!define _JabRef_LANGUAGES_FRENCH_NSH_

!ifdef JabRef_LANG
  !undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_FRENCH}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "Cet assistant va vous guider au cours de l'installation de JabRef.\r\n\
                                            \r\n\
					    $_CLICK"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Association aux fichiers BibTeX"
LangString SecDesktopTitle "${JabRef_LANG}" "Ic�ne du bureau"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Les fichiers ayant l'extension .bib seront automatiquement ouverts avec JabRef."
LangString SecDesktopDescription "${JabRef_LANG}" "Ajoute l'ic�ne de JabRef sur le bureau."

LangString StillInstalled "${JabRef_LANG}" "JabRef ${Version} est dej� install�!"

LangString FinishPageMessage "${JabRef_LANG}" "F�licitations ! JabRef a �t� correctement install�."
LangString FinishPageRun "${JabRef_LANG}" "Lancer JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "Vous devez avoir les droits d'administration pour d�sinstaller JabRef !"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Etes-vous s�r de vouloir d�sinstaller compl�tement JabRef ?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "JabRef a �t� correctement d�sinstall� de votre ordinateur."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_FRENCH_NSH_

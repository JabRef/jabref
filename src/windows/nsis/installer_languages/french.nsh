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
LangString SecDesktopTitle "${JabRef_LANG}" "Icône du bureau"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Les fichiers ayant l'extension .bib seront automatiquement ouverts avec JabRef."
LangString SecDesktopDescription "${JabRef_LANG}" "Ajoute l'icône de JabRef sur le bureau."

LangString StillInstalled "${JabRef_LANG}" "JabRef ${Version} est dejà installé!"

LangString FinishPageMessage "${JabRef_LANG}" "Félicitations ! JabRef a été correctement installé."
LangString FinishPageRun "${JabRef_LANG}" "Lancer JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "Vous devez avoir les droits d'administration pour désinstaller JabRef !"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Etes-vous sûr de vouloir désinstaller complètement JabRef ?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "JabRef a été correctement désinstallé de votre ordinateur."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_FRENCH_NSH_

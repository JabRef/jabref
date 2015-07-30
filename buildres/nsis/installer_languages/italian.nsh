!ifndef _JabRef_LANGUAGES_ITALIAN_NSH_
!define _JabRef_LANGUAGES_ITALIAN_NSH_

!ifdef JabRef_LANG
  !undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_ITALIAN}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "Questa procedura ti guiderà durante il processo di installazione di JabRef.\r\n\
                                            \r\n\
					    $_CLICK"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Associazione dei file BibTeX"
LangString SecDesktopTitle "${JabRef_LANG}" "Icona del Desktop"

LangString SecAssociateBibDescription "${JabRef_LANG}" "I file con estensione .bib saranno aperti automaticamente con JabRef."
LangString SecDesktopDescription "${JabRef_LANG}" "Aggiungi l'icona di JabRef sul Desktop"

LangString StillInstalled "${JabRef_LANG}" "JabRef ${Version} is already installed!"

LangString FinishPageMessage "${JabRef_LANG}" "Congratulazioni! L'installazione di JabRef è terminata con successo."
LangString FinishPageRun "${JabRef_LANG}" "Avvia JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "Sono necessari privilegi di amministratore per disinstallare JabRef!"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Sei sicuro di voler disinstallare completamente JabRef?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "JabRef è stato correttamente disinstallato dal computer."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_ITALIAN_NSH_

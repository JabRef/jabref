!ifndef _JabRef_LANGUAGES_SPANISH_NSH_
!define _JabRef_LANGUAGES_SPANISH_NSH_

!ifdef JabRef_LANG
!undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_SPANISH}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "Este asistente le guiará a través del proceso de instalación de JabRef.\r\n\
					     \r\n\
					     $_CLICK"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Asociar archivos BibTex"
LangString SecDesktopTitle "${JabRef_LANG}" "Icono de escritorio"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Los ficheros con extensión .bib serán abiertos con JabRef"
LangString SecDesktopDescription "${JabRef_LANG}" "Inserta el icono de JabRef en el escritorio."

LangString StillInstalled "${JabRef_LANG}" "¡JabRef ${Version} ya está instalado!"

LangString FinishPageMessage "${JabRef_LANG}" "Enhorabuena. JabRef se ha instalado con éxito."
LangString FinishPageRun "${JabRef_LANG}" "Iniciar JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "Debe tener privilegios de administrador para poder instalar JabRef!"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "¿Está seguro de querer eliminar JabRef por completo?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "La desinstalación de JabRef ha tenido éxito."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_SPANISH_NSH_
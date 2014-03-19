!ifndef _JabRef_LANGUAGES_RUSSIAN_NSH_
!define _JabRef_LANGUAGES_RUSSIAN_NSH_

!ifdef JabRef_LANG
  !undef JabRef_LANG
!endif
!define JabRef_LANG ${LANG_RUSSIAN}

LicenseLangString JabRefLicenseData ${JabRef_LANG} "${PRODUCT_LICENSE_FILE}"

LangString WelcomePageText "${JabRef_LANG}" "Вас приветствует мастер установки приложения JabRef!\r\n\
					     \r\n\
					     $_CLICK"

LangString SecAssociateBibTitle "${JabRef_LANG}" "Связать с файлами BibTeX"
LangString SecDesktopTitle "${JabRef_LANG}" "Значок на рабочем столе"

LangString SecAssociateBibDescription "${JabRef_LANG}" "Файлы с расширением .bib будут открываться с помощью приложения JabRef по умолчанию."
LangString SecDesktopDescription "${JabRef_LANG}" "Поместить значок приложения JabRef на рабочий стол."

LangString StillInstalled "${JabRef_LANG}" "Существует установленный экземпляр приложения JabRef ${Version}!"

LangString FinishPageMessage "${JabRef_LANG}" "Поздравляем! Приложение JabRef успешно установлено."
LangString FinishPageRun "${JabRef_LANG}" "Запустить JabRef"

LangString UnNotAdminLabel "${JabRef_LANG}" "Для удаления приложения JabRef вы должны иметь права администратора!"
LangString UnReallyRemoveLabel "${JabRef_LANG}" "Приложение JabRef будет полностью удалено. Продолжить?"
LangString UnRemoveSuccessLabel "${JabRef_LANG}" "Приложение JabRef успешно удалено."


!undef JabRef_LANG

!endif ; _JabRef_LANGUAGES_RUSSIAN_NSH_

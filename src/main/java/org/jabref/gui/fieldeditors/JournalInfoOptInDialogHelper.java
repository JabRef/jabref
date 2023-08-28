package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.logic.l10n.Localization;

public class JournalInfoOptInDialogHelper {

    /**
     * Using the journal information data fetcher service needs to be opt-in for GDPR compliance.
     */
    public static boolean isJournalInfoEnabled(DialogService dialogService, EntryEditorPreferences preferences) {
        if (preferences.shouldEnableJournalPopup() == EntryEditorPreferences.JournalPopupEnabled.ENABLED) {
            return true;
        }

        if (preferences.shouldEnableJournalPopup() == EntryEditorPreferences.JournalPopupEnabled.DISABLED) {
            dialogService.notify(
                    Localization.lang("Please enable journal information fetching in %0 > %1",
                            Localization.lang("Preferences"),
                            Localization.lang("Web search"))
            );
            return false;
        }

        boolean journalInfoEnabled = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remote services"),
                Localization.lang("Allow sending ISSN to a JabRef online service (SCimago) for fetching journal information"));

        preferences.setEnableJournalPopup(journalInfoEnabled
                ? EntryEditorPreferences.JournalPopupEnabled.ENABLED
                : EntryEditorPreferences.JournalPopupEnabled.DISABLED);
        return journalInfoEnabled;
    }
}

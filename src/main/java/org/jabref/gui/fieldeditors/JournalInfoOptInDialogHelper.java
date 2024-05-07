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
            boolean enableJournalPopup = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Enable Journal Information Fetching?"),
                    Localization.lang("Would you like to enable fetching of journal information? This can be changed later in %0 > %1.",
                            Localization.lang("Preferences"),
                            Localization.lang("Entry editor")), Localization.lang("Enable"), Localization.lang("Keep disabled")
            );

            preferences.setEnableJournalPopup(enableJournalPopup
                    ? EntryEditorPreferences.JournalPopupEnabled.ENABLED
                    : EntryEditorPreferences.JournalPopupEnabled.DISABLED);

            return enableJournalPopup;
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

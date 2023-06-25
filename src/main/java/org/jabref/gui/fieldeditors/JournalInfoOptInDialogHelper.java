package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.logic.journals.JournalInformationPreferences;
import org.jabref.logic.l10n.Localization;

public class JournalInfoOptInDialogHelper {

    /**
     * Using the journal information data fetcher service needs to be opt-in for GDPR compliance.
     */
    public static boolean isJournalInfoEnabled(DialogService dialogService, JournalInformationPreferences preferences) {
        if (preferences.isJournalInfoEnabled()) {
            return true;
        }

        if (preferences.isJournalInfoOptOut()) {
            return false;
        }

        boolean journalInfoEnabled = dialogService.showConfirmationDialogWithOptOutAndWait(
                Localization.lang("Remote services"),
                Localization.lang("Allow sending ISSN to a JabRef online service (SCimago) for fetching journal information"),
                Localization.lang("Do not ask again"),
                preferences::setJournalInfoOptOut);
        preferences.journalInfoEnabledProperty().setValue(journalInfoEnabled);
        return journalInfoEnabled;
    }
}

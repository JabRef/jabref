package org.jabref.gui.importer;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

/**
 * Metadata extraction from PDFs and plaintext works very well using Grobid, but we do not want to enable it by default
 * due to data privacy concerns.
 * To make users aware of the feature, we ask each time before querrying Grobid, giving the option to opt-out.
 */
public class GrobidOptInDialogHelper {

    /**
     * If Grobid is not enabled but the user has not expicitly opted-out of Grobid, we ask for permission to send data
     * to Grobid using a dialog and giving an opt-out option.
     * @param dialogService the DialogService to use
     * @return if the user enabled Grobid, either in the past or after being asked by the dialog.
     */
    public static boolean showAndWaitIfUserIsUndecided(DialogService dialogService) {
        PreferencesService preferences = Globals.prefs;
        if (preferences.getImportSettingsPreferences().isGrobidEnabled()) {
            return true;
        }
        if (preferences.getImportSettingsPreferences().isGrobidOptOut()) {
            return false;
        }
        boolean grobidEnabled = dialogService.showConfirmationDialogWithOptOutAndWait(
                Localization.lang("Remote services"),
                Localization.lang("Allow sending PDF files and raw citation strings to a JabRef online service (Grobid) to determine Metadata. This produces better results."),
                Localization.lang("Do not ask again"),
                (optOut) -> preferences.storeImportSettingsPreferences(preferences.getImportSettingsPreferences().withGrobidOptOut(optOut))
        );
        preferences.storeImportSettingsPreferences(preferences.getImportSettingsPreferences().withGrobidEnabled(grobidEnabled));
        return grobidEnabled;
    }
}

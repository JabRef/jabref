package org.jabref.gui.importer;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.l10n.Localization;

/**
 * Metadata extraction from PDFs and plaintext works very well using Grobid, but we do not want to enable it by default
 * due to data privacy concerns.
 * To make users aware of the feature, we ask each time before querying Grobid, giving the option to opt-out.
 */
public class GrobidPreferenceDialogHelper {

    /**
     * If Grobid is not enabled but the user has not explicitly opted-out of Grobid, we ask for permission to send data
     * to Grobid by using a dialog and giving an explicit 'save preference' option.
     *
     * @param dialogService the DialogService to use
     * @return if the user enabled Grobid, either in the past or after being asked by the dialog, save preference
     * if specified.
     */
    public static boolean showAndWaitIfUserIsUndecided(DialogService dialogService, GrobidPreferences preferences) {
        if (preferences.isGrobidPreference()) {
            return preferences.isGrobidEnabled();
        }
        boolean grobidEnabled = dialogService.showConfirmationDialogWithOptOutAndWait(
                Localization.lang("Remote services"),
                Localization.lang("Allow sending PDF files and raw citation strings to a JabRef online service (Grobid) to determine Metadata. This produces better results."),
                Localization.lang("Save Preference"),
                preferences::setGrobidPreference);
        preferences.setGrobidEnabled(grobidEnabled);
        return grobidEnabled;
    }
}

package org.jabref.gui.importer;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.l10n.Localization;

/**
 * Metadata extraction from PDFs and plaintext works very well using Grobid, but we do not want to enable it by default
 * due to data privacy concerns.
 * To make users aware of the feature, we ask before querying for the first time Grobid, saving the users' preference
 */
public class GrobidUseDialogHelper {

    /**
     * If the user has not explicitly opted-in/out of Grobid, we ask for permission to send data to Grobid by using
     * a dialog. The users' preference is saved.
     *
     * @param dialogService the DialogService to use
     * @return if the user enabled Grobid, either in the past or after being asked by the dialog.
     */
    public static boolean showAndWaitIfUserIsUndecided(DialogService dialogService, GrobidPreferences preferences) {
        if (preferences.isGrobidUseAsked()) {
            return preferences.isGrobidEnabled();
        }
        if (preferences.isGrobidEnabled()) {
            preferences.setGrobidUseAsked(true);
            return true;
        }
        boolean grobidEnabled = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remote services"),
                Localization.lang("Allow sending PDF files and raw citation strings to a JabRef online service (Grobid) to determine Metadata. This produces better results."),
                Localization.lang("Send to Grobid"),
                Localization.lang("Do not send"));
        preferences.setGrobidUseAsked(true);
        preferences.setGrobidEnabled(grobidEnabled);
        return grobidEnabled;
    }
}

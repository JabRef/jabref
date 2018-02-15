package org.jabref.gui.dialogs;

import javax.swing.JOptionPane;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MergeReviewIntoCommentUIManager {

    public boolean askUserForMerge(BibEntry entry) {
        int answer = JOptionPane.showConfirmDialog(
                null,
                entry.getAuthorTitleYear(150) +
                        Localization.lang("has both a comment and a review field.") + "\n" +
                        Localization.lang("Since the 'Review' field was deprected in JabRef 4.2, these two fields are about to be merged into the 'Comment' field."),
                Localization.lang("Review Field Migrated!"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        return 0 == answer;
    }
}

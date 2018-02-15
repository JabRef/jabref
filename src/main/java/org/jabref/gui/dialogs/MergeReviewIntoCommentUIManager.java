package org.jabref.gui.dialogs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MergeReviewIntoCommentUIManager {

    public boolean askUserForMerge(List<BibEntry> conflicts) {
        List<String> bibKeys = conflicts.stream()
                .map(BibEntry::getCiteKeyOptional)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        int answer = JOptionPane.showConfirmDialog(
                null,
                String.join(",\n", bibKeys) +
                        Localization.lang(" has/have both a 'Comment' and a 'Review' field.") + "\n" +
                        Localization.lang("Since the 'Review' field was deprecated in JabRef 4.2, these two fields are about to be merged into the 'Comment' field.") + "\n" +
                        Localization.lang("By clicking 'Yes' the conflicting fields of these entries will be merged into the 'Comment' field."),
                Localization.lang("Review Field Migration"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        return 0 == answer;
    }
}

package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MergeReviewIntoCommentConfirmationDialog {

    private final DialogService dialogService;

    public MergeReviewIntoCommentConfirmationDialog(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public boolean askUserForMerge(List<BibEntry> conflicts) {
        String bibKeys = conflicts
                .stream()
                .map(BibEntry::getCitationKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(",\n"));

        String content = bibKeys + " " +
                Localization.lang("has/have both a 'Comment' and a 'Review' field.") + "\n" +
                Localization.lang("Since the 'Review' field was deprecated in JabRef 4.2, these two fields are about to be merged into the 'Comment' field.") + "\n" +
                Localization.lang("The conflicting fields of these entries will be merged into the 'Comment' field.");

        return dialogService.showConfirmationDialogAndWait(
                Localization.lang("Review Field Migration"),
                content,
                Localization.lang("Merge fields")
        );
    }
}

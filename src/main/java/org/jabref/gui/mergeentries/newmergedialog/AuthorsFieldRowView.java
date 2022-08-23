package org.jabref.gui.mergeentries.newmergedialog;

import org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons.WarningButton;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class AuthorsFieldRowView extends FieldRowView {
    private final AuthorList leftEntryAuthors;
    private final AuthorList rightEntryAuthors;

    public AuthorsFieldRowView(BibEntry leftEntry, BibEntry rightEntry, BibEntry mergedEntry, FieldMergerFactory fieldMergerFactory, int rowIndex) {
        super(StandardField.AUTHOR, leftEntry, rightEntry, mergedEntry, fieldMergerFactory, rowIndex);
        var authorsParser = new AuthorListParser();
        leftEntryAuthors = authorsParser.parse(viewModel.getLeftFieldValue());
        rightEntryAuthors = authorsParser.parse(viewModel.getRightFieldValue());

        if (leftEntryAuthors.equals(rightEntryAuthors)) {
            showAuthorsAreTheSameWarning();
        }
    }

    private void showAuthorsAreTheSameWarning() {
        WarningButton warningButton = new WarningButton(Localization.lang("The authors are the same, but the fields are formatted differently."));
        getFieldNameCell().addSideButton(warningButton);
    }
}

package org.jabref.gui.mergeentries.newmergedialog;

import org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons.InfoButton;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

public class PersonsNameFieldRowView extends FieldRowView {
    private final AuthorList leftEntryNames;
    private final AuthorList rightEntryNames;

    public PersonsNameFieldRowView(Field field, BibEntry leftEntry, BibEntry rightEntry, BibEntry mergedEntry, FieldMergerFactory fieldMergerFactory, int rowIndex) {
        super(field, leftEntry, rightEntry, mergedEntry, fieldMergerFactory, rowIndex);
        assert field.getProperties().contains(FieldProperty.PERSON_NAMES);

        var authorsParser = new AuthorListParser();
        leftEntryNames = authorsParser.parse(viewModel.getLeftFieldValue());
        rightEntryNames = authorsParser.parse(viewModel.getRightFieldValue());

        if (!leftEntry.equals(rightEntry) && leftEntryNames.equals(rightEntryNames)) {
            showPersonsNamesAreTheSameInfo();
            shouldShowDiffs.set(false);
        }
    }

    private void showPersonsNamesAreTheSameInfo() {
        InfoButton infoButton = new InfoButton(Localization.lang("The %0s are the same. However, the order of field content differs", viewModel.getField().getName()));
        getFieldNameCell().addSideButton(infoButton);
    }
}

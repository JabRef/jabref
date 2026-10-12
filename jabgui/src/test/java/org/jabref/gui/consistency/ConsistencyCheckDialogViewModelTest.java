package org.jabref.gui.consistency;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConsistencyCheckDialogViewModelTest {

    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    @Test
    void columnsContainMissingRequiredFields() {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        assertEquals(List.of("Entry Type", "CitationKey", "Journal", "Title", "Year"), viewModel.getColumnNames());
    }

    @Test
    void rowsShowMissingRequiredFieldsAsAbsent() {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        assertEquals(List.of(
                List.of("Article", "first", "-", "-", "-"),
                List.of("Article", "second", "-", "-", "-")
        ), viewModel.getTableData().stream().map(ConsistencyMessage::message).toList());
    }

    /// Both entries have the same fields. Thus, they are reported only because of the missing required fields.
    private ConsistencyCheckDialogViewModel createViewModelForEntriesHavingOnlyAuthor() {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author Two");
        BibDatabaseContext bibContext = new BibDatabaseContext(new BibDatabase(List.of(first, second)));
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });
        return new ConsistencyCheckDialogViewModel(mock(DialogService.class), mock(GuiPreferences.class), entryTypesManager, bibContext.getMode(), result);
    }
}

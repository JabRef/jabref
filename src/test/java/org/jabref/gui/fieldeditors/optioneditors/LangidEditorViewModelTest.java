package org.jabref.gui.fieldeditors.optioneditors;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Langid;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangidEditorViewModelTest {

    private LangidEditorViewModel langidEditorViewModel;

    @BeforeEach
    void setUp() {
        // Mock dependencies
        BibDatabaseContext databaseContext = Mockito.mock(BibDatabaseContext.class);
        FilePreferences filePreferences = Mockito.mock(FilePreferences.class);
        JournalAbbreviationRepository abbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);

        // Initialize FieldCheckers with mocked instances
        FieldCheckers fieldCheckers = new FieldCheckers(databaseContext, filePreferences, abbreviationRepository, false);

        // Mock the SuggestionProvider
        SuggestionProvider<?> suggestionProvider = Mockito.mock(SuggestionProvider.class);

        // Initialize the LangidEditorViewModel
        langidEditorViewModel = new LangidEditorViewModel(
                StandardField.LANGUAGEID,  // Use the correct field
                suggestionProvider,  // Mocked SuggestionProvider
                BibDatabaseMode.BIBLATEX,  // Use the correct BibDatabaseMode
                fieldCheckers,  // FieldCheckers instance
                new UndoManager()  // UndoManager instance
        );
    }

    @Test
    void getItemsShouldReturnAllLangidValues() {
        Collection<Langid> items = langidEditorViewModel.getItems();
        assertEquals(Langid.values().length, items.size());
        assertTrue(items.contains(Langid.BASQUE)); // Check if it contains a specific Langid (e.g., "en" for English)
    }

    @Test
    void testStringConversion() {
        String langidString = "bulgarian";
        Langid langid = langidEditorViewModel.getStringConverter().fromString(langidString);
        assertEquals(Langid.BULGARIAN, langid, "String should convert to the corresponding Langid");

        String convertedString = langidEditorViewModel.getStringConverter().toString(Langid.BULGARIAN);
        assertEquals(langidString, convertedString, "Langid should convert back to its string representation");
    }

//    @Test
//    void testSelectedItem() {
//        // Set the selected Langid using the correct method
//        langidEditorViewModel.setValue(Langid.BASQUE);
//
//        // Verify that the selected value is now correct
//        Langid selectedLangid = langidEditorViewModel.getValue();
//        assertEquals(Langid.BASQUE, selectedLangid, "Selected value should reflect in the view model");
//    }

    @Test
    void testHandlingNullValue() {
        // Test the handling of a null value
        Langid result = langidEditorViewModel.getStringConverter().fromString(null);
        assertEquals(null, result, "Null input should return null Langid");
    }

    @Test
    void testHandlingBlankValue() {
        // Test the handling of a blank string
        Langid result = langidEditorViewModel.getStringConverter().fromString(" ");
        assertEquals(null, result, "Blank input should return null Langid");
    }
}

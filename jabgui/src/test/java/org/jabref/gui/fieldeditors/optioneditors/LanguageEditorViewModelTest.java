package org.jabref.gui.fieldeditors.optioneditors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class LanguageEditorViewModelTest {

    private LanguageEditorViewModel languageEditorViewModel;

    @BeforeEach
    void setUp() {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);
        FieldCheckers fieldCheckers = new FieldCheckers(databaseContext, filePreferences, abbreviationRepository, false);
        SuggestionProvider<?> suggestionProvider = mock(SuggestionProvider.class);

        languageEditorViewModel = new LanguageEditorViewModel(
                StandardField.LANGUAGEID,
                suggestionProvider,
                BibDatabaseMode.BIBLATEX,
                fieldCheckers,
                new UndoManager()
        );
    }

    @Test
    void getItemsShouldReturnAllLangidValues() {
        Collection<Langid> items = new HashSet<>(languageEditorViewModel.getItems());
        assertEquals(EnumSet.allOf(Langid.class), items);
    }

    @Test
    void stringConversion() {
        String langidString = "bulgarian";
        Langid langid = languageEditorViewModel.getStringConverter().fromString(langidString);
        assertEquals(Langid.BULGARIAN, langid, "String should convert to the corresponding Langid");

        String convertedString = languageEditorViewModel.getStringConverter().toString(Langid.BULGARIAN);
        assertEquals(langidString, convertedString, "Langid should convert back to its string representation");
    }

    @Test
    void stringConversionWithHumanReadableName() {
        // Test conversion from human-readable name to Langid
        String langidString = "Basque";
        Langid langid = languageEditorViewModel.getStringConverter().fromString(langidString);
        assertEquals(Langid.BASQUE, langid, "Human-readable name should convert to the corresponding Langid");

        // Test conversion from Langid to human-readable name
        String convertedString = languageEditorViewModel.getStringConverter().toString(Langid.BASQUE);
        assertEquals("basque", convertedString, "Langid should convert back to its lowercase string representation");
    }

    @Test
    void handlingNullValue() {
        // Test the handling of a null value
        Langid result = languageEditorViewModel.getStringConverter().fromString(null);
        assertNull(result, "Null input should return null Langid");
    }

    @Test
    void handlingBlankValue() {
        // Test the handling of a blank string
        Langid result = languageEditorViewModel.getStringConverter().fromString(" ");
        assertNull(result, "Blank input should return null Langid");
    }
}


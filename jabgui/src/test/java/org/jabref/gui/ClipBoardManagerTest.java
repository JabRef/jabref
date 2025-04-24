package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllowedToUseAwt("Requires AWT for clipboard access")
class ClipBoardManagerTest {

    private BibEntryTypesManager entryTypesManager;
    private ClipBoardManager clipBoardManager;

    @BeforeEach
    void setUp() {
        // create preference service mock
        CliPreferences preferences = mock(CliPreferences.class);
        Injector.setModelOrService(CliPreferences.class, preferences);
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        List<Field> fields = List.of(StandardField.URL);
        ObservableList<Field> nonWrappableFields = FXCollections.observableArrayList(fields);
        // set up mock behaviours for preferences service
        when(fieldPreferences.getNonWrappableFields()).thenReturn(nonWrappableFields);
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        // create mock clipboard
        Clipboard clipboard = mock(Clipboard.class);
        // create primary clipboard and set a temporary value
        StringSelection selection = new StringSelection("test");
        java.awt.datatransfer.Clipboard clipboardPrimary = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboardPrimary.setContents(selection, selection);

        // create mock entry manager and set up behaviour for mock
        entryTypesManager = new BibEntryTypesManager();

        // initialize a clipBoardManager
        clipBoardManager = new ClipBoardManager(clipboard, clipboardPrimary);
    }

    @DisplayName("Check that the ClipBoardManager can set a bibentry as its content from the clipboard")
    @Test
    void copyStringBibEntry() throws IOException {
        // Arrange
        String expected = "@Article{,\n author = {Claudepierre, S. G.},\n journal = {IEEE},\n}";

        // create BibEntry
        BibEntry bibEntry = new BibEntry();
        // construct an entry
        bibEntry.setType(StandardEntryType.Article);
        bibEntry.setField(StandardField.JOURNAL, "IEEE");
        bibEntry.setField(StandardField.AUTHOR, "Claudepierre, S. G.");
        // add entry to list
        List<BibEntry> bibEntries = new ArrayList<>();
        bibEntries.add(bibEntry);

        // Act
        clipBoardManager.setContent(bibEntries, entryTypesManager);

        // Assert
        String actual = ClipBoardManager.getContentsPrimary();
        // clean strings
        actual = actual.replaceAll("\\s+", " ").trim();
        expected = expected.replaceAll("\\s+", " ").trim();

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Check that the ClipBoardManager can handle a bibentry with string constants correctly from the clipboard")
    void copyStringBibEntryWithStringConstants() throws IOException {
        // Arrange
        String expected = "@String{grl = \"Geophys. Res. Lett.\"}@Article{,\n" + " author = {Claudepierre, S. G.},\n" +
                " journal = {grl},\n" + "}";
        // create BibEntry
        BibEntry bibEntry = new BibEntry();
        // construct an entry
        bibEntry.setType(StandardEntryType.Article);
        bibEntry.setField(StandardField.JOURNAL, "grl");
        bibEntry.setField(StandardField.AUTHOR, "Claudepierre, S. G.");
        // add entry to list
        List<BibEntry> bibEntries = new ArrayList<>();
        bibEntries.add(bibEntry);

        // string constants
        List<BibtexString> constants = new ArrayList<>();

        // Mock BibtexString
        BibtexString bibtexString = mock(BibtexString.class);

        // define return value for getParsedSerialization()
        when(bibtexString.getParsedSerialization()).thenReturn("@String{grl = \"Geophys. Res. Lett.\"}");
        // add the constant
        constants.add(bibtexString);

        // Act
        clipBoardManager.setContent(bibEntries, entryTypesManager, constants);

        // Assert
        String actual = ClipBoardManager.getContentsPrimary();
        // clean strings
        actual = actual.replaceAll("\\s+", " ").trim();
        expected = expected.replaceAll("\\s+", " ").trim();

        assertEquals(expected, actual);
    }
}

package org.jabref.gui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.TransferMode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClipBoardManagerTest extends ApplicationTest {

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
        when(fieldPreferences.getNonWrappableFields()).thenReturn(nonWrappableFields);
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        entryTypesManager = new BibEntryTypesManager();
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.emptyObservableList());

        AtomicReference<Clipboard> clipboard = new AtomicReference<>();
        interact(() -> {
            clipboard.set(Clipboard.getSystemClipboard());
        });
        clipBoardManager = new ClipBoardManager(stateManager, clipboard.get(), mock(java.awt.datatransfer.Clipboard.class));
    }

    @Test
    void copyStringBibEntry() throws IOException {
        String expected = """
                @Article{,
                  author  = {Claudepierre, S. G.},
                  journal = {IEEE},
                }
                """;

        BibEntry bibEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "IEEE")
                .withField(StandardField.AUTHOR, "Claudepierre, S. G.")
                .withChanged(true);

        AtomicReference<String> actual = new AtomicReference<>();
        interact(Unchecked.runnable(() -> {
            clipBoardManager.setContent(
                    TransferMode.NONE,
                    new BibDatabaseContext(new BibDatabase(List.of(bibEntry))),
                    List.of(bibEntry),
                    entryTypesManager,
                    List.of());

            actual.set(ClipBoardManager.getContents());
        }));
        assertEquals(expected, actual.get().replace("\r\n", "\n"));
    }

    @Test
    void copyStringBibEntryWithStringConstants() throws IOException {
        String expected = """
                @String{grl = "Geophys. Res. Lett."}

                @Article{,
                  author  = {Claudepierre, S. G.},
                  journal = {grl},
                }
                """;
        BibEntry bibEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "grl")
                .withField(StandardField.AUTHOR, "Claudepierre, S. G.")
                .withChanged(true);

        BibtexString bibtexString = mock(BibtexString.class);
        when(bibtexString.getParsedSerialization()).thenReturn("@String{grl = \"Geophys. Res. Lett.\"}");

        AtomicReference<String> actual = new AtomicReference<>();
        interact(Unchecked.runnable(() -> {
            clipBoardManager.setContent(
                    TransferMode.NONE,
                    new BibDatabaseContext(new BibDatabase(List.of(bibEntry))),
                    List.of(bibEntry),
                    entryTypesManager,
                    List.of(bibtexString));

            actual.set(ClipBoardManager.getContents());
        }));
        assertEquals(expected, actual.get().replace("\r\n", "\n"));
    }
}

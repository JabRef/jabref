package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.*;
import javafx.stage.Stage;

import org.jabref.gui.LibraryTab;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import org.testfx.framework.junit5.ApplicationTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplaceStringViewTest extends ApplicationTest {

    private ReplaceStringView replaceStringView;
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private DialogPane dialogPane;
    private BibEntry entry;

    @Override
    public void start (Stage stage) throws Exception {
        // Globals.prefs = JabRefPreferences.getInstance();
        // Globals.startBackgroundTasks();

        entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, "A tale from the trenches")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development")
                .withCitationKey("abc");
        List<BibEntry> entries = new ArrayList<>();
        entries.add(entry);

        when(libraryTab.getSelectedEntries()).thenReturn(entries);
        when(libraryTab.getDatabase()).thenReturn(new BibDatabase(entries));
        replaceStringView = new ReplaceStringView(libraryTab);
        dialogPane = replaceStringView.getDialogPane();
        stage.setScene(dialogPane.getScene());
        stage.show();
    }

    @Test
    public void testReplace() throws Exception {

        // verify that the Year field text is of the initial value (2020)
        assertEquals(Optional.of("2020"), entry.getField(StandardField.YEAR));

        // enter find field
        clickOn("#findField");
        write("2020");

        // enter replace field
        clickOn("#replaceField");
        write("2021");

        // click on "Replace" button
        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            if (buttonType.getText().equals("Replace")) {
                Button replaceButton = (Button) dialogPane.lookupButton(buttonType);
                clickOn(replaceButton);
            }
        }

        // verify that the Year field text has been replaced by new value (2021)
        assertEquals(Optional.of("2021"), entry.getField(StandardField.YEAR));
    }
}

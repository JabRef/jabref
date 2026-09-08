package org.jabref.gui.libraryproperties;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.JabRefGuiUndoManager;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// The dialog end of [LibraryPropertiesViewModel#storeAllSettings]: that the dialog reaches the
/// library's journal at all, and that accepting it puts one step on that journal.
///
/// The view model's own test drives it with stub tabs; this one builds the seven real ones and
/// changes the settings through their controls.
class LibraryPropertiesViewTest extends ApplicationTest {

    private final BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
    private final JabRefGuiUndoManager journal = new JabRefGuiUndoManager();

    private LibraryPropertiesView view;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        when(stateManager.getUndoManager(databaseContext)).thenReturn(journal);

        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(ThemeManager.class, mock(ThemeManager.class));
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());

        databaseContext.getMetaData().setMode(BibDatabaseMode.BIBTEX);

        view = new LibraryPropertiesView(databaseContext);
        interact(() -> view.show());
    }

    /// Two tabs' worth of settings, one step, and the library reports itself changed until it is
    /// saved.
    @Test
    // [utest->req~logic.undo.library-settings-recorded~1]
    void acceptingTheDialogRecordsOneUndoableStep() {
        ComboBox<BibDatabaseMode> mode = lookup("#databaseMode").queryComboBox();
        TextField keywordSeparator = lookup("#keywordSeparator").query();

        interact(() -> {
            mode.setValue(BibDatabaseMode.BIBLATEX);
            keywordSeparator.setText(";");
            accept();
        });

        assertEquals(Optional.of(BibDatabaseMode.BIBLATEX), databaseContext.getMetaData().getMode());
        assertEquals(Optional.of(';'), databaseContext.getMetaData().getKeywordSeparator());
        assertTrue(journal.canUndo(), "accepting the dialog put a step on the stack");
        assertTrue(journal.hasChanged());

        interact(journal::undo);

        assertEquals(Optional.of(BibDatabaseMode.BIBTEX), databaseContext.getMetaData().getMode());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getKeywordSeparator());
        assertFalse(journal.canUndo(), "the whole dialog is one step");
    }

    /// Accepting an untouched dialog is not a no-op: the tabs write back the defaults their
    /// controls were filled with, so a library that had no encoding gains one. That was a change
    /// nothing could take back before; it is one step now.
    @Test
    void acceptingAnUntouchedDialogRecordsWhatTheTabsMaterialise() {
        assertEquals(Optional.empty(), databaseContext.getMetaData().getEncoding());

        interact(this::accept);

        assertEquals(Optional.of(StandardCharsets.UTF_8), databaseContext.getMetaData().getEncoding());

        interact(journal::undo);

        assertEquals(Optional.empty(), databaseContext.getMetaData().getEncoding());
        assertEquals(Optional.of(BibDatabaseMode.BIBTEX), databaseContext.getMetaData().getMode());
        assertFalse(journal.canUndo(), "the whole dialog is one step");
    }

    private void accept() {
        view.getDialogPane()
            .getButtonTypes()
            .stream()
            .filter(buttonType -> buttonType.getButtonData() == ButtonType.OK.getButtonData())
            .findFirst()
            .ifPresent(buttonType -> ((Button) view.getDialogPane().lookupButton(buttonType)).fire());
    }
}

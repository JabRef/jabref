package org.jabref.gui.importer;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.controlsfx.control.CheckListView;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/// Drives the dialog itself, so that the wiring between the check lists and the view model is covered, too.
@NullMarked
class ImportCustomEntryTypesDialogTest extends JavaFxTest {

    private static final BibDatabaseMode MODE = BibDatabaseMode.BIBLATEX;

    private static final BibEntryType MANUSCRIPT_FROM_FILE = parse(
            "jabref-entrytype: manuscript: req[library;location;shelfmark] opt[origin;scribe]");
    private static final BibEntryType AUDIO_FROM_FILE = parse(
            "jabref-entrytype: audio: req[author;date;publisher;title] opt[url;urldate]");

    private CliPreferences preferences;
    private BibEntryTypesManager entryTypesManager;
    private ImportCustomEntryTypesDialog dialog;

    private static BibEntryType parse(String comment) {
        return MetaDataParser.parseCustomEntryType(comment).orElseThrow();
    }

    @Override
    public void start(Stage stage) {
        Localization.setLanguage(Language.ENGLISH);

        preferences = mock(CliPreferences.class);
        entryTypesManager = new BibEntryTypesManager();
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());

        stage.setScene(new Scene(new StackPane(), 1024, 768));

        dialog = new ImportCustomEntryTypesDialog(MODE, List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE));
        dialog.initOwner(stage);
    }

    @AfterAll
    static void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void unknownTypesAndDifferentCustomizationsAreShownSeparately() {
        interact(() -> {
            assertEquals(List.of(MANUSCRIPT_FROM_FILE), List.copyOf(unknownTypes().getItems()));
            assertEquals(List.of(AUDIO_FROM_FILE), differentCustomizations().getItems().stream()
                                                                            .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                                                                            .toList());
            assertTrue(differentCustomizationBox().isVisible());
        });
    }

    /// Regression test for <https://github.com/JabRef/jabref/issues/9930>: a checked customization of a type
    /// JabRef ships has to be stored, otherwise the dialog shows up at every start again.
    @Test
    void checkedTypesAreStoredOnOk() {
        interact(() -> {
            unknownTypes().getCheckModel().checkAll();
            differentCustomizations().getCheckModel().checkAll();
            buttonOf(ButtonType.OK).fire();
        });

        assertEquals(List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE),
                List.copyOf(entryTypesManager.getAllCustomizedTypes(MODE)));
        verify(preferences).storeCustomEntryTypesRepository(entryTypesManager);
        assertFalse(entryTypesManager.isDifferentCustomOrModifiedType(AUDIO_FROM_FILE, MODE));
        assertFalse(entryTypesManager.isDifferentCustomOrModifiedType(MANUSCRIPT_FROM_FILE, MODE));
    }

    @Test
    void unselectedTypesAreNotStored() {
        interact(() -> {
            differentCustomizations().getCheckModel().checkAll();
            buttonOf(ButtonType.OK).fire();
        });

        assertEquals(List.of(AUDIO_FROM_FILE), List.copyOf(entryTypesManager.getAllCustomizedTypes(MODE)));
        assertTrue(entryTypesManager.enrich(new UnknownEntryType("manuscript"), MODE).isEmpty());
    }

    @Test
    void nothingIsStoredOnCancel() {
        interact(() -> {
            unknownTypes().getCheckModel().checkAll();
            differentCustomizations().getCheckModel().checkAll();
            buttonOf(ButtonType.CANCEL).fire();
        });

        assertTrue(entryTypesManager.getAllCustomizedTypes(MODE).isEmpty());
        assertTrue(entryTypesManager.enrich(BiblatexNonStandardEntryType.Audio, MODE).isPresent());
        verify(preferences, never()).storeCustomEntryTypesRepository(entryTypesManager);
    }

    @SuppressWarnings("unchecked")
    private CheckListView<BibEntryType> unknownTypes() {
        return (CheckListView<BibEntryType>) dialogPane().lookup("#unknownEntryTypesCheckList");
    }

    @SuppressWarnings("unchecked")
    private CheckListView<BibEntryTypePrefsAndFileViewModel> differentCustomizations() {
        return (CheckListView<BibEntryTypePrefsAndFileViewModel>) dialogPane().lookup("#differentCustomizationCheckList");
    }

    private VBox differentCustomizationBox() {
        return (VBox) dialogPane().lookup("#boxDifferentCustomization");
    }

    private Button buttonOf(ButtonType buttonType) {
        return (Button) dialogPane().lookupButton(buttonType);
    }

    private DialogPane dialogPane() {
        return dialog.getDialogPane();
    }
}

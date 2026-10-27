/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.conferences.ConferenceAbbreviationLoader;
import org.jabref.logic.conferences.ConferenceAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
@ResourceLock("Injector")
class ConferenceAbbreviationsTabTest extends JavaFxTest {
    private final DialogService dialogService = mock(DialogService.class);
    private final ConferenceAbbreviationPreferences preferences = ConferenceAbbreviationPreferences.getDefault();
    private ConferenceAbbreviationsTab tab;
    private VBox root;

    @Override
    public void start(Stage stage) {
        GuiPreferences guiPreferences = mock(GuiPreferences.class);
        when(guiPreferences.getConferenceAbbreviationPreferences()).thenReturn(preferences);
        Injector.setModelOrService(GuiPreferences.class, guiPreferences);
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
        tab = new ConferenceAbbreviationsTab();
        tab.setValues();
        root = new VBox(8, new Label(tab.getTabName()), tab.getContent());
        stage.setScene(new Scene(root, 900, 620));
        stage.show();
    }

    @AfterEach
    void clearInjector() {
        Injector.forgetAll();
    }

    @Test
    void conferencePageUsesConferenceColumnsAndOmitsJournalOnlySettings() {
        assertEquals(Localization.lang("Conference abbreviations"), tab.getTabName());
        interact(() -> assertEquals(List.of(Localization.lang("Full conference name"), Localization.lang("Abbreviation name"), Localization.lang("Shortest unique abbreviation"), ""),
                table().getColumns().stream().map(TableColumn::getText).toList()));
        assertEquals(List.of(), JavaFxExtension.lookupAll(root, ".check-box", CheckBox.class));
    }

    @Test
    void importingAndEditingThroughTablePersistsConferenceCsv(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, "International Conference on Business Process Management,BPM");
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(csv));
        Button open = JavaFxExtension.lookup(root, ".button", Button.class, button -> Optional.ofNullable(button.getTooltip())
                .map(Tooltip::getText).filter(Localization.lang("Open existing list")::equals).isPresent());
        interact(open::fire);
        interact(() -> assertEquals(List.of("BPM"), table().getItems().stream().map(AbbreviationViewModel::getAbbreviation).toList()));

        interact(() -> {
            TableView<AbbreviationViewModel> table = table();
            TableColumn<AbbreviationViewModel, String> column = abbreviationColumn();
            Event.fireEvent(column, new TableColumn.CellEditEvent<>(table, new TablePosition<>(table, 0, column), TableColumn.editCommitEvent(), "BPM edited"));
            tab.storeSettings();
        });

        assertEquals(List.of(csv.toAbsolutePath().toString()), preferences.getExternalLists());
        assertEquals(Optional.of("BPM edited"), ConferenceAbbreviationLoader.loadRepository(preferences)
                .getDefaultAbbreviation("International Conference on Business Process Management"));
    }

    @SuppressWarnings("unchecked")
    private TableColumn<AbbreviationViewModel, String> abbreviationColumn() {
        return (TableColumn<AbbreviationViewModel, String>) table().getColumns().get(1);
    }

    @SuppressWarnings("unchecked")
    private TableView<AbbreviationViewModel> table() {
        return (TableView<AbbreviationViewModel>) root.lookup(".table-view");
    }
}

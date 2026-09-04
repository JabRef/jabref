package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSelectionPageTest extends JavaFxTest {

    private FileSelectionPage page;

    @Override
    public void start(Stage stage) {
        UnlinkedFilesDialogViewModel viewModel = mock(UnlinkedFilesDialogViewModel.class);
        when(viewModel.progressValueProperty()).thenReturn(new SimpleDoubleProperty());
        when(viewModel.progressTextProperty()).thenReturn(new SimpleStringProperty());
        when(viewModel.taskActiveProperty()).thenReturn(new SimpleBooleanProperty());
        SimpleObjectProperty<Optional<FileNodeViewModel>> treeRoot = new SimpleObjectProperty<>(Optional.empty());
        when(viewModel.treeRootProperty()).thenReturn(treeRoot);
        when(viewModel.checkedFileListProperty()).thenReturn(new SimpleListProperty<>(FXCollections.<TreeItem<FileNodeViewModel>>observableArrayList()));

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.createThrottler(300)).thenReturn(mock(DelayTaskThrottler.class));

        page = new FileSelectionPage(
                mock(StateManager.class),
                viewModel,
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                taskExecutor);

        // The result content (including the preview pane) is only shown once a search has produced a tree
        treeRoot.set(Optional.of(new FileNodeViewModel(Path.of(""))));

        stage.setScene(new Scene(page));
        stage.show();
    }

    /// [utest->req~jabgui.externalfiles.unlinked-files.preview.close~1]
    @Test
    void previewPaneCanBeClosedAndShownAgain() {
        Button closeButton = findButtonWithTooltip(Localization.lang("Close PDF preview"));

        interact(closeButton::fire);

        assertEquals(0, previewPanes());
        Button showButton = page.lookupAll(".button").stream()
                                .map(Button.class::cast)
                                .filter(button -> button.getText().equals(Localization.lang("Show PDF preview")))
                                .findFirst()
                                .orElseThrow();
        assertTrue(showButton.isVisible());

        interact(showButton::fire);

        assertEquals(1, previewPanes());
        assertFalse(showButton.isVisible());
    }

    private Button findButtonWithTooltip(String tooltipText) {
        return page.lookupAll(".button").stream()
                   .map(Button.class::cast)
                   .filter(button -> button.getTooltip() != null)
                   .filter(button -> button.getTooltip().getText().equals(tooltipText))
                   .findFirst()
                   .orElseThrow();
    }

    private long previewPanes() {
        return page.lookupAll(".titled-pane").stream()
                   .map(TitledPane.class::cast)
                   .filter(pane -> pane.getText().equals(Localization.lang("PDF preview")))
                   .count();
    }
}

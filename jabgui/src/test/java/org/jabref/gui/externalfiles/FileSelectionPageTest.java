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
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class FileSelectionPageTest {

    @Start
    void onStart(Stage stage) {
        UnlinkedFilesDialogViewModel viewModel = mock(UnlinkedFilesDialogViewModel.class);
        when(viewModel.progressValueProperty()).thenReturn(new SimpleDoubleProperty());
        when(viewModel.progressTextProperty()).thenReturn(new SimpleStringProperty());
        when(viewModel.taskActiveProperty()).thenReturn(new SimpleBooleanProperty());
        SimpleObjectProperty<Optional<FileNodeViewModel>> treeRoot = new SimpleObjectProperty<>(Optional.empty());
        when(viewModel.treeRootProperty()).thenReturn(treeRoot);
        when(viewModel.checkedFileListProperty()).thenReturn(new SimpleListProperty<>(FXCollections.observableArrayList()));

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.createThrottler(300)).thenReturn(mock(DelayTaskThrottler.class));

        FileSelectionPage page = new FileSelectionPage(
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
    void previewPaneCanBeClosedAndShownAgain(FxRobot robot) {
        Button closeButton = findButtonWithTooltip(robot, Localization.lang("Close PDF preview"));

        robot.interact(closeButton::fire);

        assertEquals(0, previewPanes(robot));
        Button showButton = robot.lookup(".button")
                                 .match(Button.class::isInstance)
                                 .match(node -> ((Button) node).getText().equals(Localization.lang("Show PDF preview")))
                                 .queryButton();
        assertTrue(showButton.isVisible());

        robot.interact(showButton::fire);

        assertEquals(1, previewPanes(robot));
        assertFalse(showButton.isVisible());
    }

    private Button findButtonWithTooltip(FxRobot robot, String tooltipText) {
        return robot.lookup(".button")
                    .match(Button.class::isInstance)
                    .match(node -> ((Button) node).getTooltip() != null)
                    .match(node -> ((Button) node).getTooltip().getText().equals(tooltipText))
                    .queryButton();
    }

    private long previewPanes(FxRobot robot) {
        return robot.lookup(".titled-pane")
                    .match(TitledPane.class::isInstance)
                    .match(node -> ((TitledPane) node).getText().equals(Localization.lang("PDF preview")))
                    .queryAll().size();
    }
}

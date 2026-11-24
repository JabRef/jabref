package org.jabref.gui.theme;

import java.util.ArrayList;
import java.util.List;

import javafx.application.ColorScheme;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupDialogHeader;
import org.jabref.gui.groups.GroupDialogView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.PopOver;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class GroupIconPickerThemeTest extends JavaFxTest {

    private GroupDialogView dialog;

    @Override
    public void start(Stage stage) {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        when(preferences.getGroupsPreferences().getDefaultHierarchicalContext()).thenReturn(GroupHierarchyType.INDEPENDENT);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());

        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());

        dialog = new GroupDialogView(new BibDatabaseContext(), null, null, GroupDialogHeader.GROUP);
        dialog.show();
    }

    @ParameterizedTest
    @EnumSource(value = ThemePreset.class, names = {"JABREF", "NORD", "PRIMER"})
    void iconPickerFollowsThemeColorsWhenSwitchingColorSchemes(ThemePreset theme) {
        interact(() -> {
            List<String> stylesheets = new ArrayList<>();
            theme.getParent().ifPresent(parent -> stylesheets.add(parent.getStyleSheet().getSceneStylesheetLocation()));
            stylesheets.add(theme.getStyleSheet().getSceneStylesheetLocation());
            stylesheets.add(ThemeManager.JABREF_BASE_STYLE_SHEET.getSceneStylesheetLocation());
            dialog.getDialogPane().getScene().getStylesheets().setAll(stylesheets);
            ((Button) dialog.getDialogPane().lookup("#iconPickerButton")).fire();
        });
        awaitEvents();

        interact(() -> {
            PopOver picker = Window.getWindows().stream()
                                   .filter(PopOver.class::isInstance)
                                   .map(PopOver.class::cast)
                                   .findFirst().orElseThrow();
            Scene scene = picker.getScene();
            Region reference = new Region();
            reference.getStyleClass().addAll("bg-secondary", "bordered");
            reference.setManaged(false);
            picker.getRoot().getChildren().add(reference);

            List<Paint> backgrounds = new ArrayList<>();
            for (ColorScheme scheme : List.of(ColorScheme.DARK, ColorScheme.LIGHT, ColorScheme.DARK)) {
                dialog.getDialogPane().getScene().getPreferences().setColorScheme(scheme);
                scene.getPreferences().setColorScheme(scheme);
                dialog.getDialogPane().getScene().getRoot().applyCss();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                Path border = (Path) picker.getRoot().lookup(".border");
                backgrounds.add(border.getFill());
                assertEquals(reference.getBackground().getFills().getFirst().getFill(), border.getFill());
                assertEquals(reference.getBorder().getStrokes().getFirst().getTopStroke(), border.getStroke());
                GridCell<?> cell = picker.getRoot().lookupAll(".grid-cell").stream()
                                         .map(node -> (GridCell<?>) node)
                                         .filter(candidate -> !candidate.isEmpty())
                                         .findFirst().orElseThrow();
                assertEquals(reference.getBorder().getStrokes().getFirst().getTopStroke(), cell.getBorder().getStrokes().getFirst().getTopStroke());
            }
            assertNotEquals(backgrounds.getFirst(), backgrounds.get(1));
            assertEquals(backgrounds.getFirst(), backgrounds.getLast());
        });
    }
}

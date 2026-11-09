package org.jabref.gui.theme;

import java.util.ArrayList;
import java.util.List;

import javafx.application.ColorScheme;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.component.MarkdownTextFlow;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
@ResourceLock("Localization.lang")
class TextFlowHighlightThemeTest extends JavaFxTest {
    private StackPane root;
    private MarkdownTextFlow flow;

    @Override
    public void start(Stage stage) {
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        root = new StackPane();
        flow = new MarkdownTextFlow(root);
        flow.setMarkdown("Chocolate and **chocolate**");
        root.getChildren().add(flow);
        stage.setScene(new Scene(root, 400, 200));
        stage.show();
    }

    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void highlightsFollowThemeWhenSwitchingColorSchemes(ThemePreset theme) {
        interact(() -> {
            List<String> stylesheets = new ArrayList<>();
            theme.getParent().ifPresent(parent -> stylesheets.add(parent.getStyleSheet().getSceneStylesheetLocation()));
            stylesheets.add(theme.getStyleSheet().getSceneStylesheetLocation());
            stylesheets.add(ThemeManager.JABREF_BASE_STYLE_SHEET.getSceneStylesheetLocation());
            root.getScene().getStylesheets().setAll(stylesheets);

            Path accent = new Path();
            accent.getStyleClass().add("text-accent");
            accent.setManaged(false);
            root.getChildren().add(accent);

            for (ColorScheme scheme : List.of(ColorScheme.LIGHT, ColorScheme.DARK, ColorScheme.LIGHT)) {
                root.getScene().getPreferences().setColorScheme(scheme);
                root.applyCss();
                root.layout();
                assertEquals(2, flow.highlightOccurrences("chocolate", 0));
                flow.selectAll();
                root.applyCss();

                Path current = (Path) flow.getCurrentOccurrence().orElseThrow();
                Path selection = (Path) root.lookup(".text-selection-highlight");
                Path other = (Path) root.lookupAll(".text-search-highlight").stream()
                                        .filter(node -> !node.getStyleClass().contains("current"))
                                        .findFirst().orElseThrow();
                assertEquals(accent.getFill(), current.getStroke());
                assertEquals(selection.getFill(), current.getFill());
                assertNotEquals(other.getFill(), current.getFill());
                assertEquals(1.5, current.getStrokeWidth());
                assertTrue(current.getViewOrder() > selection.getViewOrder());
                assertTrue(selection.getViewOrder() > flow.getViewOrder());
                flow.clearSelection();
            }
        });
    }
}

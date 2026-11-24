package org.jabref.gui.entryeditor.citationrelationtab;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.MainTableTooltip;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.sciteTallies.TalliesResponse;
import org.jabref.model.util.DummyFileUpdateMonitor;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class CitationRelationsTabTest extends JavaFxTest {
    private final ObjectProperty<CitationsRelationsTabViewModel.SciteStatus> status =
            new SimpleObjectProperty<>(CitationsRelationsTabViewModel.SciteStatus.IN_PROGRESS);
    private BorderPane content;
    private ScrollPane scrollPane;
    private TabPane tabPane;
    private TaskExecutor taskExecutor;

    @Override
    public void start(Stage stage) {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getEntryEditorPreferences().citationFetcherTypeProperty())
                .thenReturn(new SimpleObjectProperty<>(CitationFetcherType.OPEN_ALEX));
        taskExecutor = mock(TaskExecutor.class);

        try (MockedConstruction<MainTableTooltip> tooltip = mockConstruction(MainTableTooltip.class);
             MockedConstruction<CitationsRelationsTabViewModel> viewModel = mockConstruction(CitationsRelationsTabViewModel.class, (model, _) -> {
                 when(model.lastImportedEntryProperty()).thenReturn(new SimpleObjectProperty<>());
                 when(model.statusProperty()).thenReturn(status);
                 when(model.isTrackedCitationSearch(any(), any())).thenReturn(true);
                 when(model.getCurrentResult()).thenReturn(Optional.of(new TalliesResponse("10.1038/nature14539", 123456, 1000, 200, 120000, 2256, 100000)));
             })) {
            CitationRelationsTab tab = new CitationRelationsTab(
                    mock(DialogService.class),
                    mock(StateManager.class),
                    new DummyFileUpdateMonitor(),
                    preferences,
                    taskExecutor,
                    new BibEntryTypesManager(),
                    mock(SearchCitationsRelationsService.class),
                    mock(BibTeXSyntaxHighlighter.class));
            tab.bindToEntry(new BibEntry().withCitationKey("LeCun2015").withField(StandardField.DOI, "10.1038/nature14539"));
            tabPane = new TabPane(tab);
            Scene scene = new Scene(tabPane, 500, 400);
            scene.getStylesheets().addAll(
                    CitationRelationsTabTest.class.getResource("/org/jabref/gui/theme/themes.jabref.org/jabref-theme.css").toExternalForm(),
                    CitationRelationsTabTest.class.getResource("/org/jabref/gui/theme/internal/jabref-base.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            tabPane.applyCss();
            tabPane.layout();
            scrollPane = (ScrollPane) tab.getContent();
            content = (BorderPane) scrollPane.getContent();
        }
    }

    @Test
    void narrowWindowCanScrollToRightHandCitationControls() {
        interact(() -> {
            resize(300, 400);
            assertTrue(horizontalScrollBar().isVisible());
            scrollPane.setHvalue(scrollPane.getHmax());
            tabPane.layout();
            BorderPane header = (BorderPane) citationColumn(1).getChildren().getFirst();
            assertRightEdgeReachable(header.getRight());
            HBox topBar = (HBox) content.getTop();
            assertRightEdgeReachable(topBar.getChildren().getLast());
        });
    }

    @Test
    void metricsRemainReachableWhenTheyArriveInNarrowWindow() {
        interact(() -> {
            status.set(CitationsRelationsTabViewModel.SciteStatus.FOUND);
            resize(500, 400);
            assertTrue(horizontalScrollBar().isVisible());
            scrollPane.setHvalue(scrollPane.getHmax());
            tabPane.layout();
            GridPane metrics = (GridPane) content.getBottom();
            BorderPane tallies = (BorderPane) metrics.getChildren().getFirst();
            assertRightEdgeReachable(tallies.getRight());
            assertTrue(horizontalScrollBar().localToScene(horizontalScrollBar().getLayoutBounds()).getMinY()
                    >= metrics.localToScene(metrics.getLayoutBounds()).getMaxY());
        });
    }

    @Test
    void wideningWindowFillsAvailableSpaceAndKeepsScrollbarVisible() {
        interact(() -> {
            status.set(CitationsRelationsTabViewModel.SciteStatus.FOUND);
            resize(500, 400);
            assertTrue(horizontalScrollBar().isVisible());
            assertTrue(content.getWidth() > scrollPane.getViewportBounds().getWidth());
            resize(2200, 600);
            assertTrue(horizontalScrollBar().isVisible());
            assertEquals(scrollPane.getViewportBounds().getWidth(), content.getWidth(), 1);
            assertEquals(scrollPane.getViewportBounds().getHeight(), content.getHeight(), 1);
        });
    }

    @Test
    void longFetchErrorsDoNotPushControlsOutsideFullWidthWindow() {
        interact(() -> {
            status.set(CitationsRelationsTabViewModel.SciteStatus.FOUND);
            resize(1600, 600);
            SplitPane lists = (SplitPane) content.getCenter();
            double originalDividerPosition = lists.getDividerPositions()[0];
            ArgumentCaptor<BackgroundTask<?>> tasks = ArgumentCaptor.captor();
            verify(taskExecutor, times(2)).execute(tasks.capture());
            for (BackgroundTask<?> task : tasks.getAllValues()) {
                task.getOnException().accept(new IOException("""
                        URL: https://api.semanticscholar.org/graph/v1/paper/DOI:10.1038/nature14539/references?fields=title,authors,year,citationCount,referenceCount,externalIds,publicationTypes,abstract,url&limit=1000
                        HTTP 404 Not Found
                        {"error":"Paper with id DOI:10.1038/nature14539 not found"}
                        """));
            }
            resize(1600, 600);
            assertEquals(scrollPane.getViewportBounds().getWidth(), content.getWidth(), 1);
            assertEquals(originalDividerPosition, lists.getDividerPositions()[0], 1.0 / lists.getWidth());
            HBox topBar = (HBox) content.getTop();
            assertRightEdgeReachable(topBar.getChildren().getLast());
            BorderPane header = (BorderPane) citationColumn(1).getChildren().getFirst();
            assertRightEdgeReachable(header.getRight());

            resize(500, 600);
            assertTrue(content.getWidth() > scrollPane.getViewportBounds().getWidth());
            scrollPane.setHvalue(scrollPane.getHmax());
            tabPane.layout();
            assertRightEdgeReachable(topBar.getChildren().getLast());

            resize(1600, 600);
            assertEquals(scrollPane.getViewportBounds().getWidth(), content.getWidth(), 1);
            assertRightEdgeReachable(topBar.getChildren().getLast());
        });
    }

    private void resize(double width, double height) {
        tabPane.resize(width, height);
        tabPane.applyCss();
        tabPane.layout();
    }

    private VBox citationColumn(int listIndex) {
        SplitPane lists = (SplitPane) content.getCenter();
        return (VBox) lists.getItems().get(listIndex);
    }

    private ScrollBar horizontalScrollBar() {
        return scrollPane.lookupAll(".scroll-bar").stream()
                         .map(ScrollBar.class::cast)
                         .filter(bar -> bar.getParent() == scrollPane && bar.getOrientation() == Orientation.HORIZONTAL)
                         .findFirst().orElseThrow();
    }

    private void assertRightEdgeReachable(Node node) {
        Node viewport = scrollPane.lookup(".viewport");
        Bounds viewportBounds = viewport.localToScene(viewport.getLayoutBounds());
        Bounds nodeBounds = node.localToScene(node.getLayoutBounds());
        assertTrue(nodeBounds.getMaxX() <= viewportBounds.getMaxX() + 1);
        assertTrue(nodeBounds.getMinX() >= viewportBounds.getMinX() - 1);
    }
}

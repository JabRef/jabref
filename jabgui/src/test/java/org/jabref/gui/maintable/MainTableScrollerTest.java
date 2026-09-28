package org.jabref.gui.maintable;

import java.util.Optional;
import java.util.stream.IntStream;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxTest;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class MainTableScrollerTest extends JavaFxTest {

    private TableView<Integer> table;

    @Override
    public void start(Stage stage) {
        table = new TableView<>(FXCollections.observableArrayList(IntStream.range(0, 100).boxed().toList()));
        table.setFixedCellSize(24);
        table.getColumns().add(new TableColumn<>("Value"));
        stage.setScene(new Scene(table, 320, 288));
        stage.show();
    }

    @Test
    void selectedRowBelowViewportIsCentered() {
        interact(() -> {
            VisibleRange visibleRange = centerSelectedRow(50, 0);

            assertEquals(50, visibleRange.center(), 1.0);
        });
    }

    @Test
    void selectedRowAboveViewportIsCentered() {
        interact(() -> {
            VisibleRange visibleRange = centerSelectedRow(50, 99);

            assertEquals(50, visibleRange.center(), 1.0);
        });
    }

    @Test
    void selectedRowNearStartIsClampedToFirstRow() {
        interact(() -> {
            VisibleRange visibleRange = centerSelectedRow(2, 99);

            assertEquals(0, visibleRange.firstIndex());
        });
    }

    @Test
    void selectedRowNearEndIsClampedToLastRow() {
        interact(() -> {
            VisibleRange visibleRange = centerSelectedRow(98, 0);

            assertEquals(99, visibleRange.lastIndex());
        });
    }

    @Test
    void noSelectionDoesNotScroll() {
        interact(() -> {
            table.getSelectionModel().clearSelection();
            table.scrollTo(40);
            layoutTable();
            int firstVisibleIndex = visibleRange().firstIndex();

            MainTableScroller.centerSelectedRow(table);
            layoutTable();

            assertEquals(firstVisibleIndex, visibleRange().firstIndex());
        });
    }

    @Test
    void selectedRowIsMadeVisibleBeforeSkinIsAvailable() {
        interact(() -> {
            RecordingTableView tableWithoutSkin = new RecordingTableView();
            tableWithoutSkin.getItems().addAll(IntStream.range(0, 100).boxed().toList());
            tableWithoutSkin.getSelectionModel().select(80);

            MainTableScroller.centerSelectedRow(tableWithoutSkin);

            assertEquals(80, tableWithoutSkin.requestedIndex());
        });
    }

    @Test
    void shortTableKeepsAllRowsVisible() {
        interact(() -> {
            table.setItems(FXCollections.observableArrayList(
                    IntStream.range(0, 5).boxed().toList()));

            VisibleRange visibleRange = centerSelectedRow(4, 0);

            assertEquals(0, visibleRange.firstIndex());
            assertEquals(4, visibleRange.lastIndex());
        });
    }

    private VisibleRange centerSelectedRow(int selectedIndex, int initialScrollIndex) {
        table.getSelectionModel().select(selectedIndex);
        table.scrollTo(initialScrollIndex);
        layoutTable();

        MainTableScroller.centerSelectedRow(table);
        layoutTable();

        return visibleRange();
    }

    private void layoutTable() {
        table.applyCss();
        table.layout();
    }

    private VisibleRange visibleRange() {
        VirtualFlow<?> flow = MainTableScroller.findVirtualFlow(table).orElseThrow();
        int firstVisibleIndex = Optional.ofNullable(flow.getFirstVisibleCell()).orElseThrow().getIndex();
        int lastVisibleIndex = Optional.ofNullable(flow.getLastVisibleCell()).orElseThrow().getIndex();
        return new VisibleRange(firstVisibleIndex, lastVisibleIndex);
    }

    private record VisibleRange(int firstIndex, int lastIndex) {
        double center() {
            return (firstIndex + lastIndex) / 2.0;
        }
    }

    private static final class RecordingTableView extends TableView<Integer> {
        private int requestedIndex = -1;

        @Override
        public void scrollTo(int index) {
            requestedIndex = index;
        }

        int requestedIndex() {
            return requestedIndex;
        }
    }
}

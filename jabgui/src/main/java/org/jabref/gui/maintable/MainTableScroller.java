package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;

import org.jspecify.annotations.NullMarked;

/// Positions the selected row in a table's visible viewport.
@NullMarked
final class MainTableScroller {

    private MainTableScroller() {
    }

    static void centerSelectedRow(TableView<?> table) {
        int selectedIndex = table.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        findVirtualFlow(table)
                .flatMap(MainTableScroller::visibleRows)
                .ifPresentOrElse(
                        rows -> scrollToCenter(table, selectedIndex, rows),
                        () -> table.scrollTo(selectedIndex));
    }

    static Optional<VirtualFlow<?>> findVirtualFlow(TableView<?> table) {
        return Optional.ofNullable(table.lookup(".virtual-flow"))
                       .filter(VirtualFlow.class::isInstance)
                       .map(node -> (VirtualFlow<?>) node);
    }

    private static Optional<VisibleRows> visibleRows(VirtualFlow<?> flow) {
        return Optional.ofNullable(flow.getFirstVisibleCell())
                       .flatMap(first -> Optional.ofNullable(flow.getLastVisibleCell())
                                                 .map(last -> new VisibleRows(flow, first.getIndex(), last.getIndex())));
    }

    private static void scrollToCenter(TableView<?> table, int selectedIndex, VisibleRows rows) {
        int visibleRowCount = (rows.lastIndex() - rows.firstIndex()) + 1;
        if (visibleRowCount <= 0) {
            table.scrollTo(selectedIndex);
            return;
        }

        int lastFullViewportStart = Math.max(0, table.getItems().size() - visibleRowCount);
        int centeredViewportStart = Math.clamp(selectedIndex - (visibleRowCount / 2), 0, lastFullViewportStart);
        rows.flow().scrollToTop(centeredViewportStart);
    }

    private record VisibleRows(VirtualFlow<?> flow, int firstIndex, int lastIndex) {
    }
}

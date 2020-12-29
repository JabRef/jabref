package org.jabref.gui.maintable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resize policy is almost the same as {@link TableView#CONSTRAINED_RESIZE_POLICY}
 * We make sure that the width of all columns sums up to the total width of the table.
 * However, in contrast to {@link TableView#CONSTRAINED_RESIZE_POLICY} we size the columns initially by their preferred width.
 */
public class SmartConstrainedResizePolicy implements Callback<TableView.ResizeFeatures, Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartConstrainedResizePolicy.class);

    // Stores the share of the resizable columns.
    // Invariant: sum(shares) = 100%
    private Map<TableColumnBase<?, ?>, Double> expansionShare = new HashMap<>();

    List<? extends TableColumn<?, ?>> resizableColumns;
    private TableColumn<?, ?> lastModifiedColumn = null;

    @Override
    public Boolean call(TableView.ResizeFeatures prop) {
        TableColumn<?, ?> column = prop.getColumn();
        Boolean result;
        if (column == null) {
            // happens at initialization and at window resize
            LOGGER.debug("Table is fully rendered");
            result = doInitialTableRendering(prop);
        } else {
            LOGGER.debug("Column width changed");
            result = doColumnRearrangement(prop, column);
        }
        LOGGER.debug("Result: {}", result);
        return result;
    }

    private Boolean doInitialTableRendering(TableView.ResizeFeatures prop) {
        TableView<?> table = prop.getTable();
        if (table.getWidth() == 0.0d) {
            LOGGER.debug("Table width is 0. Returning false");
            return false;
        }
        resizableColumns = table.getVisibleLeafColumns().stream()
                                .filter(TableColumnBase::isResizable)
                                .collect(Collectors.toList());
        if (contentFitsIntoTable(table)) {
            LOGGER.debug("Content fits into table initially");
            determineExpansionShare();
            LOGGER.debug("Rearranging columns");
            return rearrangeColumns(table);
        } else {
            LOGGER.debug("Content too wide for displayed table. Using default alignment");
        }
        return false;
    }

    private Boolean doColumnRearrangement(TableView.ResizeFeatures prop, TableColumn<?, ?> column) {
        if (!column.equals(lastModifiedColumn)) {
            LOGGER.debug("Column changed");
            lastModifiedColumn = column;
            determineExpansionShareWithoutColumn(column);
        }
        return constrainedResize(prop);
    }

    private boolean contentFitsIntoTable(TableView<?> table) {
        double tableWidth = table.getWidth();
        Double contentWidth = getContentWidth(table);
        boolean comparisonResult = tableWidth >= contentWidth;
        LOGGER.debug("tableWidth {} >= contentWidth {}: {}", tableWidth, contentWidth, comparisonResult);
        return comparisonResult;
    }

    /**
     * Determines the share of the total width each column has.
     * This way, the proportions of the columns are kept during resize of the window
     */
    private void determineExpansionShare() {
        determineExpansionShare(resizableColumns.stream());
    }

    /**
     * This way, the proportions of the columns are kept during resize of the window
     */
    private void determineExpansionShareWithoutColumn(TableColumn excludedVisibleColumn) {
        Stream<? extends TableColumn<?, ?>> streamofColumns = resizableColumns.stream().filter(column -> !column.equals(excludedVisibleColumn));
        determineExpansionShare(streamofColumns);
    }

    private void determineExpansionShare(Stream<? extends TableColumn<?, ?>> streamofColumns) {
        // We need to store the initial preferred width, because "setWidth()" does not exist
        // There is only "setMinWidth", "setMaxWidth", and "setPrefWidth
        Double allResizableColumnsWidth = streamofColumns.mapToDouble(TableColumnBase::getPrefWidth).sum();
        for (TableColumnBase<?, ?> column : resizableColumns) {
            expansionShare.put(column, column.getPrefWidth() / allResizableColumnsWidth);
        }
    }

    /**
     * Determines the new width of the column based on the requested delta. It respects the min and max width of the column.
     * <br>
     * This method is also called if the table content is wider than the window. Thus, it does not respect the overall table width;
     * "just" the width constraints of the given column
     *
     * @param column The column the resize is requested
     * @param delta  The delta requested
     * @return the new size, Optional.empty() if no resize is possible
     */
    private Optional<Double> determineNewWidth(TableColumnBase<?, ?> column, Double delta) {
        // This is com.sun.javafx.scene.control.skin.Utils.boundedSize with more comments and Optionals

        LOGGER.trace("Column {}", column.getText());

        // Calculate newWidth based on delta and constraint of the column
        double oldWidth = column.getWidth();
        double newWidth;
        if (delta < 0) {
            double minWidth = column.getMinWidth();
            LOGGER.trace("MinWidth {}", minWidth);
            newWidth = Math.max(minWidth, oldWidth + delta);
        } else {
            double maxWidth = column.getMaxWidth();
            LOGGER.trace("MaxWidth {}", maxWidth);
            newWidth = Math.min(maxWidth, oldWidth + delta);
        }
        LOGGER.trace("Size: {} -> {}", oldWidth, newWidth);
        if (oldWidth == newWidth) {
            return Optional.empty();
        }
        return Optional.of(newWidth);
    }

    /**
     * Resizes the table based on the content. The main driver is that if the content might fit into the table without horizontal scroll bar.
     * In case the content fitted before the resize and will fit afterwards, the delta is distributed among the remaining columns - instead of just moving the columns right of the current column.
     * In case the content does not fit anymore, a horizontal scroll bar is shown.
     * In all cases the minimum/maximum width of each column is respected.
     */
    private Boolean constrainedResize(TableView.ResizeFeatures<?> prop) {
        TableView<?> table = prop.getTable();
        Double tableWidth = getContentWidth(table);
        Double currentTableContentWidth = getContentWidth(table);
        Double delta = prop.getDelta();

        TableColumn<?, ?> userChosenColumnToResize = prop.getColumn();

        boolean columnsCanFitTable = currentTableContentWidth + delta < tableWidth;
        LOGGER.debug("currentTableContentWidth {} + delta {} < tableWidth {} = {}", currentTableContentWidth, delta, tableWidth, columnsCanFitTable);
        if (columnsCanFitTable) {
            LOGGER.debug("User changed column size in a way that window can contain all the columns");
            Optional<Double> newWidthOptional;
            if (currentTableContentWidth >= tableWidth) {
                LOGGER.debug("Before, the content did not fit. Now it fits. Rearrange everything.");
                return rearrangeColumns(table);
            }
            LOGGER.debug("Everything already fit. We distribute the delta now.");

            // Content does already fit
            // We "just" need to readjust the column widths
            newWidthOptional = determineNewWidth(userChosenColumnToResize, delta);
            newWidthOptional.ifPresent(newWidth -> {
                distributeDelta(table, userChosenColumnToResize, newWidth);
            });
            return newWidthOptional.isPresent();
        }

        LOGGER.debug("Window smaller than content");

        Optional<Double> newWidth = determineNewWidth(userChosenColumnToResize, delta);
        newWidth.ifPresent(userChosenColumnToResize::setPrefWidth);
        return newWidth.isPresent();
    }

    private void distributeDelta(TableView<?> table, TableColumnBase userChosenColumnToResize, Double newWidth) {
        userChosenColumnToResize.setPrefWidth(newWidth);
        List<? extends TableColumn<?, ?>> columnsToResize = resizableColumns.stream().filter(col -> !col.equals(userChosenColumnToResize)).collect(Collectors.toList());
        rearrangeColumns(table, columnsToResize);
    }

    /**
     * Rearranges the widths of all columns according to their shares (proportional to their preferred widths)
     */
    private Boolean rearrangeColumns(TableView<?> table) {
        Double initialContentWidth = getContentWidth(table);
        LOGGER.debug("initialContentWidth {}", initialContentWidth);

        // Implementation idea:
        // Each column has to have at least the minimum width
        // First, set the minimum width of all columns
        // Then, there is available non-assigned space
        // Distribute this space in a fair way to all resizable columns

        for (TableColumnBase<?, ?> col : resizableColumns) {
            col.setPrefWidth(col.getMinWidth());
        }
        LOGGER.debug("Width after setting min width: {}", getContentWidth(table));

        rearrangeColumns(table, resizableColumns);
        LOGGER.debug("Width after rearranging columns: {}", getContentWidth(table));

        return (initialContentWidth - getContentWidth(table)) != 0d;
    }

    private void rearrangeColumns(TableView<?> table, List<? extends TableColumn<?, ?>> columnsToResize) {
        Double tableWidth = table.getWidth();
        Double newContentWidth;

        do
        {
            // in case the userChosenColumnToResize got bigger, the remaining available width will get below 0 --> other columns need to be shrunk
            LOGGER.debug("tableWidth {}", tableWidth);
            Double contentWidth = getContentWidth(table);
            LOGGER.debug("contentWidth {}", contentWidth);
            Double remainingAvailableWidth = tableWidth - contentWidth;
            // Double remainingAvailableWidth = Math.max(0, tableWidth - contentWidth);
            LOGGER.debug("Distributing remainingAvailableWidth {}", remainingAvailableWidth);
            for (TableColumnBase<?, ?> col : columnsToResize) {
                double share = expansionShare.get(col);
                // Precondition in our case: col has to have minimum width
                double delta = share * remainingAvailableWidth;
                LOGGER.debug("share {} * remainingAvailableWidth {} = delta {}", share, remainingAvailableWidth, delta);
                determineNewWidth(col, delta)
                        // in case we can do something, do it
                        // otherwise, the next loop iteration will distribute it
                        .ifPresent(col::setPrefWidth);
            }
            newContentWidth = getContentWidth(table);
            LOGGER.debug("newContentWidth {}", newContentWidth);
            LOGGER.debug("tableWidth - newContentWidth = {}", tableWidth - newContentWidth);
        } while (tableWidth - newContentWidth > 20d);
    }

    /**
     * Sums up the minimum widths of each column in the table
     */
    private Double getSumMinWidthOfColumns(TableView<?> table) {
        return table.getColumns().stream().mapToDouble(TableColumnBase::getMinWidth).sum();
    }

    /**
     * Computes and returns the width required by the content of the table
     */
    private Double getContentWidth(TableView<?> table) {
        // The current table content width contains all visible columns: the resizable ones and the non-resizable ones
        return table.getVisibleLeafColumns().stream().mapToDouble(TableColumnBase::getWidth).sum();
    }
}

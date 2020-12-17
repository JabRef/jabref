package org.jabref.gui.maintable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private Map<TableColumnBase, Double> expansionShare = new HashMap<>();

    @Override
    public Boolean call(TableView.ResizeFeatures prop) {
        if (prop.getColumn() == null) {
            // happens at initialization and at window resize
            LOGGER.debug("Table is fully rendered");
            // This way, the proportions of the columns are kept during resize of the window
            determineWidthShare(prop.getTable());
            return rearrangeColumns(prop.getTable());
        } else {
            return constrainedResize(prop);
        }
    }

    /**
     * Determines the share of the total width each column has
     */
    private void determineWidthShare(TableView table) {
        // We need to store the initial preferred width, because "setWidth()" does not exist
        // There is only "setMinWidth", "setMaxWidth", and "setPrefWidth

        Double totalInitialPreferredWidths;
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();
        totalInitialPreferredWidths = visibleLeafColumns.stream()
                                                        .filter(TableColumnBase::isResizable)
                                                        .mapToDouble(TableColumnBase::getPrefWidth).sum();
        for (TableColumnBase<?, ?> column : visibleLeafColumns) {
            if (column.isResizable()) {
                expansionShare.put(column, column.getPrefWidth() / totalInitialPreferredWidths);
            } else {
                expansionShare.put(column, 0d);
            }
        }
    }

    /**
     * Determines the new width of the column based on the requested delta. It respects the min and max width of the column.
     *
     * @param column The column the resize is requested
     * @param delta  The delta requested
     * @return the new size, Optional.empty() if no resize is possible
     */
    private Optional<Double> determineNewWidth(TableColumn<?, ?> column, Double delta) {
        // This is com.sun.javafx.scene.control.skin.Utils.boundedSize with more comments and Optionals

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
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();
        Double requiredWidth = visibleLeafColumns.stream().mapToDouble(TableColumnBase::getWidth).sum();
        Double delta = prop.getDelta();

        TableColumn<?, ?> userChosenColumnToResize = prop.getColumn();

        boolean columnsCanFitTable = requiredWidth + delta < tableWidth;
        if (columnsCanFitTable) {
            LOGGER.debug("User shrunk column in that way thus that window can contain all the columns");
            if (requiredWidth >= tableWidth) {
                LOGGER.debug("Before, the content did not fit. Rearrange everything.");
                return rearrangeColumns(table);
            }
            LOGGER.debug("Everything already fit. We distribute the delta now.");

            // Content does already fit
            // We "just" need to readjust the column widths
            determineNewWidth(userChosenColumnToResize, delta)
                    .ifPresent(newWidth -> {
                        double currentWidth = userChosenColumnToResize.getWidth();
                        Double deltaToApply = newWidth - currentWidth;
                        for (TableColumnBase<?, ?> col : visibleLeafColumns) {
                            Double share = expansionShare.get(col);
                            Double newSize;
                            if (col.equals(prop.getColumn())) {
                                // The column resized by the user gets the delta;
                                newSize = newWidth;
                            } else {
                                // The columns not explicitly resized by the user get the negative delta
                                newSize = col.getWidth() - share * deltaToApply;
                            }
                            newSize = Math.min(newSize, col.getMaxWidth());
                            col.setPrefWidth(newSize);
                        }
                    });
            return true;
        }

        LOGGER.debug("Window smaller than content");

        determineNewWidth(userChosenColumnToResize, delta).ifPresent(newWidth ->
                userChosenColumnToResize.setPrefWidth(newWidth));
        return true;
    }

    /**
     * Rearranges the widths of all columns according to their shares (proportional to their preferred widths)
     */
    private Boolean rearrangeColumns(TableView<?> table) {
        Double tableWidth = getContentWidth(table);
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();

        Double remainingAvailableWidth = tableWidth - getSumMinWidthOfColumns(table);

        for (TableColumnBase<?, ?> col : visibleLeafColumns) {
            double share = expansionShare.get(col);
            double newSize = col.getMinWidth() + share * remainingAvailableWidth;

            // Just to make sure that we are staying under the total table width (due to rounding errors)
            newSize -= 2;

            newSize = Math.min(newSize, col.getMaxWidth());

            col.setPrefWidth(newSize);
        }
        return true;
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
        try {
            // TODO: reflective access, should be removed
            Field privateStringField = TableView.class.getDeclaredField("contentWidth");
            privateStringField.setAccessible(true);
            return (Double) privateStringField.get(table);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return 0d;
        }
    }
}

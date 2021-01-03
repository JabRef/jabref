package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This resize policy supports following properties &ndash; preferably user-configurable:
 *
 * <ul>
 *  <li>Honoring the minimal and maximal width for each column</li>
 *  <li>Honoring a <emph>desired width</emph> of each column (if available). It is used at initial table rendering (e.g., program startup) and for the threshold (see below). It is not used in other cases.</li>
 *  <li>Honoring non-resizable columns (i.e., that a column should not be scaled)</li>
 *  <li>Desired width derivation (e.g., <code>80%</code> of the desired width) is a point from which on the column should not be shrunk any more. This is called the <emph>threshold</emph>.</li>
 * </ul>
 *
 * This resize policy supports following properties (non user-configurable):
 *
 * <ul>
 *  <li>Between each resizing, the properties of each column (minimal width, resizable, ...) can change.</li>
 *  <li>Automatic sizing of the columns:
 *    <ul>
 *      <li>Each column should have at least its minimal size.</li>
 *      <li>If the space honoring the minimal width is not sufficient, a horizontal scrollbar should be used.</li>
 *      <li>Columns with fixed size should use those fixed sizes</li>
 *      <li>If there is additional space left the remaining columns should be broadened to use this space</li>
 *    </ul>
 *  </li>
 *  <li>We balance between a complete fit into the table space and the desired width. A "huge" derivation from the desired width is not accepted.</li>
 *  <li>Ideally, the columns should have the desired width</li>
 *  <li>If a user changes the size of a column, the new size of that column should be set.</li>
 *  <li>No behavior toggle buttons by the user --&gt; this policy is a smart one</li>
 *  <li>We distinguish between a column being resized by the user and a column automatically resized.</li>
 *  <li>In case a user changes a column manually:
 *    <ul>
 *      <li>The column shrinks/enlarges by the requested delta.</li>
 *      <li>The column must not shrink below the minimum width / enlarge above the maximum width.</li>
 *      <li>Thereby, the desired width is <emph>not</emph> changed.</li>
 *      <li>The other columns adapt "smartly": The other columns according to their current ratio. This way, the column proportions are respected.</li>
 *    </ul>
 *  </li>
 *  <li>The ratio used for enlargement of columns respects the current column width. The ratio for shrinkage is constant for all columns.</li>
 * </ul>
 *
 * The implementation is driven by following factors:
 *
 * <ul>
 *   <li>Minimal width is an "absolute" minimal making the content nearly barely visible</li>
 *   <li>Maximal width is an "absolute" maximal width making the content too much visible</li>
 *   <li>The preferred width holds the current active width of a column (due to JavaFX design decisions).</li>
 *   <li>Desired width is set to a reasonable value. The desired width is a "globally" preferred width.</li>
 * </ul>
 *
 * <h2>RC0 - Resizing of column</h2>
 *
 * <ul>
 *   <li>RCE0 - If enlarged
 *     <ul>
 *         <li>RCE1 - Content has fit into table. Then, content does not fit into table (because of delta). Column gets enlarged by the delta. Other columns should not below a certain "reasonable" size ("threshold"). All columns are shrunk (all equally) until all columns hit the threshold. In case no column can shrink any more (they are smaller or equal the threshold), they are not shrunk. Thus, the table gets wider than the table space. This leads to a scrollbar.</li>
 *         <li>RCE2 - Content has not fit into table. Then, content still does not fit into table (because of delta). Column gets enlarged by the delta. Other columns are not changed. Scrollbar gets wider.</li>
 *     </ul>
 *   </li>
 *   <li>RCS0 - If shrunk
 *     <ul>
 *       <li>RCS1 - Content has fit into table. Then, content still fits into table (because of delta). Column must not shrink below minimum width. If minimum is reached, nothing happens. Remaining delta is distributed among other columns. No scrollbar present.</li>
 *       <li>RCS2 - Content has not fit into table. If the delta is applied, the content fits into the table. Delta is applied to the column. Remaining delta splits up of delta-in-bounds and delta-out-of-bounds. Delta-in-bounds is distributed among other columns. Scrollbar disappears.</li>
 *       <li>RCS3 - Content has not fit into table. If the delta is applied, the content still does not fit into table. Column is shrunk respecting the delta. Other columns are resized. Scrollbar shrinks.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>RW0 - Resizing of window</h2>
 *
 * <ul>
 *   <li>RWE0 - If enlarged
 *     <ul>
 *       <li>RWE1 - Content has fit into table. Then, content still has to fit into table. Thus, enlarge all columns respecting the ratio (the content is enlarged proportional to actual width). Scrollbar not present.</li>
 *       <li>RWE2 - Content has not fit into table. Case: Content still not fits into table. No resize action. Scrollbar shrinks.</li>
 *       <li>RWE3 - Content has not fit into table. Case: Content fits into table. Calculate the delta and distribute across all resizable columns using the ratio. Scrollbar not present any more.</li>
 *     </ul>
 *   </li>
 *   <li>RWS0 - If shrunk
 *     <ul>
 *       <li>RWS1 - Content has fit into table. Content does not fit into table anymore (because of shrinkage). Delta is absorbed by columns until threshold is reached. If not complete delta can be absorbed, scrollbar appears.</li>
 *       <li>RWS2 - Content has not fit into table. Content still does not fit into table (because of shrinkage). Scrollbar enlarges. </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>I0 - Initial table rendering</h2>
 *
 * Decision to take: a) Use last setting or b) rerender with desired widths. We opt for b), because we assume that user-configuration of desired widths is easy.
 *
 * <ul>
 *   <li>I1 - Initially, all columns takes the desired width.</li>
 *   <li>I2 - If content fits into table, distribute remaining delta to table space according to the shares. No scollbar.</li>
 *   <li>I3 - If content does not fit into table, all columns are shrunk (all equally) until all columns hit the threshold. In case no column can shrink any more (they are smaller or equal the threshold), they are not shrunk. Thus, the table gets wider than the table space. This leads to a scrollbar.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 *
 * <ul>
 *   <li>Design goal of this class is to be self-contained and not dependent on other non-JavaFX classes.</li>
 *   <li>TODO: In case the desired column width is updated, the SmartConstrainedResizePolicy needs to be reinstantiated. A more advanced code would use JavaFX Properties for that.</li>
 * </ul>
 *
 * <p>Related Work</p>
 * <ul>
 *   <li><a href="https://github.com/edvin/tornadofx/wiki/TableView-SmartResize">TableView SmartResize Policy</a></li>
 *   <li><a href="https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TableView.html#CONSTRAINED_RESIZE_POLICY">CONSTRAINED_RESIZE_POLICY</a>: This policy a) initially adjust the column widths of all columns in a way that the table fits the whole table space, b) adjusts the width of the columns right to the current column to have the whole table fit into the table space. The policy starts with the minimum width, not with the preferred width. The policy does not support the case if the content does not fit into the table space.</li>
 *   <li><a href="https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TableView.html#UNCONSTRAINED_RESIZE_POLICY">UNCONSTRAINED_RESIZE_POLICY</a>: This policy just resizes the specified column by the provided delta and shifts all other columns (to the right of the given column) further to the right (when the delta is positive) or to the left (when the delta is negative).</li>
 *   <li><a href="https://github.com/JoshuaD84/HypnosMusicPlayer/blob/06ce94cd69382f13901f0b73491bb93afd4b84ee/src/net/joshuad/hypnos/fxui/HypnosResizePolicy.java">HypnosResizePolicy</a>: Similar to CONSTRAINED_RESIZE_POLICY. However, at resize extra space is given to columns that aren't at their pref width and need that type of space (negative or positive) on a proportional basis first.</li>
 * </ul>
 */
public class SmartConstrainedResizePolicy implements Callback<TableView.ResizeFeatures, Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartConstrainedResizePolicy.class);

    private List<? extends TableColumn<?, ?>> resizableColumns;

    private TableColumn<?, ?> lastModifiedColumn = null;

    private Map<TableColumn, Double> desiredColumnWidths;
    private Double thresholdPercent;
    private boolean firstTimeRun = true;

    public SmartConstrainedResizePolicy() {
        this.desiredColumnWidths = Collections.emptyMap();
        // default is 80%
        this.thresholdPercent = .8;
    }

    /**
     * Sets an desired column width for a set of columns. A "desired" width is a width which is wished by the user
     *
     * @param desiredColumnWidths
     * @param thresholdPercent Value between 0 and 1 (normal percentage calculation: 1 is 100%, .8 is 80%, ...)
     */
    public SmartConstrainedResizePolicy(Map<TableColumn, Double> desiredColumnWidths, Double thresholdPercent) {
        this.desiredColumnWidths = desiredColumnWidths;
        this.thresholdPercent = thresholdPercent;
    }

    public Double getMinWidthThreshold(TableColumn column) {
        return getDesiredColumnWidth(column) * thresholdPercent;
    }

    private Double getDesiredColumnWidth(TableColumn column) {
        return desiredColumnWidths.getOrDefault(column, column.getPrefWidth());
    }

    @Override
    public Boolean call(TableView.ResizeFeatures prop) {
        if (firstTimeRun) {
            if (prop.getTable().getWidth() == 0.0d) {
                LOGGER.debug("Table width is 0. Returning false");
                return false;
            }

            firstTimeGlobalVariablesInitializations(prop.getTable());



            // case I0
            LOGGER.debug("Table is rendered the first time");
            doInitialTableRendering(prop.getTable());
            firstTimeRun = false;
            return true;
        }

        TableColumn<?, ?> column = prop.getColumn();
        Boolean result;
        if (column == null) {
            // happens at window resize
            LOGGER.debug("Table is fully rendered");
            result = doFullTableRendering(prop);
        } else {
            LOGGER.debug("Column width changed");
            result = doColumnRearrangement(prop);
        }
        LOGGER.debug("Result: {}", result);
        return result;
    }

    private void firstTimeGlobalVariablesInitializations(TableView<?> table) {
        resizableColumns = table.getVisibleLeafColumns().stream()
                                .filter(TableColumn::isResizable)
                                .collect(Collectors.toList());
    }

    private void doInitialTableRendering(TableView table) {
        // case I1
        resizableColumns.forEach(column -> column.setPrefWidth(getDesiredColumnWidth(column)));

        if (contentFitsIntoTable(table)) {
            // case I2
            Map<TableColumn<?, ?>, Double> expansionRatio = determineExpansionRatio(resizableColumns);
            rearrangeColumns(table, resizableColumns, expansionRatio);
        } else {
            // case I3
            Map<TableColumn<?, ?>, Double> shrinkageRatio = determineShrinkageRatio(resizableColumns);
            rearrangeColumns(table, resizableColumns, shrinkageRatio);
        }
    }

    private Boolean doFullTableRendering(TableView.ResizeFeatures prop) {
        TableView<?> table = prop.getTable();
        if (table.getWidth() == 0.0d) {
            LOGGER.debug("Table width is 0. Returning false");
            return false;
        }
        if (contentFitsIntoTable(table)) {
            LOGGER.debug("Content fits into table initially");
            LOGGER.debug("Rearranging columns");
            return rearrangeColumns(table);
        } else {
            LOGGER.debug("Content too wide for displayed table. Using default alignment");
        }
        return false;
    }

    private boolean contentFitsIntoTable(TableView<?> table) {
        double tableWidth = table.getWidth();
        Double contentWidth = getContentWidth(table);
        boolean comparisonResult = tableWidth >= contentWidth;
        LOGGER.debug("tableWidth {} >= contentWidth {}: {}", tableWidth, contentWidth, comparisonResult);
        return comparisonResult;
    }

    /**
     * This way, the proportions of the columns are kept during resize of the window
     */
    private Map<TableColumn<?, ?>, Double> determineExpansionRatioWithoutColumn(TableColumn excludedVisibleColumn) {
        List<? extends TableColumn<?, ?>> columns = resizableColumns.stream().filter(column -> !column.equals(excludedVisibleColumn)).collect(Collectors.toList());
        return determineExpansionRatio(columns);
    }

    /**
     * Determines the share of the given columns
     * Invariant: sum(shares) = 100%
     */
    private Map<TableColumn<?, ?>, Double> determineExpansionRatio(List<? extends TableColumn<?, ?>> columns) {
        Map<TableColumn<?, ?>, Double> expansionRatio = new HashMap<>();

        // We need to store the initial preferred width, because "setWidth()" does not exist
        // There is only "setMinWidth", "setMaxWidth", and "setPrefWidth
        Double allColumnsWidth = columns.stream().mapToDouble(TableColumn::getWidth).sum();
        LOGGER.debug("allColumnsWidth: {}", allColumnsWidth);
        for (TableColumn column : columns) {
            double share = column.getWidth() / allColumnsWidth;
            LOGGER.debug("share of {}: {}", column.getText(), share);
            expansionRatio.put(column, share);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sum of shares: {}", expansionRatio.values().stream().mapToDouble(Double::doubleValue).sum());
        }
        return expansionRatio;
    }

    /**
     * Determines the shrinkage ratio. It is a uniform distribution.
     */
    private Map<TableColumn<?,?>, Double> determineShrinkageRatio(List<? extends TableColumn<?,?>> resizableColumns) {
        Map<TableColumn<?, ?>, Double> shrinkageRatio = new HashMap<>();
        Double share = 1.0 / resizableColumns.size();
        resizableColumns.forEach(tableColumn -> shrinkageRatio.put(tableColumn, share));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sum of shares: {}", shrinkageRatio.values().stream().mapToDouble(Double::doubleValue).sum());
        }
        return shrinkageRatio;
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
    private Optional<Double> determineNewWidth(TableColumn column, Double delta) {
        // This is com.sun.javafx.scene.control.skin.Utils.boundedSize with more comments and Optionals

        LOGGER.trace("Column {}", column.getText());

        // Calculate newWidth based on delta and constraint of the column
        double oldWidth = column.getWidth();
        double newWidth;
        if (delta < 0) {
            double minWidth = getMinWidthThreshold(column);
            LOGGER.trace("getMinWidthThreshold {}", minWidth);
            newWidth = Math.max(minWidth, oldWidth + delta);
        } else {
            double maxWidth = column.getMaxWidth();
            LOGGER.trace("MaxWidth {}", maxWidth);
            newWidth = Math.min(maxWidth, oldWidth + delta);
        }
        newWidth = Math.floor(newWidth) - 2d;
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
    private Boolean doColumnRearrangement(TableView.ResizeFeatures<?> prop) {
        TableColumn userChosenColumnToResize = prop.getColumn();

        TableView<?> table = prop.getTable();
        Double tableWidth = table.getWidth();
        Double currentTableContentWidth = getContentWidth(table);
        Double delta = prop.getDelta();
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

            TableColumn<?, ?> column = prop.getColumn();
            if (!column.equals(lastModifiedColumn)) {
                LOGGER.debug("Column changed");
                lastModifiedColumn = column;
                determineExpansionRatioWithoutColumn(column);
            }

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

    private void distributeDelta(TableView<?> table, TableColumn userChosenColumnToResize, Double newWidth) {
        userChosenColumnToResize.setPrefWidth(newWidth);
        List<? extends TableColumn<?, ?>> columnsToResize = resizableColumns.stream().filter(col -> !col.equals(userChosenColumnToResize)).collect(Collectors.toList());
        rearrangeColumns(table, columnsToResize);
    }

    /**
     * Rearranges the widths of all columns according to their shares (proportional to their preferred widths)
     */
    private Boolean rearrangeColumns(TableView<?> table) {
        determineExpansionRatio();

        Double initialContentWidth = getContentWidth(table);
        LOGGER.debug("initialContentWidth {}", initialContentWidth);

        // Implementation idea:
        // Each column has to have at least the minimum width
        // First, set the minimum width of all columns
        // Then, there is available non-assigned space
        // Distribute this space in a fair way to all resizable columns

        for (TableColumn col : resizableColumns) {
            col.setPrefWidth(col.getMinWidth());
        }
        LOGGER.debug("Width after setting min width: {}", getContentWidth(table));

        rearrangeColumns(table, resizableColumns);
        LOGGER.debug("Width after rearranging columns: {}", getContentWidth(table));

        return (initialContentWidth - getContentWidth(table)) != 0d;
    }

    /**
     * Completely rearranges the given columnsToResize so that the complete table content fits into the table space (in case threshold of columns is not hit)
     *
     * Handles cases I2 and I3
     *
     * @param table The table to handle
     * @param columnsToResize The columns allowed to change the widths
     * @param ratio The expansion/shrinkage ratio of the columns
     */
    private void rearrangeColumns(TableView<?> table, List<? extends TableColumn<?, ?>> columnsToResize, Map<TableColumn<?, ?>, Double> ratio) {
        Double tableWidth = table.getWidth();
        Double newContentWidth;

        int iterations = 0;

        double remainingPixels;
        do {
            iterations++;
            // in case the userChosenColumnToResize got bigger, the remaining available width will get below 0 --> other columns need to be shrunk
            LOGGER.debug("tableWidth {}", tableWidth);
            Double contentWidth = getContentWidth(table);
            LOGGER.debug("contentWidth {}", contentWidth);
            Double remainingAvailableWidth = tableWidth - contentWidth;
            // Double remainingAvailableWidth = Math.max(0, tableWidth - contentWidth);
            LOGGER.debug("Distributing remainingAvailableWidth {}", remainingAvailableWidth);
            for (TableColumn column : columnsToResize) {
                LOGGER.debug("Column {}", column.getText());
                double share = ratio.get(column);
                // Precondition in our case: column has to have minimum width
                double delta = share * remainingAvailableWidth;
                LOGGER.debug("share {} * remainingAvailableWidth {} = delta {}", share, remainingAvailableWidth, delta);
                determineNewWidth(column, delta)
                        // in case we can do something, do it
                        // otherwise, the next loop iteration will distribute it
                        .ifPresent(column::setPrefWidth);
            }
            newContentWidth = getContentWidth(table);
            LOGGER.debug("newContentWidth {}", newContentWidth);
            remainingPixels = Math.abs(tableWidth - newContentWidth);
            LOGGER.debug("|tableWidth - newContentWidth| = {}", remainingPixels);
        } while (remainingPixels > 20d && iterations <= 2);

        /*

        Quick hack - has to be moved elsewhere

        // in case the table has less than 7 pixels, a scroll bar is there
        // quickfix by removing pixels
        double amountOfRemainingPixelsRequiredForNoScrollBar = 7.0;
        if (remainingPixels < amountOfRemainingPixelsRequiredForNoScrollBar) {
            double pixelsToRemove = amountOfRemainingPixelsRequiredForNoScrollBar - remainingPixels;
            Iterator<? extends TableColumn<?, ?>> iterator = columnsToResize.iterator();
            while (pixelsToRemove > 0) {
                if (!iterator.hasNext()) {
                    iterator = columnsToResize.iterator();
                }
                TableColumn<?, ?> column = iterator.next();
                Optional<Double> aDouble = determineNewWidth(column, -1.0);
                if (aDouble.isPresent()) {
                    column.setPrefWidth(aDouble.get());
                    pixelsToRemove--;
                }
            }
        }

         */
    }

    /**
     * Sums up the minimum widths of each column in the table
     */
    private Double getSumMinWidthOfColumns(TableView<?> table) {
        return table.getColumns().stream().mapToDouble(TableColumn::getMinWidth).sum();
    }

    /**
     * Computes and returns the width required by the content of the table
     */
    private Double getContentWidth(TableView<?> table) {
        // The current table content width contains all visible columns: the resizable ones and the non-resizable ones
        return table.getVisibleLeafColumns().stream().mapToDouble(TableColumn::getWidth).sum();
    }
}

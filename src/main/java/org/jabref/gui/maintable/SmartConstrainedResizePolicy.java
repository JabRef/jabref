package org.jabref.gui.maintable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javafx.scene.control.ResizeFeaturesBase;
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

    @Override
    public Boolean call(TableView.ResizeFeatures prop) {
        if (prop.getColumn() == null) {
            // table is initialized
            // no need to adjust
            return false;
        } else {
            return constrainedResize(prop);
        }
    }

    private Boolean constrainedResize(TableView.ResizeFeatures<?> prop) {
        TableView<?> table = prop.getTable();
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();
        Double delta = prop.getDelta();
        TableColumn<?, ?> userChosenColumnToResize = prop.getColumn();

        double oldWidth = userChosenColumnToResize.getWidth();
        double newWidth;
        if (delta < 0) {
            double minWidth = userChosenColumnToResize.getMinWidth();
            LOGGER.trace("MinWidth {}", minWidth);
            newWidth = Math.max(minWidth, oldWidth + delta);
        } else {
            double maxWidth = userChosenColumnToResize.getMaxWidth();
            LOGGER.trace("MaxWidth {}", maxWidth);
            newWidth = Math.min(maxWidth, oldWidth + delta);
        }
        LOGGER.trace("Size: {} -> {}", oldWidth, newWidth);
        if (oldWidth == newWidth) {
            return false;
        }
        userChosenColumnToResize.setPrefWidth(newWidth);
        return true;
    }
}

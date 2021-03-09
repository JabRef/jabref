package org.jabref.gui.maintable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javafx.scene.control.ResizeFeaturesBase;
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
            return initColumnSize(prop.getTable());
        } else {
            return constrainedResize(prop);
        }
    }

    private Boolean initColumnSize(TableView<?> table) {
        double tableWidth = getContentWidth(table);
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();
        double totalWidth = visibleLeafColumns.stream().mapToDouble(TableColumnBase::getWidth).sum();

        if (Math.abs(totalWidth - tableWidth) > 1) {
            double totalPrefWidth = visibleLeafColumns.stream().mapToDouble(TableColumnBase::getPrefWidth).sum();
            double currPrefWidth = 0;
            if (totalPrefWidth > 0) {
                for (TableColumnBase col : visibleLeafColumns) {
                    double share = col.getPrefWidth() / totalPrefWidth;
                    double newSize = tableWidth * share;

                    // Just to make sure that we are staying under the total table width (due to rounding errors)
                    currPrefWidth += newSize;
                    if (currPrefWidth > tableWidth) {
                        newSize -= currPrefWidth - tableWidth;
                        currPrefWidth -= tableWidth;
                    }

                    resize(col, newSize - col.getWidth());
                }
            }
        }

        return false;
    }

    private void resize(TableColumnBase column, double delta) {
        // We have to use reflection since TableUtil is not visible to us
        try {
            // TODO: reflective access, should be removed
            Class<?> clazz = Class.forName("javafx.scene.control.TableUtil");
            Method constrainedResize = clazz.getDeclaredMethod("resize", TableColumnBase.class, double.class);
            constrainedResize.setAccessible(true);
            constrainedResize.invoke(null, column, delta);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            LOGGER.error("Could not invoke resize in TableUtil", e);
        }
    }

    private Boolean constrainedResize(TableView.ResizeFeatures<?> prop) {
        TableView<?> table = prop.getTable();
        List<? extends TableColumnBase<?, ?>> visibleLeafColumns = table.getVisibleLeafColumns();
        return constrainedResize(prop,
                false,
                getContentWidth(table) - 2,
                visibleLeafColumns);
    }

    private Boolean constrainedResize(TableView.ResizeFeatures prop, Boolean isFirstRun, Double contentWidth, List<? extends TableColumnBase<?, ?>> visibleLeafColumns) {
        // We have to use reflection since TableUtil is not visible to us
        try {
            // TODO: reflective access, should be removed
            Class<?> clazz = Class.forName("javafx.scene.control.TableUtil");
            Method constrainedResize = clazz.getDeclaredMethod("constrainedResize", ResizeFeaturesBase.class, Boolean.TYPE, Double.TYPE, List.class);
            constrainedResize.setAccessible(true);
            Object returnValue = constrainedResize.invoke(null, prop, isFirstRun, contentWidth, visibleLeafColumns);
            return (Boolean) returnValue;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            LOGGER.error("Could not invoke constrainedResize in TableUtil", e);
            return false;
        }
    }

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

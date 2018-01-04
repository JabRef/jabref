package org.jabref.gui.maintable;

import javafx.scene.control.TableView;
import javafx.util.Callback;

public class SmartConstraintedResizePolicy implements Callback<TableView.ResizeFeatures, Boolean> {
    private boolean isFirstRun = true;
    private Callback<TableView.ResizeFeatures, Boolean> basePolicy = TableView.CONSTRAINED_RESIZE_POLICY;

    @Override
    public Boolean call(TableView.ResizeFeatures prop) {
        Boolean result = basePolicy.call(prop);

        // The CONSTRAINED_RESIZE_POLICY resizes all columns to the same width
        // We want to resize them to (almost) their preferred width
        if (isFirstRun) {

        }

        isFirstRun = isFirstRun && !result;
        return result;
    }
}

package org.jabref.preferences;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;

public class PreviewPreferences {

    private final ObservableList<PreviewLayout> layoutCycle;
    private final IntegerProperty layoutCyclePosition;
    private final ObjectProperty<TextBasedPreviewLayout> customPreviewLayout;
    private final StringProperty defaultCustomPreviewLayout;
    private final BooleanProperty showPreviewAsExtraTab;

    public PreviewPreferences(List<PreviewLayout> layoutCycle,
                              int layoutCyclePosition,
                              TextBasedPreviewLayout customPreviewLayout,
                              String defaultCustomPreviewLayout,
                              boolean showPreviewAsExtraTab) {
        this.layoutCycle = FXCollections.observableArrayList(layoutCycle);
        this.layoutCyclePosition = new SimpleIntegerProperty(layoutCyclePosition);
        this.customPreviewLayout = new SimpleObjectProperty<>(customPreviewLayout);
        this.defaultCustomPreviewLayout = new SimpleStringProperty(defaultCustomPreviewLayout);
        this.showPreviewAsExtraTab = new SimpleBooleanProperty(showPreviewAsExtraTab);
    }

    public ObservableList<PreviewLayout> getLayoutCycle() {
        return layoutCycle;
    }

    public int getLayoutCyclePosition() {
        return layoutCyclePosition.getValue();
    }

    public IntegerProperty layoutCyclePositionProperty() {
        return layoutCyclePosition;
    }

    public void setLayoutCyclePosition(int position) {
        if (layoutCycle.isEmpty()) {
            this.layoutCyclePosition.setValue(0);
        } else {
            int previewCyclePosition = position;
            while (previewCyclePosition < 0) {
                previewCyclePosition += layoutCycle.size();
            }
            this.layoutCyclePosition.setValue(previewCyclePosition % layoutCycle.size());
        }
    }

    public PreviewLayout getSelectedPreviewLayout() {
        if (layoutCycle.size() <= 0
                || layoutCyclePosition.getValue() < 0
                || layoutCyclePosition.getValue() >= layoutCycle.size()) {
            return getCustomPreviewLayout();
        } else {
            return layoutCycle.get(layoutCyclePosition.getValue());
        }
    }

    public TextBasedPreviewLayout getCustomPreviewLayout() {
        return customPreviewLayout.getValue();
    }

    public ObjectProperty<TextBasedPreviewLayout> customPreviewLayoutProperty() {
        return customPreviewLayout;
    }

    public void setCustomPreviewLayout(TextBasedPreviewLayout layout) {
        this.customPreviewLayout.set(layout);
    }

    public String getDefaultCustomPreviewLayout() {
        return defaultCustomPreviewLayout.getValue();
    }

    public boolean shouldShowPreviewAsExtraTab() {
        return showPreviewAsExtraTab.getValue();
    }

    public BooleanProperty showPreviewAsExtraTabProperty() {
        return showPreviewAsExtraTab;
    }

    public void setShowPreviewAsExtraTab(boolean showPreviewAsExtraTab) {
        this.showPreviewAsExtraTab.set(showPreviewAsExtraTab);
    }
}

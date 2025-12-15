package org.jabref.gui.preview;

import java.nio.file.Path;
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

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;

import com.airhacks.afterburner.injection.Injector;

public class PreviewPreferences {

    private final ObservableList<PreviewLayout> layoutCycle;
    private final IntegerProperty layoutCyclePosition;
    private final ObjectProperty<TextBasedPreviewLayout> customPreviewLayout;
    private final StringProperty defaultCustomPreviewLayout;
    private final BooleanProperty showPreviewAsExtraTab;
    private final BooleanProperty showPreviewEntryTableTooltip;
    private final ObservableList<Path> bstPreviewLayoutPaths;

    public PreviewPreferences(List<PreviewLayout> layoutCycle,
                              int layoutCyclePosition,
                              TextBasedPreviewLayout customPreviewLayout,
                              String defaultCustomPreviewLayout,
                              boolean showPreviewAsExtraTab,
                              boolean showPreviewEntryTableTooltip,
                              List<Path> bstPreviewLayoutPaths) {
        this.layoutCycle = FXCollections.observableArrayList(layoutCycle);
        this.layoutCyclePosition = new SimpleIntegerProperty(layoutCyclePosition);
        this.customPreviewLayout = new SimpleObjectProperty<>(customPreviewLayout);
        this.defaultCustomPreviewLayout = new SimpleStringProperty(defaultCustomPreviewLayout);
        this.showPreviewAsExtraTab = new SimpleBooleanProperty(showPreviewAsExtraTab);
        this.showPreviewEntryTableTooltip = new SimpleBooleanProperty(showPreviewEntryTableTooltip);
        this.bstPreviewLayoutPaths = FXCollections.observableList(bstPreviewLayoutPaths);
    }

    private PreviewPreferences() {
        this(

                FXCollections.observableArrayList(new TextBasedPreviewLayout(
                        "Preview",
                        Injector.instantiateModelOrService(LayoutFormatterPreferences.class),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class)
                )),                                 // Default layout cycle with one default layout
                0,                                  // Default cycle position
                new TextBasedPreviewLayout(
                        "Preview",
                        Injector.instantiateModelOrService(LayoutFormatterPreferences.class),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class)
                ),                                  // Default custom preview layout
                "Preview",                          // Default style name
                false,                              // Default show as tab
                false,                              // Default show tooltip
                FXCollections.observableArrayList() // Default BST paths (empty)
        );
    }

    public static PreviewPreferences getDefault() {
        return new PreviewPreferences();
    }

    public void setAll(PreviewPreferences preferences) {
        this.layoutCycle.setAll(preferences.getLayoutCycle());
        this.layoutCyclePosition.set(preferences.getLayoutCyclePosition());
        this.customPreviewLayout.set(preferences.getCustomPreviewLayout());
        this.defaultCustomPreviewLayout.set(preferences.getDefaultCustomPreviewLayout());
        this.showPreviewAsExtraTab.set(preferences.shouldShowPreviewAsExtraTab());
        this.showPreviewEntryTableTooltip.set(preferences.shouldShowPreviewEntryTableTooltip());
        this.bstPreviewLayoutPaths.setAll(preferences.getBstPreviewLayoutPaths());
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
        if (layoutCycle.isEmpty()
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

    public void setShowPreviewAsExtraTab(boolean showPreviewAsExtraTab) {
        this.showPreviewAsExtraTab.set(showPreviewAsExtraTab);
    }

    public BooleanProperty showPreviewAsExtraTabProperty() {
        return showPreviewAsExtraTab;
    }

    public boolean shouldShowPreviewEntryTableTooltip() {
        return showPreviewEntryTableTooltip.getValue();
    }

    public void setShowPreviewEntryTableTooltip(boolean showPreviewEntryTableTooltip) {
        this.showPreviewEntryTableTooltip.set(showPreviewEntryTableTooltip);
    }

    public BooleanProperty showPreviewEntryTableTooltip() {
        return showPreviewEntryTableTooltip;
    }

    public ObservableList<Path> getBstPreviewLayoutPaths() {
        return bstPreviewLayoutPaths;
    }

    public void setBstPreviewLayoutPaths(List<Path> bstPreviewLayoutPaths) {
        this.bstPreviewLayoutPaths.setAll(bstPreviewLayoutPaths);
    }
}

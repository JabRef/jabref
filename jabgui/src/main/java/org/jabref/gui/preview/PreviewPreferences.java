package org.jabref.gui.preview;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.model.entry.BibEntryTypesManager;

public class PreviewPreferences {
    private final ObservableList<PreviewLayout> layoutCycle;
    private final IntegerProperty layoutCyclePosition;
    private final ObservableList<TextBasedPreviewLayout> customPreviewLayouts;
    private final BooleanProperty showPreviewAsExtraTab;
    private final BooleanProperty showPreviewEntryTableTooltip;
    private final ObservableList<Path> bstPreviewLayoutPaths;

    private final BooleanProperty shouldDownloadCovers;

    public PreviewPreferences(List<PreviewLayout> layoutCycle,
                              int layoutCyclePosition,
                              List<TextBasedPreviewLayout> customPreviewLayouts,
                              boolean showPreviewAsExtraTab,
                              boolean showPreviewEntryTableTooltip,
                              List<Path> bstPreviewLayoutPaths,
                              boolean shouldDownloadCovers) {
        this.layoutCycle = FXCollections.observableArrayList(layoutCycle);
        this.layoutCyclePosition = new SimpleIntegerProperty(layoutCyclePosition);
        this.customPreviewLayouts = FXCollections.observableArrayList(customPreviewLayouts);
        this.showPreviewAsExtraTab = new SimpleBooleanProperty(showPreviewAsExtraTab);
        this.showPreviewEntryTableTooltip = new SimpleBooleanProperty(showPreviewEntryTableTooltip);
        this.bstPreviewLayoutPaths = FXCollections.observableList(bstPreviewLayoutPaths);
        this.shouldDownloadCovers = new SimpleBooleanProperty(shouldDownloadCovers);
    }

    private PreviewPreferences() {
        this(
                List.of(),  // Layout cycle - empty by default, see JabRefPreferences::getPreviewPreferencesFromBackingStore
                0,          // Layout cycle position
                List.of(),  // Custom preview layouts - empty by default, same reasoning as layoutCycle above
                false,      // Show preview as an extra tab
                false,      // Show the preview entry table tooltip
                List.of(),  // BST-Paths
                false       // Download cover images disabled per default - similar to Mr. DLib; see [org.jabref.logic.preferences.JabRefCliPreferences.ACCEPT_RECOMMENDATIONS].
        );
    }

    /// Provides default values WITHOUT default styles
    public static PreviewPreferences getDefault() {
        return new PreviewPreferences();
    }

    public static PreviewPreferences getDefaultWithStyles(LayoutFormatterPreferences layoutFormatterPreferences,
                                                          JournalAbbreviationRepository abbreviationRepository,
                                                          BibEntryTypesManager entryTypesManager) {
        // This will retrieve the "Customized preview style" name from within the delegation constructor
        PreviewPreferences defaults = getDefault();
        TextBasedPreviewLayout defaultCustomLayout = TextBasedPreviewLayout.of(
                TextBasedPreviewLayout.DEFAULT,
                layoutFormatterPreferences,
                abbreviationRepository);

        PreviewLayout defaultCslLayout = PreviewLayout.of(
                CSLStyleLoader.DEFAULT_STYLE,
                defaults.getCustomPreviewLayouts(),
                List.of(),
                layoutFormatterPreferences,
                abbreviationRepository,
                entryTypesManager);

        defaults.getLayoutCycle().addAll(defaultCustomLayout, defaultCslLayout);
        return defaults;
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
            int newPosition = Math.floorMod(position, layoutCycle.size());
            this.layoutCyclePosition.setValue(newPosition);
        }
    }

    public PreviewLayout getSelectedPreviewLayout() {
        if (layoutCycle.isEmpty()
                || layoutCyclePosition.getValue() < 0
                || layoutCyclePosition.getValue() >= layoutCycle.size()) {
            // Fallback dummy layout
            String fallbackText = customPreviewLayouts.isEmpty() ? "" : customPreviewLayouts.getFirst().getText();
            return new TextBasedPreviewLayout(
                    fallbackText,
                    LayoutFormatterPreferences.getDefault(),
                    new JournalAbbreviationRepository());
        } else {
            return layoutCycle.get(layoutCyclePosition.getValue());
        }
    }

    public void setCustomPreviewLayouts(List<TextBasedPreviewLayout> customPreviewLayouts) {
        this.customPreviewLayouts.setAll(customPreviewLayouts);
    }

    public ObservableList<TextBasedPreviewLayout> getCustomPreviewLayouts() {
        return customPreviewLayouts;
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

    public boolean shouldDownloadCovers() {
        return shouldDownloadCovers.get();
    }

    public BooleanProperty shouldDownloadCoversProperty() {
        return shouldDownloadCovers;
    }

    public void setShouldDownloadCovers(boolean value) {
        this.shouldDownloadCovers.set(value);
    }
}

package org.jabref.gui.preview;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.model.entry.BibEntryTypesManager;

public class PreviewPreferences {
    private final ObservableList<PreviewLayout> layoutCycle;
    private final IntegerProperty layoutCyclePosition;
    private final ObservableList<CustomizedPreviewStyle> customizedPreviewStyles;
    private final BooleanProperty showPreviewAsExtraTab;
    private final BooleanProperty showPreviewEntryTableTooltip;
    private final ObservableList<Path> bstPreviewLayoutPaths;

    private final BooleanProperty shouldDownloadCovers;

    public PreviewPreferences(List<PreviewLayout> layoutCycle,
                              int layoutCyclePosition,
                              List<CustomizedPreviewStyle> customizedPreviewStyles,
                              boolean showPreviewAsExtraTab,
                              boolean showPreviewEntryTableTooltip,
                              List<Path> bstPreviewLayoutPaths,
                              boolean shouldDownloadCovers) {
        this.layoutCycle = FXCollections.observableArrayList(layoutCycle);
        this.layoutCyclePosition = new SimpleIntegerProperty(layoutCyclePosition);
        this.customizedPreviewStyles = FXCollections.observableArrayList(customizedPreviewStyles);
        this.showPreviewAsExtraTab = new SimpleBooleanProperty(showPreviewAsExtraTab);
        this.showPreviewEntryTableTooltip = new SimpleBooleanProperty(showPreviewEntryTableTooltip);
        this.bstPreviewLayoutPaths = FXCollections.observableList(bstPreviewLayoutPaths);
        this.shouldDownloadCovers = new SimpleBooleanProperty(shouldDownloadCovers);
    }

    private PreviewPreferences() {
        this(
                List.of(),  // Layout cycle - empty by default, see JabRefPreferences::getPreviewPreferencesFromBackingStore
                0,          // Layout cycle position
                List.of(new CustomizedPreviewStyle(UUID.randomUUID().toString(), TextBasedPreviewLayout.NAME, TextBasedPreviewLayout.DEFAULT)),   // default custom style
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
        PreviewPreferences defaults = getDefault();
        String defaultCustomizedStyleId = defaults.getCustomizedPreviewStyles().getFirst().id();
        defaults.getLayoutCycle().addAll(Stream.of(defaultCustomizedStyleId, CSLStyleLoader.DEFAULT_STYLE).map(layout ->
                                                       PreviewLayout.of(
                                                               layout,
                                                               defaults.getCustomizedPreviewStyles(),
                                                               List.of(),
                                                               layoutFormatterPreferences,
                                                               abbreviationRepository,
                                                               entryTypesManager))
                                               .flatMap(Optional::stream).toList());
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
            String fallbackText = customizedPreviewStyles.isEmpty()
                                  ? TextBasedPreviewLayout.DEFAULT
                                  : customizedPreviewStyles.getFirst().text();
            return TextBasedPreviewLayout.of(
                    fallbackText,
                    LayoutFormatterPreferences.getDefault(),
                    new JournalAbbreviationRepository());
        } else {
            return layoutCycle.get(layoutCyclePosition.getValue());
        }
    }

    public ObservableList<CustomizedPreviewStyle> getCustomizedPreviewStyles() {
        return customizedPreviewStyles;
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

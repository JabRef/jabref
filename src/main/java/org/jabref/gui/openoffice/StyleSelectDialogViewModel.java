package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

public class StyleSelectDialogViewModel {

    private final DialogService dialogService;
    private final JStyleLoader jStyleLoader;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final ListProperty<StyleSelectItemViewModel> jStyles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<StyleSelectItemViewModel> selectedItem = new SimpleObjectProperty<>();
    private final ObservableList<CitationStylePreviewLayout> availableLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<CitationStylePreviewLayout> selectedLayoutProperty = new SimpleObjectProperty<>();
    private final FilteredList<CitationStylePreviewLayout> filteredAvailableLayouts = new FilteredList<>(availableLayouts);
    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();
    private final CSLStyleLoader cslStyleLoader;

    public StyleSelectDialogViewModel(DialogService dialogService,
                                      JStyleLoader jStyleLoader,
                                      GuiPreferences preferences,
                                      TaskExecutor taskExecutor,
                                      BibEntryTypesManager bibEntryTypesManager) {
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.openOfficePreferences = preferences.getOpenOfficePreferences();
        this.jStyleLoader = jStyleLoader;
        this.cslStyleLoader = new CSLStyleLoader(openOfficePreferences);

        jStyles.addAll(loadJStyles());

        OOStyle currentStyle = openOfficePreferences.getCurrentStyle();

        if (currentStyle instanceof JStyle jStyle) {
            selectedItem.setValue(getStyleOrDefault(jStyle.getPath()));
        }

        BackgroundTask.wrap(cslStyleLoader::getStyles)
                      .onSuccess(styles -> {
                          List<CitationStylePreviewLayout> layouts = styles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                           .toList();
                          availableLayouts.setAll(layouts);

                          if (currentStyle instanceof CitationStyle citationStyle) {
                              // Find the matching style - first try exact path match for external styles
                              Optional<CitationStylePreviewLayout> matchingLayout = availableLayouts.stream()
                                                                                                    .filter(layout -> layout.getFilePath().equals(citationStyle.getFilePath()))
                                                                                                    .findFirst();

                              // If not found, match by name (for internal style)
                              if (matchingLayout.isEmpty()) {
                                  matchingLayout = availableLayouts.stream()
                                                                   .filter(layout -> layout.getDisplayName().equals(citationStyle.getTitle()))
                                                                   .findFirst();
                              }

                              selectedLayoutProperty.set(matchingLayout.orElse(availableLayouts.getFirst()));
                          }
                      })
                      .onFailure(ex -> dialogService.showErrorDialogAndWait("Error discovering citation styles", ex))
                      .executeWith(taskExecutor);
    }

    public StyleSelectItemViewModel fromOOBibStyle(JStyle style) {
        return new StyleSelectItemViewModel(style.getName(), String.join(", ", style.getJournals()), style.isInternalStyle() ? Localization.lang("Internal style") : style.getPath(), style);
    }

    public JStyle toJStyle(StyleSelectItemViewModel item) {
        return item.getJStyle();
    }

    public void addJStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withDefaultExtension(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();
        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            if (jStyleLoader.addStyleIfValid(stylePath)) {
                openOfficePreferences.setCurrentJStyle(stylePath);
                jStyles.setAll(loadJStyles());
                selectedItem.setValue(getStyleOrDefault(stylePath));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid style selected"), Localization.lang("You must select a valid style file. Your style is probably missing a line for the type \"default\"."));
            }
        });
    }

    public List<StyleSelectItemViewModel> loadJStyles() {
        return jStyleLoader.getStyles().stream().map(this::fromOOBibStyle).toList();
    }

    public ListProperty<StyleSelectItemViewModel> jStylesProperty() {
        return jStyles;
    }

    public void deleteJStyle() {
        JStyle jStyle = selectedItem.getValue().getJStyle();
        if (jStyleLoader.removeStyle(jStyle)) {
            jStyles.remove(selectedItem.get());
        }
    }

    public ObjectProperty<StyleSelectItemViewModel> selectedItemProperty() {
        return selectedItem;
    }

    public void editJStyle() {
        JStyle jStyle = selectedItem.getValue().getJStyle();
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt("jstyle", externalApplicationsPreferences);
        try {
            NativeDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), externalApplicationsPreferences, filePreferences, jStyle.getPath(), type);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void viewJStyle(StyleSelectItemViewModel item) {
        DialogPane pane = new DialogPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        TextArea styleView = new TextArea(item.getJStyle().getLocalCopy());
        scrollPane.setContent(styleView);
        pane.setContent(scrollPane);
        dialogService.showCustomDialogAndWait(item.getJStyle().getName(), pane, ButtonType.OK);
    }

    public void storePrefs() {
        List<String> externalStyles = jStyles.stream()
                                             .map(this::toJStyle)
                                             .filter(style -> !style.isInternalStyle())
                                             .map(JStyle::getPath)
                                             .toList();

        openOfficePreferences.setExternalStyles(externalStyles);

        OOStyle selectedStyle = getSelectedStyle();
        openOfficePreferences.setCurrentStyle(selectedStyle);

        // Handle backward-compatibility with pure JStyle preferences (formerly OOBibStyle):
        if (selectedStyle instanceof JStyle jStyle) {
            openOfficePreferences.setCurrentJStyle(jStyle.getPath());
        }
    }

    private StyleSelectItemViewModel getStyleOrDefault(String stylePath) {
        return jStyles.stream().filter(style -> style.getStylePath().equals(stylePath)).findFirst().orElse(jStyles.getFirst());
    }

    public ObservableList<CitationStylePreviewLayout> getAvailableLayouts() {
        return filteredAvailableLayouts;
    }

    public ObjectProperty<CitationStylePreviewLayout> selectedLayoutProperty() {
        return selectedLayoutProperty;
    }

    public void setAvailableLayoutsFilter(String searchTerm) {
        filteredAvailableLayouts.setPredicate(layout ->
                searchTerm.isEmpty() || layout.getDisplayName().toLowerCase().contains(searchTerm.toLowerCase()));
    }

    public Tab getSelectedTab() {
        return selectedTab.get();
    }

    public void setSelectedTab(Tab tab) {
        if (tab != null) {
            selectedTab.set(tab);
        }
    }

    public void handleCslStyleSelection() {
        CitationStylePreviewLayout selectedLayout = selectedLayoutProperty.get();
        openOfficePreferences.setCurrentStyle(selectedLayout.getCitationStyle());
    }

    public OOStyle getSelectedStyle() {
        Tab currentTab = getSelectedTab();
        if (currentTab != null) {
            String tabText = currentTab.getText();
            if ("JStyles".equals(tabText)) {
                if (selectedItem.get() != null) {
                    return selectedItem.get().getJStyle();
                }
            } else if ("CSL Styles".equals(tabText)) {
                if (selectedLayoutProperty.get() != null) {
                    return selectedLayoutProperty.get().getCitationStyle();
                }
            }
        }
        return openOfficePreferences.getCurrentStyle();
    }

    public OOStyle getSetStyle() {
        return openOfficePreferences.getCurrentStyle();
    }

    /**
     * Handles importing a custom CSL style file
     */
    public void addCslStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("CSL Style file"), StandardFileType.CITATION_STYLE)
                .withDefaultExtension(Localization.lang("CSL Style file"), StandardFileType.CITATION_STYLE)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);

        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            Optional<CitationStyle> newStyleOptional = cslStyleLoader.addStyleIfValid(stylePath);

            if (newStyleOptional.isPresent()) {
                CitationStyle newStyle = newStyleOptional.get();

                List<CitationStyle> allStyles = cslStyleLoader.getStyles();
                List<CitationStylePreviewLayout> updatedLayouts = allStyles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, Injector.instantiateModelOrService(BibEntryTypesManager.class)))
                                                                           .toList();

                availableLayouts.setAll(updatedLayouts);

                Optional<CitationStylePreviewLayout> newLayoutOptional = updatedLayouts.stream()
                                                                                       .filter(layout -> layout.getFilePath().equals(stylePath))
                                                                                       .findFirst();

                if (newLayoutOptional.isPresent()) {
                    CitationStylePreviewLayout newLayout = newLayoutOptional.get();
                    selectedLayoutProperty.set(newLayout);

                    openOfficePreferences.setCurrentStyle(newStyle);

                    dialogService.showInformationDialogAndWait(
                            Localization.lang("Style added"),
                            Localization.lang("The CSL style has been added successfully.")
                    );
                } else {
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Style not found"),
                            Localization.lang("The CSL style was added but could not be found in the list.")
                    );
                }
            } else {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Invalid style selected"),
                        Localization.lang("You must select a valid CSL style file.")
                );
            }
        });
    }

    public void deleteCslStyle(CitationStyle style) {
        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Delete style"),
                Localization.lang("Are you sure you want to delete the style '%0'?", style.getTitle()),
                Localization.lang("Delete"),
                Localization.lang("Cancel"));

        if (deleteConfirmed) {
            if (cslStyleLoader.removeStyle(style)) {
                Optional<CitationStylePreviewLayout> layoutToRemove = availableLayouts.stream()
                                                                                      .filter(layout -> layout.getFilePath().equals(style.getFilePath()))
                                                                                      .findFirst();

                layoutToRemove.ifPresent(availableLayouts::remove);

                // If the deleted style was the current selection, select another style
                if (selectedLayoutProperty.get() != null &&
                        selectedLayoutProperty.get().getFilePath().equals(style.getFilePath())) {
                    if (!availableLayouts.isEmpty()) {
                        selectedLayoutProperty.set(availableLayouts.getFirst());
                    } else {
                        selectedLayoutProperty.set(null);
                    }
                }

                // Update the currently set style to default (ieee) if it was the deleted one
                if (openOfficePreferences.getCurrentStyle() instanceof CitationStyle currentStyle &&
                        currentStyle.getFilePath().equals(style.getFilePath())) {
                    openOfficePreferences.setCurrentStyle(CSLStyleLoader.getDefaultStyle());
                }
            } else {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Cannot delete style"),
                        Localization.lang("Could not delete style. It might be an internal style that cannot be removed."));
            }
        }
    }
}

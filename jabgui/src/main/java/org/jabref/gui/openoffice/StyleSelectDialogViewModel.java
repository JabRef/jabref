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
import javafx.scene.control.Tab;

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
import org.jabref.logic.journals.JournalAbbreviationRepository;
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

    private final CSLStyleLoader cslStyleLoader;
    private final JStyleLoader jStyleLoader;

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;

    private final BibEntryTypesManager bibEntryTypesManager;

    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();

    private final ListProperty<JStyleSelectViewModel> jStyles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<JStyleSelectViewModel> selectedJStyle = new SimpleObjectProperty<>();

    private final ObservableList<CitationStylePreviewLayout> availableCslLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<CitationStylePreviewLayout> selectedCslLayoutProperty = new SimpleObjectProperty<>();
    private final FilteredList<CitationStylePreviewLayout> filteredAvailableCslLayouts = new FilteredList<>(availableCslLayouts);

    public StyleSelectDialogViewModel(DialogService dialogService,
                                      CSLStyleLoader cslStyleLoader,
                                      JStyleLoader jStyleLoader,
                                      GuiPreferences preferences,
                                      TaskExecutor taskExecutor,
                                      BibEntryTypesManager bibEntryTypesManager) {
        this.dialogService = dialogService;

        this.cslStyleLoader = cslStyleLoader;
        this.jStyleLoader = jStyleLoader;

        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.openOfficePreferences = preferences.getOpenOfficePreferences(Injector.instantiateModelOrService(JournalAbbreviationRepository.class));

        this.bibEntryTypesManager = bibEntryTypesManager;

        jStyles.addAll(loadJStyles());

        OOStyle currentStyle = openOfficePreferences.getCurrentStyle();

        if (currentStyle instanceof JStyle jStyle) {
            selectedJStyle.setValue(getJStyleOrDefault(jStyle.getPath()));
        }

        BackgroundTask.wrap(CSLStyleLoader::getStyles)
                      .onSuccess(styles -> {
                          List<CitationStylePreviewLayout> layouts = styles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                           .toList();
                          availableCslLayouts.setAll(layouts);

                          if (currentStyle instanceof CitationStyle citationStyle) {
                              // Find the matching style - first try exact path match for external styles
                              Optional<CitationStylePreviewLayout> matchingLayout = availableCslLayouts.stream()
                                                                                                       .filter(layout -> layout.getFilePath().equals(citationStyle.getFilePath()))
                                                                                                       .findFirst();

                              // If not found, match by name (for internal style)
                              if (matchingLayout.isEmpty()) {
                                  matchingLayout = availableCslLayouts.stream()
                                                                      .filter(layout -> layout.getDisplayName().equals(citationStyle.getTitle()))
                                                                      .findFirst();
                              }

                              selectedCslLayoutProperty.set(matchingLayout.orElse(availableCslLayouts.getFirst()));
                          }
                      })
                      .onFailure(ex -> dialogService.showErrorDialogAndWait("Error discovering citation styles", ex))
                      .executeWith(taskExecutor);
    }

    // region - general methods

    public Tab getSelectedTab() {
        return selectedTab.get();
    }

    public void setSelectedTab(Tab tab) {
        if (tab != null) {
            selectedTab.set(tab);
        }
    }

    public OOStyle getSetStyle() {
        return openOfficePreferences.getCurrentStyle();
    }

    public OOStyle getSelectedStyle() {
        Tab currentTab = getSelectedTab();
        if (currentTab != null) {
            String tabText = currentTab.getText();
            if ("JStyles".equals(tabText)) {
                if (selectedJStyle.get() != null) {
                    return selectedJStyle.get().getJStyle();
                }
            } else if ("CSL Styles".equals(tabText)) {
                if (selectedCslLayoutProperty.get() != null) {
                    return selectedCslLayoutProperty.get().citationStyle();
                }
            }
        }
        return openOfficePreferences.getCurrentStyle();
    }

    public void storeStylePreferences() {
        // save external jstyles
        List<String> externalJStyles = jStyles.stream()
                                              .map(this::toJStyle)
                                              .filter(style -> !style.isInternalStyle())
                                              .map(JStyle::getPath)
                                              .toList();

        openOfficePreferences.setExternalJStyles(externalJStyles);

        // save the current style selection
        OOStyle selectedStyle = getSelectedStyle();
        openOfficePreferences.setCurrentStyle(selectedStyle);

        // Handle backward-compatibility with pure JStyle (formerly OOBibStyle) preferences ("ooBibliographyStyleFile")
        if (selectedStyle instanceof JStyle jStyle) {
            openOfficePreferences.setCurrentJStyle(jStyle.getPath());
        }
    }

    // endregion

    // region - csl-specific methods

    public ObservableList<CitationStylePreviewLayout> getAvailableCslLayouts() {
        return filteredAvailableCslLayouts;
    }

    public ObjectProperty<CitationStylePreviewLayout> selectedCslLayoutProperty() {
        return selectedCslLayoutProperty;
    }

    public void setAvailableCslLayoutsFilter(String searchTerm) {
        filteredAvailableCslLayouts.setPredicate(layout ->
                searchTerm.isEmpty() || layout.getDisplayName().toLowerCase().contains(searchTerm.toLowerCase()));
    }

    /**
     * Handles importing a custom CSL style file
     */
    public void addCslStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("%0 file", StandardFileType.CITATION_STYLE.getName()), StandardFileType.CITATION_STYLE)
                .withDefaultExtension(Localization.lang("%0 file", StandardFileType.CITATION_STYLE.getName()), StandardFileType.CITATION_STYLE)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);

        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            Optional<CitationStyle> newStyleOptional = cslStyleLoader.addStyleIfValid(stylePath);

            if (newStyleOptional.isPresent()) {
                CitationStyle newStyle = newStyleOptional.get();

                List<CitationStyle> allStyles = CSLStyleLoader.getStyles();
                List<CitationStylePreviewLayout> updatedLayouts = allStyles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                           .toList();

                availableCslLayouts.setAll(updatedLayouts);

                Optional<CitationStylePreviewLayout> newLayoutOptional = updatedLayouts.stream()
                                                                                       .filter(layout -> layout.getFilePath().equals(stylePath))
                                                                                       .findFirst();

                if (newLayoutOptional.isPresent()) {
                    CitationStylePreviewLayout newLayout = newLayoutOptional.get();
                    selectedCslLayoutProperty.set(newLayout);

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
                Optional<CitationStylePreviewLayout> layoutToRemove = availableCslLayouts.stream()
                                                                                         .filter(layout -> layout.getFilePath().equals(style.getFilePath()))
                                                                                         .findFirst();

                layoutToRemove.ifPresent(availableCslLayouts::remove);

                // If the deleted style was the current selection, select another style
                if (selectedCslLayoutProperty.get() != null &&
                        selectedCslLayoutProperty.get().getFilePath().equals(style.getFilePath())) {
                    if (!availableCslLayouts.isEmpty()) {
                        selectedCslLayoutProperty.set(availableCslLayouts.getFirst());
                    } else {
                        selectedCslLayoutProperty.set(null);
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

    // endregion

    // region - jstyle-specific methods

    public JStyleSelectViewModel fromJStyle(JStyle style) {
        return new JStyleSelectViewModel(style.getName(), String.join(", ", style.getJournals()), style.isInternalStyle() ? Localization.lang("Internal style") : style.getPath(), style);
    }

    public JStyle toJStyle(JStyleSelectViewModel item) {
        return item.getJStyle();
    }

    public ListProperty<JStyleSelectViewModel> jStylesProperty() {
        return jStyles;
    }

    public ObjectProperty<JStyleSelectViewModel> selectedJStyleProperty() {
        return selectedJStyle;
    }

    public List<JStyleSelectViewModel> loadJStyles() {
        return jStyleLoader.getStyles().stream().map(this::fromJStyle).toList();
    }

    private JStyleSelectViewModel getJStyleOrDefault(String stylePath) {
        return jStyles.stream().filter(style -> style.getStylePath().equals(stylePath)).findFirst().orElse(jStyles.getFirst());
    }

    public void addJStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("%0 file", StandardFileType.JSTYLE.getName()), StandardFileType.JSTYLE)
                .withDefaultExtension(Localization.lang("%0 file", StandardFileType.JSTYLE.getName()), StandardFileType.JSTYLE)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();
        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        path.map(Path::toAbsolutePath).ifPresent(stylePath -> {
            if (jStyleLoader.addStyleIfValid(stylePath)) {
                openOfficePreferences.setCurrentJStyle(stylePath.toString());
                jStyles.setAll(loadJStyles());
                selectedJStyle.setValue(getJStyleOrDefault(stylePath.toString()));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid style selected"), Localization.lang("You must select a valid style file. Your style is probably missing a line for the type \"default\"."));
            }
        });
    }

    public void editJStyle() {
        JStyle jStyle = selectedJStyle.getValue().getJStyle();
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt("jstyle", externalApplicationsPreferences);
        try {
            NativeDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), externalApplicationsPreferences, filePreferences, jStyle.getPath(), type);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void deleteJStyle() {
        JStyle jStyle = selectedJStyle.getValue().getJStyle();
        if (jStyleLoader.removeStyle(jStyle)) {
            jStyles.remove(selectedJStyle.get());
        }
    }

    // endregion
}

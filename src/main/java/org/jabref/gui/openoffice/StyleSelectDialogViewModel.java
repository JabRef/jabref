package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class StyleSelectDialogViewModel {

    private final DialogService dialogService;
    private final StyleLoader styleLoader;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final ListProperty<StyleSelectItemViewModel> styles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<StyleSelectItemViewModel> selectedItem = new SimpleObjectProperty<>();
    private final ObservableList<CitationStylePreviewLayout> availableLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<CitationStylePreviewLayout> selectedLayoutProperty = new SimpleObjectProperty<>();
    private final FilteredList<CitationStylePreviewLayout> filteredAvailableLayouts = new FilteredList<>(availableLayouts);
    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();

    public StyleSelectDialogViewModel(DialogService dialogService,
                                      StyleLoader styleLoader,
                                      GuiPreferences preferences,
                                      TaskExecutor taskExecutor,
                                      BibEntryTypesManager bibEntryTypesManager) {
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.openOfficePreferences = preferences.getOpenOfficePreferences();
        this.styleLoader = styleLoader;

        styles.addAll(loadStyles());

        OOStyle currentStyle = openOfficePreferences.getCurrentStyle();

        if (currentStyle instanceof JStyle jStyle) {
            selectedItem.setValue(getStyleOrDefault(jStyle.getPath()));
        }

        BackgroundTask.wrap(CitationStyle::discoverCitationStyles)
                      .onSuccess(styles -> {
                          List<CitationStylePreviewLayout> layouts = styles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                           .collect(Collectors.toList());
                          availableLayouts.setAll(layouts);

                          if (currentStyle instanceof CitationStyle citationStyle) {
                              selectedLayoutProperty.set(availableLayouts.stream().filter(csl -> csl.getFilePath().equals(citationStyle.getFilePath())).findFirst().orElse(availableLayouts.getFirst()));
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

    public void addStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withDefaultExtension(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();
        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            if (styleLoader.addStyleIfValid(stylePath)) {
                openOfficePreferences.setCurrentJStyle(stylePath);
                styles.setAll(loadStyles());
                selectedItem.setValue(getStyleOrDefault(stylePath));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid style selected"), Localization.lang("You must select a valid style file. Your style is probably missing a line for the type \"default\"."));
            }
        });
    }

    public List<StyleSelectItemViewModel> loadStyles() {
        return styleLoader.getStyles().stream().map(this::fromOOBibStyle).collect(Collectors.toList());
    }

    public ListProperty<StyleSelectItemViewModel> stylesProperty() {
        return styles;
    }

    public void deleteStyle() {
        JStyle jStyle = selectedItem.getValue().getJStyle();
        if (styleLoader.removeStyle(jStyle)) {
            styles.remove(selectedItem.get());
        }
    }

    public ObjectProperty<StyleSelectItemViewModel> selectedItemProperty() {
        return selectedItem;
    }

    public void editStyle() {
        JStyle jStyle = selectedItem.getValue().getJStyle();
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt("jstyle", externalApplicationsPreferences);
        try {
            NativeDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), externalApplicationsPreferences, filePreferences, jStyle.getPath(), type);
        } catch (
                IOException e) {
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void viewStyle(StyleSelectItemViewModel item) {
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
        List<String> externalStyles = styles.stream()
                                            .map(this::toJStyle)
                                            .filter(style -> !style.isInternalStyle())
                                            .map(JStyle::getPath)
                                            .collect(Collectors.toList());

        openOfficePreferences.setExternalStyles(externalStyles);

        OOStyle selectedStyle = getSelectedStyle();
        openOfficePreferences.setCurrentStyle(selectedStyle);

        // Handle backward-compatibility with pure JStyle preferences (formerly OOBibStyle):
        if (selectedStyle instanceof JStyle jStyle) {
            openOfficePreferences.setCurrentJStyle(jStyle.getPath());
        }
    }

    private StyleSelectItemViewModel getStyleOrDefault(String stylePath) {
        return styles.stream().filter(style -> style.getStylePath().equals(stylePath)).findFirst().orElse(styles.getFirst());
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
}

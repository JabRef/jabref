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
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class StyleSelectDialogViewModel {

    private final DialogService dialogService;
    private final StyleLoader styleLoader;
    private final OpenOfficePreferences openOfficePreferences;
    private final PreferencesService preferencesService;
    private final ListProperty<StyleSelectItemViewModel> styles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<StyleSelectItemViewModel> selectedItem = new SimpleObjectProperty<>();

    public StyleSelectDialogViewModel(DialogService dialogService, StyleLoader styleLoader, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.openOfficePreferences = preferencesService.getOpenOfficePreferences();
        this.styleLoader = styleLoader;

        styles.addAll(loadStyles());

        String currentStyle = openOfficePreferences.getCurrentStyle();
        selectedItem.setValue(getStyleOrDefault(currentStyle));
    }

    public StyleSelectItemViewModel fromOOBibStyle(OOBibStyle style) {
        return new StyleSelectItemViewModel(style.getName(), String.join(", ", style.getJournals()), style.isInternalStyle() ? Localization.lang("Internal style") : style.getPath(), style);
    }

    public OOBibStyle toOOBibStyle(StyleSelectItemViewModel item) {
        return item.getStyle();
    }

    public void addStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withDefaultExtension(Localization.lang("Style file"), StandardFileType.JSTYLE)
                .withInitialDirectory(preferencesService.getFilePreferences().getWorkingDirectory())
                .build();
        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            if (styleLoader.addStyleIfValid(stylePath)) {
                openOfficePreferences.setCurrentStyle(stylePath);
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
        OOBibStyle style = selectedItem.getValue().getStyle();
        if (styleLoader.removeStyle(style)) {
            styles.remove(selectedItem.get());
        }
    }

    public void editStyle() {
        OOBibStyle style = selectedItem.getValue().getStyle();
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt("jstyle", preferencesService.getFilePreferences());
        try {
            JabRefDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), preferencesService, style.getPath(), type);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void viewStyle(StyleSelectItemViewModel item) {
        DialogPane pane = new DialogPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        TextArea styleView = new TextArea(item.getStyle().getLocalCopy());
        scrollPane.setContent(styleView);
        pane.setContent(scrollPane);
        dialogService.showCustomDialogAndWait(item.getStyle().getName(), pane, ButtonType.OK);
    }

    public ObjectProperty<StyleSelectItemViewModel> selectedItemProperty() {
        return selectedItem;
    }

    public void storePrefs() {
        List<String> externalStyles = styles.stream()
                                            .map(this::toOOBibStyle)
                                            .filter(style -> !style.isInternalStyle())
                                            .map(OOBibStyle::getPath)
                                            .collect(Collectors.toList());
        openOfficePreferences.setExternalStyles(externalStyles);
        openOfficePreferences.setCurrentStyle(selectedItem.getValue().getStylePath());
    }

    private StyleSelectItemViewModel getStyleOrDefault(String stylePath) {
        return styles.stream().filter(style -> style.getStylePath().equals(stylePath)).findFirst().orElse(styles.get(0));
    }
}

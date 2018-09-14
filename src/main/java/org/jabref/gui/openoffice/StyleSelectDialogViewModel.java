package org.jabref.gui.openoffice;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

public class StyleSelectDialogViewModel {

    private final DialogService dialogService;
    private final StyleLoader loader;
    private final OpenOfficePreferences preferences;
    private final PreferencesService preferencesService;
    private final ListProperty<StyleSelectItemViewModel> styles = new SimpleListProperty<>(FXCollections.observableArrayList());

    public StyleSelectDialogViewModel(DialogService dialogService, StyleLoader loader, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferences = preferencesService.getOpenOfficePreferences();
        this.loader = loader;
        this.preferencesService = preferencesService;

        styles.addAll(loader.getStyles().stream().map(this::fromOOBibStyle).collect(Collectors.toList()));

    }

    public StyleSelectItemViewModel fromOOBibStyle(OOBibStyle style) {
        return new StyleSelectItemViewModel(style.getName(), String.join(", ", style.getJournals()), style.isFromResource() ? Localization.lang("Internal style") : style.getFile().getName(), style);
    }

    public OOBibStyle toOOBibStyle(StyleSelectItemViewModel item) {
        return item.getStyle();
    }

    public void addStyleFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .addExtensionFilter(Localization.lang("Style file"), StandardFileType.JSTYLE)
                                                                                               .withDefaultExtension(Localization.lang("Style file"), StandardFileType.JSTYLE)
                                                                                               .withInitialDirectory(preferencesService.getWorkingDir())
                                                                                               .build();
        Optional<Path> path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        path.map(Path::toAbsolutePath).map(Path::toString).ifPresent(stylePath -> {
            if (loader.addStyleIfValid(stylePath)) {
                preferences.setCurrentStyle(stylePath);
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid style selected"));
            }
        });

    }

    public ListProperty<StyleSelectItemViewModel> getStyles() {
        return styles;
    }

}

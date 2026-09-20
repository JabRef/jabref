package org.jabref.testutils.interactive.styletester;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.theme.StyleSheet;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.StandardFileType;

import com.airhacks.afterburner.views.ViewLoader;

public class StyleTesterView {

    private final WorkspacePreferences preferences;
    private final DialogService dialogService;

    @FXML private ComboBox<ThemePreset> themeCbx;
    @FXML private ComboBox<ThemeColorScheme> colorSchemeCbx;
    @FXML private Button loadCustomThemeBtn;
    @FXML private Button clearCustomThemeBtn;

    @FXML private Button normalButtonHover;
    @FXML private Button normalButtonPressed;
    @FXML private Button normalButtonFocused;
    @FXML private Button textButtonHover;
    @FXML private Button textButtonPressed;
    @FXML private Button textButtonFocused;
    @FXML private Button containedButtonHover;
    @FXML private Button containedButtonPressed;
    @FXML private Button containedButtonFocused;

    private final Parent content;

    StyleTesterView(WorkspacePreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        content = ViewLoader.view(this)
                            .load()
                            .getView();

        themeCbx.setConverter(toStringConverter(ThemePreset::getLocalizedName));
        themeCbx.setItems(FXCollections.observableArrayList(ThemePreset.values()));
        themeCbx.valueProperty().addListener((_, _,
                                              themePreset) -> setThemePreset(themePreset));
        themeCbx.setValue(ThemePreset.JABREF);

        colorSchemeCbx.setConverter(toStringConverter(ThemeColorScheme::getLocalizedName));
        colorSchemeCbx.setItems(FXCollections.observableArrayList(ThemeColorScheme.values()));
        colorSchemeCbx.valueProperty().addListener((_, _,
                                                    themeColorScheme) -> setThemeColorScheme(themeColorScheme));
        colorSchemeCbx.setValue(ThemeColorScheme.FOLLOW_SYSTEM);

        loadCustomThemeBtn.setOnAction(_ -> loadCustomTheme());
        clearCustomThemeBtn.setOnAction(_ -> clearCustomTheme());

        clearCustomTheme();
        setStates();
    }

    private void clearCustomTheme() {
        clearCustomThemeBtn.setDisable(true);

        preferences.setCustomTheme(Optional.empty());
    }

    private void loadCustomTheme() {
        themeCbx.setValue(ThemePreset.JABREF);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.getCustomTheme().map(StyleSheet::getName).orElse(null))
                .build();

        Optional<Path> pathOptional = dialogService.showFileOpenDialog(fileDialogConfiguration);

        if (pathOptional.isPresent()) {
            preferences.setCustomTheme(StyleSheet.create(pathOptional.get().toAbsolutePath().toString()));
            clearCustomThemeBtn.setDisable(false);
        }
    }

    private void setThemeColorScheme(ThemeColorScheme themeColorScheme) {
        preferences.setColorScheme(themeColorScheme);
    }

    private void setThemePreset(ThemePreset themePreset) {
        preferences.setTheme(themePreset);
    }

    private void setStates() {
        PseudoClass hover = PseudoClass.getPseudoClass("hover");
        normalButtonHover.pseudoClassStateChanged(hover, true);
        textButtonHover.pseudoClassStateChanged(hover, true);
        containedButtonHover.pseudoClassStateChanged(hover, true);

        PseudoClass pressed = PseudoClass.getPseudoClass("pressed");
        normalButtonPressed.pseudoClassStateChanged(pressed, true);
        textButtonPressed.pseudoClassStateChanged(pressed, true);
        containedButtonPressed.pseudoClassStateChanged(pressed, true);

        PseudoClass focused = PseudoClass.getPseudoClass("focused");
        normalButtonFocused.pseudoClassStateChanged(focused, true);
        textButtonFocused.pseudoClassStateChanged(focused, true);
        containedButtonFocused.pseudoClassStateChanged(focused, true);
    }

    private <T> StringConverter<T> toStringConverter(Function<T, String> toStringFunction) {
        return new StringConverter<>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return null;
                }

                return toStringFunction.apply(object);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    public Parent getContent() {
        return content;
    }
}

package org.jabref.gui.preferences;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

class AppearancePrefsTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;
    private final CheckBox fontTweaksLAF;
    private final TextField fontSize;
    private final CheckBox overrideFonts;
    private final VBox container = new VBox();
    private final DialogService dialogService;
    private final RadioButton lightTheme;
    private final RadioButton darkTheme;

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        this.prefs = prefs;

        overrideFonts = new CheckBox(Localization.lang("Override default font settings"));
        fontSize = new TextField();
        fontSize.setTextFormatter(ControlHelper.getIntegerTextFormatter());
        Label fontSizeLabel = new Label(Localization.lang("Font size:"));
        HBox fontSizeContainer = new HBox(fontSizeLabel, fontSize);
        VBox.setMargin(fontSizeContainer, new Insets(0, 0, 0, 35));
        fontSizeContainer.disableProperty().bind(overrideFonts.selectedProperty().not());
        fontTweaksLAF = new CheckBox(Localization.lang("Tweak font rendering for entry editor on Linux"));

        ToggleGroup themeGroup = new ToggleGroup();
        lightTheme = new RadioButton("Light theme");
        lightTheme.setToggleGroup(themeGroup);
        darkTheme = new RadioButton("Dark theme");
        darkTheme.setToggleGroup(themeGroup);

        String cssFileName = prefs.get(JabRefPreferences.FX_THEME);
        if (StringUtil.isBlank(cssFileName) || ThemeLoader.MAIN_CSS.equalsIgnoreCase(cssFileName)) {
            lightTheme.setSelected(true);
        } else if (ThemeLoader.DARK_CSS.equals(cssFileName)) {
            darkTheme.setSelected(true);
        }

        container.getChildren().addAll(overrideFonts, fontSizeContainer, fontTweaksLAF, lightTheme, darkTheme);
    }

    @Override
    public Node getBuilder() {
        return container;
    }

    @Override
    public void setValues() {
        fontTweaksLAF.setSelected(prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK));
        overrideFonts.setSelected(prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE));
        fontSize.setText(String.valueOf(prefs.getInt(JabRefPreferences.MAIN_FONT_SIZE)));
    }

    @Override
    public void storeSettings() {
        // Java FX font rendering tweak
        final boolean oldFxTweakValue = prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK);
        prefs.putBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK, fontTweaksLAF.isSelected());

        final boolean oldOverrideDefaultFontSize = prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE);
        final int oldFontSize = prefs.getInt(JabRefPreferences.MAIN_FONT_SIZE);
        prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE, overrideFonts.isSelected());
        int newFontSize = Integer.parseInt(fontSize.getText());
        prefs.putInt(JabRefPreferences.MAIN_FONT_SIZE, newFontSize);

        boolean isThemeChanged = false;

        if (lightTheme.isSelected() && !prefs.get(JabRefPreferences.FX_THEME).equals(ThemeLoader.MAIN_CSS)) {
            prefs.put(JabRefPreferences.FX_THEME, ThemeLoader.MAIN_CSS);
            isThemeChanged = true;
        } else if (darkTheme.isSelected() && !prefs.get(JabRefPreferences.FX_THEME).equals(ThemeLoader.DARK_CSS)) {
            prefs.put(JabRefPreferences.FX_THEME, ThemeLoader.DARK_CSS);
            isThemeChanged = true;
        }

        boolean isRestartRequired =
                (oldFxTweakValue != fontTweaksLAF.isSelected())
                        || (oldOverrideDefaultFontSize != overrideFonts.isSelected())
                        || (oldFontSize != newFontSize)
                        || isThemeChanged;
        if (isRestartRequired) {
            dialogService.showWarningDialogAndWait(Localization.lang("Settings"),
                    Localization.lang("Some appearance settings you changed require to restart JabRef to come into effect."));
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

package org.jabref.gui.preferences;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import java.io.*;
import java.net.URISyntaxException;

class AppearancePrefsTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;
    private final CheckBox fontTweaksLAF;
    private final CheckBox accessDarkThemes;
    private final TextField fontSize;
    private final CheckBox overrideFonts;
    private final VBox container = new VBox();
    private final DialogService dialogService;
    private static final String DEFAULT_PATH_MAIN_CSS= JabRefFrame.class.getResource("Base.css").getPath();
    private static final String DARK_THEME_PATH_CSS= JabRefFrame.class.getResource("Dark.css").getPath();
    private static final String DEFAULT_PREF_MAIN_CSS= PreferencesDialog.class.getResource("PreferencesDialog.css").getPath();
    private static final String DARK_PREF_MAIN_CSS= JabRefFrame.class.getResource("DarkPrefBox.css").getPath();

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */

    public AppearancePrefsTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        this.prefs = prefs;

        overrideFonts = new CheckBox(Localization.lang("Override default font settings"));
        accessDarkThemes=new CheckBox(Localization.lang("Access Dark Theme"));
        fontSize = new TextField();
        fontSize.setTextFormatter(ControlHelper.getIntegerTextFormatter());
        Label fontSizeLabel = new Label(Localization.lang("Font size:"));
        HBox fontSizeContainer = new HBox(fontSizeLabel, fontSize);
        VBox.setMargin(fontSizeContainer, new Insets(0, 0, 0, 35));
        fontSizeContainer.disableProperty().bind(overrideFonts.selectedProperty().not());
        fontSizeContainer.disableProperty().bind(accessDarkThemes.selectedProperty().not());
        fontTweaksLAF = new CheckBox(Localization.lang("Tweak font rendering for entry editor on Linux"));
        container.getChildren().addAll(overrideFonts,fontSizeContainer, fontTweaksLAF,accessDarkThemes);

    }

    public Node getBuilder() {
        return container;
    }

    //Basically replace each of the files with each other
    //Ultimately you have to read the contents of the file and replace it
    private void addLogic(CheckBox accessDarkTheme) throws URISyntaxException {
        
        if(accessDarkTheme.isSelected()==true){

            File basecaseFile= new File(DEFAULT_PATH_MAIN_CSS);
            File darkthemecssFile= new File(DARK_THEME_PATH_CSS);


            File originalprefcss= new File(DEFAULT_PREF_MAIN_CSS);
            File darkprefcss= new File(DARK_PREF_MAIN_CSS);

            try {

                copy(basecaseFile,darkthemecssFile);
                copy(originalprefcss,darkprefcss);

                Alert alertWindow= new Alert(AlertType.INFORMATION);
                alertWindow.setTitle("Dark Theme Information");
                alertWindow.setHeaderText("Information Box");
                alertWindow.setContentText("In order to and/remove the" +
                        "" +
                        " dark theme you will have to restart the application");
                alertWindow.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
            }
            accessDarkTheme.setSelected(true);
        }

    }

    private void copy(File file1,File file2) throws IOException {

        BufferedReader readerofFile= new BufferedReader(new FileReader(file1));
        BufferedWriter tempFile= new BufferedWriter(new FileWriter("temp.txt"));
        String line;

        while( (line=readerofFile.readLine())!=null){

            tempFile.write(line);
            tempFile.newLine();
            tempFile.flush();

        }

        BufferedReader readerofFile2= new BufferedReader(new FileReader(file2));
        BufferedWriter tempFile2= new BufferedWriter(new FileWriter(file1));
        String lineforsecondFile;

        while((lineforsecondFile=readerofFile2.readLine())!=null){

            tempFile2.write(lineforsecondFile);
            tempFile2.newLine();
            tempFile2.flush();

        }

        BufferedReader readerofFile3= new BufferedReader(new FileReader("temp.txt"));
        BufferedWriter tempFile3= new BufferedWriter(new FileWriter(file2));
        String lineforLastFile;


        while((lineforLastFile=readerofFile3.readLine())!=null){

            tempFile3.write(lineforLastFile);
            tempFile3.newLine();
            tempFile3.flush();

        }
    }

    @Override
    public void setValues() {
        fontTweaksLAF.setSelected(prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK));
        overrideFonts.setSelected(prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE));
        fontSize.setText(String.valueOf(prefs.getInt(JabRefPreferences.MAIN_FONT_SIZE)));
        accessDarkThemes.setSelected(false);
    }

    @Override
    public void storeSettings() {
        // Java FX font rendering tweak
        try {
            addLogic(accessDarkThemes);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        final boolean oldFxTweakValue = prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK);
        prefs.putBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK, fontTweaksLAF.isSelected());

        prefs.putBoolean(JabRefPreferences.ACCESS_DARK_THEMES, accessDarkThemes.isSelected());

        final boolean oldOverrideDefaultFontSize = prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE);
        final int oldFontSize = prefs.getInt(JabRefPreferences.MAIN_FONT_SIZE);
        prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONT_SIZE, overrideFonts.isSelected());
        int newFontSize = Integer.parseInt(fontSize.getText());
        prefs.putInt(JabRefPreferences.MAIN_FONT_SIZE, newFontSize);

        boolean isRestartRequired =
                oldFxTweakValue != fontTweaksLAF.isSelected()
                        || oldOverrideDefaultFontSize != overrideFonts.isSelected()
                        || oldFontSize != newFontSize;
        if (isRestartRequired) {
            dialogService.showWarningDialogAndWait(Localization.lang("Settings"),
                    Localization.lang("Some appearance settings you changed require to restart JabRef to come into effect. Restart the application to see results"));
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

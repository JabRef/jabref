package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppearancePrefsTab.class);

    private final JabRefPreferences prefs;

    private final JCheckBox overrideFonts;
    private final Font usedFont = GUIGlobals.currentFont;
    private int oldMenuFontSize;
    private int oldSmallIconSize;
    private int oldLargeIconSize;
    private boolean oldOverrideFontSize;
    private final JTextField fontSize;
    private final JTextField largeIconsTextField;
    private final JTextField smallIconsTextField;
    private final JCheckBox fxFontTweaksLAF;

    private final DialogService dialogService;

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        this.prefs = prefs;
        setLayout(new BorderLayout());

        // Font sizes:
        fontSize = new JTextField(5);

        // Icon sizes:
        largeIconsTextField = new JTextField(5);
        smallIconsTextField = new JTextField(5);

        overrideFonts = new JCheckBox(Localization.lang("Override default font settings"));

        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        fxFontTweaksLAF = new JCheckBox(Localization.lang("Tweak font rendering for entry editor on Linux"));
        // Only list L&F which are available

        // only the default L&F shows the OSX specific first drop-down menu

        builder.append(fxFontTweaksLAF);
        builder.nextLine();

        builder.leadingColumnOffset(2);

        // General appearance settings
        builder.appendSeparator(Localization.lang("General"));

        FormBuilder generalBuilder = FormBuilder.create();
        JPanel generalPanel = generalBuilder.columns("left:pref, left:pref, 3dlu, pref, 7dlu, right:pref, 3dlu, pref")
                .rows("pref, 3dlu, pref, 3dlu, pref")
                .columnGroup(2, 6)
                .columnGroup(4, 8)
                .add(overrideFonts)
                .xyw(1, 1, 5)
                .add(new JLabel("    "))
                .xy(1, 3)
                .add(new JLabel(Localization.lang("Menu and label font size") + ":"))
                .xy(2, 3)
                .add(fontSize)
                .xy(4, 3)
                .add(new JLabel(Localization.lang("Size of large icons") + ":"))
                .xy(2, 5)
                .add(largeIconsTextField)
                .xy(4, 5)
                .add(new JLabel(Localization.lang("Size of small icons") + ":"))
                .xy(6, 5)
                .add(smallIconsTextField)
                .xy(8, 5)
                .build();

        builder.append(generalPanel);
        builder.nextLine();

        JPanel upper = new JPanel();
        JPanel sort = new JPanel();
        JPanel namesp = new JPanel();
        JPanel iconCol = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        upper.setLayout(gbl);
        sort.setLayout(gbl);
        namesp.setLayout(gbl);
        iconCol.setLayout(gbl);

        overrideFonts.addActionListener(e -> fontSize.setEnabled(overrideFonts.isSelected()));
        overrideFonts.addActionListener(e -> largeIconsTextField.setEnabled(overrideFonts.isSelected()));
        overrideFonts.addActionListener(e -> smallIconsTextField.setEnabled(overrideFonts.isSelected()));

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        // L&F

        fxFontTweaksLAF.setSelected(prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK));

        oldOverrideFontSize = prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS);
        oldMenuFontSize = prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
        oldLargeIconSize = prefs.getInt(JabRefPreferences.ICON_SIZE_LARGE);
        oldSmallIconSize = prefs.getInt(JabRefPreferences.ICON_SIZE_SMALL);

        overrideFonts.setSelected(oldOverrideFontSize);
        fontSize.setText(String.valueOf(oldMenuFontSize));
        smallIconsTextField.setText(String.valueOf(oldSmallIconSize));
        largeIconsTextField.setText(String.valueOf(oldLargeIconSize));

        fontSize.setEnabled(overrideFonts.isSelected());
        smallIconsTextField.setEnabled(overrideFonts.isSelected());
        largeIconsTextField.setEnabled(overrideFonts.isSelected());
    }

    @Override
    public void storeSettings() {
        boolean isRestartRequired = false;

        // Java FX font rendering tweak
        final boolean oldFxTweakValue = prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK);
        prefs.putBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK, fxFontTweaksLAF.isSelected());
        isRestartRequired |= oldFxTweakValue != fxFontTweaksLAF.isSelected();

        prefs.put(JabRefPreferences.FONT_FAMILY, usedFont.getFamily());
        prefs.putInt(JabRefPreferences.FONT_STYLE, usedFont.getStyle());
        prefs.putInt(JabRefPreferences.FONT_SIZE, usedFont.getSize());
        prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS, overrideFonts.isSelected());
        GUIGlobals.currentFont = usedFont;
        try {
            int size = Integer.parseInt(fontSize.getText());
            int smallIconSize = Integer.parseInt(smallIconsTextField.getText());
            int largeIconSize = Integer.parseInt(largeIconsTextField.getText());
            if (overrideFonts.isSelected()) {
                if (size != oldMenuFontSize) {
                    prefs.putInt(JabRefPreferences.MENU_FONT_SIZE, size);
                    isRestartRequired = true;
                }
                if (smallIconSize != oldSmallIconSize) {
                    prefs.putInt(JabRefPreferences.ICON_SIZE_SMALL, smallIconSize);
                    isRestartRequired = true;
                }
                if (largeIconSize != oldLargeIconSize) {
                    prefs.putInt(JabRefPreferences.ICON_SIZE_LARGE, largeIconSize);
                    isRestartRequired = true;
                }
            } else if (overrideFonts.isSelected() != oldOverrideFontSize) {
                prefs.remove(JabRefPreferences.ICON_SIZE_SMALL);
                prefs.remove(JabRefPreferences.ICON_SIZE_LARGE);
                prefs.remove(JabRefPreferences.MENU_FONT_SIZE);
                isRestartRequired = true;
            }

            if (isRestartRequired) {
                dialogService.showWarningDialogAndWait(Localization.lang("Settings"),
                        Localization.lang("Some appearance settings you changed require to restart JabRef to come into effect."));
            }
        } catch (NumberFormatException ex) {
            // should not happen as values are checked beforehand
            LOGGER.error("Invalid data value, integer expected", ex);
        }
    }

    private boolean validateIntegerField(String fieldName, String fieldValue, String errorTitle) {
        try {
            // Test if the field value is a number:
            Integer.parseInt(fieldValue);
        } catch (NumberFormatException ex) {

            dialogService.showErrorDialogAndWait(errorTitle, Localization.lang("You must enter an integer value in the text field for") + " '" + fieldName + "'");
            return false;
        }
        return true;
    }

    @Override
    public boolean validateSettings() {
        // Test if font size is a number:
        if (!validateIntegerField(Localization.lang("Menu and label font size"), fontSize.getText(),
                Localization.lang("Invalid setting"))) {
            return false;
        }

        if (!validateIntegerField(Localization.lang("Size of large icons"), largeIconsTextField.getText(),
                Localization.lang("Invalid setting"))) {
            return false;
        }

        return validateIntegerField(Localization.lang("Size of small icons"), smallIconsTextField.getText(),
                Localization.lang("Invalid setting"));
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

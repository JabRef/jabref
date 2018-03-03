package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jabref.gui.GUIGlobals;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppearancePrefsTab.class);

    private final JabRefPreferences prefs;

    private final JCheckBox colorCodes;
    private final JCheckBox resolvedColorCodes;
    private final JCheckBox overrideFonts;
    private final JCheckBox showGrid;
    private final ColorSetupPanel colorPanel;
    private Font usedFont = GUIGlobals.currentFont;
    private int oldMenuFontSize;
    private int oldSmallIconSize;
    private int oldLargeIconSize;
    private boolean oldOverrideFontSize;
    private final JTextField fontSize;
    private final JTextField largeIconsTextField;
    private final JTextField smallIconsTextField;
    private final JTextField rowPadding;
    // look and feel
    private final JComboBox<String> classNamesLAF;
    private String currentLAF = "";
    private boolean useDefaultLAF;
    private final JCheckBox customLAF;
    private final JCheckBox fxFontTweaksLAF;

    static class LookAndFeel {

        public static Set<String> getAvailableLookAndFeels() {
            return Arrays.stream(UIManager.getInstalledLookAndFeels()).map(LookAndFeelInfo::getClassName).collect(Collectors.toSet());
        }
    }

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        setLayout(new BorderLayout());

        // Font sizes:
        fontSize = new JTextField(5);

        // Row padding size:
        rowPadding = new JTextField(5);

        // Icon sizes:
        largeIconsTextField = new JTextField(5);
        smallIconsTextField = new JTextField(5);

        colorCodes = new JCheckBox(
                Localization.lang("Color codes for required and optional fields"));

        resolvedColorCodes = new JCheckBox(Localization.lang("Color code for resolved fields"));

        overrideFonts = new JCheckBox(Localization.lang("Override default font settings"));

        showGrid = new JCheckBox(Localization.lang("Show gridlines"));

        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        customLAF = new JCheckBox(Localization.lang("Use other look and feel"));
        fxFontTweaksLAF = new JCheckBox(Localization.lang("Tweak font rendering for entry editor on Linux"));
        // Only list L&F which are available
        Set<String> lookAndFeels = LookAndFeel.getAvailableLookAndFeels();
        classNamesLAF = new JComboBox<>(lookAndFeels.toArray(new String[lookAndFeels.size()]));
        classNamesLAF.setEditable(true);
        customLAF.addChangeListener(e -> classNamesLAF.setEnabled(((JCheckBox) e.getSource()).isSelected()));

        colorPanel = new ColorSetupPanel(colorCodes, resolvedColorCodes, showGrid);

        // only the default L&F shows the OSX specific first drop-down menu
        if (!OS.OS_X) {
            JPanel pan = new JPanel();
            builder.appendSeparator(Localization.lang("Look and feel"));
            JLabel lab = new JLabel(
                    Localization.lang("Default look and feel") + ": " + UIManager.getSystemLookAndFeelClassName());
            builder.nextLine();
            builder.append(pan);
            builder.append(lab);
            builder.nextLine();
            builder.append(pan);
            builder.append(customLAF);
            builder.nextLine();
            builder.append(pan);
            JPanel pan2 = new JPanel();
            lab = new JLabel(Localization.lang("Class name") + ':');
            pan2.add(lab);
            pan2.add(classNamesLAF);
            builder.append(pan2);
            builder.nextLine();
            builder.append(pan);
            lab = new JLabel(Localization
                    .lang("Note that you must specify the fully qualified class name for the look and feel,"));
            builder.append(lab);
            builder.nextLine();
            builder.append(pan);
            lab = new JLabel(
                    Localization.lang("and the class must be available in your classpath next time you start JabRef."));
            builder.append(lab);
            builder.nextLine();
            builder.append(pan);
            builder.append(fxFontTweaksLAF);
            builder.nextLine();
        }

        builder.leadingColumnOffset(2);

        // General appearance settings
        builder.appendSeparator(Localization.lang("General"));

        FormBuilder generalBuilder = FormBuilder.create();
        JPanel generalPanel = generalBuilder.columns("left:pref, left:pref, 3dlu, pref, 7dlu, right:pref, 3dlu, pref")
                .rows("pref, 3dlu, pref, 3dlu, pref")
                .columnGroup(2, 6)
                .columnGroup(4, 8)
                .add(overrideFonts).xyw(1, 1, 5)
                .add(new JLabel("    ")).xy(1, 3)
                .add(new JLabel(Localization.lang("Menu and label font size") + ":")).xy(2, 3)
                .add(fontSize).xy(4, 3)
                .add(new JLabel(Localization.lang("Size of large icons") + ":")).xy(2, 5)
                .add(largeIconsTextField).xy(4, 5)
                .add(new JLabel(Localization.lang("Size of small icons") + ":")).xy(6, 5)
                .add(smallIconsTextField).xy(8, 5)
                .build();

        builder.append(generalPanel);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Table appearance"));
        JPanel p2 = new JPanel();
        p2.add(new JLabel(Localization.lang("Table row height padding") + ":"));
        p2.add(rowPadding);
        builder.append(p2);
        builder.nextLine();
        builder.append(colorCodes);
        builder.nextLine();
        builder.append(resolvedColorCodes);
        builder.nextLine();
        builder.append(showGrid);
        builder.nextLine();
        JButton fontButton = new JButton(Localization.lang("Set table font"));
        builder.append(fontButton);
        builder.nextLine();
        builder.appendSeparator(Localization.lang("Table and entry editor colors"));
        builder.append(colorPanel);

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

        fontButton.addActionListener(
                e -> new FontSelectorDialog(null, usedFont).getSelectedFont().ifPresent(x -> usedFont = x));

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        // L&F
        useDefaultLAF = prefs.getBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL);
        fxFontTweaksLAF.setSelected(prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK));
        currentLAF = prefs.get(JabRefPreferences.WIN_LOOK_AND_FEEL);
        customLAF.setSelected(!useDefaultLAF);
        classNamesLAF.setSelectedItem(currentLAF);
        classNamesLAF.setEnabled(!useDefaultLAF);

        colorCodes.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON));
        resolvedColorCodes.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_RESOLVED_COLOR_CODES_ON));
        rowPadding.setText(String.valueOf(prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING)));

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
        showGrid.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID));
        colorPanel.setValues();
    }

    @Override
    public void storeSettings() {
        boolean isRestartRequired;

        // L&F
        prefs.putBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL, !customLAF.isSelected());
        prefs.put(JabRefPreferences.WIN_LOOK_AND_FEEL, classNamesLAF.getSelectedItem().toString());
        isRestartRequired = (customLAF.isSelected() == useDefaultLAF) ||
                !currentLAF.equals(classNamesLAF.getSelectedItem().toString());

        // Java FX font rendering tweak
        final boolean oldFxTweakValue = prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK);
        prefs.putBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK, fxFontTweaksLAF.isSelected());
        isRestartRequired |= oldFxTweakValue != fxFontTweaksLAF.isSelected();

        prefs.putBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON, colorCodes.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_RESOLVED_COLOR_CODES_ON, resolvedColorCodes.isSelected());
        prefs.put(JabRefPreferences.FONT_FAMILY, usedFont.getFamily());
        prefs.putInt(JabRefPreferences.FONT_STYLE, usedFont.getStyle());
        prefs.putInt(JabRefPreferences.FONT_SIZE, usedFont.getSize());
        prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS, overrideFonts.isSelected());
        GUIGlobals.currentFont = usedFont;
        colorPanel.storeSettings();
        prefs.putBoolean(JabRefPreferences.TABLE_SHOW_GRID, showGrid.isSelected());
        try {
            int size = Integer.parseInt(fontSize.getText());
            int smallIconSize = Integer.parseInt(smallIconsTextField.getText());
            int largeIconSize = Integer.parseInt(largeIconsTextField.getText());
            int padding = Integer.parseInt(rowPadding.getText());
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
                JOptionPane.showMessageDialog(
                        null,
                        Localization.lang("Some appearance settings you changed require to restart JabRef to come into effect."),
                        Localization.lang("Settings"), JOptionPane.WARNING_MESSAGE);
            }

            prefs.putInt(JabRefPreferences.TABLE_ROW_PADDING, padding);
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
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You must enter an integer value in the text field for") + " '" + fieldName + "'",
                    errorTitle, JOptionPane.ERROR_MESSAGE);
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

        if (!validateIntegerField(Localization.lang("Size of small icons"), smallIconsTextField.getText(),
                Localization.lang("Invalid setting"))) {
            return false;
        }

        // Test if row padding is a number:
        return validateIntegerField(Localization.lang("Table row height padding"), rowPadding.getText(),
                Localization.lang("Invalid setting"));
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

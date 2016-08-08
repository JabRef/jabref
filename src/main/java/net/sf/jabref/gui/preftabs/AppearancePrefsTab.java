/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class AppearancePrefsTab extends JPanel implements PrefsTab {
    private static final Log LOGGER = LogFactory.getLog(AppearancePrefsTab.class);

    private final JabRefPreferences prefs;

    private final JCheckBox colorCodes;
    private final JCheckBox resolvedColorCodes;
    private final JCheckBox overrideFonts;
    private final JCheckBox showGrid;
    private final ColorSetupPanel colorPanel = new ColorSetupPanel();
    private Font usedFont = GUIGlobals.currentFont;
    private int oldMenuFontSize;
    private boolean oldOverrideFontSize;
    private final JTextField fontSize;
    private final JTextField rowPadding;
    // look and feel
    private final JComboBox<String> classNamesLAF;
    private String currentLAF = "";
    private boolean useDefaultLAF;
    private final JCheckBox customLAF;

    static class LookAndFeel {
        private static final List<String> looks = Arrays.asList(
                UIManager.getSystemLookAndFeelClassName(),
                UIManager.getCrossPlatformLookAndFeelClassName(),
                "com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
                "com.jgoodies.looks.windows.WindowsLookAndFeel");

        public static List<String> getAvailableLookAndFeels() {
            List<String> lookAndFeels = new ArrayList<>();

            for (String l : looks) {
                try {
                    // Try to find L&F
                    Class.forName(l);
                    lookAndFeels.add(l);
                } catch (ClassNotFoundException ignored) {
                    // Ignored
                }
            }
            return lookAndFeels;
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

        colorCodes = new JCheckBox(
                Localization.lang("Color codes for required and optional fields"));

        resolvedColorCodes = new JCheckBox(Localization.lang("Color codes for resolved fields"));

        overrideFonts = new JCheckBox(Localization.lang("Override default font settings"));

        showGrid = new JCheckBox(Localization.lang("Show gridlines"));

        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        customLAF = new JCheckBox(Localization.lang("Use other look and feel"));
        // Only list L&F which are available
        List<String> lookAndFeels = LookAndFeel.getAvailableLookAndFeels();
        classNamesLAF = new JComboBox<>(lookAndFeels.toArray(new String[lookAndFeels.size()]));
        classNamesLAF.setEditable(true);
        final JComboBox<String> clName = classNamesLAF;
        customLAF.addChangeListener(e -> clName.setEnabled(((JCheckBox) e.getSource()).isSelected()));

        // only the default L&F shows the the OSX specific first dropdownmenu
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
        }

        builder.leadingColumnOffset(2);
        JLabel lab;
        builder.appendSeparator(Localization.lang("General"));
        JPanel p1 = new JPanel();
        lab = new JLabel(Localization.lang("Menu and label font size") + ":");
        p1.add(lab);
        p1.add(fontSize);
        builder.append(p1);
        builder.nextLine();
        builder.append(overrideFonts);
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

        fontButton.addActionListener(
                e -> new FontSelectorDialog(null, GUIGlobals.currentFont).getSelectedFont().ifPresent(x -> usedFont = x));

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        // L&F
        useDefaultLAF = prefs.getBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL);
        currentLAF = prefs.get(JabRefPreferences.WIN_LOOK_AND_FEEL);
        customLAF.setSelected(!useDefaultLAF);
        classNamesLAF.setSelectedItem(currentLAF);
        classNamesLAF.setEnabled(!useDefaultLAF);

        colorCodes.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON));
        resolvedColorCodes.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_RESOLVED_COLOR_CODES_ON));
        fontSize.setText(String.valueOf(prefs.getInt(JabRefPreferences.MENU_FONT_SIZE)));
        rowPadding.setText(String.valueOf(prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING)));
        oldMenuFontSize = prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
        overrideFonts.setSelected(prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS));
        oldOverrideFontSize = overrideFonts.isSelected();
        fontSize.setEnabled(overrideFonts.isSelected());
        showGrid.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID));
        colorPanel.setValues();
    }

    @Override
    public void storeSettings() {
        // L&F
        prefs.putBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL, !customLAF.isSelected());
        prefs.put(JabRefPreferences.WIN_LOOK_AND_FEEL, classNamesLAF.getSelectedItem().toString());
        if ((customLAF.isSelected() == useDefaultLAF) || !currentLAF.equals(classNamesLAF.getSelectedItem().toString())) {
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You have changed the look and feel setting.").concat(" ")
                            .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                    Localization.lang("Changed look and feel settings"), JOptionPane.WARNING_MESSAGE);
        }

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
            if ((overrideFonts.isSelected() != oldOverrideFontSize) ||
                    (size != oldMenuFontSize)) {
                prefs.putInt(JabRefPreferences.MENU_FONT_SIZE, size);
                JOptionPane.showMessageDialog(null,
                        Localization.lang("You have changed the menu and label font size.")
                                .concat(" ")
                                .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                        Localization.lang("Changed font settings"),
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            LOGGER.info("Invalid font size", ex);
        }
        try {
            int padding = Integer.parseInt(rowPadding.getText());
            prefs.putInt(JabRefPreferences.TABLE_ROW_PADDING, padding);
        } catch (NumberFormatException ex) {
            LOGGER.info("Invalid row padding", ex);
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

        // Test if row padding is a number:
        return validateIntegerField(Localization.lang("Table row height padding"), rowPadding.getText(),
                Localization.lang("Invalid setting"));
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

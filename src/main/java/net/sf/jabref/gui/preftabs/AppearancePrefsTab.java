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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.ColorSetupPanel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.logic.l10n.Localization;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private final JCheckBox colorCodes;
    private final JCheckBox overrideFonts;
    private final JCheckBox showGrid;
    private final ColorSetupPanel colorPanel = new ColorSetupPanel();
    private Font font = GUIGlobals.CURRENTFONT;
    private int oldMenuFontSize;
    private boolean oldOverrideFontSize;
    private final JTextField fontSize;
    private final JTextField rowPadding;


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

        overrideFonts = new JCheckBox(Localization.lang("Override default font settings"));

        showGrid = new JCheckBox(Localization.lang("Show gridlines"));

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref",
                        "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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

        overrideFonts.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fontSize.setEnabled(overrideFonts.isSelected());
            }
        });

        fontButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Font f = new FontSelectorDialog
                        (null, GUIGlobals.CURRENTFONT).getSelectedFont();
                if (f != null) {
                    font = f;
                }
            }
        });

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        colorCodes.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON));
        fontSize.setText("" + prefs.getInt(JabRefPreferences.MENU_FONT_SIZE));
        rowPadding.setText("" + prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING));
        oldMenuFontSize = prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
        overrideFonts.setSelected(prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS));
        oldOverrideFontSize = overrideFonts.isSelected();
        fontSize.setEnabled(overrideFonts.isSelected());
        showGrid.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID));
        colorPanel.setValues();
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        prefs.putBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON, colorCodes.isSelected());
        prefs.put(JabRefPreferences.FONT_FAMILY, font.getFamily());
        prefs.putInt(JabRefPreferences.FONT_STYLE, font.getStyle());
        prefs.putInt(JabRefPreferences.FONT_SIZE, font.getSize());
        prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS, overrideFonts.isSelected());
        GUIGlobals.CURRENTFONT = font;
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
            ex.printStackTrace();
        }
        try {
            int padding = Integer.parseInt(rowPadding.getText());
            prefs.putInt(JabRefPreferences.TABLE_ROW_PADDING, padding);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
    }

    private boolean validateIntegerField(String fieldName, String fieldValue, String errorTitle)
    {
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
                Localization.lang("Changed font settings"))) {
            return false;
        }

        // Test if row padding is a number:
        if (!validateIntegerField(Localization.lang("Table row height padding"), rowPadding.getText(),
                Localization.lang("Changed table appearance settings"))) {
            return false;
        }

        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

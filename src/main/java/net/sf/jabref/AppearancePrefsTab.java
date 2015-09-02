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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.gui.ColorSetupPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences _prefs;
    private final JCheckBox colorCodes;
    private final JCheckBox overrideFonts;
    private final JCheckBox showGrid;//, useCustomIconTheme;
    private final ColorSetupPanel colorPanel = new ColorSetupPanel();
    private Font font = GUIGlobals.CURRENTFONT;
    private int oldMenuFontSize;
    private boolean oldOverrideFontSize;
    private final JTextField fontSize;
    private final JTextField rowPadding;//, customIconThemeFile;


    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(JabRefPreferences prefs) {
        _prefs = prefs;
        setLayout(new BorderLayout());

        // Font sizes:
        fontSize = new JTextField(5);

        // Row padding size:
        rowPadding = new JTextField(5);

        colorCodes = new JCheckBox(
                Globals.lang("Color codes for required and optional fields"));
        /*antialias = new JCheckBox(Globals.lang
                  ("Use antialiasing font"));*/
        overrideFonts = new JCheckBox(Globals.lang("Override default font settings"));

        showGrid = new JCheckBox(Globals.lang("Show gridlines"));

        //useCustomIconTheme = new JCheckBox(Globals.lang("Use custom icon theme"));
        //customIconThemeFile = new JTextField();
        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref",
                        "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.leadingColumnOffset(2);
        JLabel lab;
        builder.appendSeparator(Globals.lang("General"));
        JPanel p1 = new JPanel();
        lab = new JLabel(Globals.lang("Menu and label font size") + ":");
        p1.add(lab);
        p1.add(fontSize);
        builder.append(p1);
        builder.nextLine();
        builder.append(overrideFonts);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Table appearance"));
        //builder.append(antialias);
        //builder.nextLine();
        JPanel p2 = new JPanel();
        p2.add(new JLabel(Globals.lang("Table row height padding") + ":"));
        p2.add(rowPadding);
        builder.append(p2);
        builder.nextLine();
        builder.append(colorCodes);
        builder.nextLine();
        builder.append(showGrid);
        builder.nextLine();
        JButton fontButton = new JButton(Globals.lang("Set table font"));
        builder.append(fontButton);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Table and entry editor colors"));
        builder.append(colorPanel);
        //builder.appendSeparator(Globals.lang("Custom icon theme"));
        //builder.append(useCustomIconTheme);
        //builder.nextLine();
        //JPanel p2 = new JPanel();
        //lab = new JLabel(Globals.lang("Custom icon theme file")+":");
        //p2.add(lab);
        //p2.add(customIconThemeFile);
        //BrowseAction browse = new BrowseAction(null, customIconThemeFile, false);
        //JButton browseBut = new JButton(Globals.lang("Browse"));
        //browseBut.addActionListener(browse);
        //p2.add(browseBut);
        //builder.append(p2);

        JPanel upper = new JPanel(), sort = new JPanel(), namesp = new JPanel(), iconCol = new JPanel();
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
        /*menuFontButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 Font f=new FontSelectorDialog
                     (null, menuFont).getSelectedFont();
                 if(f==null)
                     return;
                 else
                     menuFont = f;
             }
             });*/

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        colorCodes.setSelected(_prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON));
        //antialias.setSelected(_prefs.getBoolean("antialias"));
        fontSize.setText("" + _prefs.getInt(JabRefPreferences.MENU_FONT_SIZE));
        rowPadding.setText("" + _prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING));
        oldMenuFontSize = _prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
        overrideFonts.setSelected(_prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS));
        oldOverrideFontSize = overrideFonts.isSelected();
        fontSize.setEnabled(overrideFonts.isSelected());
        //useCustomIconTheme.setSelected(_prefs.getBoolean("useCustomIconTheme"));
        //customIconThemeFile.setText(_prefs.get("customIconThemeFile"));
        showGrid.setSelected(_prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID));
        colorPanel.setValues();
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        _prefs.putBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON, colorCodes.isSelected());
        //_prefs.putBoolean("antialias", antialias.isSelected());
        _prefs.put(JabRefPreferences.FONT_FAMILY, font.getFamily());
        _prefs.putInt(JabRefPreferences.FONT_STYLE, font.getStyle());
        _prefs.putInt(JabRefPreferences.FONT_SIZE, font.getSize());
        _prefs.putBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS, overrideFonts.isSelected());
        GUIGlobals.CURRENTFONT = font;
        colorPanel.storeSettings();
        _prefs.putBoolean(JabRefPreferences.TABLE_SHOW_GRID, showGrid.isSelected());
        try {
            int size = Integer.parseInt(fontSize.getText());
            if ((overrideFonts.isSelected() != oldOverrideFontSize) ||
                    (size != oldMenuFontSize)) {
                _prefs.putInt(JabRefPreferences.MENU_FONT_SIZE, size);
                JOptionPane.showMessageDialog(null,
                        Globals.lang("You have changed the menu and label font size.")
                                .concat(" ")
                                .concat(Globals.lang("You must restart JabRef for this to come into effect.")),
                        Globals.lang("Changed font settings"),
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        try {
            int padding = Integer.parseInt(rowPadding.getText());
            _prefs.putInt(JabRefPreferences.TABLE_ROW_PADDING, padding);
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
            JOptionPane.showMessageDialog
                    (null, Globals.lang("You must enter an integer value in the text field for") + " '" +
                            Globals.lang(fieldName) + "'", Globals.lang(errorTitle),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public boolean readyToClose() {
        // Test if font size is a number:
        if (validateIntegerField("Menu and label font size", fontSize.getText(), "Changed font settings") == false) {
            return false;
        }

        // Test if row padding is a number:
        if (validateIntegerField("Table row height padding", rowPadding.getText(), "Changed table appearance settings") == false) {
            return false;
        }

        return true;

    }

    @Override
    public String getTabName() {
        return Globals.lang("Appearance");
    }
}

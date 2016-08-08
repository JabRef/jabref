/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 10, 2005
 * Time: 4:29:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ColorSetupPanel extends JPanel {

    private static final int ICON_WIDTH = 30;
    private static final int ICON_HEIGHT = 20;
    private final List<ColorButton> buttons = new ArrayList<>();

    public ColorSetupPanel() {

        FormLayout layout = new FormLayout
                ("30dlu, 4dlu, fill:pref, 4dlu, fill:pref, 8dlu, 30dlu, 4dlu, fill:pref, 4dlu, fill:pref",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref");
        FormBuilder builder = FormBuilder.create().layout(layout);

        buttons.add(new ColorButton(JabRefPreferences.TABLE_TEXT, Localization.lang("Table text color")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND0, Localization.lang("Marking color %0", "1")));
        buttons.add(new ColorButton(JabRefPreferences.TABLE_BACKGROUND, Localization.lang("Table background color")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND1, Localization.lang("Marking color %0", "2")));
        buttons.add(new ColorButton(JabRefPreferences.TABLE_REQ_FIELD_BACKGROUND, Localization.lang("Background color for required fields")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND2, Localization.lang("Marking color %0", "3")));
        buttons.add(new ColorButton(JabRefPreferences.TABLE_OPT_FIELD_BACKGROUND, Localization.lang("Background color for optional fields")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND3, Localization.lang("Marking color %0", "4")));
        buttons.add(new ColorButton(JabRefPreferences.INCOMPLETE_ENTRY_BACKGROUND, Localization.lang("Color for marking incomplete entries")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND4, Localization.lang("Marking color %0", "5")));
        buttons.add(new ColorButton(JabRefPreferences.GRID_COLOR, Localization.lang("Table grid color")));
        buttons.add(new ColorButton(JabRefPreferences.MARKED_ENTRY_BACKGROUND5, Localization.lang("Import marking color")));

        buttons.add(new ColorButton(JabRefPreferences.FIELD_EDITOR_TEXT_COLOR, Localization.lang("Entry editor font color")));
        buttons.add(new ColorButton(JabRefPreferences.VALID_FIELD_BACKGROUND_COLOR, Localization.lang("Entry editor background color")));
        buttons.add(new ColorButton(JabRefPreferences.ACTIVE_FIELD_EDITOR_BACKGROUND_COLOR, Localization.lang("Entry editor active background color")));
        buttons.add(new ColorButton(JabRefPreferences.INVALID_FIELD_BACKGROUND_COLOR, Localization.lang("Entry editor invalid field color")));
        buttons.add(new ColorButton(JabRefPreferences.TABLE_RESOLVED_FIELD_BACKGROUND,
                Localization.lang("Background color for resolved fields")));

        int rowcnt = 0;
        int col = 0;
        int row;
        for (ColorButton but : buttons) {
            row = (2*(rowcnt/2)) + 1; // == 2*floor(rowcnt/2) + 1
            builder.add((JButton)but).xy((6*col)+1, row);
            builder.add(but.getDefaultButton()).xy((6*col)+3, row);
            builder.add(but.getName()).xy((6*col)+5, row);
            but.addActionListener(new ColorButtonListener(but));
            col = 1-col;  // Change 0 -> 1 -> 0 ...
            rowcnt++;
        }

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        setValues();

    }

    public void setValues() {
        for (ColorButton but : buttons) {
            but.setColor(Globals.prefs.getColor(but.getKey()));
        }

    }

    public void storeSettings() {
        for (ColorButton but : buttons) {
            Globals.prefs.putColor(but.getKey(), but.getColor());
        }
    }


    static class ColorButtonListener implements ActionListener {

        private final ColorButton button;


        public ColorButtonListener(ColorButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Color chosen = JColorChooser.showDialog(null, button.getName(), button.getColor());
            if (chosen != null) {
                button.setColor(chosen);
            }
        }
    }

    /**
     * A button to display the chosen color, and hold key information about a color setting.
     * Includes a method to produce a Default button for this setting.
     */
    static class ColorButton extends JButton implements Icon {
        private Color color = Color.white;
        private final String key;
        private final String name;


        public ColorButton(String key, String name) {
            setIcon(this);
            this.key = key;
            this.name = name;
            setBorder(BorderFactory.createRaisedBevelBorder());
        }

        public JButton getDefaultButton() {
            JButton toDefault = new JButton(Localization.lang("Default"));
            toDefault.addActionListener(e -> {
                setColor(Globals.prefs.getDefaultColor(key));
                repaint();
            });
            return toDefault;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String getName() {
            return name;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Rectangle r = g.getClipBounds();
            g.setColor(color);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        public int getIconWidth() {
            return ColorSetupPanel.ICON_WIDTH;
        }

        @Override
        public int getIconHeight() {
            return ColorSetupPanel.ICON_HEIGHT;
        }
    }

}

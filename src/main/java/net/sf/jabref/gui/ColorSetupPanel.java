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
package net.sf.jabref.gui;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;

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
    private final ArrayList<ColorButton> buttons = new ArrayList<ColorButton>();


    public ColorSetupPanel() {

        FormLayout layout = new FormLayout
                ("30dlu, 4dlu, fill:pref, 4dlu, fill:pref, 8dlu, 30dlu, 4dlu, fill:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        buttons.add(new ColorButton("tableText", Localization.lang("Table text color")));
        buttons.add(new ColorButton("markedEntryBackground0", Localization.lang("Marking color %0", "1")));
        buttons.add(new ColorButton("tableBackground", Localization.lang("Table background color")));
        buttons.add(new ColorButton("markedEntryBackground1", Localization.lang("Marking color %0", "2")));
        buttons.add(new ColorButton("tableReqFieldBackground", Localization.lang("Background color for required fields")));
        buttons.add(new ColorButton("markedEntryBackground2", Localization.lang("Marking color %0", "3")));
        buttons.add(new ColorButton("tableOptFieldBackground", Localization.lang("Background color for optional fields")));
        buttons.add(new ColorButton("markedEntryBackground3", Localization.lang("Marking color %0", "4")));
        buttons.add(new ColorButton("incompleteEntryBackground", Localization.lang("Color for marking incomplete entries")));
        buttons.add(new ColorButton("markedEntryBackground4", Localization.lang("Marking color %0", "5")));
        buttons.add(new ColorButton("gridColor", Localization.lang("Table grid color")));
        buttons.add(new ColorButton("markedEntryBackground5", Localization.lang("Import marking color")));

        buttons.add(new ColorButton("fieldEditorTextColor", Localization.lang("Entry editor font color")));
        buttons.add(new ColorButton("validFieldBackgroundColor", Localization.lang("Entry editor background color")));
        buttons.add(new ColorButton("activeFieldEditorBackgroundColor", Localization.lang("Entry editor active background color")));
        buttons.add(new ColorButton("invalidFieldBackgroundColor", Localization.lang("Entry editor invalid field color")));

        for (ColorButton but : buttons) {
            builder.append(but);
            builder.append(but.getDefaultButton());
            builder.append(but.getName());
            but.addActionListener(new ColorButtonListener(but));

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


    class ColorButtonListener implements ActionListener {

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
    class ColorButton extends JButton implements Icon {

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
            toDefault.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setColor(Globals.prefs.getDefaultColor(key));
                    repaint();
                }
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

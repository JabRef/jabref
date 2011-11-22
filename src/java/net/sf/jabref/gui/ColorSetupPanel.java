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
import java.util.Iterator;

import net.sf.jabref.Globals;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 10, 2005
 * Time: 4:29:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ColorSetupPanel extends JPanel {

    private final static int ICON_WIDTH=30, ICON_HEIGHT=20;
    private ArrayList<ColorButton> buttons = new ArrayList<ColorButton>();

    public ColorSetupPanel() {

        FormLayout layout = new FormLayout
                ("30dlu, 4dlu, fill:pref, 4dlu, fill:pref, 8dlu, 30dlu, 4dlu, fill:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        buttons.add(new ColorButton("tableText", Globals.lang("Table text color")));
        buttons.add(new ColorButton("markedEntryBackground0", Globals.lang("Marking color %0", "1")));
        buttons.add(new ColorButton("tableBackground", Globals.lang("Table background color")));
        buttons.add(new ColorButton("markedEntryBackground1", Globals.lang("Marking color %0", "2")));
        buttons.add(new ColorButton("tableReqFieldBackground", Globals.lang("Background color for required fields")));
        buttons.add(new ColorButton("markedEntryBackground2", Globals.lang("Marking color %0", "3")));
        buttons.add(new ColorButton("tableOptFieldBackground", Globals.lang("Background color for optional fields")));
        buttons.add(new ColorButton("markedEntryBackground3", Globals.lang("Marking color %0", "4")));
        buttons.add(new ColorButton("incompleteEntryBackground", Globals.lang("Color for marking incomplete entries")));
        buttons.add(new ColorButton("markedEntryBackground4", Globals.lang("Marking color %0", "5")));
        buttons.add(new ColorButton("gridColor", Globals.lang("Table grid color")));
        buttons.add(new ColorButton("markedEntryBackground5", Globals.lang("Import marking color")));

        buttons.add(new ColorButton("fieldEditorTextColor", Globals.lang("Entry editor font color")));
        buttons.add(new ColorButton("validFieldBackgroundColor", Globals.lang("Entry editor background color")));
        buttons.add(new ColorButton("activeFieldEditorBackgroundColor", Globals.lang("Entry editor active background color")));
        buttons.add(new ColorButton("invalidFieldBackgroundColor", Globals.lang("Entry editor invalid field color")));

        for (Iterator<ColorButton> i=buttons.iterator(); i.hasNext();) {
            ColorButton but = i.next();
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
        for (Iterator<ColorButton> i=buttons.iterator(); i.hasNext();) {
            ColorButton but = i.next();
            but.setColor(Globals.prefs.getColor(but.getKey()));
        }

    }

    public void storeSettings() {
        for (Iterator<ColorButton> i=buttons.iterator(); i.hasNext();) {
            ColorButton but = i.next();
            Globals.prefs.putColor(but.getKey(), but.getColor());
        }
    }

    class ColorButtonListener implements ActionListener {
        private ColorButton button;

        public ColorButtonListener(ColorButton button) {
            this.button = button;
        }
        public void actionPerformed(ActionEvent e) {
            Color chosen = JColorChooser.showDialog(null, button.getName(), button.getColor());
            if (chosen != null)
                button.setColor(chosen);
        }
    }

    /**
     * A button to display the chosen color, and hold key information about a color setting.
     * Includes a method to produce a Default button for this setting.
     */
    class ColorButton extends JButton implements Icon {
        private Color color = Color.white;
        private String key, name;

        public ColorButton(String key, String name) {
            setIcon(this);
            this.key = key;
            this.name = name;
            setBorder(BorderFactory.createRaisedBevelBorder());
        }

        public JButton getDefaultButton() {
            JButton toDefault = new JButton(Globals.lang("Default"));
            toDefault.addActionListener(new ActionListener() {
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

        public String getName() {
            return name;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Rectangle r = g.getClipBounds();
            g.setColor(color);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        public int getIconWidth() {
            return ICON_WIDTH;
        }

        public int getIconHeight() {
            return ICON_HEIGHT;
        }
    }

}

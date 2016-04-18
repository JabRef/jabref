/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.specialfields;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.util.OS;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

public class SpecialFieldDropDown {

    public static JButton generateSpecialFieldButtonWithDropDown(SpecialField field, JabRefFrame frame) {
        Dimension buttonDim = new Dimension(23, 23);
        JButton button = new JButton(field.getRepresentingIcon());
        button.setToolTipText(field.getToolTip());
        button.setPreferredSize(buttonDim);
        if (!OS.OS_X) {
            button.setMargin(new Insets(1, 0, 2, 0));
        }
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);
        button.setOpaque(false);
        button.setBounds(0, 0, buttonDim.width, buttonDim.height);
        button.setSize(buttonDim);
        button.setMinimumSize(buttonDim);
        button.setMaximumSize(buttonDim);
        button.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        button.addActionListener(new MenuButtonActionListener(field, frame, button, buttonDim));
        return button;
    }


    private static class MenuButtonActionListener implements ActionListener {

        private JPopupMenu popup;
        private final Dimension dim;
        private final JabRefFrame frame;
        private final SpecialField field;
        private final JButton button;


        public MenuButtonActionListener(SpecialField field, JabRefFrame frame, JButton button, Dimension dim) {
            this.field = field;
            this.dim = dim;
            this.frame = frame;
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (popup == null) {
                popup = new JPopupMenu();
                for (SpecialFieldValue val : field.getValues()) {
                    JMenuItem item = new JMenuItem(val.getIcon());
                    item.setText(val.getMenuString());
                    item.setToolTipText(val.getToolTipText());
                    item.addActionListener(new PopupitemActionListener(frame.getCurrentBasePanel(), val.getActionName()));
                    item.setMargin(new Insets(0, 0, 0, 0));
                    popup.add(item);
                }
            }
            popup.show(button, 0, dim.height);
        }


        private class PopupitemActionListener implements ActionListener {

            private final BasePanel panel;
            private final String actionName;


            public PopupitemActionListener(BasePanel panel, String actionName) {
                this.panel = panel;
                this.actionName = actionName;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                panel.runCommand(actionName);
                popup.setVisible(false);
            }

        }

    }

}

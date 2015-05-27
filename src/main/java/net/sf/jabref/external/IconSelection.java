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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Dialog box for choosing an icon for an external file type.
 */
public class IconSelection extends JDialog {

    JList<JLabel> icons;
    List<String> iconKeys;
    DefaultListModel<JLabel> listModel;
    JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    private boolean okPressed = false;
    private int selected = -1;
    private JDialog parent;

    public IconSelection(JDialog parent, String initialSelection) {
        super(parent, Globals.lang("Select icon"), true);
        this.parent = parent;
        init(initialSelection);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            okPressed = false;
            selected = -1;
        }
        super.setVisible(visible);
    }

    /**
     * After dialog has closed, this method reports whether a selection was made
     * or it was cancelled.
     * @return true if a selection was made.
     */
    public boolean isOkPressed() {
        return okPressed;
    }

    public String getSelectedIconKey() {
        if (selected >= 0)
            return iconKeys.get(selected);
        else
            return null;
    }

    private void init(String initialSelection) {
        int initSelIndex = -1;
        iconKeys = new ArrayList<String>();
        Map<String, String> icns = GUIGlobals.getAllIcons();
        HashSet<ImageIcon> iconSet = new LinkedHashSet<ImageIcon>();
        for (String key : icns.keySet()){
            ImageIcon icon = GUIGlobals.getImage(key);
            if (!iconSet.contains(icon)) {
                iconKeys.add(key);
                if (key.equals(initialSelection))
                    initSelIndex = iconKeys.size()-1;
            }
            iconSet.add(icon);

        }

        listModel = new DefaultListModel<JLabel>();
        icons = new JList<JLabel>(listModel);
        for (ImageIcon anIconSet : iconSet) {
            listModel.addElement(new JLabel(anIconSet));
        }
        class MyRenderer implements ListCellRenderer<JLabel> {
            JLabel comp = new JLabel();
            public MyRenderer() {
                comp.setOpaque(true);
                comp.setIconTextGap(0);
                comp.setHorizontalAlignment(JLabel.CENTER);
            }

            public Component getListCellRendererComponent(JList<? extends JLabel> list, JLabel value, int i,
                                                          boolean isSelected, 
                                                          boolean hasFocus) {
                comp.setText(null);
                comp.setIcon(value.getIcon());
                if (isSelected) {
                    comp.setBackground(list.getSelectionBackground());
                    comp.setForeground(list.getSelectionForeground());
                    comp.setBorder(BorderFactory.createEtchedBorder());
                } else {
                    comp.setBackground(list.getBackground());
                    comp.setForeground(list.getForeground());
                    comp.setBorder(null);
                }

                return comp;
            }
        }

        if (initSelIndex >= 0)
            icons.setSelectedIndex(initSelIndex);
        icons.setCellRenderer(new MyRenderer());
        icons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        icons.setLayoutOrientation(JList.HORIZONTAL_WRAP);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = true;
                if (icons.getSelectedValue() != null)
                    selected = icons.getSelectedIndex(); 
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = false;
                dispose();
            }
        });

        getContentPane().add(new JScrollPane(icons), BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }
}

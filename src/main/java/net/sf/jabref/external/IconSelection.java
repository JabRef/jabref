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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Dialog box for choosing an icon for an external file type.
 */
class IconSelection extends JDialog {

    private JList icons;
    private List<String> iconKeys;
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private boolean okPressed;
    private int selected = -1;
    private final JDialog parent;


    public IconSelection(JDialog parent, String initialSelection) {
        super(parent, Localization.lang("Select icon"), true);
        this.parent = parent;
        init(initialSelection);
    }

    @Override
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
        if (selected >= 0) {
            return iconKeys.get(selected);
        } else {
            return null;
        }
    }

    private void init(String initialSelection) {
        int initSelIndex = -1;
        iconKeys = new ArrayList<>();
        Map<String, String> icns = IconTheme.getAllIcons();
        HashSet<Icon> iconSet = new LinkedHashSet<>();
        for (String key : icns.keySet()) {
            Icon icon = IconTheme.getImage(key);
            if (!iconSet.contains(icon)) {
                iconKeys.add(key);
                if (key.equals(initialSelection)) {
                    initSelIndex = iconKeys.size() - 1;
                }
            }
            iconSet.add(icon);

        }

        DefaultListModel<JLabel> listModel = new DefaultListModel();
        icons = new JList(listModel);
        for (Icon anIconSet : iconSet) {
            listModel.addElement(new JLabel(anIconSet));
        }
        class MyRenderer implements ListCellRenderer {

            final JLabel comp = new JLabel();


            public MyRenderer() {
                comp.setOpaque(true);
                comp.setIconTextGap(0);
                comp.setHorizontalAlignment(JLabel.CENTER);
            }

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int i,
                    boolean isSelected,
                    boolean hasFocus) {
                comp.setText(null);
                comp.setIcon(((JLabel) value).getIcon());
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

        if (initSelIndex >= 0) {
            icons.setSelectedIndex(initSelIndex);
        }
        icons.setCellRenderer(new MyRenderer());
        icons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        icons.setLayoutOrientation(JList.HORIZONTAL_WRAP);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = true;
                if (icons.getSelectedValue() != null) {
                    selected = icons.getSelectedIndex();
                }
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
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

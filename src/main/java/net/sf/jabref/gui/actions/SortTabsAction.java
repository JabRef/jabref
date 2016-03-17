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
package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Action;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This action rearranges all tabs in the main tabbed pane of the given JabRefFrame
 * in alphabetical order.
 */
public class SortTabsAction extends MnemonicAwareAction implements Comparator<String> {

    private final JabRefFrame frame;


    public SortTabsAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Sort tabs"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Rearrange tabs alphabetically by title"));
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Make a sorted Map that compares case-insensitively:
        TreeMap<String, BasePanel> map = new TreeMap<>(this);

        for (BasePanel panel : frame.getBasePanelList()) {
            map.put(panel.getTabTitle(), panel);
        }

        frame.getTabbedPane().removeAll();
        for (Map.Entry<String, BasePanel> entry : map.entrySet()) {
            frame.addTab(entry.getValue(), false);
        }
    }

    @Override
    public int compare(String o1, String o2) {
        return o1.toLowerCase().compareTo(o2.toLowerCase());
    }
}

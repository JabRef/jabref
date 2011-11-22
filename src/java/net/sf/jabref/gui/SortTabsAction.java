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

import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JTabbedPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;

/**
 * This action rearranges all tabs in the main tabbed pane of the given JabRefFrame
 * in alphabetical order.
 */
public class SortTabsAction extends MnemonicAwareAction implements Comparator<String> {
    private JabRefFrame frame;

    public SortTabsAction(JabRefFrame frame) {
        putValue(NAME, "Sort tabs");
        putValue(SHORT_DESCRIPTION, Globals.lang("Rearrange tabs alphabetically by title"));
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        JTabbedPane tabbedPane = frame.getTabbedPane();
       // Make a sorted Map that compares case-insensitively:
        TreeMap<String, BasePanel> map = new TreeMap<String, BasePanel>(this);

        for (int i=0; i<tabbedPane.getTabCount(); i++) {
            BasePanel panel = (BasePanel)tabbedPane.getComponent(i);
            map.put(tabbedPane.getTitleAt(i), panel);
        }
        tabbedPane.removeAll();
        for (Iterator<String> i=map.keySet().iterator(); i.hasNext();) {
            String title = i.next();
            BasePanel panel = map.get(title);
            tabbedPane.addTab(title, panel);
        }
    }

    public int compare(String o1, String o2) {
        return o1.toLowerCase().compareTo(o2.toLowerCase());
    }
}

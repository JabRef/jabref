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

import net.sf.jabref.undo.NamedCompound;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class MarkEntriesAction extends AbstractWorker implements ActionListener {

    private JabRefFrame frame;
    final int level;
    private JMenuItem menuItem;
    private int besLength = 0;

    public MarkEntriesAction(JabRefFrame frame, int level) {
        this.frame = frame;
        this.level = level;

        //menuItem = new JMenuItem(Globals.menuTitle("Mark entries").replaceAll("&",""));
        menuItem = new JMenuItem("               ");
        menuItem.setMnemonic(String.valueOf(level+1).charAt(0));
        menuItem.setBackground(Globals.prefs.getColor("markedEntryBackground"+this.level));
        menuItem.setOpaque(true);
        menuItem.addActionListener(this);
    }

    public JMenuItem getMenuItem() {
        return menuItem;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            this.init();
            getWorker().run();
            getCallBack().update();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void run() {
        BasePanel panel = frame.basePanel();
        NamedCompound ce = new NamedCompound(Globals.lang("Mark entries"));
        BibtexEntry[] bes = panel.getSelectedEntries();
        besLength = bes.length;

        for (int i=0; i<bes.length; i++) {
            Util.markEntry(bes[i], level+1, false, ce);
        }
        ce.end();
        panel.undoManager.addEdit(ce);
    }

    @Override
    public void update() {
        frame.basePanel().markBaseChanged();
        frame.output(Globals.lang("Marked selected")+" "+Globals.lang(besLength>0?"entry":"entries"));
    }
}

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
 * Creates the Edit Ranking Menu 
 */
public class PriorityEntriesAction extends AbstractWorker implements ActionListener {

    private JabRefFrame frame;
    final int priorityLevel;
    private JMenuItem menuItem;
    private int besLength = 0;

    public PriorityEntriesAction(JabRefFrame frame, int priorityLevel) {
        this.frame = frame;
        this.priorityLevel = priorityLevel;
        
        if (priorityLevel == 0) {
        	menuItem = new JMenuItem();
	        menuItem.setText((Globals.lang("Reset Priority")));
	        menuItem.setOpaque(true);
	        menuItem.addActionListener(this);
        }else{
	        menuItem = new JMenuItem(new ImageIcon(GUIGlobals.getIconUrl(GUIGlobals.getIconString(priorityLevel))));
	        menuItem.setText((Globals.lang("Set Priority to") + " " + GUIGlobals.getPrioString(priorityLevel)));
	        //menuItem.setMnemonic(String.valueOf(priorityLevel+1).charAt(0));
	        //menuItem.setBackground(Globals.prefs.getColor("markedEntryBackground"+this.priorityLevel));
	        menuItem.setOpaque(true);
	        menuItem.addActionListener(this);
        }
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
        
        if (priorityLevel == 0) {
        	for (int i=0; i<bes.length; i++) {
	            Util.resetPriorityEntry(bes[i], false, this.frame.basePanel().database(), ce);
	        }
        }else{
	        for (int i=0; i<bes.length; i++) {
	            Util.setPriorityEntry(bes[i], false, this.frame.basePanel().database(), ce, priorityLevel);
	        }
        }
        ce.end();
        panel.undoManager.addEdit(ce);
    }

    @Override
    public void update() {
        frame.basePanel().markBaseChanged();
        frame.output(Globals.lang("Changed Priority for")+" "+Globals.lang(besLength>0?"entry":"entries"));
    }
}

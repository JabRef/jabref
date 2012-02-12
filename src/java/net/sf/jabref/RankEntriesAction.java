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
public class RankEntriesAction extends AbstractWorker implements ActionListener {

    private JabRefFrame frame;
    final int rankingLevel;
    private JMenuItem menuItem;
    private int besLength = 0;

    public RankEntriesAction(JabRefFrame frame, int rankingLevel) {
        this.frame = frame;
        this.rankingLevel = rankingLevel;

        if (rankingLevel == 0){
        	menuItem = new JMenuItem();
	        menuItem.setText(Globals.lang("Reset Ranking"));
	        menuItem.setOpaque(true);
	        menuItem.addActionListener(this);
        }else{
	        menuItem = new JMenuItem(new ImageIcon(GUIGlobals.getIconUrl("rank" + rankingLevel)));
	        menuItem.setText((Globals.lang("Set Ranking to") + " " + rankingLevel));
	        menuItem.setMnemonic(String.valueOf(rankingLevel+1).charAt(0));
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
        
        if (rankingLevel == 0){
        	for (int i=0; i<bes.length; i++) {
	            Util.resetRanking(bes[i], false, this.frame.basePanel().database(), ce);
	        }
	        ce.end();
	        panel.undoManager.addEdit(ce);
        }else{
	        for (int i=0; i<bes.length; i++) {
	            Util.setRanking(bes[i], false, this.frame.basePanel().database(), ce, rankingLevel);
	        }
	        ce.end();
	        panel.undoManager.addEdit(ce);
        }
    }

    @Override
    public void update() {
        frame.basePanel().markBaseChanged();
        frame.output(Globals.lang("Changed Ranking for")+" "+Globals.lang(besLength>0?"entry":"entries"));
    }
}

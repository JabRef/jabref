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
package net.sf.jabref.util;

/* Mp3dings - manage mp3 meta-information
 * Copyright (C) 2003 Moritz Ringler
 * $Id$
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import net.sf.jabref.Globals;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CaseChangeMenu extends JMenu implements ActionListener{
    static CaseChanger cc = new CaseChanger();
    JMenuItem changeCaseItems[];
    private JTextComponent parent;

    public CaseChangeMenu(JTextComponent opener){
        /* case */
        super(Globals.lang("Change case"));
        parent = opener;
        int m = CaseChanger.getNumModes();
        changeCaseItems = new JMenuItem[m];
        for (int i=0;i<m;i++){
            changeCaseItems[i]=new JMenuItem(Globals.lang(CaseChanger.getModeName(i)));
            changeCaseItems[i].addActionListener(this);
            this.add(changeCaseItems[i]);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object source = (e.getSource());
        for(int i=0, m=CaseChanger.getNumModes(); i<m; i++){
            if(source == changeCaseItems[i]){
                caseChange(i);
                break;
            }
        }
    }

    private void caseChange(int mode){
        parent.setText(CaseChanger.changeCase(parent.getText(), mode));
    }
}

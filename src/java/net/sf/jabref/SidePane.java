/*
Copyright (C) 2003  Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;

public class SidePane extends JPanel {

    Dimension PREFERRED_SIZE = new Dimension
	(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 100);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JScrollPane sp;
    //JButton close = new JButton("X");
    //JSplitPane parent;
    JPanel mainPanel = new JPanel(),
	pan = new JPanel();

    public SidePane() {
	//	parent = _parent;

	setLayout(new BorderLayout());
	mainPanel.setLayout(gbl);
	//setBackground(GUIGlobals.lightGray);//(Color.white);
	//mainPanel.setBackground(GUIGlobals.lightGray);

	/*sp = new JScrollPane
	    (mainPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);*/

	//super.add(sp, BorderLayout.CENTER);
	super.add(mainPanel, BorderLayout.NORTH);
	JPanel pan = new JPanel();
	//pan.setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.red));
	//mainPanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.yellow));
	//setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.green));
	//pan.setBackground(GUIGlobals.lightGray);
        
	super.add(pan, BorderLayout.CENTER);
    }

    public void setComponents(Vector comps) {
      mainPanel.removeAll();
      con.anchor = GridBagConstraints.NORTH;
      con.fill = GridBagConstraints.BOTH;
      con.gridwidth = GridBagConstraints.REMAINDER;
      con.insets = new Insets(1, 1, 1, 1);
      con.gridheight = 1;
      con.weightx = 1;
      con.weighty = 0;

      for (int i=0; i<comps.size(); i++) {
        Component c = (Component)comps.elementAt(i);
        gbl.setConstraints(c, con);
        mainPanel.add(c);
	//System.out.println(c.getPreferredSize().toString());
      }
      con.weighty = 1;
      Component bx = Box.createVerticalGlue();
      gbl.setConstraints(bx, con);
      mainPanel.add(bx);

      revalidate();
      repaint();
    }

    public void remove(Component c) {
	mainPanel.remove(c);
    }

    public Dimension getMaximumSize() {
	return PREFERRED_SIZE;
    }
    
    public Dimension getPreferredSize() {
	return PREFERRED_SIZE;
    }
}

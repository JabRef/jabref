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
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.JToolBar;

public class SidePaneHeader extends JPanel implements ActionListener {

  private JButton close = new JButton(new ImageIcon(GUIGlobals.closeIconFile));
    private JLabel nameLabel, imageIcon;
    private SidePaneComponent parent;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();

    /*
    public SidePaneHeader(String name, URL image, JButton button,
			  JComponent parent_) {

			  }*/

    public SidePaneHeader(String name, URL image, SidePaneComponent parent_) {
	addPart(name, image, parent_);
    }

    private void addPart(String name, URL image, SidePaneComponent parent_) {
	parent = parent_;
	setLayout(gbl);
        setPreferredSize(new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 20));
        setMinimumSize(new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 20));
	//imageIcon = new JLabel(new ImageIcon(image));
	nameLabel = new JLabel(name, new ImageIcon(image),
			       SwingConstants.LEFT);
	//nameLabel.setPreferredSize(new Dimension(70, 24));
        /*AbstractAction close = new AbstractAction("Close", new ImageIcon(GUIGlobals.closeIconFile)) {
          public void actionPerformed(ActionEvent e) {
            parent.hideAway();
          }
        };
	close.putValue(close.SHORT_DESCRIPTION, "Close");
        JToolBar tlb = new JToolBar();
        tlb.setFloatable(false);
        tlb.setMargin(new Insets(0,0,0,0));
        tlb.setSize(20, 20);
        tlb.add(close);*/
  close.setPreferredSize(new Dimension(20, 20));
  close.setMaximumSize(new Dimension(20, 20));
  close.setMinimumSize(new Dimension(20, 20));
  close.addActionListener(this);

	//setBorder(BorderFactory.createEtchedBorder());
	//add(imageIcon, BorderLayout.WEST);
	con.insets = new Insets(2, 0, 0, 0);
	con.gridwidth = 1;
	con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.NONE;
	gbl.setConstraints(nameLabel, con);
	add(nameLabel);
	JPanel pan = new JPanel();
	con.fill = GridBagConstraints.HORIZONTAL;
	con.weightx = 1;
	gbl.setConstraints(pan, con);
	add(pan);
	con.weightx = 0;
	con.fill = GridBagConstraints.NONE;
	con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(close, con);
	add(close);
    }

    public void actionPerformed(ActionEvent e) {
	parent.hideAway(); //setVisible(false);
    }
}

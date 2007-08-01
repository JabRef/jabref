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

public class SidePaneHeader extends JPanel implements ActionListener {

  private JButton close = new JButton(GUIGlobals.getImage("close2"));
    private JLabel nameLabel;
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

    public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D)g;
      Paint oldPaint = g2.getPaint();
      //g2.putColor(Color.red);
      Insets ins = getInsets();
      int width = getWidth() - ins.left - ins.right,
          height = getHeight() - ins.top - ins.bottom;
      //g2.setPaint(new GradientPaint(0, 0, GUIGlobals.gradientGray,
     //                              width, height, GUIGlobals.gradientBlue, false));
     g2.setPaint(new GradientPaint(ins.left, ins.top, GUIGlobals.gradientGray,
                                   width, height, GUIGlobals.gradientBlue, false));
      g2.fillRect(ins.left, ins.top, width-1, height);
      //g2.fillRect(0, 0, 100, 10);
      g2.setPaint(oldPaint);
      //super.paintComponent(g);
    }

    //public boolean isOpaque() { return true; }

    private void addPart(String name, URL image, SidePaneComponent parent_) {
    parent = parent_;
    setLayout(gbl);
        //setPreferredSize(new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 18));
        //setMinimumSize(new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 18));
    //imageIcon = new JLabel(new ImageIcon(image));
    nameLabel = new JLabel(Globals.lang(name), new ImageIcon(image),
                   SwingConstants.LEFT);
//        setBackground(new Color(0, 0, 175)); //SystemColor.activeCaption);

        //close.setOpaque(false);
        nameLabel.setForeground(new Color(230, 230, 230));
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
  //close.setMargin(new Insets(0,0,0,0));
  //close.setRolloverEnabled(true);
  close.setBorder(null);
  close.setOpaque(false);
  close.setPreferredSize(new Dimension(15, 15));
  close.setMaximumSize(new Dimension(15, 15));
  close.setMinimumSize(new Dimension(15, 15));
  close.addActionListener(this);

  //setBorder(BorderFactory.createEtchedBorder());
  //setBorder(BorderFactory.createMatteBorder(1,1,1,2,new Color(150,150,150)));
    //add(imageIcon, BorderLayout.WEST);
    con.insets = new Insets(1, 1, 1, 1);
    con.gridwidth = 1;
    con.anchor = GridBagConstraints.WEST;
    con.fill = GridBagConstraints.NONE;
    gbl.setConstraints(nameLabel, con);
    add(nameLabel);
    JPanel pan = new JPanel();
        pan.setOpaque(false);
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

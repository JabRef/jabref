/*
 Copyright (C) 2004 R. Nagel

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

// created by : r.nagel 04.11.2004
//
// function : supports an underlying text for jcomponents
//
// todo     :
//
// modified :
//


package net.sf.jabref.wizard.text.gui ;

import javax.swing.* ;
import java.awt.Dimension;
import java.awt.*;

public class OverlayPanel
    extends JPanel
{

  private JLabel label ;
  private JComponent overlay ;

  public OverlayPanel(JComponent over, String text)
  {
    overlay = over ;
    overlay.setOpaque(false);

    setBackground( overlay.getBackground());
    label = new JLabel(text) ;
    label.setFont(new Font("dialog", Font.ITALIC, 18));
//    label.setForeground(Color.lightGray);
    label.setForeground( new Color(224, 220, 220) );

    add(overlay) ;
    add(label) ;
  }

  public void paint(Graphics g)
  {
    int len = label.getWidth() ;

    Dimension dim = this.getSize() ;
    if ((dim.height > 25) && (dim.width > len+10))
    {
      int x = (dim.width-len) / 2 ;
      int y = dim.height / 2 ;

      label.setLocation(x, y) ;
      overlay.setLocation(0, 0);
    }

    super.paint(g);
  }

}

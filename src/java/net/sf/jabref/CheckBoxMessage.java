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

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;


public class CheckBoxMessage extends JPanel {
  BorderLayout borderLayout1 = new BorderLayout();
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con = new GridBagConstraints();
  JCheckBox cb;

  public CheckBoxMessage(String message, String cbText, boolean defaultValue) {
    cb = new JCheckBox(cbText, defaultValue);
    setLayout(gbl);
    con.gridwidth = GridBagConstraints.REMAINDER;

    JLabel lab = new JLabel(message+"\n");
    cb.setHorizontalAlignment(JLabel.LEFT);
    gbl.setConstraints(lab, con);
    add(lab);
    con.anchor = GridBagConstraints.WEST;
    con.insets = new Insets(10, 0, 0, 0);
    gbl.setConstraints(cb, con);
    add(cb);
  }

  public boolean isSelected() {
    return cb.isSelected();
  }
}

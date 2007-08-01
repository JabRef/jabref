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

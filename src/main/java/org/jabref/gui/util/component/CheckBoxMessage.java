package org.jabref.gui.util.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class CheckBoxMessage extends JPanel {

    private final JCheckBox cb;


    public CheckBoxMessage(String message, String cbText, boolean defaultValue) {
        cb = new JCheckBox(cbText, defaultValue);
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.gridwidth = GridBagConstraints.REMAINDER;

        JLabel lab = new JLabel(message + '\n');
        cb.setHorizontalAlignment(SwingConstants.LEFT);
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

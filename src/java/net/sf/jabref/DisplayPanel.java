package net.sf.jabref;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 12.mar.2005
 * Time: 14:58:21
 * To change this template use File | Settings | File Templates.
 */
public class DisplayPanel {

    private JPanel panel = new JPanel();
    private BibtexEntry activeEntry = null;

    public DisplayPanel() {
        panel.setLayout(new BorderLayout());
    }

    public void setEntry(BibtexEntry entry) {
        activeEntry = entry;
    }

    public JPanel getPanel() {
        return panel;
    }
}

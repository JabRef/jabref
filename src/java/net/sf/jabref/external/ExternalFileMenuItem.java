package net.sf.jabref.external;

import net.sf.jabref.Util;
import net.sf.jabref.MetaData;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * The menu item used in the popup menu for opening external resources associated
 * with an entry. Shows the resource name and icon given, and adds an action listener
 * to process the request if the user clicks this menu item.
 */
public class ExternalFileMenuItem extends JMenuItem implements ActionListener {

    final String link;
    final MetaData metaData;

    public ExternalFileMenuItem(String name, String link, Icon icon, MetaData metaData) {
        super(name, icon);
        this.link = link;
        this.metaData = metaData;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {

        try {
            Util.openExternalFileAnyFormat(metaData, link);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }
}

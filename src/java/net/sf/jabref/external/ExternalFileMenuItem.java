package net.sf.jabref.external;

import net.sf.jabref.Util;
import net.sf.jabref.MetaData;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.File;

/**
 * The menu item used in the popup menu for opening external resources associated
 * with an entry. Shows the resource name and icon given, and adds an action listener
 * to process the request if the user clicks this menu item.
 */
public class ExternalFileMenuItem extends JMenuItem implements ActionListener {

    final String link;
    final MetaData metaData;
    final ExternalFileType fileType;
    final JabRefFrame frame;

    public ExternalFileMenuItem(JabRefFrame frame, String name, String link, Icon icon, MetaData metaData,
                                ExternalFileType fileType) {
        super(name, icon);
        this.frame = frame;
        this.link = link;
        this.metaData = metaData;
        this.fileType = fileType;
        addActionListener(this);
    }

    public ExternalFileMenuItem(JabRefFrame frame, String name, String link, Icon icon, MetaData metaData) {
        this(frame, name, link, icon, metaData, null);
    }

    public void actionPerformed(ActionEvent e) {
        
        try {

            if (this.fileType != null)
                Util.openExternalFileAnyFormat(frame, metaData, link, fileType);
            else {
                // We don't already know the file type, so we try to deduce it from the extension:
                File file = new File(link);
                // We try to check the extension for the file:
                String name = file.getName();
                int pos = name.indexOf('.');
                String extension = ((pos >= 0) && (pos < name.length() - 1)) ? name.substring(pos + 1)
                    .trim().toLowerCase() : null;
                // Now we know the extension, check if it is one we know about:
                ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt(extension);

                Util.openExternalFileAnyFormat(frame, metaData, link, fileType);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }
}

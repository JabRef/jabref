package net.sf.jabref.external;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JMenuItem;

import net.sf.jabref.*;

/**
 * The menu item used in the popup menu for opening external resources associated
 * with an entry. Shows the resource name and icon given, and adds an action listener
 * to process the request if the user clicks this menu item.
 */
public class ExternalFileMenuItem extends JMenuItem implements ActionListener {

    private BibtexEntry entry;
    final String link;
    final MetaData metaData;
    final ExternalFileType fileType;
    final JabRefFrame frame;

    public ExternalFileMenuItem(JabRefFrame frame, BibtexEntry entry, String name,
                                String link, Icon icon,
                                MetaData metaData,
                                ExternalFileType fileType) {
        super(name, icon);
        this.frame = frame;
        this.entry = entry;
        this.link = link;
        this.metaData = metaData;
        this.fileType = fileType;
        addActionListener(this);
    }

    public ExternalFileMenuItem(JabRefFrame frame, BibtexEntry entry, String name,
                                String link, Icon icon, MetaData metaData) {
        this(frame, entry, name, link, icon, metaData, null);
    }

    public void actionPerformed(ActionEvent e) {
        
        try {
            ExternalFileType type = fileType;
            if (this.fileType == null) {
                // We don't already know the file type, so we try to deduce it from the extension:
                File file = new File(link);
                // We try to check the extension for the file:
                String name = file.getName();
                int pos = name.indexOf('.');
                String extension = ((pos >= 0) && (pos < name.length() - 1)) ? name.substring(pos + 1)
                    .trim().toLowerCase() : null;
                // Now we know the extension, check if it is one we know about:
                type = Globals.prefs.getExternalFileTypeByExt(extension);
            }

            if (type instanceof UnknownExternalFileType)
                Util.openExternalFileUnknown(frame, entry, metaData, link, 
                        (UnknownExternalFileType)type);
            else
                Util.openExternalFileAnyFormat(metaData, link, type);



        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }
}

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
package net.sf.jabref.external;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.gui.desktop.JabRefDesktop;

/**
 * The menu item used in the popup menu for opening external resources associated
 * with an entry. Shows the resource name and icon given, and adds an action listener
 * to process the request if the user clicks this menu item.
 */
public class ExternalFileMenuItem extends JMenuItem implements ActionListener {

    private final BibEntry entry;
    private final String link;
    private final MetaData metaData;
    private ExternalFileType fileType;
    private final JabRefFrame frame;
    private String fieldName;


    public ExternalFileMenuItem(JabRefFrame frame, BibEntry entry, String name,
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

    public ExternalFileMenuItem(JabRefFrame frame, BibEntry entry, String name,
            String link, Icon icon, MetaData metaData, String fieldName) {
        this(frame, entry, name, link, icon, metaData, (ExternalFileType) null);
        this.fieldName = fieldName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean success = openLink();
        if (!success) {
            frame.output(Localization.lang("Unable to open link."));
        }
    }

    public boolean openLink() {
        frame.output(Localization.lang("External viewer called") + ".");
        try {
            ExternalFileType type = fileType;
            if (this.fileType == null) {
                if (this.fieldName != null) {
                    JabRefDesktop.openExternalViewer(frame.getCurrentBasePanel().getBibDatabaseContext().getMetaData(), link, fieldName);
                    return true;
                }
                else {
                    // We don't already know the file type, so we try to deduce it from the extension:
                    File file = new File(link);
                    // We try to check the extension for the file:
                    String name = file.getName();
                    int pos = name.indexOf('.');
                    String extension = (pos >= 0) && (pos < (name.length() - 1)) ? name.substring(pos + 1)
                            .trim().toLowerCase() : null;
                    // Now we know the extension, check if it is one we know about:
                    type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension);
                    fileType = type;
                }
            }

            if (type instanceof UnknownExternalFileType) {
                return JabRefDesktop.openExternalFileUnknown(frame, entry, metaData, link,
                        (UnknownExternalFileType) type);
            } else {
                return JabRefDesktop.openExternalFileAnyFormat(metaData, link, type);
            }

        } catch (IOException e1) {
            // See if we should show an error message concerning the application to open the
            // link with. We check if the file type is set, and if the file type has a non-empty
            // application link. If that link is referred by the error message, we can assume
            // that the problem is in the open-with-application setting:
            if ((fileType != null) && (fileType.getOpenWith() != null)
                    && !fileType.getOpenWith().isEmpty() &&
                    e1.getMessage().contains(fileType.getOpenWith())) {

                JOptionPane.showMessageDialog(frame, Localization.lang("Unable to open link. "
                                        + "The application '%0' associated with the file type '%1' could not be called.",
                                fileType.getOpenWith(), fileType.getName()),
                        Localization.lang("Could not open link"), JOptionPane.ERROR_MESSAGE);
                return false;
            }

            e1.printStackTrace();
        }
        return false;
    }
}

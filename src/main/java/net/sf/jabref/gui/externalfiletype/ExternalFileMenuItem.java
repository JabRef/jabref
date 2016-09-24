package net.sf.jabref.gui.externalfiletype;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The menu item used in the popup menu for opening external resources associated
 * with an entry. Shows the resource name and icon given, and adds an action listener
 * to process the request if the user clicks this menu item.
 */
public class ExternalFileMenuItem extends JMenuItem implements ActionListener {

    private static final Log LOGGER = LogFactory.getLog(ExternalFileMenuItem.class);

    private final BibEntry entry;
    private final String link;
    private final BibDatabaseContext databaseContext;
    private Optional<ExternalFileType> fileType;
    private final JabRefFrame frame;
    private String fieldName;


    public ExternalFileMenuItem(JabRefFrame frame, BibEntry entry, String name, String link, Icon icon,
            BibDatabaseContext databaseContext, Optional<ExternalFileType> fileType) {
        super(name, icon);
        this.frame = frame;
        this.entry = entry;
        this.link = link;
        this.databaseContext = databaseContext;
        this.fileType = fileType;
        addActionListener(this);
    }

    public ExternalFileMenuItem(JabRefFrame frame, BibEntry entry, String name, String link, Icon icon,
            BibDatabaseContext databaseContext, String fieldName) {
        this(frame, entry, name, link, icon, databaseContext, Optional.empty());
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
            Optional<ExternalFileType> type = fileType;
            if (!this.fileType.isPresent()) {
                if (this.fieldName == null) {
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
                } else {
                    JabRefDesktop.openExternalViewer(databaseContext, link, fieldName);
                    return true;
                }
            }

            if (type.isPresent() && (type.get() instanceof UnknownExternalFileType)) {
                return JabRefDesktop.openExternalFileUnknown(frame, entry, databaseContext, link,
                        (UnknownExternalFileType) type.get());
            } else {
                return JabRefDesktop.openExternalFileAnyFormat(databaseContext, link, type);
            }

        } catch (IOException e1) {
            // See if we should show an error message concerning the application to open the
            // link with. We check if the file type is set, and if the file type has a non-empty
            // application link. If that link is referred by the error message, we can assume
            // that the problem is in the open-with-application setting:
            if ((fileType.isPresent()) && (!fileType.get().getOpenWithApplication().isEmpty())
                    && e1.getMessage().contains(fileType.get().getOpenWithApplication())) {

                JOptionPane.showMessageDialog(frame, Localization.lang("Unable to open link. "
                                        + "The application '%0' associated with the file type '%1' could not be called.",
                        fileType.get().getOpenWithApplication(), fileType.get().getName()),
                        Localization.lang("Could not open link"), JOptionPane.ERROR_MESSAGE);
                return false;
            }

            LOGGER.warn("Unable to open link", e1);
        }
        return false;
    }
}

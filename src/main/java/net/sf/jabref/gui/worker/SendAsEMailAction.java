/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.worker;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sends the selected entry as email - by Oliver Kopp
 *
 * It uses the mailto:-mechanism
 *
 * Microsoft Outlook does not support attachments via mailto
 * Therefore, the folder(s), where the file(s) belonging to the entry are stored,
 * are opened. This feature is disabled by default and can be switched on at
 * preferences/external programs
 */
public class SendAsEMailAction extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(SendAsEMailAction.class);

    private String message;
    private final JabRefFrame frame;


    public SendAsEMailAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void run() {
        if (!Desktop.isDesktopSupported()) {
            message = Localization.lang("Error creating email");
            return;
        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel == null) {
            return;
        }
        if (panel.getSelectedEntries().isEmpty()) {
            message = Localization.lang("This operation requires one or more entries to be selected.");
            return;
        }

        StringWriter sw = new StringWriter();
        List<BibEntry> bes = panel.getSelectedEntries();

        // write the entries using sw, which is used later to form the email content
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), true);

        for (BibEntry entry : bes) {
            try {
                bibtexEntryWriter.write(entry, sw, panel.getBibDatabaseContext().getMode());
            } catch (IOException e) {
                LOGGER.warn("Problem creating BibTeX file for mailing.", e);
            }
        }

        List<String> attachments = new ArrayList<>();

        // open folders is needed to indirectly support email programs, which cannot handle
        //   the unofficial "mailto:attachment" property
        boolean openFolders = JabRefPreferences.getInstance().getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES);

        List<File> fileList = FileUtil.getListOfLinkedFiles(bes,
                frame.getCurrentBasePanel().getBibDatabaseContext().getFileDirectory());
        for (File f : fileList) {
            attachments.add(f.getPath());
            if (openFolders) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(f.getAbsolutePath());
                } catch (IOException e) {
                    LOGGER.debug("Cannot open file", e);
                }
            }
        }

        String mailTo = "?Body=".concat(sw.getBuffer().toString());
        mailTo = mailTo.concat("&Subject=");
        mailTo = mailTo.concat(JabRefPreferences.getInstance().get(JabRefPreferences.EMAIL_SUBJECT));
        for (String path : attachments) {
            mailTo = mailTo.concat("&Attachment=\"").concat(path);
            mailTo = mailTo.concat("\"");
        }

        URI uriMailTo;
        try {
            uriMailTo = new URI("mailto", mailTo, null);
        } catch (URISyntaxException e1) {
            message = Localization.lang("Error creating email");
            LOGGER.warn(message, e1);
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.mail(uriMailTo);
        } catch (IOException e) {
            message = Localization.lang("Error creating email");
            LOGGER.warn(message, e);
            return;
        }

        message = String.format("%s: %d", Localization.lang("Entries added to an email"), bes.size());
    }

    @Override
    public void update() {
        frame.output(message);
    }

}

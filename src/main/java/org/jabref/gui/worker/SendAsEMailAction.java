package org.jabref.gui.worker;

import java.awt.Desktop;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SendAsEMailAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendAsEMailAction.class);
    private final JabRefFrame frame;

    public SendAsEMailAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void action() {
        BackgroundTask.wrap(this::sendEmail)
                      .onSuccess(frame.getDialogService()::notify)
                      .onFailure(e -> {
                          String message = Localization.lang("Error creating email");
                          LOGGER.warn(message, e);
                          frame.getDialogService().notify(message);
                      })
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private String sendEmail() throws Exception {
        if (!Desktop.isDesktopSupported()) {
            return Localization.lang("Error creating email");
        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel == null) {
            throw new IllegalStateException("Base panel is not available.");
        }
        if (panel.getSelectedEntries().isEmpty()) {
            return Localization.lang("This operation requires one or more entries to be selected.");
        }

        StringWriter sw = new StringWriter();
        List<BibEntry> bes = panel.getSelectedEntries();

        // write the entries using sw, which is used later to form the email content
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), Globals.entryTypesManager);

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

        List<Path> fileList = FileUtil.getListOfLinkedFiles(bes, frame.getCurrentBasePanel().getBibDatabaseContext()
                                                                      .getFileDirectoriesAsPaths(Globals.prefs.getFilePreferences()));
        for (Path f : fileList) {
            attachments.add(f.toAbsolutePath().toString());
            if (openFolders) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(f.toAbsolutePath());
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

        URI uriMailTo = new URI("mailto", mailTo, null);

        Desktop desktop = Desktop.getDesktop();
        desktop.mail(uriMailTo);

        return String.format("%s: %d", Localization.lang("Entries added to an email"), bes.size());
    }
}

package org.jabref.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the selected entry as email
 * <p>
 * It uses the mailto:-mechanism
 * <p>
 * Microsoft Outlook does not support attachments via mailto
 * Therefore, the folder(s), where the file(s) belonging to the entry are stored,
 * are opened. This feature is disabled by default and can be switched on at
 * preferences/external programs
 */
public class SendAsEMailAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendAsEMailAction.class);
    private DialogService dialogService;
    private StateManager stateManager;

    public SendAsEMailAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(this::sendEmail)
                      .onSuccess(dialogService::notify)
                      .onFailure(e -> {
                          String message = Localization.lang("Error creating email");
                          LOGGER.warn(message, e);
                          dialogService.notify(message);
                      })
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private String sendEmail() throws Exception {
        if (!Desktop.isDesktopSupported() || stateManager.getActiveDatabase().isEmpty()) {
            return Localization.lang("Error creating email");
        }

        if (stateManager.getSelectedEntries().isEmpty()) {
            return Localization.lang("This operation requires one or more entries to be selected.");
        }

        StringWriter rawEntries = new StringWriter();
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // write the entries using sw, which is used later to form the email content
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(Globals.prefs.getFieldWriterPreferences()), Globals.entryTypesManager);

        for (BibEntry entry : entries) {
            try {
                bibtexEntryWriter.write(entry, rawEntries, databaseContext.getMode());
            } catch (IOException e) {
                LOGGER.warn("Problem creating BibTeX file for mailing.", e);
            }
        }

        List<String> attachments = new ArrayList<>();

        // open folders is needed to indirectly support email programs, which cannot handle
        //   the unofficial "mailto:attachment" property
        boolean openFolders = JabRefPreferences.getInstance().getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES);

        List<Path> fileList = FileUtil.getListOfLinkedFiles(entries, databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFilePreferences()));
        for (Path path : fileList) {
            attachments.add(path.toAbsolutePath().toString());
            if (openFolders) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(path.toAbsolutePath());
                } catch (IOException e) {
                    LOGGER.debug("Cannot open file", e);
                }
            }
        }

        String mailTo = "?Body=".concat(rawEntries.getBuffer().toString());
        mailTo = mailTo.concat("&Subject=");
        mailTo = mailTo.concat(JabRefPreferences.getInstance().get(JabRefPreferences.EMAIL_SUBJECT));
        for (String path : attachments) {
            mailTo = mailTo.concat("&Attachment=\"").concat(path);
            mailTo = mailTo.concat("\"");
        }

        URI uriMailTo = new URI("mailto", mailTo, null);

        Desktop desktop = Desktop.getDesktop();
        desktop.mail(uriMailTo);

        return String.format("%s: %d", Localization.lang("Entries added to an email"), entries.size());
    }
}

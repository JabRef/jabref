package org.jabref.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

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
@AllowedToUseAwt("Requires AWT to send an email")
public abstract class SendAsEMailAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendAsEMailAction.class);
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    public SendAsEMailAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
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
        BibWriter bibWriter = new BibWriter(rawEntries, OS.NEWLINE);
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        List<BibEntry> entries = stateManager.getSelectedEntries();

        // write the entries via this writer to "rawEntries" (being a StringWriter), which  used later to form the email content
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(preferencesService.getFieldPreferences()), Globals.entryTypesManager);

        for (BibEntry entry : entries) {
            try {
                bibtexEntryWriter.write(entry, bibWriter, databaseContext.getMode());
            } catch (IOException e) {
                LOGGER.warn("Problem creating BibTeX file for mailing.", e);
            }
        }

        List<String> attachments = new ArrayList<>();

        // open folders is needed to indirectly support email programs, which cannot handle
        //   the unofficial "mailto:attachment" property
        boolean openFolders = preferencesService.getExternalApplicationsPreferences().shouldAutoOpenEmailAttachmentsFolder();

        List<Path> fileList = FileUtil.getListOfLinkedFiles(entries, databaseContext.getFileDirectories(preferencesService.getFilePreferences()));
        for (Path path : fileList) {
            attachments.add(path.toAbsolutePath().toString());
            if (openFolders) {
                try {
                    JabRefDesktop.openFolderAndSelectFile(path.toAbsolutePath(), preferencesService, dialogService);
                } catch (IOException e) {
                    LOGGER.debug("Cannot open file", e);
                }
            }
        }

        URI uriMailTo = getUriMailTo(rawEntries, attachments);

        Desktop desktop = Desktop.getDesktop();
        desktop.mail(uriMailTo);

        return String.format("%s: %d", Localization.lang("Entries added to an email"), entries.size());
    }

    private URI getUriMailTo(StringWriter rawEntries, List<String> attachments) throws URISyntaxException {
        StringBuilder mailTo = new StringBuilder();

        mailTo.append(getEmailAddress());
        mailTo.append("?Body=").append(getBody(rawEntries));
        mailTo.append("&Subject=").append(getSubject());

        for (String path : attachments) {
            mailTo.append("&Attachment=\"").append(path);
            mailTo.append("\"");
        }

        return new URI("mailto", mailTo.toString(), null);
    }

    protected abstract String getEmailAddress();

    protected abstract String getSubject();

    protected abstract String getBody(StringWriter rawEntries);
}

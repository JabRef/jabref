package org.jabref.gui.frame;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

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
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;

    public SendAsEMailAction(DialogService dialogService,
                             GuiPreferences preferences,
                             StateManager stateManager,
                             TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
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
                      .executeWith(taskExecutor);
    }

    private String sendEmail() throws Exception {
        if (!Desktop.isDesktopSupported() || stateManager.getActiveDatabase().isEmpty()) {
            return Localization.lang("Error creating email");
        }

        if (stateManager.getSelectedEntries().isEmpty()) {
            return Localization.lang("This operation requires one or more entries to be selected.");
        }

        List<BibEntry> entries = stateManager.getSelectedEntries();
        URI uriMailTo = getUriMailTo(entries);

        Desktop desktop = Desktop.getDesktop();
        desktop.mail(uriMailTo);

        return "%s: %d".formatted(Localization.lang("Entries added to an email"), entries.size());
    }

    private URI getUriMailTo(List<BibEntry> entries) throws URISyntaxException {
        StringBuilder mailTo = new StringBuilder();

        mailTo.append(getEmailAddress());
        mailTo.append("?Body=").append(getBody());
        mailTo.append("&Subject=").append(getSubject());

        List<String> attachments = getAttachments(entries);
        for (String path : attachments) {
            mailTo.append("&Attachment=\"").append(path);
            mailTo.append("\"");
        }

        return new URI("mailto", mailTo.toString(), null);
    }

    private List<String> getAttachments(List<BibEntry> entries) {
        // open folders is needed to indirectly support email programs, which cannot handle
        //   the unofficial "mailto:attachment" property
        boolean openFolders = preferences.getExternalApplicationsPreferences().shouldAutoOpenEmailAttachmentsFolder();

        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        List<Path> fileList = FileUtil.getListOfLinkedFiles(entries, databaseContext.getFileDirectories(preferences.getFilePreferences()));

        List<String> attachments = new ArrayList<>();
        for (Path path : fileList) {
            attachments.add(path.toAbsolutePath().toString());
            if (openFolders) {
                try {
                    NativeDesktop.openFolderAndSelectFile(path.toAbsolutePath(), preferences.getExternalApplicationsPreferences(), dialogService);
                } catch (IOException e) {
                    LOGGER.debug("Cannot open file", e);
                }
            }
        }
        return attachments;
    }

    protected abstract String getEmailAddress();

    protected abstract String getSubject();

    protected abstract String getBody();
}

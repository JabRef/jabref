package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartLiteratureReviewAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartLiteratureReviewAction.class);
    private final JabRefFrame frame;
    private final DialogService dialogService;

    public StartLiteratureReviewAction(JabRefFrame frame) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
    }

    @Override
    public void execute() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(getInitialDirectory())
                .build();

        Optional<Path> studyDefinitionFile = dialogService.showFileOpenDialog(fileDialogConfiguration);
        if (studyDefinitionFile.isEmpty()) {
            // Do nothing if selection was canceled
            return;
        }
        final Crawler crawler;
        try {
            crawler = new Crawler(studyDefinitionFile.get(), Globals.getFileUpdateMonitor(), JabRefPreferences.getInstance().getSavePreferences(), Globals.entryTypesManager);
        } catch (IOException | ParseException | GitAPIException e) {
            LOGGER.info("Error during reading of study definition file.", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Error during reading of study definition file."));
            return;
        }
        BackgroundTask.wrap(() -> {
            crawler.performCrawl();
            return 0; // Return any value to make this a callable instead of a runnable. This allows throwing exceptions.
        })
                      .onFailure(e -> {
                          LOGGER.info("Error during persistence of crawling results.");
                          dialogService.showErrorDialogAndWait(Localization.lang("Error during persistence of crawling results."), e);
                      })
                      .onSuccess(unused -> new OpenDatabaseAction(frame).openFile(Path.of(studyDefinitionFile.get().getParent().toString(), "studyResult.bib"), true))
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    /**
     * @return Path of current panel database directory or the working directory
     */
    private Path getInitialDirectory() {
        if (frame.getBasePanelCount() == 0) {
            return Globals.prefs.getWorkingDir();
        } else {
            Optional<Path> databasePath = frame.getCurrentLibraryTab().getBibDatabaseContext().getDatabasePath();
            return databasePath.map(Path::getParent).orElse(Globals.prefs.getWorkingDir());
        }
    }
}

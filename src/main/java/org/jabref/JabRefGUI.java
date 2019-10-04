package org.jabref;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.BasePanel;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.help.VersionWorker;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.preferences.JabRefPreferences;

import impl.org.controlsfx.skin.DecorationPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGUI {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGUI.class);

    private static JabRefFrame mainFrame;

    private final List<ParserResult> bibDatabases;
    private final boolean isBlank;
    private final List<ParserResult> failed = new ArrayList<>();
    private final List<ParserResult> toOpenTab = new ArrayList<>();

    public JabRefGUI(Stage mainStage, List<ParserResult> databases, boolean isBlank) {
        this.bibDatabases = databases;
        this.isBlank = isBlank;
        mainFrame = new JabRefFrame(mainStage);

        openWindow(mainStage);
        new VersionWorker(Globals.BUILD_INFO.getVersion(), Globals.prefs.getVersionPreferences().getIgnoredVersion(), mainFrame.getDialogService(), Globals.TASK_EXECUTOR)
                .checkForNewVersionDelayed();
    }

    private void openWindow(Stage mainStage) {
        applyFontRenderingTweak();

        GUIGlobals.init();

        LOGGER.debug("Initializing frame");
        mainFrame.init();

        // Restore window location and/or maximised state
        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            mainStage.setMaximized(true);
        } else {
            mainStage.setX(Globals.prefs.getDouble(JabRefPreferences.POS_X));
            mainStage.setY(Globals.prefs.getDouble(JabRefPreferences.POS_Y));
            mainStage.setWidth(Globals.prefs.getDouble(JabRefPreferences.SIZE_X));
            mainStage.setHeight(Globals.prefs.getDouble(JabRefPreferences.SIZE_Y));
        }

        // We create a decoration pane ourselves for performance reasons
        // (otherwise it has to be injected later, leading to a complete redraw/relayout of the complete scene)
        DecorationPane root = new DecorationPane();
        root.getChildren().add(JabRefGUI.mainFrame);

        Scene scene = new Scene(root, 800, 800);
        Globals.getThemeLoader().installCss(scene, Globals.prefs);
        mainStage.setTitle(JabRefFrame.FRAME_TITLE);
        mainStage.getIcons().addAll(IconTheme.getLogoSetFX());
        mainStage.setScene(scene);
        mainStage.show();

        mainStage.setOnCloseRequest(event -> {
            saveWindowState(mainStage);
            boolean reallyQuit = mainFrame.quit();
            if (!reallyQuit) {
                event.consume();
            }
        });

        Platform.runLater(this::openDatabases);
    }

    private void openDatabases() {
        // If the option is enabled, open the last edited libraries, if any.
        if (!isBlank && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            openLastEditedDatabases();
        }

        // passed file (we take the first one) should be focused
        String focusedFile = bibDatabases.stream()
                                         .findFirst()
                                         .flatMap(ParserResult::getFile)
                                         .map(File::getAbsolutePath)
                                         .orElse(Globals.prefs.get(JabRefPreferences.LAST_FOCUSED));

        // Add all bibDatabases databases to the frame:
        boolean first = false;
        if (!bibDatabases.isEmpty()) {
            for (Iterator<ParserResult> parserResultIterator = bibDatabases.iterator(); parserResultIterator.hasNext();) {
                ParserResult pr = parserResultIterator.next();
                // Define focused tab
                if (pr.getFile().filter(path -> path.getAbsolutePath().equals(focusedFile)).isPresent()) {
                    first = true;
                }

                if (pr.isInvalid()) {
                    failed.add(pr);
                    parserResultIterator.remove();
                } else if (pr.getDatabase().isShared()) {
                    try {
                        new SharedDatabaseUIManager(mainFrame).openSharedDatabaseFromParserResult(pr);
                    } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                            NotASharedDatabaseException e) {
                        pr.getDatabaseContext().clearDatabaseFile(); // do not open the original file
                        pr.getDatabase().clearSharedDatabaseID();

                        LOGGER.error("Connection error", e);
                        mainFrame.getDialogService().showErrorDialogAndWait(
                                Localization.lang("Connection error"),
                                Localization.lang("A local copy will be opened."),
                                e);
                    }
                    toOpenTab.add(pr);
                } else if (pr.toOpenTab()) {
                    // things to be appended to an opened tab should be done after opening all tabs
                    // add them to the list
                    toOpenTab.add(pr);
                } else {
                    mainFrame.addParserResult(pr, first);
                    first = false;
                }
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            mainFrame.addParserResult(pr, first);
            first = false;
        }

        for (ParserResult pr : failed) {
            String message = Localization.lang("Error opening file '%0'.", pr.getFile().get().getName()) + "\n"
                    + pr.getErrorMessage();

            mainFrame.getDialogService().showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }

        // Display warnings, if any
        int tabNumber = 0;
        for (ParserResult pr : bibDatabases) {
            ParserResultWarningDialog.showParserResultWarningDialog(pr, mainFrame, tabNumber++);
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loadedDatabases, but not to getBasePanelCount()

        for (int i = 0; (i < bibDatabases.size()) && (i < mainFrame.getBasePanelCount()); i++) {
            ParserResult pr = bibDatabases.get(i);
            BasePanel panel = mainFrame.getBasePanelAt(i);
            OpenDatabaseAction.performPostOpenActions(panel, pr);
        }

        LOGGER.debug("Finished adding panels");
    }

    private void saveWindowState(Stage mainStage) {
        Globals.prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, mainStage.isMaximized());
        Globals.prefs.putDouble(JabRefPreferences.POS_X, mainStage.getX());
        Globals.prefs.putDouble(JabRefPreferences.POS_Y, mainStage.getY());
        Globals.prefs.putDouble(JabRefPreferences.SIZE_X, mainStage.getWidth());
        Globals.prefs.putDouble(JabRefPreferences.SIZE_Y, mainStage.getHeight());
    }

    private void openLastEditedDatabases() {
        if (Globals.prefs.get(JabRefPreferences.LAST_EDITED) == null) {
            return;
        }
        List<String> lastFiles = Globals.prefs.getStringList(JabRefPreferences.LAST_EDITED);

        for (String fileName : lastFiles) {
            File dbFile = new File(fileName);

            // Already parsed via command line parameter, e.g., "jabref.jar somefile.bib"
            if (isLoaded(dbFile) || !dbFile.exists()) {
                continue;
            }

            if (BackupManager.checkForBackupFile(dbFile.toPath())) {
                BackupUIManager.showRestoreBackupDialog(mainFrame.getDialogService(), dbFile.toPath());
            }

            ParserResult parsedDatabase = OpenDatabase.loadDatabase(fileName,
                    Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());

            if (parsedDatabase.isEmpty()) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + dbFile.getPath() + "'");
            } else {
                bibDatabases.add(parsedDatabase);
            }
        }
    }

    private boolean isLoaded(File fileToOpen) {
        for (ParserResult pr : bibDatabases) {
            if (pr.getFile().isPresent() && pr.getFile().get().equals(fileToOpen)) {
                return true;
            }
        }
        return false;
    }

    private void applyFontRenderingTweak() {
        // On Linux, Java FX fonts look blurry per default. This can be improved by using a non-default rendering setting.
        // See https://github.com/woky/javafx-hates-linux
        if (Globals.prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK)) {
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }
    }

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }
}

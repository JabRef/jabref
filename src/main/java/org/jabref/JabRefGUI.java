package org.jabref;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.worker.VersionWorker;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.Version;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGUI {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGUI.class);

    private static JabRefFrame mainFrame;

    private final List<ParserResult> bibDatabases;
    private final boolean isBlank;
    private final List<ParserResult> failed = new ArrayList<>();
    private final List<ParserResult> toOpenTab = new ArrayList<>();
    private final DialogService dialogService;

    private final String focusedFile;

    public JabRefGUI(Stage mainStage, List<ParserResult> argsDatabases, boolean isBlank) {
        this.bibDatabases = argsDatabases;
        this.isBlank = isBlank;
        this.dialogService = new FXDialogService(mainStage);

        // passed file (we take the first one) should be focused
        focusedFile = argsDatabases.stream()
                                   .findFirst()
                                   .flatMap(ParserResult::getFile)
                                   .map(File::getAbsolutePath)
                                   .orElse(Globals.prefs.get(JabRefPreferences.LAST_FOCUSED));

        openWindow(mainStage);
        JabRefGUI.checkForNewVersion(false);
    }

    public static void checkForNewVersion(boolean manualExecution) {
        Version toBeIgnored = Globals.prefs.getVersionPreferences().getIgnoredVersion();
        Version currentVersion = Globals.BUILD_INFO.getVersion();
        new VersionWorker(JabRefGUI.getMainFrame(), manualExecution, currentVersion, toBeIgnored).execute();
    }

    private void openWindow(Stage mainStage) {
        // Set antialiasing on everywhere. This only works in JRE >= 1.5.
        // Or... it doesn't work, period.
        // TODO test and maybe remove this! I found this commented out with no additional info ( payload@lavabit.com )
        // Enabled since JabRef 2.11 beta 4
        System.setProperty("swing.aatext", "true");
        // Default is "on".
        // "lcd" instead of "on" because of http://wiki.netbeans.org/FaqFontRendering and http://docs.oracle.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        // look and feel. This MUST be the first thing to do before loading any Swing-specific code!
        setLookAndFeel();

        // If the option is enabled, open the last edited libraries, if any.
        if (!isBlank && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            openLastEditedDatabases();
        }

        GUIGlobals.init();

        LOGGER.debug("Initializing frame");
        JabRefGUI.mainFrame = new JabRefFrame(mainStage);

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
                        dialogService.showErrorDialogAndWait(
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
                    JabRefGUI.getMainFrame().addParserResult(pr, first);
                    first = false;
                }
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            JabRefGUI.getMainFrame().addParserResult(pr, first);
            first = false;
        }

        // If we are set to remember the window location, we also remember the maximised
        // state. This needs to be set after the window has been made visible, so we
        // do it here:
        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            mainStage.setMaximized(true);
        } else {
            mainStage.setX(Globals.prefs.getDouble(JabRefPreferences.POS_X));
            mainStage.setY(Globals.prefs.getDouble(JabRefPreferences.POS_Y));
            mainStage.setWidth(Globals.prefs.getDouble(JabRefPreferences.SIZE_X));
            mainStage.setHeight(Globals.prefs.getDouble(JabRefPreferences.SIZE_Y));
        }

        Scene scene = new Scene(JabRefGUI.mainFrame, 800, 800);
        Globals.getThemeLoader().installBaseCss(scene, Globals.prefs);
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

        for (ParserResult pr : failed) {
            String message = Localization.lang("Error opening file '%0'.", pr.getFile().get().getName()) + "\n"
                             + pr.getErrorMessage();

            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);

        }

        // Display warnings, if any
        int tabNumber = 0;
        for (ParserResult pr : bibDatabases) {
            ParserResultWarningDialog.showParserResultWarningDialog(pr, JabRefGUI.getMainFrame(), tabNumber++);
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loadedDatabases, but not to getBasePanelCount()

        for (int i = 0; (i < bibDatabases.size()) && (i < JabRefGUI.getMainFrame().getBasePanelCount()); i++) {
            ParserResult pr = bibDatabases.get(i);
            BasePanel panel = JabRefGUI.getMainFrame().getBasePanelAt(i);
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
                BackupUIManager.showRestoreBackupDialog(dialogService, dbFile.toPath());
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

    private void setLookAndFeel() {
        // On Linux, Java FX fonts look blurry per default. This can be improved by using a non-default rendering
        // setting. See https://github.com/woky/javafx-hates-linux
        if (Globals.prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK)) {
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }
    }

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }

    // Only used for testing, other than that do NOT set the mainFrame...
    public static void setMainFrame(JabRefFrame mainFrame) {
        JabRefGUI.mainFrame = mainFrame;
    }
}

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
package net.sf.jabref.importer;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.exporter.SaveSession;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.ParserResultWarningDialog;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.migrations.FileLinksUpgradeWarning;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// The action concerned with opening an existing database.

public class OpenDatabaseAction extends MnemonicAwareAction {

    private static final Log LOGGER = LogFactory.getLog(OpenDatabaseAction.class);

    private final boolean showDialog;
    private final JabRefFrame frame;

    // List of actions that may need to be called after opening the file. Such as
    // upgrade actions etc. that may depend on the JabRef version that wrote the file:
    private static final List<PostOpenAction> POST_OPEN_ACTIONS = new ArrayList<>();

    static {
        // Add the action for checking for new custom entry types loaded from the bib file:
        POST_OPEN_ACTIONS.add(new CheckForNewEntryTypesAction());
        // Add the action for converting legacy entries in ExplicitGroup
        POST_OPEN_ACTIONS.add(new ConvertLegacyExplicitGroups());
        // Add the action for the new external file handling system in version 2.3:
        POST_OPEN_ACTIONS.add(new FileLinksUpgradeWarning());
        // Add the action for warning about and handling duplicate BibTeX keys:
        POST_OPEN_ACTIONS.add(new HandleDuplicateWarnings());
    }

    public OpenDatabaseAction(JabRefFrame frame, boolean showDialog) {
        super(IconTheme.JabRefIcon.OPEN.getIcon());
        this.frame = frame;
        this.showDialog = showDialog;
        putValue(Action.NAME, Localization.menuTitle("Open database"));
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.OPEN_DATABASE));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Open BibTeX database"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<File> filesToOpen = new ArrayList<>();

        if (showDialog) {
            List<String> chosenStrings = FileDialogs.getMultipleFiles(frame,
                    new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)), ".bib", true);
            for (String chosen : chosenStrings) {
                if (chosen != null) {
                    filesToOpen.add(new File(chosen));
                }
            }
        } else {
            LOGGER.info(Action.NAME + " " + e.getActionCommand());
            filesToOpen.add(new File(StringUtil.getCorrectFileName(e.getActionCommand(), "bib")));
        }

        openFiles(filesToOpen, true);
    }

    class OpenItSwingHelper implements Runnable {

        private final BasePanel basePanel;
        private final boolean raisePanel;

        OpenItSwingHelper(BasePanel basePanel, boolean raisePanel) {
            this.basePanel = basePanel;
            this.raisePanel = raisePanel;
        }

        @Override
        public void run() {
            frame.addTab(basePanel, raisePanel);

        }
    }

    /**
     * Opens the given file. If null or 404, nothing happens
     *
     * @param file the file, may be null or not existing
     */
    public void openFile(File file, boolean raisePanel) {
        List<File> filesToOpen = new ArrayList<>();
        filesToOpen.add(file);
        openFiles(filesToOpen, raisePanel);
    }

    public void openFilesAsStringList(List<String> fileNamesToOpen, boolean raisePanel) {
        List<File> filesToOpen = new ArrayList<>();
        for (String fileName : fileNamesToOpen) {
            filesToOpen.add(new File(fileName));
        }
        openFiles(filesToOpen, raisePanel);
    }

    /**
     * Opens the given files. If one of it is null or 404, nothing happens
     *
     * @param filesToOpen the filesToOpen, may be null or not existing
     */
    public void openFiles(List<File> filesToOpen, boolean raisePanel) {
        BasePanel toRaise = null;
        int initialCount = filesToOpen.size();
        int removed = 0;

        // Check if any of the files are already open:
        for (Iterator<File> iterator = filesToOpen.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
                BasePanel basePanel = frame.getBasePanelAt(i);
                if ((basePanel.getBibDatabaseContext().getDatabaseFile() != null) && basePanel.getBibDatabaseContext().getDatabaseFile().equals(file)) {
                    iterator.remove();
                    removed++;
                    // See if we removed the final one. If so, we must perhaps
                    // raise the BasePanel in question:
                    if (removed == initialCount) {
                        toRaise = basePanel;
                    }
                    // no more bps to check, we found a matching one
                    break;
                }
            }
        }

        // Run the actual open in a thread to prevent the program
        // locking until the file is loaded.
        if (!filesToOpen.isEmpty()) {
            final List<File> theFiles = Collections.unmodifiableList(filesToOpen);
            JabRefExecutorService.INSTANCE.execute((Runnable) () -> {
                for (File theFile : theFiles) {
                    openTheFile(theFile, raisePanel);
                }
            });
            for (File theFile : theFiles) {
                frame.getFileHistory().newFile(theFile.getPath());
            }
        }
        // If no files are remaining to open, this could mean that a file was
        // already open. If so, we may have to raise the correct tab:
        else if (toRaise != null) {
            frame.output(Localization.lang("File '%0' is already open.", toRaise.getBibDatabaseContext().getDatabaseFile().getPath()));
            frame.getTabbedPane().setSelectedComponent(toRaise);
        }

        frame.output(Localization.lang("Files opened") + ": " + (filesToOpen.size()));
    }

    /**
     * @param file the file, may be null or not existing
     */
    private void openTheFile(File file, boolean raisePanel) {
        if ((file != null) && file.exists()) {
            File fileToLoad = file;
            frame.output(Localization.lang("Opening") + ": '" + file.getPath() + "'");
            boolean tryingAutosave = false;
            boolean autoSaveFound = AutoSaveManager.newerAutoSaveExists(file);
            if (autoSaveFound && !Globals.prefs.getBoolean(JabRefPreferences.PROMPT_BEFORE_USING_AUTOSAVE)) {
                // We have found a newer autosave, and the preferences say we should load
                // it without prompting, so we replace the fileToLoad:
                fileToLoad = AutoSaveManager.getAutoSaveFile(file);
                tryingAutosave = true;
            } else if (autoSaveFound) {
                // We have found a newer autosave, but we are not allowed to use it without
                // prompting.
                int answer = JOptionPane.showConfirmDialog(null,
                        "<html>" + Localization
                                .lang("An autosave file was found for this database. This could indicate "
                                        + "that JabRef didn't shut down cleanly last time the file was used.")
                                + "<br>" + Localization.lang("Do you want to recover the database from the autosave file?")
                                + "</html>", Localization.lang("Recover from autosave"), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    fileToLoad = AutoSaveManager.getAutoSaveFile(file);
                    tryingAutosave = true;
                }
            }

            boolean done = false;
            while (!done) {
                String fileName = file.getPath();
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getPath());
                // Should this be done _after_ we know it was successfully opened?

                if (FileBasedLock.hasLockFile(file)) {
                    long modificationTIme = FileBasedLock.getLockFileTimeStamp(file);
                    if ((modificationTIme != -1)
                            && ((System.currentTimeMillis() - modificationTIme) > SaveSession.LOCKFILE_CRITICAL_AGE)) {
                        // The lock file is fairly old, so we can offer to "steal" the file:
                        int answer = JOptionPane.showConfirmDialog(null,
                                "<html>" + Localization.lang("Error opening file") + " '" + fileName + "'. "
                                        + Localization.lang("File is locked by another JabRef instance.") + "<p>"
                                        + Localization.lang("Do you want to override the file lock?"),
                                Localization.lang("File locked"), JOptionPane.YES_NO_OPTION);
                        if (answer == JOptionPane.YES_OPTION) {
                            FileBasedLock.deleteLockFile(file);
                        } else {
                            return;
                        }
                    } else if (!FileBasedLock.waitForFileLock(file, 10)) {
                        JOptionPane.showMessageDialog(null,
                                Localization.lang("Error opening file") + " '" + fileName + "'. "
                                        + Localization.lang("File is locked by another JabRef instance."),
                                Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                }

                Charset encoding = Globals.prefs.getDefaultEncoding();
                ParserResult result;
                String errorMessage = null;
                try {
                    result = OpenDatabaseAction.loadDatabase(fileToLoad, encoding);
                } catch (IOException ex) {
                    LOGGER.error("Error loading database " + fileToLoad, ex);
                    result = ParserResult.getNullResult();
                }
                if (result.isNullResult()) {
                    JOptionPane.showMessageDialog(null, Localization.lang("Error opening file") + " '" + fileName + "'",
                            Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);

                    String message = "<html>" + errorMessage + "<p>"
                            + (tryingAutosave ? Localization.lang(
                                    "Error opening autosave of '%0'. Trying to load '%0' instead.",
                                    file.getName()) : ""/*Globals.lang("Error opening file '%0'.", file.getName())*/)
                            + "</html>";
                    JOptionPane.showMessageDialog(null, message, Localization.lang("Error opening file"),
                            JOptionPane.ERROR_MESSAGE);

                    if (tryingAutosave) {
                        tryingAutosave = false;
                        fileToLoad = file;
                    } else {
                        done = true;
                    }
                    continue;
                } else {
                    done = true;
                }

                final BasePanel panel = addNewDatabase(result, file, raisePanel);
                if (tryingAutosave) {
                    panel.markNonUndoableBaseChanged();
                }

                // After adding the database, go through our list and see if
                // any post open actions need to be done. For instance, checking
                // if we found new entry types that can be imported, or checking
                // if the database contents should be modified due to new features
                // in this version of JabRef:
                final ParserResult finalReferenceToResult = result;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        OpenDatabaseAction.performPostOpenActions(panel, finalReferenceToResult, true);
                    }
                });
            }

        }
    }

    /**
     * Go through the list of post open actions, and perform those that need to be performed.
     *
     * @param panel  The BasePanel where the database is shown.
     * @param result The result of the bib file parse operation.
     */
    public static void performPostOpenActions(BasePanel panel, ParserResult result, boolean mustRaisePanel) {
        for (PostOpenAction action : OpenDatabaseAction.POST_OPEN_ACTIONS) {
            if (action.isActionNecessary(result)) {
                if (mustRaisePanel) {
                    panel.frame().getTabbedPane().setSelectedComponent(panel);
                }
                action.performAction(panel, result);
            }
        }
    }

    public BasePanel addNewDatabase(ParserResult result, final File file, boolean raisePanel) {

        String fileName = file.getPath();
        BibDatabase database = result.getDatabase();
        MetaData meta = result.getMetaData();

        if (result.hasWarnings()) {
            JabRefExecutorService.INSTANCE.execute(() -> ParserResultWarningDialog.showParserResultWarningDialog(result, frame));
        }

        Defaults defaults = new Defaults(BibDatabaseMode.fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
        BasePanel basePanel = new BasePanel(frame, new BibDatabaseContext(database, meta, file, defaults), result.getEncoding());

        // file is set to null inside the EventDispatcherThread
        SwingUtilities.invokeLater(new OpenItSwingHelper(basePanel, raisePanel));

        frame.output(Localization.lang("Opened database") + " '" + fileName + "' " + Localization.lang("with") + " "
                + database.getEntryCount() + " " + Localization.lang("entries") + ".");

        return basePanel;
    }

    /**
     * Opens a new database.
     */
    public static ParserResult loadDatabase(File fileToOpen, Charset defaultEncoding) throws IOException {

        // We want to check if there is a JabRef signature in the file, because that would tell us
        // which character encoding is used. However, to read the signature we must be using a compatible
        // encoding in the first place. Since the signature doesn't contain any fancy characters, we can
        // read it regardless of encoding, with either UTF-8 or UTF-16. That's the hypothesis, at any rate.
        // 8 bit is most likely, so we try that first:
        Optional<Charset> suppliedEncoding = Optional.empty();
        try (Reader utf8Reader = ImportFormatReader.getUTF8Reader(fileToOpen)) {
            suppliedEncoding = OpenDatabaseAction.getSuppliedEncoding(utf8Reader);
        }
        // Now if that didn't get us anywhere, we check with the 16 bit encoding:
        if (!suppliedEncoding.isPresent()) {
            try (Reader utf16Reader = ImportFormatReader.getUTF16Reader(fileToOpen)) {
                suppliedEncoding = OpenDatabaseAction.getSuppliedEncoding(utf16Reader);
            }
        }

        // Open and parse file
        try (InputStreamReader reader = openFile(fileToOpen, suppliedEncoding, defaultEncoding)) {
            BibtexParser parser = new BibtexParser(reader);

            ParserResult result = parser.parse();
            result.setEncoding(Charset.forName(reader.getEncoding()));
            result.setFile(fileToOpen);

            if (SpecialFieldsUtils.keywordSyncEnabled()) {
                NamedCompound compound = new NamedCompound("SpecialFieldSync");
                for (BibEntry entry : result.getDatabase().getEntries()) {
                    SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, compound);
                }
                LOGGER.debug("Synchronized special fields based on keywords");
            }

            return result;
        }
    }

    /**
     * Opens the file with the provided encoding. If this fails (or no encoding is provided), then the fallback encoding
     * will be used.
     */
    private static InputStreamReader openFile(File fileToOpen, Optional<Charset> encoding, Charset defaultEncoding)
            throws IOException {
        if (encoding.isPresent()) {
            try {
                return ImportFormatReader.getReader(fileToOpen, encoding.get());
            } catch (IOException ex) {
                LOGGER.warn("Problem getting reader", ex);
                // The supplied encoding didn't work out, so we use the fallback.
                return ImportFormatReader.getReader(fileToOpen, defaultEncoding);
            }
        } else {
            // We couldn't find a header with info about encoding. Use fallback:
            return ImportFormatReader.getReader(fileToOpen, defaultEncoding);

        }
    }

    /**
     * Searches the file for "Encoding: myEncoding" and returns the found supplied encoding.
     */
    private static Optional<Charset> getSuppliedEncoding(Reader reader) {
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                // Line does not start with %, so there are no comment lines for us and we can stop parsing
                if (!line.startsWith("%")) {
                    return Optional.empty();
                }

                // Only keep the part after %
                line = line.substring(1).trim();

                if (line.startsWith(Globals.SIGNATURE)) {
                    // Signature line, so keep reading and skip to next line
                } else if (line.startsWith(Globals.ENCODING_PREFIX)) {
                    // Line starts with "Encoding: ", so the rest of the line should contain the name of the encoding
                    // Except if there is already a @ symbol signaling the starting of a BibEntry
                    Integer atSymbolIndex = line.indexOf('@');
                    String encoding;
                    if (atSymbolIndex > 0) {
                        encoding = line.substring(Globals.ENCODING_PREFIX.length(), atSymbolIndex);
                    } else {
                        encoding = line.substring(Globals.ENCODING_PREFIX.length());
                    }

                    return Optional.of(Charset.forName(encoding));
                } else {
                    // Line not recognized so stop parsing
                    return Optional.empty();
                }
            }
        } catch (IOException ignored) {
            // Ignored
        }
        return Optional.empty();
    }

    /**
     * Load database (bib-file) or, if there exists, a newer autosave version, unless the flag is set to ignore the autosave
    *
    * @param name Name of the bib-file to open
    * @param ignoreAutosave true if autosave version of the file should be ignored
    * @return ParserResult which never is null
    */

    public static ParserResult loadDatabaseOrAutoSave(String name, boolean ignoreAutosave) {
        // String in OpenDatabaseAction.java
        LOGGER.info("Opening: " + name);
        File file = new File(name);
        if (!file.exists()) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            LOGGER.error(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;

        }
        try {

            if (!ignoreAutosave) {
                boolean autoSaveFound = AutoSaveManager.newerAutoSaveExists(file);
                if (autoSaveFound) {
                    // We have found a newer autosave. Make a note of this, so it can be
                    // handled after startup:
                    ParserResult postp = new ParserResult(null, null, null);
                    postp.setPostponedAutosaveFound(true);
                    postp.setFile(file);
                    return postp;
                }
            }

            if (!FileBasedLock.waitForFileLock(file, 10)) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return ParserResult.getNullResult();
            }

            Charset encoding = Globals.prefs.getDefaultEncoding();
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            pr.setFile(file);
            if (pr.hasWarnings()) {
                for (String aWarn : pr.warnings()) {
                    LOGGER.warn(aWarn);
                }
            }
            return pr;
        } catch (Throwable ex) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            pr.setErrorMessage(ex.getMessage());
            LOGGER.info("Problem opening .bib-file", ex);
            return pr;
        }

    }

}

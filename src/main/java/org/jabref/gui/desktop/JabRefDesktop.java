package org.jabref.gui.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.desktop.os.DefaultDesktop;
import org.jabref.gui.desktop.os.Linux;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.desktop.os.OSX;
import org.jabref.gui.desktop.os.Windows;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypeEntryEditor;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Eprint;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Replace by http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html
 * http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
 */
public class JabRefDesktop {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefDesktop.class);

    private static final NativeDesktop NATIVE_DESKTOP = getNativeDesktop();
    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private JabRefDesktop() {
    }

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(BibDatabaseContext databaseContext, String initialLink,
            String initialFieldName) throws IOException {
        String link = initialLink;
        String fieldName = initialFieldName;
        if (FieldName.PS.equals(fieldName) || FieldName.PDF.equals(fieldName)) {
            // Find the default directory for this field type:
            List<String> dir = databaseContext.getFileDirectories(fieldName, Globals.prefs.getFileDirectoryPreferences());

            Optional<Path> file = FileHelper.expandFilename(link, dir);

            // Check that the file exists:
            if (!file.isPresent() || !Files.exists(file.get())) {
                throw new IOException("File not found (" + fieldName + "): '" + link + "'.");
            }
            link = file.get().toAbsolutePath().toString();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.get().getFileName().toString().split("\\.");
            if (split.length >= 2) {
                if ("pdf".equalsIgnoreCase(split[split.length - 1])) {
                    fieldName = FieldName.PDF;
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                        || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    fieldName = FieldName.PS;
                }
            }
        } else if (FieldName.DOI.equals(fieldName)) {
            openDoi(link);
            return;
        } else if (FieldName.EPRINT.equals(fieldName)) {
            link = Eprint.build(link).map(Eprint::getURIAsASCIIString).orElse(link);
            // should be opened in browser
            fieldName = FieldName.URL;
        }

        if (FieldName.URL.equals(fieldName)) {
            openBrowser(link);
        } else if (FieldName.PS.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PS);
            } catch (IOException e) {
                LOGGER.error("An error occurred on the command: " + link, e);
            }
        } else if (FieldName.PDF.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PDF);
            } catch (IOException e) {
                LOGGER.error("An error occurred on the command: " + link, e);
            }
        } else {
            LOGGER.info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    private static void openDoi(String doi) throws IOException {
        String link = DOI.parse(doi).map(DOI::getURIAsASCIIString).orElse(doi);
        openBrowser(link);
    }

    /**
     * Open an external file, attempting to use the correct viewer for it.
     *
     * @param databaseContext
     *            The database this file belongs to.
     * @param link
     *            The filename.
     * @return false if the link couldn't be resolved, true otherwise.
     */
    public static boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext, String link,
            final Optional<ExternalFileType> type) throws IOException {
        boolean httpLink = false;

        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase(Locale.ROOT)).matches()) {
            httpLink = true;
        }

        // For other platforms we'll try to find the file type:
        Path file = null;
        if (!httpLink) {
            Optional<Path> tmp = FileHelper.expandFilename(databaseContext, link,
                    Globals.prefs.getFileDirectoryPreferences());
            if (tmp.isPresent()) {
                file = tmp.get();
            }
        }

        // Check if we have arrived at a file type, and either an http link or an existing file:
        if (httpLink || ((file != null) && Files.exists(file) && (type.isPresent()))) {
            // Open the file:
            String filePath = httpLink ? link : file.toString();
            openExternalFilePlatformIndependent(type, filePath);
            return true;
        } else {
            // No file matched the name, or we did not know the file type.
            return false;
        }
    }

    public static boolean openExternalFileAnyFormat(Path file, final BibDatabaseContext databaseContext, final Optional<ExternalFileType> type) throws IOException {
        return openExternalFileAnyFormat(databaseContext, file.toString(), type);
    }

    private static void openExternalFilePlatformIndependent(Optional<ExternalFileType> fileType, String filePath)
            throws IOException {
        if (fileType.isPresent()) {
            String application = fileType.get().getOpenWithApplication();

            if (application.isEmpty()) {
                NATIVE_DESKTOP.openFile(filePath, fileType.get().getExtension());
            } else {
                NATIVE_DESKTOP.openFileWithApplication(filePath, application);
            }
        }
    }

    public static boolean openExternalFileUnknown(JabRefFrame frame, BibEntry entry, BibDatabaseContext databaseContext,
            String link, UnknownExternalFileType fileType) throws IOException {

        String cancelMessage = Localization.lang("Unable to open file.");
        String[] options = new String[] {Localization.lang("Define '%0'", fileType.getName()),
                Localization.lang("Change file type"), Localization.lang("Cancel")};
        String defOption = options[0];
        int answer = JOptionPane.showOptionDialog(frame,
                Localization.lang("This external link is of the type '%0', which is undefined. What do you want to do?",
                        fileType.getName()),
                Localization.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, defOption);
        if (answer == JOptionPane.CANCEL_OPTION) {
            frame.output(cancelMessage);
            return false;
        } else if (answer == JOptionPane.YES_OPTION) {
            // User wants to define the new file type. Show the dialog:
            ExternalFileType newType = new ExternalFileType(fileType.getName(), fileType.getExtension(), "", "", "new",
                    IconTheme.JabRefIcon.FILE.getSmallIcon());
            ExternalFileTypeEntryEditor editor = new ExternalFileTypeEntryEditor(frame, newType);
            editor.setVisible(true);
            if (editor.okPressed()) {
                // Get the old list of types, add this one, and update the list in prefs:
                List<ExternalFileType> fileTypes = new ArrayList<>(
                        ExternalFileTypes.getInstance().getExternalFileTypeSelection());
                fileTypes.add(newType);
                Collections.sort(fileTypes);
                ExternalFileTypes.getInstance().setExternalFileTypes(fileTypes);
                // Finally, open the file:
                return openExternalFileAnyFormat(databaseContext, link, Optional.of(newType));
            } else {
                // Canceled:
                frame.output(cancelMessage);
                return false;
            }
        } else {
            // User wants to change the type of this link.
            // First get a model of all file links for this entry:
            FileListTableModel tModel = new FileListTableModel();
            Optional<String> oldValue = entry.getField(FieldName.FILE);
            oldValue.ifPresent(tModel::setContent);
            FileListEntry flEntry = null;
            // Then find which one we are looking at:
            for (int i = 0; i < tModel.getRowCount(); i++) {
                FileListEntry iEntry = tModel.getEntry(i);
                if (iEntry.getLink().equals(link)) {
                    flEntry = iEntry;
                    break;
                }
            }
            if (flEntry == null) {
                // This shouldn't happen, so I'm not sure what to put in here:
                throw new RuntimeException("Could not find the file list entry " + link + " in " + entry);
            }

            FileListEntryEditor editor = new FileListEntryEditor(flEntry.toParsedFileField(), false, true, databaseContext);
            editor.setVisible(true, false);
            if (editor.okPressed()) {
                // Store the changes and add an undo edit:
                String newValue = tModel.getStringRepresentation();
                UndoableFieldChange ce = new UndoableFieldChange(entry, FieldName.FILE, oldValue.orElse(null),
                        newValue);
                entry.setField(FieldName.FILE, newValue);
                frame.getCurrentBasePanel().getUndoManager().addEdit(ce);
                frame.getCurrentBasePanel().markBaseChanged();
                // Finally, open the link:
                return openExternalFileAnyFormat(databaseContext, flEntry.getLink(), flEntry.getType());
            } else {
                // Canceled:
                frame.output(cancelMessage);
                return false;
            }
        }
    }

    /**
     * Opens a file browser of the folder of the given file. If possible, the file is selected
     * @param fileLink the location of the file
     * @throws IOException
     */
    public static void openFolderAndSelectFile(Path fileLink) throws IOException {
        NATIVE_DESKTOP.openFolderAndSelectFile(fileLink);
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     * @throws IOException
     */
    public static void openBrowser(String url) throws IOException {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
        openExternalFilePlatformIndependent(fileType, url);
    }

    public static void openBrowser(URI url) throws IOException {
        openBrowser(url.toASCIIString());
    }

    /**
     * Opens the url with the users standard Browser.
     * If that fails a popup will be shown to instruct the user to open the link manually
     * and the link gets copied to the clipboard
     * @param url
     */
    public static void openBrowserShowPopup(String url) {
        try {
            openBrowser(url);
        } catch (IOException exception) {
            new ClipBoardManager().setClipboardContents(url);
            LOGGER.error("Could not open browser", exception);
            String couldNotOpenBrowser = Localization.lang("Could not open browser.");
            String openManually = Localization.lang("Please open %0 manually.", url);
            String copiedToClipboard = Localization.lang("The link has been copied to the clipboard.");
            JabRefGUI.getMainFrame().output(couldNotOpenBrowser);
            JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(), couldNotOpenBrowser + "\n" + openManually + "\n" +
                    copiedToClipboard, couldNotOpenBrowser, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a new console starting on the given file location
     *
     * If no command is specified in {@link Globals},
     * the default system console will be executed.
     *
     * @param file Location the console should be opened at.
     */
    public static void openConsole(File file) throws IOException {
        if (file == null) {
            return;
        }

        String absolutePath = file.toPath().toAbsolutePath().getParent().toString();
        boolean usingDefault = Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION);

        if (usingDefault) {
            NATIVE_DESKTOP.openConsole(absolutePath);
        } else {
            String command = Globals.prefs.get(JabRefPreferences.CONSOLE_COMMAND);
            command = command.trim();

            if (!command.isEmpty()) {
                command = command.replaceAll("\\s+", " "); // normalize white spaces
                String[] subcommands = command.split(" ");

                // replace the placeholder if used
                String commandLoggingText = command.replace("%DIR", absolutePath);

                JabRefGUI.getMainFrame().output(Localization.lang("Executing command \"%0\"...", commandLoggingText));
                LOGGER.info("Executing command \"" + commandLoggingText + "\"...");

                try {
                    new ProcessBuilder(subcommands).start();
                } catch (IOException exception) {
                    LOGGER.error("Open console", exception);

                    JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(),
                            Localization.lang("Error occured while executing the command \"%0\".", commandLoggingText),
                            Localization.lang("Open console") + " - " + Localization.lang("Error"),
                            JOptionPane.ERROR_MESSAGE);
                    JabRefGUI.getMainFrame().output(null);
                }
            }
        }
    }

    // TODO: Move to OS.java
    public static NativeDesktop getNativeDesktop() {
        if (OS.WINDOWS) {
            return new Windows();
        } else if (OS.OS_X) {
            return new OSX();
        } else if (OS.LINUX) {
            return new Linux();
        }
        return new DefaultDesktop();
    }
}

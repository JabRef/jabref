package net.sf.jabref.gui.desktop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.desktop.os.DefaultDesktop;
import net.sf.jabref.gui.desktop.os.Linux;
import net.sf.jabref.gui.desktop.os.NativeDesktop;
import net.sf.jabref.gui.desktop.os.OSX;
import net.sf.jabref.gui.desktop.os.Windows;
import net.sf.jabref.gui.externalfiletype.ExternalFileType;
import net.sf.jabref.gui.externalfiletype.ExternalFileTypeEntryEditor;
import net.sf.jabref.gui.externalfiletype.ExternalFileTypes;
import net.sf.jabref.gui.externalfiletype.UnknownExternalFileType;
import net.sf.jabref.gui.filelist.FileListEntry;
import net.sf.jabref.gui.filelist.FileListEntryEditor;
import net.sf.jabref.gui.filelist.FileListTableModel;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO: Replace by http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html
 * http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
 */
public class JabRefDesktop {

    private static final NativeDesktop NATIVE_DESKTOP = getNativeDesktop();
    private static final Log LOGGER = LogFactory.getLog(JabRefDesktop.class);
    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private static final String ARXIV_LOOKUP_PREFIX = "http://arxiv.org/abs/";

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(BibDatabaseContext databaseContext, String initialLink,
            String initialFieldName) throws IOException {
        String link = initialLink;
        String fieldName = initialFieldName;
        if (FieldName.PS.equals(fieldName) || FieldName.PDF.equals(fieldName)) {
            // Find the default directory for this field type:
            List<String> dir = databaseContext.getFileDirectory(fieldName, Globals.prefs.getFileDirectoryPreferences());

            Optional<File> file = FileUtil.expandFilename(link, dir);

            // Check that the file exists:
            if (!file.isPresent() || !file.get().exists()) {
                throw new IOException("File not found (" + fieldName + "): '" + link + "'.");
            }
            link = file.get().getCanonicalPath();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.get().getName().split("\\.");
            if (split.length >= 2) {
                if ("pdf".equalsIgnoreCase(split[split.length - 1])) {
                    fieldName = FieldName.PDF;
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                        || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    fieldName = FieldName.PS;
                }
            }
        } else if (FieldName.DOI.equals(fieldName)) {
            Optional<DOI> doiUrl = DOI.build(link);
            if (doiUrl.isPresent()) {
                link = doiUrl.get().getURIAsASCIIString();
            }
            // should be opened in browser
            fieldName = FieldName.URL;
        } else if (FieldName.EPRINT.equals(fieldName)) {
            fieldName = FieldName.URL;

            // Check to see if link field already contains a well formated URL
            if (!link.startsWith("http://")) {
                link = ARXIV_LOOKUP_PREFIX + link;
            }
        }

        if (FieldName.URL.equals(fieldName)) { // html
            try {
                openBrowser(link);
            } catch (IOException e) {
                LOGGER.error("Error opening file '" + link + "'", e);
                // TODO: should we rethrow the exception?
                // In BasePanel.java, the exception is catched and a text output to the frame
                // throw e;
            }
        } else if (FieldName.PS.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PS);
            } catch (IOException e) {
                LOGGER.error("An error occured on the command: " + link, e);
            }
        } else if (FieldName.PDF.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PDF);
            } catch (IOException e) {
                LOGGER.error("An error occured on the command: " + link, e);
            }
        } else {
            LOGGER.info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
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

        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
            httpLink = true;
        }

        // For other platforms we'll try to find the file type:
        File file = new File(link);

        if (!httpLink) {
            Optional<File> tmp = FileUtil.expandFilename(databaseContext, link,
                    Globals.prefs.getFileDirectoryPreferences());
            if (tmp.isPresent()) {
                file = tmp.get();
            }
        }

        // Check if we have arrived at a file type, and either an http link or an existing file:
        if ((httpLink || file.exists()) && (type.isPresent())) {
            // Open the file:
            String filePath = httpLink ? link : file.getPath();
            openExternalFilePlatformIndependent(type, filePath);
            return true;
        } else {
            // No file matched the name, or we did not know the file type.
            return false;
        }
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
            ExternalFileType newType = new ExternalFileType(fileType.getName(), "", "", "", "new",
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
                if (iEntry.link.equals(link)) {
                    flEntry = iEntry;
                    break;
                }
            }
            if (flEntry == null) {
                // This shouldn't happen, so I'm not sure what to put in here:
                throw new RuntimeException("Could not find the file list entry " + link + " in " + entry);
            }

            FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true, databaseContext);
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
                return openExternalFileAnyFormat(databaseContext, flEntry.link, flEntry.type);
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
    public static void openFolderAndSelectFile(String fileLink) throws IOException {
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
            String copiedToClipboard = Localization.lang("The_link_has_been_copied_to_the_clipboard.");
            JabRefGUI.getMainFrame().output(couldNotOpenBrowser);
            JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(), couldNotOpenBrowser + "\n" + openManually +"\n"+
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
                            Localization.lang("Error_occured_while_executing_the_command_\"%0\".", commandLoggingText),
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

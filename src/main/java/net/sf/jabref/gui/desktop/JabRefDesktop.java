package net.sf.jabref.gui.desktop;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypeEntryEditor;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.desktop.os.Linux;
import net.sf.jabref.gui.desktop.os.OSX;
import net.sf.jabref.gui.desktop.os.Windows;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * TODO: Replace by http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html
 * http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
 */
public class JabRefDesktop {
    private static final Log LOGGER = LogFactory.getLog(JabRefDesktop.class);

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(MetaData metaData, String initialLink, String initialFieldName)
            throws IOException {
        String link = initialLink;
        String fieldName = initialFieldName;
        if ("ps".equals(fieldName) || "pdf".equals(fieldName)) {
            // Find the default directory for this field type:
            List<String> dir = metaData.getFileDirectory(fieldName);

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
                    fieldName = "pdf";
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                        || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    fieldName = "ps";
                }
            }
        } else if ("doi".equals(fieldName)) {
            Optional<DOI> doiUrl = DOI.build(link);
            if(doiUrl.isPresent()) {
                link = doiUrl.get().getURLAsASCIIString();
            }
            // should be opened in browser
            fieldName = "url";
        } else if ("eprint".equals(fieldName)) {
            fieldName = "url";

            // Check to see if link field already contains a well formated URL
            if (!link.startsWith("http://")) {
                link = Util.ARXIV_LOOKUP_PREFIX + link;
            }
        }

        if ("url".equals(fieldName)) { // html
            try {
                openBrowser(link);
            } catch (IOException e) {
                LOGGER.error("Error opening file '" + link + "'", e);
                // TODO: should we rethrow the exception?
                // In BasePanel.java, the exception is catched and a text output to the frame
                // throw e;
            }
        } else if ("ps".equals(fieldName)) {
            try {
                if (OS.OS_X) {
                    OSX.openFile(link);
                } else if (OS.WINDOWS) {
                    Windows.openFile(link);
                } else {
                    ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("ps");
                    String viewer = type == null ? "xdg-open" : type.getOpenWith();
                    String[] cmdArray = new String[2];
                    cmdArray[0] = viewer;
                    cmdArray[1] = link;
                    Runtime.getRuntime().exec(cmdArray);
                }
            } catch (IOException e) {
                LOGGER.error("An error occured on the command: " + Globals.prefs.get(JabRefPreferences.PDFVIEWER) + " #"
                        + link, e);
            }
        } else if ("pdf".equals(fieldName)) {
            try {
                if (OS.OS_X) {
                    OSX.openFile(link, "pdf");
                } else if (OS.WINDOWS) {
                    Windows.openFile(link);
                    /*
                     * String[] spl = link.split("\\\\"); StringBuffer sb = new
                     * StringBuffer(); for (int i = 0; i < spl.length; i++) { if
                     * (i > 0) sb.append("\\"); if (spl[i].indexOf(" ") >= 0)
                     * spl[i] = "\"" + spl[i] + "\""; sb.append(spl[i]); }
                     * //pr(sb.toString()); link = sb.toString();
                     *
                     * String cmd = "cmd.exe /c start " + link;
                     *
                     * Process child = Runtime.getRuntime().exec(cmd);
                     */
                } else {
                    Linux.openFile(link, "pdf");
                }
            } catch (IOException e) {
                LOGGER.error("An error occured on the command: " + Globals.prefs.get(JabRefPreferences.PDFVIEWER) + " #"
                        + link, e);
            }
        } else {
            LOGGER.info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    /**
     * Open an external file, attempting to use the correct viewer for it.
     *
     * @param metaData
     *            The MetaData for the database this file belongs to.
     * @param link
     *            The filename.
     * @return false if the link couldn't be resolved, true otherwise.
     */
    public static boolean openExternalFileAnyFormat(final MetaData metaData, String link, final ExternalFileType fileType) throws IOException {

        boolean httpLink = false;

        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
            httpLink = true;
        }

        // For other platforms we'll try to find the file type:
        File file = new File(link);

        if (!httpLink) {
            Optional<File> tmp = FileUtil.expandFilename(metaData, link);
            if (tmp.isPresent()) {
                file = tmp.get();
            }
        }

        // Check if we have arrived at a file type, and either an http link or an existing file:
        if ((httpLink || file.exists()) && (fileType != null)) {
            // Open the file:
            String filePath = httpLink ? link : file.getPath();
            openExternalFilePlatformIndependent(fileType, filePath);
            return true;

        } else {

            return false;
            // No file matched the name, or we didn't know the file type.

        }

    }

    private static void openExternalFilePlatformIndependent(ExternalFileType fileType, String filePath)
            throws IOException {
        // For URLs, other solutions are
        //  * https://github.com/rajing/browserlauncher2, but it is not available in maven
        //  * a the solution combining http://stackoverflow.com/a/5226244/873282 and http://stackoverflow.com/a/28807079/873282
        if (OS.OS_X) {
            // Use "-a <application>" if the app is specified, and just "open <filename>" otherwise:
            String[] cmd = (fileType.getOpenWith() != null) && !fileType.getOpenWith().isEmpty() ?
                    new String[] {"/usr/bin/open", "-a", fileType.getOpenWith(), filePath} :
                    new String[] {"/usr/bin/open", filePath};
            Runtime.getRuntime().exec(cmd);
        } else if (OS.WINDOWS) {
            if ((fileType.getOpenWith() != null) && !fileType.getOpenWith().isEmpty()) {
                // Application is specified. Use it:
                Windows.openFileWithApplication(filePath, fileType.getOpenWith());
            } else {
                Windows.openFile(filePath);
            }
        } else {
            // Use the given app if specified, and the universal "xdg-open" otherwise:
            String[] openWith;
            if ((fileType.getOpenWith() != null) && !fileType.getOpenWith().isEmpty()) {
                openWith = fileType.getOpenWith().split(" ");
            } else {
                openWith = new String[] {"xdg-open"};
            }

            String[] cmdArray = new String[openWith.length + 1];
            System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
            cmdArray[cmdArray.length - 1] = filePath;
            Runtime.getRuntime().exec(cmdArray);
        }
    }

    public static boolean openExternalFileUnknown(JabRefFrame frame, BibEntry entry, MetaData metaData,
            String link, UnknownExternalFileType fileType) throws IOException {

        String cancelMessage = Localization.lang("Unable to open file.");
        String[] options = new String[] {Localization.lang("Define '%0'", fileType.getName()),
                Localization.lang("Change file type"),
                Localization.lang("Cancel")};
        String defOption = options[0];
        int answer = JOptionPane.showOptionDialog(frame, Localization.lang("This external link is of the type '%0', which is undefined. What do you want to do?",
                        fileType.getName()),
                Localization.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, defOption);
        if (answer == JOptionPane.CANCEL_OPTION) {
            frame.output(cancelMessage);
            return false;
        }
        else if (answer == JOptionPane.YES_OPTION) {
            // User wants to define the new file type. Show the dialog:
            ExternalFileType newType = new ExternalFileType(fileType.getName(), "", "", "", "new", IconTheme.JabRefIcon.FILE.getSmallIcon());
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
                return openExternalFileAnyFormat(metaData, link, newType);
            } else {
                // Cancelled:
                frame.output(cancelMessage);
                return false;
            }
        } else {
            // User wants to change the type of this link.
            // First get a model of all file links for this entry:
            FileListTableModel tModel = new FileListTableModel();
            String oldValue = entry.getField(Globals.FILE_FIELD);
            tModel.setContent(oldValue);
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

            FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true, metaData);
            editor.setVisible(true, false);
            if (editor.okPressed()) {
                // Store the changes and add an undo edit:
                String newValue = tModel.getStringRepresentation();
                UndoableFieldChange ce = new UndoableFieldChange(entry, Globals.FILE_FIELD,
                        oldValue, newValue);
                entry.setField(Globals.FILE_FIELD, newValue);
                frame.getCurrentBasePanel().undoManager.addEdit(ce);
                frame.getCurrentBasePanel().markBaseChanged();
                // Finally, open the link:
                return openExternalFileAnyFormat(metaData, flEntry.link, flEntry.type);
            } else {
                // Cancelled:
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
        if (OS.WINDOWS) {
            Windows.openFolderAndSelectFile(fileLink);
        } else if (OS.LINUX) {
            Linux.openFolderAndSelectFile(fileLink);
        } else {
            File f = new File(fileLink);
            Desktop.getDesktop().open(f.getParentFile());
        }
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     * @throws IOException
     */
    public static void openBrowser(String url) throws IOException {
        ExternalFileType fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
        openExternalFilePlatformIndependent(fileType, url);
    }

    public static void openConsole(File file) throws IOException {
        if (file == null) {
            return;
        }

        String absolutePath = file.getAbsolutePath();
        absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator) + 1);

        if (OS.LINUX) {
            Linux.openConsole(absolutePath);
        } else if (OS.WINDOWS) {
            Windows.openConsole(absolutePath);
        } else if (OS.OS_X) {
            OSX.openConsole(absolutePath);
        } else {
            LOGGER.info("Operating system is not supported by this feature.");
        }
    }
}

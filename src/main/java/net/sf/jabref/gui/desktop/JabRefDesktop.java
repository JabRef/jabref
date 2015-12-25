package net.sf.jabref.gui.desktop;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypeEntryEditor;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.util.io.URLUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * TODO: Replace by http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html
 * http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
 */
public class JabRefDesktop {

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(MetaData metaData, String link, String fieldName) throws IOException {

        if ("ps".equals(fieldName) || "pdf".equals(fieldName)) {

            // Find the default directory for this field type:
            String[] dir = metaData.getFileDirectory(fieldName);

            File file = FileUtil.expandFilename(link, dir);

            // Check that the file exists:
            if ((file == null) || !file.exists()) {
                throw new IOException("File not found (" + fieldName + "): '" + link + "'.");
            }
            link = file.getCanonicalPath();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.getName().split("\\.");
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

            link = URLUtil.sanitizeUrl(link);

            // Check to see if link field already contains a well formated URL
            if (!link.startsWith("http://")) {
                link = Util.ARXIV_LOOKUP_PREFIX + link;
            }
        }

        if ("url".equals(fieldName)) { // html
            try {
                openBrowser(link);
            } catch (IOException e) {
                System.err.println(Localization.lang("Error_opening_file_'%0'.", link));
                e.printStackTrace();
                // TODO: should we rethrow the exception?
                // In BasePanel.java, the exception is catched and a text output to the frame
                // throw e;
            }
        } else if ("ps".equals(fieldName)) {
            try {
                if (OS.OS_X) {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmd = {"/usr/bin/open", "-a", viewer, link};
                    Runtime.getRuntime().exec(cmd);
                } else if (OS.WINDOWS) {
                    openFileOnWindows(link);
                    /*
                     * cmdArray[0] = Globals.prefs.get("psviewer"); cmdArray[1] =
                     * link; Process child = Runtime.getRuntime().exec(
                     * cmdArray[0] + " " + cmdArray[1]);
                     */
                } else {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : "xdg-open";
                    String[] cmdArray = new String[2];
                    cmdArray[0] = viewer;
                    cmdArray[1] = link;
                    Runtime.getRuntime().exec(cmdArray);
                }
            } catch (IOException e) {
                System.err.println("An error occured on the command: " + Globals.prefs.get("psviewer") + " " + link);
            }
        } else if ("pdf".equals(fieldName)) {
            try {
                if (OS.OS_X) {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("pdf");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmd = {"/usr/bin/open", "-a", viewer, link};
                    Runtime.getRuntime().exec(cmd);
                } else if (OS.WINDOWS) {
                    openFileOnWindows(link);
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
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("pdf");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmdArray = new String[2];
                    cmdArray[0] = viewer;
                    cmdArray[1] = link;
                    // Process child = Runtime.getRuntime().exec(cmdArray[0]+"
                    // "+cmdArray[1]);
                    Runtime.getRuntime().exec(cmdArray);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("An error occured on the command: " + Globals.prefs.get("pdfviewer") + " #" + link);
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    /**
     * Opens a file on a Windows system, using its default viewer.
     *
     * @param link
     *            The filename.
     * @throws IOException
     */
    static void openFileOnWindows(String link) throws IOException {
        // escape & and spaces
        Runtime.getRuntime().exec("cmd.exe /c start " + link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \""));
    }

    /**
     * Opens a file on a Windows system, using the given application.
     *
     * @param link The filename.
     * @param application Link to the app that opens the file.
     * @throws IOException
     */
    static void openFileWithApplicationOnWindows(String link, String application) throws IOException {
        link = link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");

        Runtime.getRuntime().exec(application + " " + link);
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

        if (Util.REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
            httpLink = true;
        }
        /*if (link.toLowerCase().startsWith("file://")) {
            link = link.substring(7);
        }
        final String ln = link;
        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
            (new Thread(new Runnable() {
                public void run() {
                    openRemoteExternalFile(metaData, ln, fileType);
                }
            })).start();

            return true;
        }*/

        //boolean httpLink = link.toLowerCase().startsWith("http:")
        //        || link.toLowerCase().startsWith("ftp:");

        // For other platforms we'll try to find the file type:
        File file = new File(link);

        if (!httpLink) {
            File tmp = FileUtil.expandFilename(metaData, link);
            if (tmp != null) {
                file = tmp;
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

    static void openExternalFilePlatformIndependent(ExternalFileType fileType, String filePath) throws IOException {
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
                openFileWithApplicationOnWindows(filePath, fileType.getOpenWith());
            } else {
                openFileOnWindows(filePath);
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

    public static void openRemoteExternalFile(final MetaData metaData,
            final String link, final ExternalFileType fileType) {
        File temp = null;
        try {
            temp = File.createTempFile("jabref-link", "." + fileType.getExtension());
            temp.deleteOnExit();
            System.out.println("Downloading to '" + temp.getPath() + "'");
            new URLDownload(new URL(link)).downloadToFile(temp);
            System.out.println("Done");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        final String ln = temp.getPath();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    openExternalFileAnyFormat(metaData, ln, fileType);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
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
                List<ExternalFileType> fileTypes = new ArrayList<>();
                ExternalFileType[] oldTypes = Globals.prefs.getExternalFileTypeSelection();
                Collections.addAll(fileTypes, oldTypes);
                fileTypes.add(newType);
                Collections.sort(fileTypes);
                Globals.prefs.setExternalFileTypes(fileTypes);
                // Finally, open the file:
                return openExternalFileAnyFormat(metaData, link, newType);
            } else {
                // Cancelled:
                frame.output(cancelMessage);
                return false;
            }
        }
        else {
            // User wants to change the type of this link.
            // First get a model of all file links for this entry:
            FileListTableModel tModel = new FileListTableModel();
            String oldValue = entry.getField(Globals.FILE_FIELD);
            tModel.setContent(oldValue);
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
                return openExternalFileAnyFormat(metaData, flEntry.getLink(), flEntry.getType());
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
            openFolderAndSelectFileOnWindows(fileLink);
        } else if (OS.LINUX) {
            openFolderAndSelectFileOnLinux(fileLink);
        } else {
            openFolderAndSelectFileGeneric(fileLink);
        }
    }

    private static void openFolderAndSelectFileOnLinux(String fileLink) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION").toLowerCase();

        String cmd;

        if (desktopSession.contains("gnome")) {
            cmd = "nautilus " + fileLink;
        } else if (desktopSession.contains("kde")) {
            cmd = "dolphin --select " + fileLink;
        } else {
            cmd = "xdg-open " + fileLink.substring(0, fileLink.lastIndexOf(File.separator));
        }

        Runtime.getRuntime().exec(cmd);
    }

    private static void openFolderAndSelectFileGeneric(String fileLink) throws IOException {
        File f = new File(fileLink);
        Desktop.getDesktop().open(f.getParentFile());
    }

    private static void openFolderAndSelectFileOnWindows(String link) throws IOException {
        link = link.replace("&", "\"&\"");

        String cmd = "explorer.exe /select,\"" + link + "\"";

        Runtime.getRuntime().exec(cmd);
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     * @throws IOException
     */
    public static void openBrowser(String url) throws IOException {
        url = URLUtil.sanitizeUrl(url);
        ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("html");
        openExternalFilePlatformIndependent(fileType, url);
    }
}

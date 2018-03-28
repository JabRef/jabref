package org.jabref.gui.externalfiles;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the download of an external file. Typically called when the user clicks
 * the "Download" button in a FileListEditor shown in an EntryEditor.
 * <p/>
 * The FileListEditor constructs the DownloadExternalFile instance, then calls the download()
 * method passing a reference to itself as a callback. The download() method asks for the URL,
 * then starts the download. When the download is completed, it calls the downloadCompleted()
 * method on the callback FileListEditor, which then needs to take care of linking to the file.
 * The local filename is passed as an argument to the downloadCompleted() method.
 * <p/>
 * If the download is canceled, or failed, the user is informed. The callback is never called.
 */
public class DownloadExternalFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadExternalFile.class);

    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final BibEntry entry;
    private FileListEntryEditor editor;
    private boolean downloadFinished;
    private boolean dontShowDialog;


    public DownloadExternalFile(JabRefFrame frame, BibDatabaseContext databaseContext, BibEntry entry) {
        this.frame = frame;
        this.databaseContext = databaseContext;
        this.entry = entry;
    }

    /**
     * Look for the last '.' in the link, and return the following characters.
     * This gives the extension for most reasonably named links.
     *
     * @param link The link
     * @return The suffix, excluding the dot (e.g. "pdf")
     */
    public static String getSuffix(final String link) {
        String strippedLink = link;
        try {
            // Try to strip the query string, if any, to get the correct suffix:
            URL url = new URL(link);
            if ((url.getQuery() != null) && (url.getQuery().length() < (link.length() - 1))) {
                strippedLink = link.substring(0, link.length() - url.getQuery().length() - 1);
            }
        } catch (MalformedURLException e) {
            // Don't report this error, since this getting the suffix is a non-critical
            // operation, and this error will be triggered and reported elsewhere.
        }
        // First see if the stripped link gives a reasonable suffix:
        String suffix;
        int strippedLinkIndex = strippedLink.lastIndexOf('.');
        if ((strippedLinkIndex <= 0) || (strippedLinkIndex == (strippedLink.length() - 1))) {
            suffix = null;
        } else {
            suffix = strippedLink.substring(strippedLinkIndex + 1);
        }
        if (!ExternalFileTypes.getInstance().isExternalFileTypeByExt(suffix)) {
            // If the suffix doesn't seem to give any reasonable file type, try
            // with the non-stripped link:
            int index = link.lastIndexOf('.');
            if ((index <= 0) || (index == (link.length() - 1))) {
                // No occurrence, or at the end
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (strippedLink.substring(strippedLinkIndex + 1).indexOf('/') >= 1) {
                    return "";
                } else {
                    return suffix; // return the first one we found, anyway.
                }
            } else {
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (link.substring(index + 1).indexOf('/') >= 1) {
                    return "";
                } else {
                    return link.substring(index + 1);
                }
            }
        } else {
            return suffix;
        }
    }

    /**
     * Start a download.
     *
     * @param callback The object to which the filename should be reported when download
     *                 is complete.
     */
    public void download(final DownloadCallback callback) throws IOException {
        dontShowDialog = false;
        final String res = JOptionPane.showInputDialog(frame, Localization.lang("Enter URL to download"));

        if ((res == null) || res.trim().isEmpty()) {
            return;
        }

        URL url;
        try {
            url = new URL(res);
        } catch (MalformedURLException ex1) {
            JOptionPane.showMessageDialog(frame, Localization.lang("Invalid URL"), Localization.lang("Download file"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        download(url, callback);
    }

    public void download(URL url, final DownloadCallback callback) throws IOException {
        // TODO: what if this takes long time?
        // TODO: stop editor dialog if this results in an error?
        String mimeType = new URLDownload(url).getMimeType();
        download(url, mimeType, callback);
    }

    private Optional<ExternalFileType> getExternalFileType(String mimeType) {
        Optional<ExternalFileType> suggestedType = Optional.empty();
        if (mimeType != null) {
            LOGGER.debug("MIME Type suggested: " + mimeType);
            suggestedType = ExternalFileTypes.getInstance().getExternalFileTypeByMimeType(mimeType);
        }
        return suggestedType;
    }

    /**
     * Start a download.
     *
     * @param callback The object to which the filename should be reported when download
     *                 is complete.
     */
    public void download(URL url, String mimeType, final DownloadCallback callback) throws IOException {
        Optional<ExternalFileType> fileType = getExternalFileType(mimeType);

        // First of all, start the download itself in the background to a temporary file:
        final Path tempFile = Files.createTempFile("jabref_download", "tmp");
        tempFile.toFile().deleteOnExit();

        final URLDownload fileDownload = new URLDownload(url);

        JabRefExecutorService.INSTANCE.execute(() -> {
            try {
                fileDownload.toFile(tempFile);
            } catch (IOException e) {
                dontShowDialog = true;
                if ((editor != null) && editor.isVisible()) {
                    editor.setVisible(false, false);
                }
                JOptionPane.showMessageDialog(frame, Localization.lang("Invalid URL") + ": " + e.getMessage(),
                        Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                LOGGER.info("Error while downloading " + "'" + url + "'", e);
                return;
            }
            // Download finished: call the method that stops the progress bar etc.:
            SwingUtilities.invokeLater(DownloadExternalFile.this::downloadFinished);
        });

        // Then, while the download is proceeding, let the user choose the details of the file:
        String suffix;
        if (fileType.isPresent()) {
            suffix = fileType.get().getExtension();
        } else {
            // If we did not find a file type from the MIME type, try based on extension:
            suffix = getSuffix(url.toString());
            if (suffix == null) {
                suffix = "";
            }
            fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt(suffix);
        }
        String suggestedName = getSuggestedFileName(suffix);
        List<String> fDirectory = databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
        String directory;
        if (fDirectory.isEmpty()) {
            directory = null;
        } else {
            directory = fDirectory.get(0);
        }
        final String suggestDir = directory == null ? System.getProperty("user.home") : directory;
        File file = new File(new File(suggestDir), suggestedName);
        LinkedFile fileListEntry = new LinkedFile("", file.getCanonicalPath(), fileType.map(ExternalFileType::getName).orElse(""));
        editor = new FileListEntryEditor(fileListEntry, true, false, databaseContext, true);
        editor.getProgressBar().setIndeterminate(true);
        editor.setOkEnabled(false);
        editor.setExternalConfirm(closeEntry -> {
            File f = directory == null ? new File(closeEntry.getLink()) : expandFilename(directory, closeEntry.getLink());
            if (f.isDirectory()) {
                JOptionPane.showMessageDialog(frame, Localization.lang("Target file cannot be a directory."),
                        Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (f.exists()) {
                return JOptionPane.showConfirmDialog(frame,
                        Localization.lang("'%0' exists. Overwrite file?", f.getName()),
                        Localization.lang("Download file"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
            } else {
                return true;
            }
        });
        if (dontShowDialog) {
            return;
        } else {
            editor.setVisible(true, false);
        }
        // Editor closed. Go on:
        if (editor.okPressed()) {
            File toFile = directory == null ? new File(fileListEntry.getLink()) : expandFilename(directory,
                    fileListEntry.getLink());
            String dirPrefix;
            if (directory == null) {
                dirPrefix = null;
            } else {
                if (directory.endsWith(OS.FILE_SEPARATOR)) {
                    dirPrefix = directory;
                } else {
                    dirPrefix = directory + OS.FILE_SEPARATOR;
                }
            }

            boolean success = FileUtil.copyFile(tempFile, Paths.get(toFile.toURI()), true);
            if (!success) {
                // OOps, the file exists!
                LOGGER.error("File already exists! DownloadExternalFile.download()");
            }

            // If the local file is in or below the main file directory, change the
            // path to relative:
            if ((dirPrefix != null) && fileListEntry.getLink().startsWith(directory)
                    && (fileListEntry.getLink().length() > dirPrefix.length())) {
                fileListEntry = new LinkedFile(fileListEntry.getDescription(),
                        fileListEntry.getLink().substring(dirPrefix.length()), fileListEntry.getFileType());
            }
            callback.downloadComplete(fileListEntry);

            if (!Files.deleteIfExists(tempFile)) {
                LOGGER.info("Cannot delete temporary file");
            }
        } else {
            // Canceled. Just delete the temp file:
            if (downloadFinished && !Files.deleteIfExists(tempFile)) {
                LOGGER.info("Cannot delete temporary file");
            }
        }
    }

    /**
     * Construct a File object pointing to the file linked, whether the link is
     * absolute or relative to the main directory.
     *
     * @param directory The main directory.
     * @param link      The absolute or relative link.
     * @return The expanded File.
     */
    private File expandFilename(String directory, String link) {
        File toFile = new File(link);
        // If this is a relative link, we should perhaps append the directory:
        String dirPrefix = directory + OS.FILE_SEPARATOR;
        if (!toFile.isAbsolute()) {
            toFile = new File(dirPrefix + link);
        }
        return toFile;
    }

    /**
     * This is called by the download thread when download is completed.
     */
    private void downloadFinished() {
        downloadFinished = true;
        editor.getProgressBar().setVisible(false);
        editor.getProgressBarLabel().setVisible(false);
        editor.setOkEnabled(true);
        editor.getProgressBar().setValue(editor.getProgressBar().getMaximum());
    }

    private String getSuggestedFileName(String suffix) {
        String plannedName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry,
                Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));

        if (!suffix.isEmpty()) {
            plannedName += "." + suffix;
        }

        /*
        * [ 1548875 ] download pdf produces unsupported filename
        *
        * http://sourceforge.net/tracker/index.php?func=detail&aid=1548875&group_id=92314&atid=600306
        * FIXME: rework this! just allow alphanumeric stuff or so?
        * https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247(v=vs.85).aspx#naming_conventions
        * http://superuser.com/questions/358855/what-characters-are-safe-in-cross-platform-file-names-for-linux-windows-and-os
        * https://support.apple.com/en-us/HT202808
        */
        if (OS.WINDOWS) {
            plannedName = plannedName.replaceAll("\\?|\\*|\\<|\\>|\\||\\\"|\\:|\\.$|\\[|\\]", "");
        } else if (OS.OS_X) {
            plannedName = plannedName.replace(":", "");
        }

        return plannedName;
    }


    /**
     * Callback interface that users of this class must implement in order to receive
     * notification when download is complete.
     */
    @FunctionalInterface
    public interface DownloadCallback {
        void downloadComplete(LinkedFile file);
    }
}

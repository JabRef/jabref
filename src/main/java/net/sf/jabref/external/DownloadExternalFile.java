/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.net.MonitoredURLDownload;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

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
 * If the download is cancelled, or failed, the user is informed. The callback is never called.
 */
public class DownloadExternalFile {
    private static final Log LOGGER = LogFactory.getLog(DownloadExternalFile.class);

    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final String bibtexKey;
    private FileListEntryEditor editor;
    private boolean downloadFinished;
    private boolean dontShowDialog;

    public DownloadExternalFile(JabRefFrame frame, BibDatabaseContext databaseContext, String bibtexKey) {
        this.frame = frame;
        this.databaseContext = databaseContext;
        this.bibtexKey = bibtexKey;
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
            JOptionPane.showMessageDialog(frame, Localization.lang("Invalid URL"),
                    Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        download(url, callback);
    }

    /**
     * Start a download.
     *
     * @param callback The object to which the filename should be reported when download
     *                 is complete.
     */
    public void download(URL url, final DownloadCallback callback) throws IOException {
        String res = url.toString();
        String mimeType;

        // First of all, start the download itself in the background to a temporary file:
        final File tmp = File.createTempFile("jabref_download", "tmp");
        tmp.deleteOnExit();

        URLDownload udl = MonitoredURLDownload.buildMonitoredDownload(frame, url);

        try {
            // TODO: what if this takes long time?
            // TODO: stop editor dialog if this results in an error:
            mimeType = udl.determineMimeType(); // Read MIME type
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, Localization.lang("Invalid URL") + ": "
                    + ex.getMessage(), Localization.lang("Download file"),
                    JOptionPane.ERROR_MESSAGE);
            LOGGER.info("Error while downloading " + "'" + res + "'", ex);
            return;
        }
        final URL urlF = url;
        final URLDownload udlF = udl;

        JabRefExecutorService.INSTANCE.execute((Runnable) () -> {
            try {
                udlF.downloadToFile(tmp);
            } catch (IOException e2) {
                dontShowDialog = true;
                if ((editor != null) && editor.isVisible()) {
                    editor.setVisible(false, false);
                }
                JOptionPane.showMessageDialog(frame, Localization.lang("Invalid URL") + ": " + e2.getMessage(),
                        Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                LOGGER.info("Error while downloading " + "'" + urlF + "'", e2);
                return;
            }
            // Download finished: call the method that stops the progress bar etc.:
            SwingUtilities.invokeLater(DownloadExternalFile.this::downloadFinished);
        });

        Optional<ExternalFileType> suggestedType = Optional.empty();
        if (mimeType != null) {
            LOGGER.debug("MIME Type suggested: " + mimeType);
            suggestedType = ExternalFileTypes.getInstance().getExternalFileTypeByMimeType(mimeType);
        }
        // Then, while the download is proceeding, let the user choose the details of the file:
        String suffix;
        if (suggestedType.isPresent()) {
            suffix = suggestedType.get().getExtension();
        } else {
            // If we didn't find a file type from the MIME type, try based on extension:
            suffix = getSuffix(res);
            if (suffix == null) {
                suffix = "";
            }
            suggestedType = ExternalFileTypes.getInstance().getExternalFileTypeByExt(suffix);
        }

        String suggestedName = getSuggestedFileName(suffix);
        List<String> fDirectory = databaseContext.getFileDirectory();
        String directory;
        if (fDirectory.isEmpty()) {
            directory = null;
        } else {
            directory = fDirectory.get(0);
        }
        final String suggestDir = directory == null ? System.getProperty("user.home") : directory;
        File file = new File(new File(suggestDir), suggestedName);
        FileListEntry entry = new FileListEntry("", file.getCanonicalPath(), suggestedType);
        editor = new FileListEntryEditor(frame, entry, true, false, databaseContext);
        editor.getProgressBar().setIndeterminate(true);
        editor.setOkEnabled(false);
        editor.setExternalConfirm(new ConfirmCloseFileListEntryEditor() {

            @Override
            public boolean confirmClose(FileListEntry entry) {
                File f = directory == null ? new File(entry.link) : expandFilename(directory, entry.link);
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
            }
        });
        if (dontShowDialog) {
            return;
        } else {
            editor.setVisible(true, false);
        }
        // Editor closed. Go on:
        if (editor.okPressed()) {
            File toFile = directory == null ? new File(entry.link) : expandFilename(directory, entry.link);
            String dirPrefix;
            if (directory == null) {
                dirPrefix = null;
            } else {
                if (directory.endsWith(System.getProperty("file.separator"))) {
                    dirPrefix = directory;
                } else {
                    dirPrefix = directory + System.getProperty("file.separator");
                }
            }

            try {
                boolean success = FileUtil.copyFile(tmp, toFile, true);
                if (!success) {
                    // OOps, the file exists!
                    LOGGER.error("File already exists! DownloadExternalFile.download()");
                }

                // If the local file is in or below the main file directory, change the
                // path to relative:
                if ((directory != null) && entry.link.startsWith(directory) &&
                        (entry.link.length() > dirPrefix.length())) {
                    entry = new FileListEntry(entry.description, entry.link.substring(dirPrefix.length()), entry.type);
                }

                callback.downloadComplete(entry);
            } catch (IOException ex) {
                LOGGER.warn("Problem downloading file", ex);
            }

            if (!tmp.delete()) {
                LOGGER.info("Cannot delete temporary file");
            }
        } else {
            // Cancelled. Just delete the temp file:
            if (downloadFinished && !tmp.delete()) {
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
        String dirPrefix = directory + System.getProperty("file.separator");
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
    // FIXME: will break download if no bibtexkey is present!
    private String getSuggestedFileName(String suffix) {
        String plannedName = bibtexKey == null ? "set-filename" : bibtexKey;
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
     * Look for the last '.' in the link, and return the following characters.
     * This gives the extension for most reasonably named links.
     *
     * @param link The link
     * @return The suffix, excluding the dot (e.g. "pdf")
     */
    private String getSuffix(final String link) {
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
                if (strippedLink.substring(strippedLinkIndex + 1).indexOf('/') > 0) {
                    return "";
                } else {
                    return suffix; // return the first one we found, anyway.
                }
            } else {
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (link.substring(index + 1).indexOf('/') > 0) {
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
     * Callback interface that users of this class must implement in order to receive
     * notification when download is complete.
     */
    @FunctionalInterface
    public interface DownloadCallback {
        void downloadComplete(FileListEntry file);
    }
}

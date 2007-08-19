package net.sf.jabref.external;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.net.URLDownload;

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
    private JabRefFrame frame;
    private MetaData metaData;
    private String bibtexKey;
    private FileListEntryEditor editor;
    private boolean downloadFinished = false;

    public DownloadExternalFile(JabRefFrame frame, MetaData metaData, String bibtexKey) {

        this.frame = frame;
        this.metaData = metaData;
        this.bibtexKey = bibtexKey;
    }

    /**
     * Start a download.
     *
     * @param callback The object to which the filename should be reported when download
     *                 is complete.
     */
    public void download(final DownloadCallback callback) throws IOException {

        final String res = JOptionPane.showInputDialog(frame,
                Globals.lang("Enter URL to download"));

        if (res == null || res.trim().length() == 0)
            return;

        // First of all, start the download itself in the background to a temporary file:
        final File tmp = File.createTempFile("jabref_download", "tmp");
        tmp.deleteOnExit();
        (new Thread() {
            public void run() {

                try {

                    URL url = new URL(res);
                    URLDownload udl = new URLDownload(frame, url, tmp);
                    try {
                        udl.download();
                    } catch (IOException e2) {
                        JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL: "
                                + e2.getMessage()), Globals.lang("Download file"),
                                JOptionPane.ERROR_MESSAGE);
                        Globals.logger("Error while downloading " + url.toString());
                        return;
                    }

                    // Download finished: call the method that stops the progress bar etc.:
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            downloadFinished();
                        }
                    });


                } catch (MalformedURLException e1) {
                    JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL"), Globals
                            .lang("Download file"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }).start();

        // Then, while the download is proceeding, let the user choose the details of the file:
        String suffix = getSuffix(res);
        String suggestedName = bibtexKey != null ? getSuggestedFileName(res, suffix) : "";
        final String directory = getFileDirectory(res);
        File file = new File(new File(directory), suggestedName);
        FileListEntry entry = new FileListEntry("", bibtexKey != null ? file.getPath() : "",
                Globals.prefs.getExternalFileTypeByExt(suffix));
        editor = new FileListEntryEditor(frame, entry, true, metaData);
        editor.getProgressBar().setIndeterminate(true);
        editor.setOkEnabled(false);
        editor.setExternalConfirm(new ConfirmCloseFileListEntryEditor() {
            public boolean confirmClose(FileListEntry entry) {
                File f = expandFilename(directory, entry.getLink());
                if (f.isDirectory()) {
                    JOptionPane.showMessageDialog(frame,
                            Globals.lang("Target file cannot be a directory."), Globals.lang("Download file"),
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (f.exists()) {
                    return JOptionPane.showConfirmDialog
                        (frame, "'"+f.getName()+"' "+Globals.lang("exists. Overwrite file?"),
                        Globals.lang("Download file"), JOptionPane.OK_CANCEL_OPTION)
                            == JOptionPane.OK_OPTION;
                } else
                    return true;
            }
        });
        editor.setVisible(true);
        // Editor closed. Go on:
        if (editor.okPressed()) {
            String dirPrefix = directory+System.getProperty("file.separator");
            File toFile = expandFilename(directory, entry.getLink());
            try {
                boolean success = Util.copyFile(tmp, toFile, true);
                if (!success) {
                    // OOps, the file exists!
                    System.out.println("File already exists! DownloadExternalFile.download()");
                }

                // If the local file is in or below the main file directory, change the
                // path to relative:
                if (entry.getLink().startsWith(directory) &&
                        (entry.getLink().length() > dirPrefix.length())) {
                    entry.setLink(entry.getLink().substring(dirPrefix.length()));
                }

                callback.downloadComplete(entry);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            tmp.delete();
        }
        else {
            // Cancelled. Just delete the temp file:
            if (downloadFinished)
                tmp.delete();
        }

    }

    /**
     * Construct a File object pointing to the file linked, whether the link is
     * absolute or relative to the main directory.
     * @param directory The main directory.
     * @param link The absolute or relative link.
     * @return The expanded File.
     */
    private File expandFilename(String directory, String link) {
        File toFile = new File(link);
        // If this is a relative link, we should perhaps append the directory:
        String dirPrefix = directory+System.getProperty("file.separator");
        if (!toFile.isAbsolute()) {
            toFile = new File(dirPrefix+link);
        }
        return toFile;
    }

    /**
     * This is called by the download thread when download is completed.
     */
    public void downloadFinished() {
        downloadFinished = true;
        editor.getProgressBar().setVisible(false);
        editor.getProgressBarLabel().setVisible(false);
        editor.setOkEnabled(true);
        editor.getProgressBar().setValue(editor.getProgressBar().getMaximum());
    }

    public String getSuggestedFileName(String res, String suffix) {
        if (suffix == null) {
            System.out.println("Link has no obvious extension (DownloadExternalFile.download()");
        }

        String plannedName = bibtexKey + "." + suffix;

        /*
        * [ 1548875 ] download pdf produces unsupported filename
        *
        * http://sourceforge.net/tracker/index.php?func=detail&aid=1548875&group_id=92314&atid=600306
        *
        */
        if (Globals.ON_WIN) {
            plannedName = plannedName.replaceAll(
                    "\\?|\\*|\\<|\\>|\\||\\\"|\\:|\\.$|\\[|\\]", "");
        } else if (Globals.ON_MAC) {
            plannedName = plannedName.replaceAll(":", "");
        }

        return plannedName;
    }

    /**
     * Look for the last '.' in the link, and returnthe following characters.
     * This gives the extension for most reasonably named links.
     *
     * @param link The link
     * @return The suffix, excluding the dot (e.g. ".pdf")
     */
    public String getSuffix(String link) {
        int index = link.lastIndexOf('.');
        if ((index <= 0) || (index == link.length() - 1)) // No occurence, or at the end
            return null;
        return link.substring(index + 1);
    }

    public String getFileDirectory(String link) {
        // TODO: getFileDirectory()
        return metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
    }

    /**
     * Callback interface that users of this class must implement in order to receive
     * notification when download is complete.
     */
    public interface DownloadCallback {
        public void downloadComplete(FileListEntry file);
    }
}

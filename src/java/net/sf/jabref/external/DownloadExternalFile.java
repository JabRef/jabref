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
    private boolean dontShowDialog = false;

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
        dontShowDialog = false;
        final String res = JOptionPane.showInputDialog(frame,
                Globals.lang("Enter URL to download"));

        if (res == null || res.trim().length() == 0)
            return;

        URL url = null;
        try {
            url = new URL(res);
        } catch (MalformedURLException ex1) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL"), Globals
                .lang("Download file"), JOptionPane.ERROR_MESSAGE);
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
        URLDownload udl = null;

        // First of all, start the download itself in the background to a temporary file:
        final File tmp = File.createTempFile("jabref_download", "tmp");
        tmp.deleteOnExit();
        //long time = System.currentTimeMillis();
        try {
            udl = new URLDownload(frame, url, tmp);
            // TODO: what if this takes long time?
            // TODO: stop editor dialog if this results in an error:
            udl.openConnectionOnly(); // Read MIME type
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL")+": "
                    + ex.getMessage(), Globals.lang("Download file"),
                    JOptionPane.ERROR_MESSAGE);
            Globals.logger("Error while downloading " + "'" + res + "'");
            return;
        }
        final URL urlF = url;
        final URLDownload udlF = udl;
        //System.out.println("Time: "+(System.currentTimeMillis()-time));
        (new Thread() {
            public void run() {

                try {
                    udlF.download();
                } catch (IOException e2) {
                    dontShowDialog = true;
                    if ((editor != null) && (editor.isVisible()))
                        editor.setVisible(false, false);
                    JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL")+": "
                            + e2.getMessage(), Globals.lang("Download file"),
                            JOptionPane.ERROR_MESSAGE);
                    Globals.logger("Error while downloading " + "'" + urlF.toString()+ "'");
                    return;
                }

                // Download finished: call the method that stops the progress bar etc.:
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        downloadFinished();
                    }
                });
            }
        }).start();

        ExternalFileType suggestedType = null;
        if (udl.getMimeType() != null) {
            System.out.println("mimetype:"+udl.getMimeType());
            suggestedType = Globals.prefs.getExternalFileTypeByMimeType(udl.getMimeType());
            /*if (suggestedType != null)
                System.out.println("Found type '"+suggestedType.getName()+"' by MIME type '"+udl.getMimeType()+"'");*/
        }
        // Then, while the download is proceeding, let the user choose the details of the file:
        String suffix;
        if (suggestedType != null) {
            suffix = suggestedType.getExtension();   
        }
        else {
            // If we didn't find a file type from the MIME type, try based on extension:
            suffix = getSuffix(res);
            suggestedType = Globals.prefs.getExternalFileTypeByExt(suffix);
        }

        String suggestedName = bibtexKey != null ? getSuggestedFileName(suffix) : "";
        String fDirectory = getFileDirectory(res);
        if ((fDirectory != null) && fDirectory.trim().equals(""))
            fDirectory = null;
        final String directory = fDirectory;
        final String suggestDir = directory != null ? directory : System.getProperty("user.home");
        File file = new File(new File(suggestDir), suggestedName);
        FileListEntry entry = new FileListEntry("", bibtexKey != null ? file.getCanonicalPath() : "",
                suggestedType);
        editor = new FileListEntryEditor(frame, entry, true, false, metaData);
        editor.getProgressBar().setIndeterminate(true);
        editor.setOkEnabled(false);
        editor.setExternalConfirm(new ConfirmCloseFileListEntryEditor() {
            public boolean confirmClose(FileListEntry entry) {
                File f = directory != null ? expandFilename(directory, entry.getLink())
                        : new File(entry.getLink());
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
        if (!dontShowDialog) // If an error occured with the URL, this flag may have been set
            editor.setVisible(true, false);
        else
            return;
        // Editor closed. Go on:
        if (editor.okPressed()) {
            File toFile = directory != null ? expandFilename(directory, entry.getLink())
                    : new File(entry.getLink());
            String dirPrefix;
            if (directory != null) {
                if (!directory.endsWith(System.getProperty("file.separator")))
                    dirPrefix = directory+System.getProperty("file.separator");
                else
                    dirPrefix = directory;
            } else
                dirPrefix = null;

            try {
                boolean success = Util.copyFile(tmp, toFile, true);
                if (!success) {
                    // OOps, the file exists!
                    System.out.println("File already exists! DownloadExternalFile.download()");
                }

                // If the local file is in or below the main file directory, change the
                // path to relative:
                if ((directory != null) && entry.getLink().startsWith(directory) &&
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

    public String getSuggestedFileName(String suffix) {
        
        String plannedName = bibtexKey;
        if (suffix.length() > 0)
            plannedName += "." + suffix;

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
     * @return The suffix, excluding the dot (e.g. "pdf")
     */
    public String getSuffix(final String link) {
        String strippedLink = link;
        try {
            // Try to strip the query string, if any, to get the correct suffix:
            URL url = new URL(link);
            if ((url.getQuery() != null) && (url.getQuery().length() < link.length()-1)) {
                strippedLink = link.substring(0, link.length()-url.getQuery().length()-1);
            }
        } catch (MalformedURLException e) {
            // Don't report this error, since this getting the suffix is a non-critical
            // operation, and this error will be triggered and reported elsewhere.
        }
        // First see if the stripped link gives a reasonable suffix:
        String suffix;
        int index = strippedLink.lastIndexOf('.');
        if ((index <= 0) || (index == strippedLink.length() - 1)) // No occurence, or at the end
            suffix = null;
        else suffix = strippedLink.substring(index + 1);
        if (Globals.prefs.getExternalFileTypeByExt(suffix) != null) {
            return suffix;
        }
        else {
            // If the suffix doesn't seem to give any reasonable file type, try
            // with the non-stripped link:
            index = link.lastIndexOf('.');
            if ((index <= 0) || (index == strippedLink.length() - 1)) {
                // No occurence, or at the end
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (suffix.indexOf('/') > 0)
                    return "";
                else
                    return suffix; // return the first one we found, anyway.
            }
            else {
                 // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (link.substring(index + 1).indexOf('/') > 0)
                    return "";
                else
                    return link.substring(index + 1);
            }
        }

    }

    public String getFileDirectory(String link) {
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

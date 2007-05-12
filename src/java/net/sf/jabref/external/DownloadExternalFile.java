package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.net.URLDownload;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

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
    public void download(final FileListEditor callback) throws IOException {

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
        String suggestedName = getSuggestedFileName(res, suffix);
        String directory = getFileDirectory(res);
        File file = new File(new File(directory), suggestedName);
        FileListEntry entry = new FileListEntry("", file.getPath(),
                Globals.prefs.getExternalFileTypeByExt(suffix));
        editor = new FileListEntryEditor(frame, entry, true);
        editor.getProgressBar().setIndeterminate(true);
        editor.setOkEnabled(false);
        editor.setVisible(true);
        // Editor closed. Go on:
        if (editor.okPressed()) {
            File toFile = new File(entry.getLink());
            // If this is a relative link, we should perhaps append the directory:
            if (!toFile.isAbsolute()) {
                toFile = new File(directory+System.getProperty("file.separator")+entry.getLink());
            }
            try {
                boolean success = Util.copyFile(tmp, toFile, false);
                if (!success) {
                    // OOps, the file exists!
                    System.out.println("File already exists! DownloadExternalFile.download()");
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
        /*
        if (!new File(directory).exists()) {
            JOptionPane.showMessageDialog(frame, Globals.lang(
                    "Could not find download directory: %0", directory),
                    Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
            return;
        }



        String textToSet = file.getPath();
        if (textToSet.startsWith(directory)) {
            // Construct path relative to pdf base dir
            textToSet = textToSet.substring(directory.length(), textToSet.length());

            // Remove leading path separator
            if (textToSet.startsWith(File.separator)) {
                textToSet = textToSet.substring(File.separator.length());
            }
        }
        */
        //callback.downloadComplete(textToSet);

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
        return "/home/alver";
    }
}

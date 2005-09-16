package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.net.URLDownload;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DnDConstants;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 7, 2005
 * Time: 7:17:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExternalFilePanel extends JPanel {

    private JButton browseBut, download, auto;
    private EntryEditor entryEditor;
    private JabRefFrame frame;
    private OpenFileFilter off;
    private BibtexEntry entry = null;

    public ExternalFilePanel(final String fieldName, final BibtexEntry entry, final OpenFileFilter off) {
        this(null, null, fieldName, off, null);
        this.entry = entry;
    }

    public ExternalFilePanel(final JabRefFrame frame, final EntryEditor entryEditor,
                             final String fieldName, final OpenFileFilter off, final FieldEditor editor) {

        this.frame = frame;
        this.off = off;
        this.entryEditor = entryEditor;

        setLayout(new GridLayout(2, 1));

        browseBut = new JButton(Globals.lang("Browse"));
        download = new JButton(Globals.lang("Download"));
        auto = new JButton(Globals.lang("Auto"));
        //((JComponent) editor).addMouseListener(new EntryEditor.ExternalViewerListener());

        browseBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browseFile(fieldName, editor);
                //editor.setText(chosenValue);
                entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
            }
        });
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downLoadFile(fieldName, editor, frame);
            }
        });

        auto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSetFile(fieldName, editor);
            }
        });

        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(1,2));
        add(browseBut);
        pan.add(auto);
        pan.add(download);
        add(pan);

        // Add drag and drop support to the field
        if (editor != null)
            ((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
                    DnDConstants.ACTION_NONE, new UrlDragDrop(entryEditor, frame, editor)));
    }

    /**
     * Change which entry this panel is operating on. This is used only when this panel
     * is not attached to an entry editor.
     */
    public void setEntry(BibtexEntry entry) {
        this.entry = entry;
    }

    protected Object getKey() {
        return (entry != null ? entry.getField(Globals.KEY_FIELD) :
            entryEditor.entry.getField(Globals.KEY_FIELD));
    }

    protected void output(String s) {
        if (frame != null)
            frame.output(s);
    }

    public void browseFile(final String fieldName, final FieldEditor editor) {
        String directory = Globals.prefs.get(fieldName+"Directory");
        if ((directory != null) && directory.equals(""))
            directory = null;

        String dir = editor.getText(), retVal = null;

        if ((directory == null) || !(new File(dir)).isAbsolute()) {
            if (directory != null)
                dir = directory;
            else
                dir = Globals.prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");
        }

        String chosenFile =
                Globals.getNewFile(frame, Globals.prefs, new File(dir), "."+fieldName,
                        JFileChooser.OPEN_DIALOG, false);

        if (chosenFile != null) {
            File newFile = new File(chosenFile);
            String position = newFile.getParent();

            if ((directory != null) && position.startsWith(directory)) {
                // Construct path relative to pdf base dir
                String relPath =
                        position.substring(directory.length(), position.length()) + File.separator
                        + newFile.getName();

                // Remove leading path separator
                if (relPath.startsWith(File.separator)) {
                    relPath = relPath.substring(File.separator.length(), relPath.length());

                    // Set relative path as field value
                }

                retVal = relPath;
            } else
                retVal = newFile.getPath();

            editor.setText(retVal);
            Globals.prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
        }

    }


    public void downLoadFile(final String fieldName, final FieldEditor editor, final Component parent) {
        String res =
                JOptionPane.showInputDialog(parent,
                        Globals.lang("Enter URL to download"));

        if (res != null) {
            class Downloader extends Thread {
                String res;

                public Downloader(String res) {
                    this.res = res;
                }

                public void run() {
                    URL url;
                    String textToSet = editor.getText();
                    editor.setEnabled(false);
                    try {
                        editor.setText(Globals.lang("Downloading..."));
                        url = new URL(res);

                        String suffix = off.getSuffix(res);
			            if (suffix == null)
			            suffix = "."+fieldName.toLowerCase();

                        String plannedName = null;
                        if (getKey() != null)
                            plannedName = getKey() + suffix;
                        else {
                            plannedName = JOptionPane.showInputDialog(parent,
                                    Globals.lang("BibTeX key not set. Enter a name for the downloaded file"));
                            if (plannedName == null)
                                return;

                            if (!off.accept(plannedName))
                                plannedName += suffix;
                        }
                        File file = new File(new File(Globals.prefs.get(fieldName+"Directory")), plannedName);

                        URLDownload udl = new URLDownload(parent, url, file);
                        output(Globals.lang("Downloading..."));

                        try {
                            udl.download();
                        } catch (IOException e2) {
                            JOptionPane.showMessageDialog(parent, Globals.lang("Invalid URL: "+e2.getMessage()),
                                    Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                            Globals.logger("Error while downloading " + url.toString());
                        }

                        output(Globals.lang("Download completed"));
                        String filename = file.getPath();
                        //System.out.println(filename);
                        String directory = Globals.prefs.get(fieldName+"Directory");
                        if (filename.startsWith(directory)) {
                            // Construct path relative to pdf base dir
                            String relPath = filename.substring(directory.length(), filename.length());

                            // Remove leading path separator
                            if (relPath.startsWith(File.separator)) {
                                relPath = relPath.substring(File.separator.length(), relPath.length());
                            }
                            filename = relPath;
                        }
                        textToSet = filename;
                       //editor.setText(filename);
                        SwingUtilities.invokeLater(new Thread() {
                            public void run() {
                                if (entryEditor != null)
                                    entryEditor.updateField(editor);
                            }
                        });
                    } catch (MalformedURLException e1) {
                        JOptionPane.showMessageDialog(parent, "Invalid URL: "+e1.getMessage(),
                                "Download file", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        editor.setText(textToSet);
                        editor.setEnabled(true);
                    }
                }
            }


            (new Downloader(res)).start();
        }
    }

    /**
     * Starts a thread that searches the external file directory for the given field name,
     * including subdirectories, and looks for files named after the current entry's bibtex
     * key. Returns a reference to the thread for callers that may want to wait for the thread
     * to finish (using join()).
     *
     * @param fieldName The field to set.
     * @param editor An EntryEditor instance where to set the value found.
     * @return A reference to the Thread that performs the operation.
     */
    public Thread autoSetFile(final String fieldName, final FieldEditor editor) {
        Object o = getKey();
        if ((o == null) || (Globals.prefs.get(fieldName+"Directory") == null)) {
            output(Globals.lang("You must set both BibTeX key and %0 directory", fieldName.toUpperCase()) + ".");
            return null;
        }
        output(Globals.lang("Searching for %0 file", fieldName.toUpperCase()) + " '" + o +
                "."+fieldName+"'...");
        Thread t = (new Thread() {
            public void run() {
                Object o = getKey();
                String found = Util.findPdf((String) o, fieldName, Globals.prefs.get(fieldName+"Directory"), off);
                if (found != null) {
                    editor.setText(found);
                    if (entryEditor != null)
                        entryEditor.updateField(editor);
                    output(Globals.lang("%0 field set", fieldName.toUpperCase()) + ".");
                } else {
                    output(Globals.lang("No %0 found", fieldName.toUpperCase()) + ".");
                }

            }
        });

        t.start();
        return t;

    }

}

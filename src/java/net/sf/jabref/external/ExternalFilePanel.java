package net.sf.jabref.external;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.EntryEditor;
import net.sf.jabref.FieldEditor;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.OpenFileFilter;
import net.sf.jabref.UrlDragDrop;
import net.sf.jabref.Util;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.util.XMPUtil;

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
    private MetaData metaData;

    public ExternalFilePanel(final String fieldName, final MetaData metaData,
                             final BibtexEntry entry, final OpenFileFilter off) {
        this(null, metaData, null, fieldName, off, null);
        this.entry = entry;
    }

    public ExternalFilePanel(final JabRefFrame frame, final MetaData metaData,
                             final EntryEditor entryEditor,
                             final String fieldName, final OpenFileFilter off, final FieldEditor editor) {

        this.frame = frame;
        this.metaData = metaData;
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

    public BibtexEntry getEntry(){
        return (entry != null ? entry : entryEditor.getEntry());
    }
    
    protected Object getKey() {
    	return getEntry().getField(BibtexFields.KEY_FIELD);
    }

    protected void output(String s) {
        if (frame != null)
            frame.output(s);
    }

    public void pushXMP(String fieldName, FieldEditor editor) {
		
        // Find the default directory for this field type, if any:
        String dir = metaData.getFileDirectory(fieldName);
        File file = null;
        if (dir != null) {
            File tmp = Util.expandFilename(editor.getText(), dir);
            if (tmp != null)
                file = tmp;
        }
        
        if (file == null){
        	file = new File(editor.getText());
        }
        
        if (file == null){
        	output(Globals.lang("No file associated"));
        }
        
        try {
			XMPUtil.writeXMP(file, getEntry());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    
    
    public void browseFile(final String fieldName, final FieldEditor editor) {

        String directory = metaData.getFileDirectory(fieldName);
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
                Globals.getNewFile(frame, new File(dir), "."+fieldName,
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


    public void downLoadFile(final String fieldName,
                             final FieldEditor editor, final Component parent) {

        String res = JOptionPane.showInputDialog(parent,
                        Globals.lang("Enter URL to download"));

        if (res != null) {
            class Downloader extends Thread {
                String res;
                BibtexEntry targetEntry = null;

                public Downloader(String res) {
                    this.res = res;
                    // If this panel belongs in an entry editor, note which entry is
                    // currently shown:
                    if (entryEditor != null)
                        targetEntry = entryEditor.getEntry();
                }

                public void run() {
                    URL url;
                    String textToSet = editor.getText();
                    editor.setEnabled(false);
                    boolean updateEditor = true;
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

                        // Find the default directory for this field type:
                        String directory = metaData.getFileDirectory(fieldName);
                        System.out.println(directory);
                        File file = new File(new File(directory), plannedName);

                        URLDownload udl = new URLDownload(parent, url, file);
                        output(Globals.lang("Downloading..."));

                        try {
                            udl.download();
                        } catch (IOException e2) {
                            JOptionPane.showMessageDialog(parent, Globals.lang("Invalid URL: "+e2.getMessage()),
                                    Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                            Globals.logger("Error while downloading " + url.toString());
                        }

                        // Check if we should update the editor text field, or update the
                        // target entry directly:
                        updateEditor = (entryEditor == null) ||
                                (entryEditor.getEntry() == targetEntry);
                        output(Globals.lang("Download completed"));
                        String filename = file.getPath();

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
                        if (updateEditor)
                            SwingUtilities.invokeLater(new Thread() {
                                public void run() {
                                    if (entryEditor != null)
                                        entryEditor.updateField(editor);
                                }
                            });
                        else {
                            // Editor has probably changed to show a different entry. So
                            // we must update the target entry directly and not set the
                            // text of the editor.
                            targetEntry.setField(fieldName, textToSet);
                        }
                    } catch (MalformedURLException e1) {
                        JOptionPane.showMessageDialog(parent, "Invalid URL: "+e1.getMessage(),
                                "Download file", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        if (updateEditor) {
                            System.out.println("Juuu");
                            editor.setText(textToSet);
                            editor.setEnabled(true);
                        }
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

                // Find the default directory for this field type:
                String dir = metaData.getFileDirectory(fieldName);

                String found = Util.findPdf((String) o, fieldName, dir, off);

                // To activate findFile:
                // String found = Util.findFile(getEntry(), null, dir, ".*[bibtexkey].*");
                
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

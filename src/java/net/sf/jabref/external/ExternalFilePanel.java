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

    public ExternalFilePanel(final JabRefFrame frame, final EntryEditor entryEditor,
                             final String fieldName, final FieldEditor editor) {

        this.entryEditor = entryEditor;
        setLayout(new GridLayout(3, 1));

        browseBut = new JButton(Globals.lang("Browse"));
        download = new JButton(Globals.lang("Download"));
        auto = new JButton(Globals.lang("Auto"));
        //((JComponent) editor).addMouseListener(new EntryEditor.ExternalViewerListener());

        browseBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String directory = Globals.prefs.get(fieldName+"Directory");
                if ((directory != null) && directory.equals(""))
                    directory = null;

                String dir = editor.getText();

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

                        editor.setText(relPath);
                    } else
                        editor.setText(newFile.getPath());

                    Globals.prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
                    entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                }
            }
        });
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String res =
                        JOptionPane.showInputDialog((Component) editor,
                                Globals.lang("Enter URL to download"));

                if (res != null) {
                    class Downloader extends Thread {
                        String res;

                        public Downloader(String res) {
                            this.res = res;
                        }

                        public void run() {
                            URL url;
                            try {
                                url = new URL(res);

                                String plannedName = null;
                                if (entryEditor.entry.getField(Globals.KEY_FIELD) != null)
                                    plannedName = entryEditor.entry.getField(Globals.KEY_FIELD) + "."+fieldName;
                                else {
                                    plannedName = JOptionPane.showInputDialog((Component) editor,
                                            Globals.lang("BibTeX key not set. Enter a name for the downloaded file"));
                                    if (plannedName == null)
                                        return;
                                    if (!plannedName.substring(4).equals("."+fieldName))
                                        plannedName += "."+fieldName;
                                }
                                File file = new File(new File(Globals.prefs.get(fieldName+"Directory")), plannedName);

                                URLDownload udl = new URLDownload((Component) editor, url, file);
                                frame.output(Globals.lang("Downloading..."));

                                try {
                                    udl.download();
                                } catch (IOException e2) {
                                    JOptionPane.showMessageDialog((Component) editor, Globals.lang("Invalid URL"),
                                            Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                                    Globals.logger("Error while downloading " + url.toString());
                                }

                                frame.output(Globals.lang("Download completed"));
                                String filename = file.getPath();
                                System.out.println(filename);
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

                                //ed.setText(file.toURL().toString());
                                editor.setText(filename);
                                SwingUtilities.invokeLater(new Thread() {
                                    public void run() {
                                        entryEditor.updateField(editor);
                                    }
                                });
                            } catch (MalformedURLException e1) {
                                JOptionPane.showMessageDialog((Component) editor, "Invalid URL",
                                        "Download file", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    ;

                    (new Downloader(res)).start();

                }
            }
        });

        auto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object o = entryEditor.entry.getField(Globals.KEY_FIELD);
                if ((o == null) || (Globals.prefs.get(fieldName+"Directory") == null)) {
                    frame.output(Globals.lang("You must set both bibtex key and %0 directory", fieldName.toUpperCase()) + ".");
                    return;
                }
                frame.output(Globals.lang("Searching for %0 file", fieldName.toUpperCase()) + " '" + o +
                        "."+fieldName+"'...");
                (new Thread() {
                    public void run() {
                        Object o = entryEditor.entry.getField(Globals.KEY_FIELD);
                        String found = Util.findPdf((String) o, fieldName, Globals.prefs.get(fieldName+"Directory"));
                        if (found != null) {
                            editor.setText(found);
                            entryEditor.updateField(editor);
                            frame.output(Globals.lang("%0 field set", fieldName.toUpperCase()) + ".");
                        } else {
                            frame.output(Globals.lang("No %0 found", fieldName.toUpperCase()) + ".");
                        }
                    }
                }).start();
            }
        });

        add(browseBut);
        add(auto);
        add(download);

        // Add drag and drop support to the field
        ((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
                DnDConstants.ACTION_NONE, new UrlDragDrop(entryEditor, frame, editor)));
    }
}

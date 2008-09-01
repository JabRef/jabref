package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.sf.jabref.BaseAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;


/**
 * This class handles the task of looking up all external files linked for a set
 * of entries. This is useful for tasks where the user wants e.g. to send a database
 * with external files included.
 */
public class AccessLinksForEntries {

    /**
     * Look up all external files linked from (at least) one of the entries in a set.
     * This method does not verify the links.
     *
     * @param entries The set of entries.
     * @return A list of FileListEntry objects pointing to the external files.
     */
    public static List<FileListEntry> getExternalLinksForEntries(List<BibtexEntry> entries) {
        List<FileListEntry> files = new ArrayList<FileListEntry>();
        FileListTableModel model = new FileListTableModel();
        for (Iterator<BibtexEntry> iterator = entries.iterator(); iterator.hasNext();) {
            BibtexEntry entry = iterator.next();
            String links = entry.getField(GUIGlobals.FILE_FIELD);
            if (links == null)
                continue;
            model.setContent(links);
            for (int i=0; i<model.getRowCount(); i++)
                files.add(model.getEntry(i));
        }
        return files;
    }

    /**
     * Take a list of external links and copy the referred files to a given directory.
     * This method should be run off the Event Dispatch Thread. A progress bar, if given,
     * will be updated on the EDT.
     *
     * @param files The list of file links.
     * @param toDir The directory to copy the files to.
     * @param metaData The MetaData for the database containing the external links. This is needed
     *  because the database might have its own file directory.
     * @param prog A JProgressBar which will be updated to show the progress of the process.
     *  This argument can be null if no progress bar is needed.
     * @param deleteOriginalFiles if true, the files in their original locations will be deleted
     *  after copying, for each file whose source directory is different from the destination
     *  directory differs.
     * @param callback An ActionListener which should be notified when the process is finished.
     *  This parameter can be null if no callback is needed.
     */
    public static void copyExternalLinksToDirectory(final List<FileListEntry> files, File toDir,
                                                    MetaData metaData, final JProgressBar prog,
                                                    boolean deleteOriginalFiles,
                                                    final ActionListener callback) {

        if (prog != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                prog.setMaximum(files.size());
                prog.setValue(0);
                prog.setIndeterminate(false);
            }
        });

        Set<String> fileNames = new HashSet<String>();

        int i=0;

        for (Iterator<FileListEntry> iterator = files.iterator(); iterator.hasNext();) {
            FileListEntry entry = iterator.next();
            File file = new File(entry.getLink());

            // We try to check the extension for the file:
            String name = file.getName();
            int pos = name.lastIndexOf('.');
            String extension = ((pos >= 0) && (pos < name.length() - 1)) ? name.substring(pos + 1)
                .trim().toLowerCase() : null;

            // Find the default directory for this field type, if any:
            String dir = metaData.getFileDirectory(extension);
            // Include the standard "file" directory:
            String fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);

            // Include the directory of the bib file:
            String databaseDir = metaData.getFile().getParent();
            File tmp = Util.expandFilename(entry.getLink(),
                    new String[] { dir, fileDir, databaseDir });
            if (tmp != null)
                file = tmp;

            // Check if we have arrived at an existing file:
            if (file.exists()) {
                if (fileNames.contains(name)) {
                    // Oops, a file of that name already exists....
                }
                else {
                    fileNames.add(name);
                    File destination = new File(toDir, name);

                    // Check if the source and destination locations differ:
                    if (!destination.equals(file)) {
                        try {
                            // Copy the file:
                            Util.copyFile(file, destination, false);
                            // Delete the original file if requested:
                            if (deleteOriginalFiles)
                                file.delete();
                            
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    else {
                        // Destination and source is the same. Do nothing.
                    }
                    // Update progress bar:
                    i++;
                    final int j = i;

                    if (prog != null) SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            prog.setValue(j);
                        }
                    });
                }
            }
            else {
                // The link could not be resolved to an existing file.
                
            }
        }

        if (callback != null) {
            callback.actionPerformed(null);
        }
    }


    public static class SaveWithLinkedFiles extends BaseAction {
        private BasePanel panel;

        public SaveWithLinkedFiles(BasePanel panel) {

            this.panel = panel;
        }

        @Override
        public void action() throws Throwable {

            ArrayList<BibtexEntry> entries = new ArrayList<BibtexEntry>();
            BibtexEntry[] sel = panel.getSelectedEntries();
            for (int i = 0; i < sel.length; i++) {
                BibtexEntry bibtexEntry = sel[i];
                entries.add(bibtexEntry);
            }
            final List<FileListEntry> links =
                    AccessLinksForEntries.getExternalLinksForEntries(entries);
            for (Iterator<FileListEntry> iterator = links.iterator(); iterator.hasNext();) {
                FileListEntry entry = iterator.next();
                System.out.println("Link: " + entry.getLink());
            }

            final JProgressBar prog = new JProgressBar();
            prog.setIndeterminate(true);
            final JDialog diag = new JDialog(panel.frame(), false);
            diag.getContentPane().add(prog, BorderLayout.CENTER);
            diag.pack();
            diag.setLocationRelativeTo(panel.frame());
            diag.setVisible(true);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    AccessLinksForEntries.copyExternalLinksToDirectory(links,
                            new File("/home/alver/tmp"), panel.metaData(), prog, false,
                            new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    diag.dispose();
                                }
                            });
                }
            });
            t.start();

            
        }
    }


}
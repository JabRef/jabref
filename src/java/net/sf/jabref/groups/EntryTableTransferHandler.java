/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.InputEvent;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.BasePanel;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.imports.OpenDatabaseAction;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.net.URLDownload;

public class EntryTableTransferHandler extends TransferHandler {
    protected final MainTable entryTable;
    protected JabRefFrame frame;
    private BasePanel panel;
    protected DataFlavor urlFlavor;
    protected DataFlavor stringFlavor;
    protected static boolean DROP_ALLOWED = true;

    /**
     * Construct the transfer handler.
     * @param entryTable The table this transfer handler should operate on. This argument is
     * allowed to equal @null, in which case the transfer handler can assume that it
     * works for a JabRef instance with no databases open, attached to the empty tabbed pane.
     * @param frame The JabRefFrame instance.
     * @param panel The BasePanel this transferhandler works for.
     */
    public EntryTableTransferHandler(MainTable entryTable, JabRefFrame frame,
                                     BasePanel panel) {
        this.entryTable = entryTable;
        this.frame = frame;
        this.panel = panel;
        stringFlavor = DataFlavor.stringFlavor;        
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            Globals.logger("Unable to configure drag and drop for main table");
            e.printStackTrace();
        }
    }

    public int getSourceActions(JComponent c) {
        return DnDConstants.ACTION_LINK;
    }

    public Transferable createTransferable(JComponent c) {
        // This method is called when dragging stuff *from* the table, so we can assume
        // it will never be called if entryTable==null:
        return new TransferableEntrySelection(entryTable.getSelectedEntries());
    }

    // add-ons -----------------------
    

    protected boolean handleDropTransfer(String dropStr, final int dropRow) throws IOException {
        if (dropStr.startsWith("file:")) {
            // This appears to be a dragged file link and not a reference
            // format. Check if we can map this to a set of files:
            if (handleDraggedFilenames(dropStr, dropRow))
                return true;
            // If not, handle it in the normal way...
        } else if (dropStr.startsWith("http:")) {
            // This is the way URL links are received on OS X and KDE (Gnome?):
            URL url = new URL(dropStr);
            //JOptionPane.showMessageDialog(null, "Making URL: "+url.toString());
            return handleDropTransfer(url, dropRow);
        }
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();
        FileWriter fw = new FileWriter(tmpfile);
        fw.write(dropStr);
        fw.close();
        
        //System.out.println("importing from " + tmpfile.getAbsolutePath());
        
        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(new String[] { tmpfile.getAbsolutePath() } );
        
        return true;
    }

    /**
     * Handle a String describing a set of files or URLs dragged into JabRef.
     * @param s String describing a set of files or URLs dragged into JabRef
     */
    private boolean handleDraggedFilenames(String s, final int dropRow) {
        // Split into lines:
        String[] lines = s.replaceAll("\r", "").split("\n");
        List files = new ArrayList();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("file:"))
                line = line.substring(5);
            else continue;
            // Under Gnome, the link is given as file:///...., so we
            // need to strip the extra slashes:
            if (line.startsWith("//"))
                line = line.substring(2);
            File f = new File(line);
            if (f.exists()) {
                files.add(f);
            }
        }
        return handleDraggedFiles(files, dropRow);

    }

    /**
     * Handle a List containing File objects for a set of files to import.
     * @param files A List containing File instances pointing to files.
     * @param dropRow
     */
    private boolean handleDraggedFiles(List files, final int dropRow) {
        final String[] fileNames = new String[files.size()];
        int i=0;
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            fileNames[i] = file.getAbsolutePath();
            i++;
        }
        // Try to load bib files normally, and import the rest into the current database.
        // This process must be spun off into a background thread:
        new Thread(new Runnable() {
            public void run() {
                loadOrImportFiles(fileNames, dropRow);
            }
        }).start();

        return true;
    }

    /**
     * Take a set of filenames. Those with names indicating bib files are opened as such
     * if possible. All other files we will attempt to import into the current database.
     * @param fileNames The names of the files to open.
     * @param dropRow
     */
    private void loadOrImportFiles(String[] fileNames, int dropRow) {

        OpenDatabaseAction openAction = new OpenDatabaseAction(frame, false);
        ArrayList notBibFiles = new ArrayList();
        String encoding = Globals.prefs.get("defaultEncoding");
        for (int i = 0; i < fileNames.length; i++) {
            // Find the file's extension, if any:
            String extension = "";
            ExternalFileType fileType = null;
            int index = fileNames[i].lastIndexOf('.');
            if ((index >= 0) && (index < fileNames[i].length())) {
                extension = fileNames[i].substring(index+1);
                //System.out.println(extension);
                fileType = Globals.prefs.getExternalFileType(extension);
            }
            if (extension.equals("bib")) {
                File f = new File(fileNames[i]);
                try {
                    ParserResult pr = OpenDatabaseAction.loadDatabase
                            (f, encoding);
                    if ((pr == null) || (pr == ParserResult.INVALID_FORMAT)) {
                        notBibFiles.add(fileNames[i]);
                    } else {
                        openAction.addNewDatabase(pr, f, false);
                    }
                } catch (IOException e) {
                    notBibFiles.add(fileNames[i]);
                    // No error message, since we want to try importing the file?
                    //
                    //Util.showQuickErrorDialog(frame, Globals.lang("Open database"), e);
                }
            }
            else if (fileType != null) {

                // This is a linkable file. If the user dropped it on an entry,
                // we should offer options for autolinking to this files:
                if (dropRow >= 0) {
                    boolean local = true; // TODO: need to signal if this is a local or autodownloaded file
                    DroppedFileHandler dfh = new DroppedFileHandler(frame, panel); // TODO: make this an instance variable?
                    dfh.handleDroppedfile(fileNames[i], fileType, local, entryTable, dropRow);
                }
                
            }
            else notBibFiles.add(fileNames[i]);
        }

        if (notBibFiles.size() > 0) {
            String[] toImport = new String[notBibFiles.size()];
            notBibFiles.toArray(toImport);

            // Import into new if entryTable==null, otherwise into current database:
            ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
            importer.automatedImport(toImport);
        }
    }

    protected boolean handleDropTransfer(URL dropLink, int dropRow) throws IOException {
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();

        //System.out.println("Import url: " + dropLink.toString());
        //System.out.println("Temp file: "+tmpfile.getAbsolutePath());

        new URLDownload(entryTable, dropLink, tmpfile).download();

        // Import into new if entryTable==null, otherwise into current database:
        ImportMenuItem importer = new ImportMenuItem(frame, (entryTable == null));
        importer.automatedImport(new String[] { tmpfile.getAbsolutePath() } );

        return true;
    }
    
    /**
     * Imports the dropped URL or plain text as a new entry in the current database.
     * @todo It would be nice to support dropping of pdfs onto the table as a way
     *       to link them to the corresponding entries.
     */
    public boolean importData(JComponent comp, Transferable t) {
        // If the drop target is the main table, we want to record which
        // row the item was dropped on, to identify the entry if needed:
        int dropRow = -1;
        if (comp instanceof JTable) {
            dropRow = ((JTable)comp).getSelectedRow();
        }
        
        try {
            
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                //JOptionPane.showMessageDialog(null, "Received javaFileListFlavor");
                // This flavor is used for dragged file links in Windows:
                List l = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
                return handleDraggedFiles(l, dropRow);
            }
            if (t.isDataFlavorSupported(urlFlavor)) {
                URL dropLink = (URL) t.getTransferData(urlFlavor);
                return handleDropTransfer(dropLink, dropRow);
            } else if (t.isDataFlavorSupported(stringFlavor)) {

                String dropStr = (String) t.getTransferData(stringFlavor);
                //JOptionPane.showMessageDialog(null, "Received stringFlavor: "+dropStr);

                return handleDropTransfer(dropStr, dropRow);
            }
        } catch (IOException ioe) {
            System.err.println("failed to read dropped data: " + ioe.toString());
        } catch (UnsupportedFlavorException ufe) {
            System.err.println("drop type error: " + ufe.toString());
        }

        // all supported flavors failed        
        System.err.println("can't transfer input: ");
        DataFlavor inflavs[] = t.getTransferDataFlavors();
        for (int i = 0; i < inflavs.length; i++) {
            System.out.println("  " + inflavs[i].toString());
        }          

        return false;
    }


    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        if (!DROP_ALLOWED) return false;


        // accept this if any input flavor matches any of our supported flavors
        for (int i = 0; i < transferFlavors.length; i++) {
            DataFlavor inflav = transferFlavors[i];
            if (inflav.match(urlFlavor) || inflav.match(stringFlavor)
                    || inflav.match(DataFlavor.javaFileListFlavor)) return true;
        }

        //System.out.println("drop type forbidden");
        // nope, never heard of this type
        return false;
    }

    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        // action is always LINK
        super.exportAsDrag(comp, e, DnDConstants.ACTION_LINK);
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        // default implementation is OK
        super.exportDone(source, data, action);
    }

    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        // default implementation is OK
        super.exportToClipboard(comp, clip, action);
    }
}
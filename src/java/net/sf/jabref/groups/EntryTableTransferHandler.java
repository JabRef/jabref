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
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.net.URLDownload;

public class EntryTableTransferHandler extends TransferHandler {
    protected final MainTable entryTable;
    protected JabRefFrame frame;
    protected DataFlavor urlFlavor;
    protected DataFlavor stringFlavor;
    protected static boolean DROP_ALLOWED = true;
    
    public EntryTableTransferHandler(MainTable entryTable, JabRefFrame frame) {
        this.entryTable = entryTable;
        this.frame = frame;
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
        return new TransferableEntrySelection(entryTable.getSelectedEntries());
    }

    // add-ons -----------------------
    

    protected boolean handleDropTransfer(String dropStr) throws IOException {
        if (dropStr.startsWith("file:")) {
            // This appears to be a dragged file link and not a reference
            // format. Check if we can map this to a set of files:
            if (handleFileLinkSet(dropStr))
                return true;
            // If not, handle it in the normal way...
        } else if (dropStr.startsWith("http:")) {
            // This is the way URL links are received on OS X and KDE (Gnome?):
            URL url = new URL(dropStr);
            JOptionPane.showMessageDialog(null, "Making URL: "+url.toString());
            return handleDropTransfer(url);
        }
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        FileWriter fw = new FileWriter(tmpfile);
        fw.write(dropStr);
        fw.close();
        
        System.out.println("importing from " + tmpfile.getAbsolutePath());
        
        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(new String[] { tmpfile.getAbsolutePath() } );
        
        return true;
    }

    private boolean handleFileLinkSet(String s) {
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
            System.out.println(f.getPath());
            if (f.exists()) {
                files.add(f);
            }
        }
        return handleFileList(files);

    }

    /**
     * Handle a List containing File objects for a set of files to import.
     * @param files A List containing File instances pointing to files.
     */
    private boolean handleFileList(List files) {
        String[] fileNames = new String[files.size()];
        int i=0;
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            fileNames[i] = file.getAbsolutePath();
            i++;
        }
        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(fileNames);
        return true;
    }

    protected boolean handleDropTransfer(URL dropLink) throws IOException {
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        //System.out.println("Import url: " + dropLink.toString());
        //System.out.println("Temp file: "+tmpfile.getAbsolutePath());
        new URLDownload(entryTable, dropLink, tmpfile).download();

        // JabRef.importFiletypeUnknown(tmpfile.getAbsolutePath());
        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(new String[] { tmpfile.getAbsolutePath() } );

        return true;
    }
    
    /**
     * Imports the dropped URL or plain text as a new entry in the current database.
     * @todo It would be nice to support dropping of pdfs onto the table as a way
     *       to link them to the corresponding entries.
     */
    public boolean importData(JComponent comp, Transferable t) {
        try {

            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                //JOptionPane.showMessageDialog(null, "Received javaFileListFlavor");
                // This flavor is used for dragged file links in Windows:
                List l = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
                return handleFileList(l);
            }
            if (t.isDataFlavorSupported(urlFlavor)) {
                URL dropLink = (URL) t.getTransferData(urlFlavor);
                return handleDropTransfer(dropLink);
            } else if (t.isDataFlavorSupported(stringFlavor)) {

                String dropStr = (String) t.getTransferData(stringFlavor);
                //JOptionPane.showMessageDialog(null, "Received stringFlavor: "+dropStr);

                return handleDropTransfer(dropStr);
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
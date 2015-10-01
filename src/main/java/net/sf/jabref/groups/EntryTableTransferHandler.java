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
package net.sf.jabref.groups;

import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import net.sf.jabref.gui.net.MonitoredURLDownload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.TransferableFileLinkSelection;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.gui.MainTableFormat;
import net.sf.jabref.importer.ImportMenuItem;
import net.sf.jabref.importer.OpenDatabaseAction;
import net.sf.jabref.importer.ParserResult;
import spl.PdfImporter;
import spl.PdfImporter.ImportPdfFilesResult;

public class EntryTableTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;

    private final MainTable entryTable;

    private final JabRefFrame frame;

    private final BasePanel panel;

    private DataFlavor urlFlavor;

    private final DataFlavor stringFlavor;

    private static final boolean DROP_ALLOWED = true;
    
    private static final Log LOGGER = LogFactory.getLog(EntryTableTransferHandler.class);


    /**
     * Construct the transfer handler.
     *
     * @param entryTable The table this transfer handler should operate on. This argument is allowed to equal null,
     *                   in which case the transfer handler can assume that it works for a JabRef instance
     *                   with no databases open, attached to the empty tabbed pane.
     * @param frame      The JabRefFrame instance.
     * @param panel      The BasePanel this transferhandler works for.
     */
    public EntryTableTransferHandler(MainTable entryTable, JabRefFrame frame, BasePanel panel) {
        this.entryTable = entryTable;
        this.frame = frame;
        this.panel = panel;
        stringFlavor = DataFlavor.stringFlavor;
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Unable to configure drag and drop for main table", e);
        }
    }

    /**
     * Overriden to indicate which types of drags are supported (only LINK).
     */
    @Override
    public int getSourceActions(JComponent c) {
        return DnDConstants.ACTION_LINK;
    }

    /**
     * This method is called when dragging stuff *from* the table.
     */
    @Override
    public Transferable createTransferable(JComponent c) {
        if (!draggingFile) {
            /* so we can assume it will never be called if entryTable==null: */
            return new TransferableEntrySelection(entryTable.getSelectedEntries());
        }
        else {
            draggingFile = false;
            return new TransferableFileLinkSelection(panel, entryTable.getSelectedEntries());//.getTransferable();
        }
    }

    /**
     * This method is called when stuff is drag to the component.
     * 
     * Imports the dropped URL or plain text as a new entry in the current
     * database.
     * 
     */
    @Override
    public boolean importData(JComponent comp, Transferable t) {

        // If the drop target is the main table, we want to record which
        // row the item was dropped on, to identify the entry if needed:
        int dropRow = -1;
        if (comp instanceof JTable) {
            dropRow = ((JTable) comp).getSelectedRow();
        }

        try {

            // This flavor is used for dragged file links in Windows:
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // JOptionPane.showMessageDialog(null, "Received
                // javaFileListFlavor");
                @SuppressWarnings("unchecked")
                List<File> l = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                return handleDraggedFiles(l, dropRow);
            }
            // Done by MrDlib
            /*if(t.isDataFlavorSupported(MindMapNodesSelection.mindMapNodesFlavor)){
                String xml = (String)t.getTransferData(MindMapNodesSelection.mindMapNodesFlavor);
                URL mindmapURL = null;
                if(t.isDataFlavorSupported(MindMapNodesSelection.mindmapUrlFlavor)){
                    mindmapURL = (URL)t.getTransferData(MindMapNodesSelection.mindmapUrlFlavor);
                }
                List<File> files = new ArrayList<File>();
                String[] xmlNodes = xml.split("<nodeseparator>");
                for(String xmlNode : xmlNodes){
                    XMLElement element = new XMLElement();
                    element.parseString(xmlNode);
                    String link = element.getStringAttribute("Link");
                    String absoluteLink = Tools.getLink(link, mindmapURL);
                    if(absoluteLink == null) continue;
                    File file = new File(absoluteLink);
                    if(file.exists()){
                        files.add(file);
                    }
                    else{
                        try {
                            URL url = new URL(absoluteLink);
                            file = new File(url.toURI());
                            if(file.exists()){
                                files.add(file);
                            }
                        } catch (URISyntaxException e) {
                            // Todo logging
                        } catch(IllegalArgumentException e){
                            // Todo logging
                        } catch(MalformedURLException e){
                            // Todo logging
                        }
                    }
                }
                if(files.size() > 0){
                    return handleDraggedFiles(files, dropRow);
                }
                else{
                    return false;
                }
            }*/
            // Done by MrDlib
            if (t.isDataFlavorSupported(urlFlavor)) {
                URL dropLink = (URL) t.getTransferData(urlFlavor);
                return handleDropTransfer(dropLink, dropRow);
            }

            if (t.isDataFlavorSupported(stringFlavor)) {
                // JOptionPane.showMessageDialog(null, "Received stringFlavor:
                // "+dropStr);
                String dropStr = (String) t.getTransferData(stringFlavor);
                return handleDropTransfer(dropStr, dropRow);
            }

        } catch (IOException ioe) {
            System.err.println("failed to read dropped data: " + ioe);
        } catch (UnsupportedFlavorException ufe) {
            System.err.println("drop type error: " + ufe);
        }

        // all supported flavors failed
        System.err.println("can't transfer input: ");
        DataFlavor[] inflavs = t.getTransferDataFlavors();
        for (DataFlavor inflav : inflavs) {
            System.out.println("  " + inflav);
        }

        return false;
    }

    /**
     * This method is called to query whether the transfer can be imported.
     * 
     * Will return true for urls, strings, javaFileLists
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        if (!EntryTableTransferHandler.DROP_ALLOWED) {
            return false;
        }

        // accept this if any input flavor matches any of our supported flavors
        for (DataFlavor inflav : transferFlavors) {
            if (inflav.match(urlFlavor) || inflav.match(stringFlavor)
                    || inflav.match(DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }

        // System.out.println("drop type forbidden");
        // nope, never heard of this type
        return false;
    }


    private boolean draggingFile;


    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        /* TODO: add support for dragging file link from table icon into other apps */
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            int col = entryTable.columnAtPoint(me.getPoint());
            String[] res = entryTable.getIconTypeForColumn(col);
            if (res == null) {
                super.exportAsDrag(comp, e, DnDConstants.ACTION_LINK);
                return;
            }
            // We have an icon column:
            if (res == MainTableFormat.FILE) {
                System.out.println("dragging file");
                draggingFile = true;
            }
        }
        super.exportAsDrag(comp, e, DnDConstants.ACTION_LINK);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // default implementation is OK
        super.exportDone(source, data, action);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        // default implementation is OK
        super.exportToClipboard(comp, clip, action);
    }

    // add-ons -----------------------

    private boolean handleDropTransfer(String dropStr, final int dropRow) throws IOException {
        if (dropStr.startsWith("file:")) {
            // This appears to be a dragged file link and not a reference
            // format. Check if we can map this to a set of files:
            if (handleDraggedFilenames(dropStr, dropRow))
             {
                return true;
            // If not, handle it in the normal way...
            }
        } else if (dropStr.startsWith("http:")) {
            // This is the way URL links are received on OS X and KDE (Gnome?):
            URL url = new URL(dropStr);
            // JOptionPane.showMessageDialog(null, "Making URL:
            // "+url.toString());
            return handleDropTransfer(url, dropRow);
        }
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();
        FileWriter fw = new FileWriter(tmpfile);
        fw.write(dropStr);
        fw.close();

        // System.out.println("importing from " + tmpfile.getAbsolutePath());

        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(new String[] {tmpfile.getAbsolutePath()});

        return true;
    }

    /**
     * Translate a String describing a set of files or URLs dragged into JabRef
     * into a List of File objects, taking care of URL special characters.
     *
     * @param s
     *            String describing a set of files or URLs dragged into JabRef
     * @return a List<File> containing the individual file objects.
     *
     */
    public static List<File> getFilesFromDraggedFilesString(String s) {
        // Split into lines:
        String[] lines = s.replaceAll("\r", "").split("\n");
        List<File> files = new ArrayList<File>();
        for (String line1 : lines) {
            String line = line1;

            // Try to use url.toURI() to translate URL specific sequences like %20 into
            // standard characters:
            File fl = null;
            try {
                URL url = new URL(line);
                fl = new File(url.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            // Unless an exception was thrown, we should have the sanitized path:
            if (fl != null) {
                line = fl.getPath();
            } else if (line.startsWith("file:")) {
                line = line.substring(5);
            } else {
                continue;
            }
            // Under Gnome, the link is given as file:///...., so we
            // need to strip the extra slashes:
            if (line.startsWith("//")) {
                line = line.substring(2);
            }

            File f = new File(line);
            if (f.exists()) {
                files.add(f);
            }
        }
        return files;
    }

    /**
     * Handle a String describing a set of files or URLs dragged into JabRef.
     * 
     * @param s
     *            String describing a set of files or URLs dragged into JabRef
     * @param dropRow The row in the table where the files were dragged.
     * @return success status for the operation
     *
     */
    private boolean handleDraggedFilenames(String s, final int dropRow) {

        return handleDraggedFiles(EntryTableTransferHandler.getFilesFromDraggedFilesString(s), dropRow);

    }

    /**
     * Handle a List containing File objects for a set of files to import.
     * 
     * @param files
     *            A List containing File instances pointing to files.
     * @param dropRow @param dropRow The row in the table where the files were dragged.
     * @return success status for the operation
     */
    private boolean handleDraggedFiles(List<File> files, final int dropRow) {
        final String[] fileNames = new String[files.size()];
        int i = 0;
        for (File file : files) {
            fileNames[i] = file.getAbsolutePath();
            i++;
        }
        // Try to load bib files normally, and import the rest into the current
        // database.
        // This process must be spun off into a background thread:
        JabRefExecutorService.INSTANCE.execute(new Runnable() {

            @Override
            public void run() {
                // Done by MrDlib
                final ImportPdfFilesResult importRes = new PdfImporter(frame, panel, entryTable, dropRow).importPdfFiles(fileNames, frame);
                if (importRes.noPdfFiles.length > 0) {
                    loadOrImportFiles(importRes.noPdfFiles, dropRow);
                }
                //loadOrImportFiles(fileNames, dropRow);
                // Done by MrDlib
            }
        });

        return true;
    }

    /**
     * Take a set of filenames. Those with names indicating bib files are opened
     * as such if possible. All other files we will attempt to import into the
     * current database.
     * 
     * @param fileNames
     *            The names of the files to open.
     * @param dropRow success status for the operation
     */
    private void loadOrImportFiles(String[] fileNames, int dropRow) {

        OpenDatabaseAction openAction = new OpenDatabaseAction(frame, false);
        ArrayList<String> notBibFiles = new ArrayList<String>();
        String encoding = Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING);
        for (String fileName : fileNames) {
            // Find the file's extension, if any:
            String extension = "";
            ExternalFileType fileType = null;
            int index = fileName.lastIndexOf('.');
            if (index >= 0 && index < fileName.length()) {
                extension = fileName.substring(index + 1).toLowerCase();
                fileType = Globals.prefs.getExternalFileTypeByExt(extension);
            }
            if (extension.equals("bib")) {
                File f = new File(fileName);
                try {
                    ParserResult pr = OpenDatabaseAction.loadDatabase(f, encoding);
                    if (pr == null || pr == ParserResult.INVALID_FORMAT) {
                        notBibFiles.add(fileName);
                    } else {
                        openAction.addNewDatabase(pr, f, true);
                        frame.getFileHistory().newFile(fileName);
                    }
                } catch (IOException e) {
                    notBibFiles.add(fileName);
                    // No error message, since we want to try importing the
                    // file?
                    //
                    // Util.showQuickErrorDialog(frame, Globals.lang("Open database"), e);
                }
                continue;
            }

            /*
             * This is a linkable file. If the user dropped it on an entry, we
             * should offer options for autolinking to this files:
             *
             * TODO we should offer an option to highlight the row the user is on too.
             */
            if (fileType != null && dropRow >= 0) {

                /*
                 * TODO: need to signal if this is a local or autodownloaded
                 * file
                 */
                boolean local = true;

                /*
                 * TODO: make this an instance variable?
                 */
                DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                dfh.handleDroppedfile(fileName, fileType, local, entryTable, dropRow);

                continue;
            }
            /*
            			if (extension.equals("pdf")) {
            				Collection c;
            				try {
            					c = XMPUtil.readXMP(fileNames[i]);
            				} catch (IOException e1) {
            					c = null;
            					frame.output(Globals.lang("No XMP metadata found in " + fileNames[i]));
            				}

            				if (c != null && c.size() > 0) {
            					Iterator it = c.iterator();

            					BasePanel panel = frame.basePanel();

            					if (panel == null) {
            						// // Create a new, empty, database.
            						BibtexDatabase database = new BibtexDatabase();
            						frame.addTab(database, null, null, Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING),
            							true);
            						frame.output(Globals.lang("New database created."));
            						panel = frame.basePanel();
            					}

            					BibtexDatabase database = frame.basePanel().database();

            					NamedCompound ce = new NamedCompound(Glbals.lang("Drop PDF"));

            					while (it.hasNext()) {
            						BibtexEntry e = (BibtexEntry) it.next();

            						try {
            							e.setId(Util.next());
            							database.insertEntry(e);
            							ce.addEdit(new UndoableInsertEntry(database, e, panel));
            						} catch (Exception e2) {
            							// Should not happen?
            						}
            					}

            					ce.end();
            					panel.undoManager.addEdit(ce);
            					panel.markBaseChanged();
            					continue;
            				}
            			}
            			*/

            notBibFiles.add(fileName);
        }

        if (!notBibFiles.isEmpty()) {
            String[] toImport = new String[notBibFiles.size()];
            notBibFiles.toArray(toImport);

            // Import into new if entryTable==null, otherwise into current
            // database:
            ImportMenuItem importer = new ImportMenuItem(frame, entryTable == null);
            importer.automatedImport(toImport);
        }
    }

    private boolean handleDropTransfer(URL dropLink, int dropRow) throws IOException {
        File tmpfile = java.io.File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();

        // System.out.println("Import url: " + dropLink.toString());
        // System.out.println("Temp file: "+tmpfile.getAbsolutePath());

        MonitoredURLDownload.buildMonitoredDownload(entryTable, dropLink).downloadToFile(tmpfile);

        // Import into new if entryTable==null, otherwise into current database:
        ImportMenuItem importer = new ImportMenuItem(frame, entryTable == null);
        importer.automatedImport(new String[] {tmpfile.getAbsolutePath()});

        return true;
    }

}

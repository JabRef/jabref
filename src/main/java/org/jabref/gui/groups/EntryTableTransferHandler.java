package org.jabref.gui.groups;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiles.DroppedFileHandler;
import org.jabref.gui.externalfiles.TransferableFileLinkSelection;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.importer.ImportMenuItem;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.maintable.MainTable;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.util.FileHelper;
import org.jabref.pdfimport.PdfImporter;
import org.jabref.pdfimport.PdfImporter.ImportPdfFilesResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryTableTransferHandler extends TransferHandler {

    private static final boolean DROP_ALLOWED = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTableTransferHandler.class);
    private final MainTable entryTable;
    private final JabRefFrame frame;
    private final BasePanel panel;
    private DataFlavor urlFlavor;
    private final DataFlavor stringFlavor;
    private boolean draggingFile;

    /**
     * Construct the transfer handler.
     *
     * @param entryTable The table this transfer handler should operate on. This argument is allowed to equal null, in
     *            which case the transfer handler can assume that it works for a JabRef instance with no databases open,
     *            attached to the empty tabbed pane.
     * @param frame The JabRefFrame instance.
     * @param panel The BasePanel this transferhandler works for.
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
     * Overridden to indicate which types of drags are supported (only LINK).
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
        if (draggingFile) {
            draggingFile = false;
            return new TransferableFileLinkSelection(panel, entryTable.getSelectedEntries());//.getTransferable();
        } else {
            /* so we can assume it will never be called if entryTable==null: */
            return new TransferableEntrySelection(entryTable.getSelectedEntries());
        }
    }

    /**
     * This method is called when stuff is drag to the component.
     *
     * Imports the dropped URL or plain text as a new entry in the current library.
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
                List<Path> files = ((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor)).stream()
                        .map(File::toPath).collect(Collectors.toList());
                return handleDraggedFiles(files, dropRow);
            } else if (t.isDataFlavorSupported(urlFlavor)) {
                URL dropLink = (URL) t.getTransferData(urlFlavor);
                return handleDropTransfer(dropLink);
            } else if (t.isDataFlavorSupported(stringFlavor)) {
                String dropStr = (String) t.getTransferData(stringFlavor);
                LOGGER.debug("Received stringFlavor: " + dropStr);
                return handleDropTransfer(dropStr, dropRow);
            }
        } catch (IOException ioe) {
            LOGGER.error("Failed to read dropped data", ioe);
        } catch (UnsupportedFlavorException | ClassCastException ufe) {
            LOGGER.error("Drop type error", ufe);
        }

        // all supported flavors failed
        LOGGER.info("Can't transfer input: ");
        DataFlavor[] inflavs = t.getTransferDataFlavors();
        for (DataFlavor inflav : inflavs) {
            LOGGER.info("  " + inflav);
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
            if (inflav.match(urlFlavor) || inflav.match(stringFlavor) || inflav.match(DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }

        // System.out.println("drop type forbidden");
        // nope, never heard of this type
        return false;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        if (e instanceof MouseEvent) {
            int columnIndex = entryTable.columnAtPoint(((MouseEvent) e).getPoint());
            int modelIndex = entryTable.getColumnModel().getColumn(columnIndex).getModelIndex();
            if (entryTable.isFileColumn(modelIndex)) {
                LOGGER.info("Dragging file");
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
            if (handleDraggedFilenames(dropStr, dropRow)) {
                return true;
                // If not, handle it in the normal way...
            }
        } else if (dropStr.startsWith("http:")) {
            // This is the way URL links are received on OS X and KDE (Gnome?):
            URL url = new URL(dropStr);
            // JOptionPane.showMessageDialog(null, "Making URL:
            // "+url.toString());
            return handleDropTransfer(url);
        }
        File tmpfile = File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmpfile)) {
            fw.write(dropStr);
        }

        // System.out.println("importing from " + tmpfile.getAbsolutePath());

        ImportMenuItem importer = new ImportMenuItem(frame, false);
        importer.automatedImport(Collections.singletonList(tmpfile.getAbsolutePath()));

        return true;
    }

    /**
     * Translate a String describing a set of files or URLs dragged into JabRef into a List of File objects, taking care
     * of URL special characters.
     *
     * @param s String describing a set of files or URLs dragged into JabRef
     * @return a List<File> containing the individual file objects.
     *
     */
    public static List<Path> getFilesFromDraggedFilesString(String s) {
        // Split into lines:
        String[] lines = s.replace("\r", "").split("\n");
        List<Path> files = new ArrayList<>();
        for (String line1 : lines) {
            String line = line1;

            // Try to use url.toURI() to translate URL specific sequences like %20 into
            // standard characters:
            File fl = null;
            try {
                URL url = new URL(line);
                fl = new File(url.toURI());
            } catch (MalformedURLException | URISyntaxException e) {
                LOGGER.warn("Could not get file", e);
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
                files.add(f.toPath());
            }
        }
        return files;
    }

    /**
     * Handle a String describing a set of files or URLs dragged into JabRef.
     *
     * @param s String describing a set of files or URLs dragged into JabRef
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
     * @param files A List containing File instances pointing to files.
     * @param dropRow @param dropRow The row in the table where the files were dragged.
     * @return success status for the operation
     */
    private boolean handleDraggedFiles(List<Path> files, final int dropRow) {
        final List<String> fileNames = new ArrayList<>();
        for (Path file : files) {
            fileNames.add(file.toAbsolutePath().toString());
        }
        // Try to load BIB files normally, and import the rest into the current
        // database.
        // This process must be spun off into a background thread:
        JabRefExecutorService.INSTANCE.execute(() -> {
            final ImportPdfFilesResult importRes = new PdfImporter(frame, panel, entryTable, dropRow)
                    .importPdfFiles(fileNames);
            if (!importRes.getNoPdfFiles().isEmpty()) {
                loadOrImportFiles(importRes.getNoPdfFiles(), dropRow);
            }
        });

        return true;
    }

    /**
     * Take a set of filenames. Those with names indicating BIB files are opened as such if possible. All other files we
     * will attempt to import into the current library.
     *
     * @param fileNames The names of the files to open.
     * @param dropRow success status for the operation
     */
    private void loadOrImportFiles(List<String> fileNames, int dropRow) {

        OpenDatabaseAction openAction = new OpenDatabaseAction(frame, false);
        List<String> notBibFiles = new ArrayList<>();
        List<String> bibFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            // Find the file's extension, if any:
            Optional<String> extension = FileHelper.getFileExtension(fileName);
            Optional<ExternalFileType> fileType;

            if (extension.isPresent() && "bib".equals(extension.get())) {
                // we assume that it is a BibTeX file.
                // When a user wants to import something with file extension "bib", but which is not a BibTeX file, he should use "file -> import"
                bibFiles.add(fileName);
                continue;
            }

            fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension.orElse(""));
            /*
             * This is a linkable file. If the user dropped it on an entry, we
             * should offer options for autolinking to this files:
             *
             * TODO we should offer an option to highlight the row the user is on too.
             */
            if ((fileType.isPresent()) && (dropRow >= 0)) {

                /*
                 * TODO: make this an instance variable?
                 */
                DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
                dfh.handleDroppedfile(fileName, fileType.get(), entryTable, dropRow);

                continue;
            }
            notBibFiles.add(fileName);
        }

        openAction.openFilesAsStringList(bibFiles, true);

        if (!notBibFiles.isEmpty()) {
            // Import into new if entryTable==null, otherwise into current
            // database:
            ImportMenuItem importer = new ImportMenuItem(frame, entryTable == null);
            importer.automatedImport(notBibFiles);
        }
    }

    private boolean handleDropTransfer(URL dropLink) throws IOException {
        File tmpfile = File.createTempFile("jabrefimport", "");
        tmpfile.deleteOnExit();

        new URLDownload(dropLink).toFile(tmpfile.toPath());

        // Import into new if entryTable==null, otherwise into current library:
        ImportMenuItem importer = new ImportMenuItem(frame, entryTable == null);
        importer.automatedImport(Collections.singletonList(tmpfile.getAbsolutePath()));

        return true;
    }

}

package org.jabref.gui.fieldeditors;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.jabref.gui.EntryContainer;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiles.DroppedFileHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.groups.EntryTableTransferHandler;
import org.jabref.model.util.FileHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileListEditorTransferHandler extends TransferHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileListEditorTransferHandler.class);

    private final DataFlavor URL_FLAVOR;
    private final JabRefFrame frame;
    private final EntryContainer entryContainer;
    private final TransferHandler textTransferHandler;

    private DroppedFileHandler droppedFileHandler;


    /**
     * @param textTransferHandler is an instance of javax.swing.plaf.basic.BasicTextUI.TextTransferHandler. That class
     *                            is not visible. Therefore, we have to "cheat"
     */
    public FileListEditorTransferHandler(JabRefFrame frame, EntryContainer entryContainer,
                                         TransferHandler textTransferHandler) {
        this.frame = frame;
        this.entryContainer = entryContainer;
        this.textTransferHandler = textTransferHandler;
        URL_FLAVOR = getUrlFlavor();
    }

    private DataFlavor getUrlFlavor() {
        DataFlavor urlFlavor;
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Unable to configure drag and drop for file link table", e);
            urlFlavor = null;
        }
        return urlFlavor;
    }

    /**
     * Overridden to indicate which types of drags are supported (only LINK + COPY). COPY is supported as no support
     * disables CTRL+C (copy of text)
     */
    @Override
    public int getSourceActions(JComponent c) {
        return DnDConstants.ACTION_LINK | DnDConstants.ACTION_COPY;
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        if (this.textTransferHandler != null) {
            this.textTransferHandler.exportToClipboard(comp, clip, action);
        }
    }

    @Override
    public boolean importData(JComponent comp, Transferable transferable) {
        // If the drop target is the main table, we want to record which
        // row the item was dropped on, to identify the entry if needed:

        try {
            List<Path> files = new ArrayList<>();
            // This flavor is used for dragged file links in Windows:
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // javaFileListFlavor returns a list of java.io.File (as the string *File* in File indicates) and not a list of java.nio.file
                // There is no DataFlavor.javaPathListFlavor, so we have to deal with java.io.File
                @SuppressWarnings("unchecked")
                List<File> transferedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                files.addAll(transferedFiles.stream().map(File::toPath).collect(Collectors.toList()));
            } else if (transferable.isDataFlavorSupported(URL_FLAVOR)) {
                URL dropLink = (URL) transferable.getTransferData(URL_FLAVOR);
                LOGGER.warn("Dropped URL, which is currently not implemented " + dropLink);
            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                // This is used when one or more files are pasted from the file manager
                // under Gnome. The data consists of the file paths, one file per line:
                String dropStr = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                files.addAll(EntryTableTransferHandler.getFilesFromDraggedFilesString(dropStr));
            } else {
                LOGGER.warn("Dropped something, which we currently cannot handle");
            }

            SwingUtilities.invokeLater(() -> {
                for (Path file : files) {
                    // Find the file's extension, if any:
                    String name = file.toAbsolutePath().toString();
                    FileHelper.getFileExtension(name).ifPresent(extension -> ExternalFileTypes.getInstance()
                            .getExternalFileTypeByExt(extension).ifPresent(fileType -> {
                                if (droppedFileHandler == null) {
                                    droppedFileHandler = new DroppedFileHandler(frame, frame.getCurrentBasePanel());
                                }
                                droppedFileHandler.handleDroppedfile(name, fileType, entryContainer.getEntry());
                            }));
                }
            });
            if (!files.isEmpty()) {
                // Found some files, return
                return true;
            }
        } catch (IOException ioe) {
            LOGGER.warn("Failed to read dropped data. ", ioe);
        } catch (UnsupportedFlavorException | ClassCastException ufe) {
            LOGGER.warn("Drop type error. ", ufe);
        }

        // all supported flavors failed
        // log the flavors to support debugging
        LOGGER.warn("Cannot transfer input: " + dataFlavorsToString(transferable.getTransferDataFlavors()));
        return false;
    }

    private String dataFlavorsToString(DataFlavor[] transferFlavors) {
        return Arrays.stream(transferFlavors)
                .map(dataFlavor -> dataFlavor.toString())
                .collect(Collectors.joining(" "));
    }

    /**
     * This method is called to query whether the transfer can be imported.
     *
     *  @return <code>true</code> for urls, strings, javaFileLists, <code>false</code> otherwise
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        // accept this if any input flavor matches any of our supported flavors
        for (DataFlavor inflav : transferFlavors) {
            if (inflav.match(URL_FLAVOR) || inflav.match(DataFlavor.stringFlavor) || inflav.match(DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }

        // nope, never heard of this type
        LOGGER.debug("Unknown data transfer flavor: " + dataFlavorsToString(transferFlavors));
        return false;
    }
}

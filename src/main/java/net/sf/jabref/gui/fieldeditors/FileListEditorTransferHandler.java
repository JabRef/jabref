/*  Copyright (C) 2003-2012 JabRef contributors.
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

package net.sf.jabref.gui.fieldeditors;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.EntryContainer;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.groups.EntryTableTransferHandler;

class FileListEditorTransferHandler extends TransferHandler {
    private DataFlavor urlFlavor;
    private final DataFlavor stringFlavor;
    private final JabRefFrame frame;
    private final EntryContainer entryContainer;
    private final TransferHandler textTransferHandler;
    private DroppedFileHandler droppedFileHandler;

    private static final Log LOGGER = LogFactory.getLog(FileListEditorTransferHandler.class);


    /**
     *
     * @param frame
     * @param entryContainer
     * @param textTransferHandler is an instance of javax.swing.plaf.basic.BasicTextUI.TextTransferHandler. That class is not visible. Therefore, we have to "cheat"
     */
    public FileListEditorTransferHandler(JabRefFrame frame, EntryContainer entryContainer, TransferHandler textTransferHandler) {
        this.frame = frame;
        this.entryContainer = entryContainer;
        this.textTransferHandler = textTransferHandler;
        stringFlavor = DataFlavor.stringFlavor;
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Unable to configure drag and drop for file link table", e);
        }
    }

    /**
     * Overridden to indicate which types of drags are supported (only LINK + COPY).
     * COPY is supported as no support disables CTRL+C (copy of text)
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
    public boolean importData(JComponent comp, Transferable t) {
        // If the drop target is the main table, we want to record which
        // row the item was dropped on, to identify the entry if needed:

        try {

            List<File> files = new ArrayList<>();
            // This flavor is used for dragged file links in Windows:
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                files.addAll((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor));
            }

            if (t.isDataFlavorSupported(urlFlavor)) {
                URL dropLink = (URL) t.getTransferData(urlFlavor);
                LOGGER.debug("URL: " + dropLink);
            }

            // This is used when one or more files are pasted from the file manager
            // under Gnome. The data consists of the file paths, one file per line:
            if (t.isDataFlavorSupported(stringFlavor)) {
                String dropStr = (String) t.getTransferData(stringFlavor);
                files.addAll(EntryTableTransferHandler.getFilesFromDraggedFilesString(dropStr));
            }

            SwingUtilities.invokeLater((Runnable) () -> {
                for (File file : files) {
                    // Find the file's extension, if any:
                    String name = file.getAbsolutePath();
                    FileUtil.getFileExtension(name).ifPresent(extension -> {
                        ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension).ifPresent(fileType -> {
                            if (droppedFileHandler == null) {
                                droppedFileHandler = new DroppedFileHandler(frame, frame.getCurrentBasePanel());
                            }
                            droppedFileHandler.handleDroppedfile(name, fileType, entryContainer.getEntry());
                        });
                    });
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
        StringBuilder logMessage = new StringBuilder("Cannot transfer input:");
        DataFlavor[] inflavs = t.getTransferDataFlavors();
        for (DataFlavor inflav : inflavs) {
            logMessage.append(' ').append(inflav);
        }
        LOGGER.warn(logMessage.toString());

        return false;
    }

    /**
     * This method is called to query whether the transfer can be imported.
     *
     * Will return true for urls, strings, javaFileLists
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {

        // accept this if any input flavor matches any of our supported flavors
        for (DataFlavor inflav : transferFlavors) {
            if (inflav.match(urlFlavor) || inflav.match(stringFlavor)
                    || inflav.match(DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }

        // nope, never heard of this type
        return false;
    }

}

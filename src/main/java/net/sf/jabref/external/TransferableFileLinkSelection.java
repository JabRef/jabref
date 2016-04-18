/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.external;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListTableModel;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 *
 */
public class TransferableFileLinkSelection implements Transferable {

    private final List<File> fileList = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(TransferableFileLinkSelection.class);


    public TransferableFileLinkSelection(BasePanel panel, List<BibEntry> selection) {
        FileListTableModel tm = new FileListTableModel();
        selection.get(0).getFieldOptional(Globals.FILE_FIELD).ifPresent(tm::setContent);
        if (tm.getRowCount() > 0) {
            // Find the default directory for this field type, if any:
            List<String> dirs = panel.getBibDatabaseContext().getFileDirectory();
            FileUtil.expandFilename(tm.getEntry(0).link, dirs).ifPresent(fileList::add);
        }

    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {DataFlavor.javaFileListFlavor};//, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        LOGGER.debug("Query: " + dataFlavor.getHumanPresentableName() + " , "
                +
                dataFlavor.getDefaultRepresentationClass() + " , " + dataFlavor.getMimeType());
        return dataFlavor.equals(DataFlavor.javaFileListFlavor)
                || dataFlavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
        //if (dataFlavor.equals(DataFlavor.javaFileListFlavor))
        return fileList;
        //else
        //    return "test";
    }
    /*
    private StringSelection ss;

    public TransferableFileLinkSelection(BasePanel panel, BibEntry[] selection) {
        String s = selection[0].getField(GUIGlobals.FILE_FIELD);
        FileListTableModel tm = new FileListTableModel();
        if (s != null)
            tm.setContent(s);
        if (tm.getRowCount() > 0) {
            // Find the default directory for this field type, if any:
            String dir = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
            // Include the standard "file" directory:
            String fileDir = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
            // Include the directory of the bib file:
            String[] dirs;
            if (panel.metaData().getDatabaseFile() != null) {
                String databaseDir = panel.metaData().getDatabaseFile().getParent();
                dirs = new String[] { dir, fileDir, databaseDir };
            }
            else
                dirs = new String[] { dir, fileDir };
            System.out.println(tm.getEntry(0).getLink());
            for (int i = 0; i < dirs.length; i++) {
                String dir1 = dirs[i];
                System.out.println("dir:"+dir1);
            }
            File expLink = Util.expandFilename(tm.getEntry(0).getLink(), dirs);
            try {
                System.out.println(expLink.toURI().toURL().toString());
                ss = new StringSelection(expLink.toURI().toURL().toString());

            } catch (MalformedURLException ex) {
                ss = new StringSelection("");
            }
        }
        else
            ss = new StringSelection("");

    }

    public Transferable getTransferable() {
        return ss;
    } */
}

package net.sf.jabref.external;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Util;
import net.sf.jabref.BasePanel;
import net.sf.jabref.gui.FileListTableModel;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;

/**
 * 
 */
public class TransferableFileLinkSelection implements Transferable {

    List<File> fileList = new ArrayList<File>();

    public TransferableFileLinkSelection(BasePanel panel, BibtexEntry[] selection) {
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
            if (panel.metaData().getFile() != null) {
                String databaseDir = panel.metaData().getFile().getParent();
                dirs = new String[] { dir, fileDir, databaseDir };
            }
            else
                dirs = new String[] { dir, fileDir };
            File expLink = Util.expandFilename(tm.getEntry(0).getLink(), dirs);
            fileList.add(expLink);

        }

    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {DataFlavor.javaFileListFlavor};//, DataFlavor.stringFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        System.out.println("Query: "+dataFlavor.getHumanPresentableName()+" , "+
            dataFlavor.getDefaultRepresentationClass()+" , "+dataFlavor.getMimeType());
        return dataFlavor.equals(DataFlavor.javaFileListFlavor)
                || dataFlavor.equals(DataFlavor.stringFlavor);
    }

    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
        //if (dataFlavor.equals(DataFlavor.javaFileListFlavor))
            return fileList;
        //else
        //    return "test";
    }
    /*
    private StringSelection ss;

    public TransferableFileLinkSelection(BasePanel panel, BibtexEntry[] selection) {
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
            if (panel.metaData().getFile() != null) {
                String databaseDir = panel.metaData().getFile().getParent();
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

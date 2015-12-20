package net.sf.jabref.importer;

import java.io.File;
import java.io.FileFilter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.gui.FindUnlinkedFilesDialog.CheckableTreeNode;
import net.sf.jabref.gui.FindUnlinkedFilesDialog.FileNodeWrapper;

/**
 * Util class for searching files on the filessystem which are not linked to a
 * provided {@link BibDatabase}.
 * 
 * @author Nosh&Dan
 * @version 09.11.2008 | 19:55:20
 * 
 */
public class UnlinkedFilesCrawler {

    /**
     * File filter, that accepts directorys only.
     */
    private final FileFilter directoryFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            if (pathname == null) {
                return false;
            }
            return pathname.isDirectory();
        }
    };
    private final BibDatabase database;


    /**
     * CONSTRUCTOR
     * 
     * @param database
     */
    public UnlinkedFilesCrawler(BibDatabase database) {
        this.database = database;
    }

    public CheckableTreeNode searchDirectory(File directory, FileFilter aFileFilter) {
        UnlinkedPDFFileFilter ff = new UnlinkedPDFFileFilter(aFileFilter, database);
        return searchDirectory(directory, ff, new int[] {1}, null);
    }

    /**
     * Searches recursively all files in the specified directory. <br>
     * <br>
     * All {@link File}s, which match the {@link FileFilter} that comes with the
     * {@link EntryFromFileCreatorManager}, are taken into the resulting tree. <br>
     * <br>
     * The result will be a tree structure of nodes of the type
     * {@link CheckableTreeNode}. <br>
     * <br>
     * The user objects that are attached to the nodes is the
     * {@link FileNodeWrapper}, which wrapps the {@link File}-Object. <br>
     * <br>
     * For ensuring the capability to cancel the work of this recursive method,
     * the first position in the integer array 'state' must be set to 1, to keep
     * the recursion running. When the states value changes, the methode will
     * resolve its recursion and return what it has saved so far.
     */
    public CheckableTreeNode searchDirectory(File directory, UnlinkedPDFFileFilter ff, int[] state, ChangeListener changeListener) {
        /* Cancellation of the search from outside! */
        if (state == null || state.length < 1 || state[0] != 1) {
            return null;
        }
        /* Return null if the directory is not valid. */
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles(ff);
        CheckableTreeNode root = new CheckableTreeNode(null);

        int filesCount = 0;

        File[] subDirectories = directory.listFiles(directoryFilter);
        for (File subDirectory : subDirectories) {
            CheckableTreeNode subRoot = searchDirectory(subDirectory, ff, state, changeListener);
            if (subRoot != null && subRoot.getChildCount() > 0) {
                filesCount += ((FileNodeWrapper) subRoot.getUserObject()).fileCount;
                root.add(subRoot);
            }
        }

        root.setUserObject(new FileNodeWrapper(directory, files.length + filesCount));

        for (File file : files) {
            root.add(new CheckableTreeNode(new FileNodeWrapper(file)));
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }

        return root;
    }

}

package org.jabref.gui.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jabref.gui.FindUnlinkedFilesDialog.CheckableTreeNode;
import org.jabref.gui.FindUnlinkedFilesDialog.FileNodeWrapper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

/**
 * Util class for searching files on the file system which are not linked to a provided {@link BibDatabase}.
 */
public class UnlinkedFilesCrawler {
    /**
     * File filter, that accepts directories only.
     */
    private static final FileFilter DIRECTORY_FILTER = pathname -> (pathname != null) && pathname.isDirectory();

    private final BibDatabaseContext databaseContext;


    public UnlinkedFilesCrawler(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    public CheckableTreeNode searchDirectory(File directory, FileFilter filter) {
        UnlinkedPDFFileFilter ff = new UnlinkedPDFFileFilter(filter, databaseContext);
        return searchDirectory(directory, ff, new AtomicBoolean(true), null);
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
     * {@link FileNodeWrapper}, which wraps the {@link File}-Object. <br>
     * <br>
     * For ensuring the capability to cancel the work of this recursive method,
     * the first position in the integer array 'state' must be set to 1, to keep
     * the recursion running. When the states value changes, the method will
     * resolve its recursion and return what it has saved so far.
     */
    public CheckableTreeNode searchDirectory(File directory, UnlinkedPDFFileFilter ff, AtomicBoolean state, ChangeListener changeListener) {
        /* Cancellation of the search from outside! */
        if ((state == null) || !state.get()) {
            return null;
        }
        // Return null if the directory is not valid.
        if ((directory == null) || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] filesArray = directory.listFiles(ff);
        List<File> files;
        if (filesArray == null) {
            files = Collections.emptyList();
        } else {
            files = Arrays.asList(filesArray);
        }
        CheckableTreeNode root = new CheckableTreeNode(null);

        int filesCount = 0;

        filesArray = directory.listFiles(DIRECTORY_FILTER);
        List<File> subDirectories;
        if (filesArray == null) {
            subDirectories = Collections.emptyList();
        } else {
            subDirectories = Arrays.asList(filesArray);
        }
        for (File subDirectory : subDirectories) {
            CheckableTreeNode subRoot = searchDirectory(subDirectory, ff, state, changeListener);
            if ((subRoot != null) && (subRoot.getChildCount() > 0)) {
                filesCount += ((FileNodeWrapper) subRoot.getUserObject()).fileCount;
                root.add(subRoot);
            }
        }

        root.setUserObject(new FileNodeWrapper(directory, files.size() + filesCount));

        for (File file : files) {
            root.add(new CheckableTreeNode(new FileNodeWrapper(file)));
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }

        return root;
    }

}

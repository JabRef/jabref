package org.jabref.gui.importer;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.scene.control.CheckBoxTreeItem;

import org.jabref.gui.externalfiles.FindUnlinkedFilesDialog.FileNodeWrapper;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

/**
 * Util class for searching files on the file system which are not linked to a provided {@link BibDatabase}.
 */
public class UnlinkedFilesCrawler extends BackgroundTask<CheckBoxTreeItem<FileNodeWrapper>> {

    private final Path directory;
    private final FileFilter fileFilter;
    private int counter;
    private final BibDatabaseContext databaseContext;

    public UnlinkedFilesCrawler(Path directory, FileFilter fileFilter, BibDatabaseContext databaseContext) {
        this.directory = directory;
        this.fileFilter = fileFilter;
        this.databaseContext = databaseContext;
    }

    @Override
    protected CheckBoxTreeItem<FileNodeWrapper> call() {
        UnlinkedPDFFileFilter unlinkedPDFFileFilter = new UnlinkedPDFFileFilter(fileFilter, databaseContext);
        return searchDirectory(directory.toFile(), unlinkedPDFFileFilter);
    }

    /**
     * Searches recursively all files in the specified directory. <br>
     * <br>
     * All files matched by the given {@link UnlinkedPDFFileFilter} are taken into the resulting tree. <br>
     * <br>
     * The result will be a tree structure of nodes of the type
     * {@link CheckBoxTreeItem}. <br>
     * <br>
     * The user objects that are attached to the nodes is the
     * {@link FileNodeWrapper}, which wraps the {@link File}-Object. <br>
     * <br>
     * For ensuring the capability to cancel the work of this recursive method,
     * the first position in the integer array 'state' must be set to 1, to keep
     * the recursion running. When the states value changes, the method will
     * resolve its recursion and return what it has saved so far.
     */
    private CheckBoxTreeItem<FileNodeWrapper> searchDirectory(File directory, UnlinkedPDFFileFilter ff) {
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
        CheckBoxTreeItem<FileNodeWrapper> root = new CheckBoxTreeItem<>(new FileNodeWrapper(directory.toPath(), 0));

        int filesCount = 0;

        filesArray = directory.listFiles(pathname -> (pathname != null) && pathname.isDirectory());
        List<File> subDirectories;
        if (filesArray == null) {
            subDirectories = Collections.emptyList();
        } else {
            subDirectories = Arrays.asList(filesArray);
        }
        for (File subDirectory : subDirectories) {
            if (isCanceled()) {
                return root;
            }

            CheckBoxTreeItem<FileNodeWrapper> subRoot = searchDirectory(subDirectory, ff);
            if ((subRoot != null) && (!subRoot.getChildren().isEmpty())) {
                filesCount += subRoot.getValue().fileCount;
                root.getChildren().add(subRoot);
            }
        }

        root.setValue(new FileNodeWrapper(directory.toPath(), files.size() + filesCount));

        for (File file : files) {
            root.getChildren().add(new CheckBoxTreeItem<>(new FileNodeWrapper(file.toPath())));

            counter++;
            if (counter == 1) {
                updateMessage(Localization.lang("One file found"));
            } else {
                updateMessage(Localization.lang("%0 files found", Integer.toString(counter)));
            }
        }

        return root;
    }
}

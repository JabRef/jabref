package org.jabref.gui.externalfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javafx.scene.control.CheckBoxTreeItem;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for searching files on the file system which are not linked to a provided {@link BibDatabase}.
 */
public class UnlinkedFilesCrawler extends BackgroundTask<FileNodeViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkedFilesCrawler.class);

    private final Path directory;
    private final Filter<Path> fileFilter;
    private final DateRange dateFilter;
    private final ExternalFileSorter sorter;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public UnlinkedFilesCrawler(Path directory, Filter<Path> fileFilter, DateRange dateFilter, ExternalFileSorter sorter, BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.directory = directory;
        this.fileFilter = fileFilter;
        this.dateFilter = dateFilter;
        this.sorter = sorter;
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    @Override
    protected FileNodeViewModel call() throws IOException {
        UnlinkedPDFFileFilter unlinkedPDFFileFilter = new UnlinkedPDFFileFilter(fileFilter, databaseContext, filePreferences);
        return searchDirectory(directory, unlinkedPDFFileFilter);
    }

    /**
     * Searches recursively all files in the specified directory. <br>
     * <br>
     * All files matched by the given {@link UnlinkedPDFFileFilter} are taken into the resulting tree. <br>
     * <br>
     * The result will be a tree structure of nodes of the type {@link CheckBoxTreeItem}. <br>
     * <br>
     * The user objects that are attached to the nodes is the {@link FileNodeViewModel}, which wraps the {@link
     * File}-Object. <br>
     * <br>
     * For ensuring the capability to cancel the work of this recursive method, the first position in the integer array
     * 'state' must be set to 1, to keep the recursion running. When the states value changes, the method will resolve
     * its recursion and return what it has saved so far.
     * <br>
     * The files are filtered according to the {@link DateRange} filter value 
     * and then sorted according to the {@link ExternalFileSorter} value.
     *
     * @throws IOException if directory is not a directory or empty
     */
    private FileNodeViewModel searchDirectory(Path directory, UnlinkedPDFFileFilter fileFilter) throws IOException {
        // Return null if the directory is not valid.
        if ((directory == null) || !Files.isDirectory(directory)) {
            throw new IOException(String.format("Invalid directory for searching: %s", directory));
        }

        FileNodeViewModel parent = new FileNodeViewModel(directory);
        Map<Boolean, List<Path>> fileListPartition;

        try (Stream<Path> filesStream = StreamSupport.stream(Files.newDirectoryStream(directory, fileFilter).spliterator(), false)) {
            fileListPartition = filesStream.collect(Collectors.partitioningBy(Files::isDirectory));
        } catch (IOException e) {
            LOGGER.error(String.format("%s while searching files: %s", e.getClass().getName(), e.getMessage()));
            return parent;
        }

        List<Path> subDirectories = fileListPartition.get(true);
        List<Path> files = new ArrayList<>(fileListPartition.get(false));
        int fileCount = 0;

        for (Path subDirectory : subDirectories) {
            FileNodeViewModel subRoot = searchDirectory(subDirectory, fileFilter);

            if (!subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getFileCount();
                parent.getChildren().add(subRoot);
            }
        }
        // filter files according to last edited date.
        List<Path> filteredFiles = new ArrayList<Path>();
        for (Path path : files) {
            if (FileFilterUtils.filterByDate(path, dateFilter)) {
                filteredFiles.add(path);
            }
        }
        // sort files according to last edited date.
        filteredFiles = FileFilterUtils.sortByDate(filteredFiles, sorter);
        parent.setFileCount(filteredFiles.size() + fileCount);
        parent.getChildren().addAll(filteredFiles.stream()
                .map(FileNodeViewModel::new)
                .collect(Collectors.toList()));
        return parent;
    }
}

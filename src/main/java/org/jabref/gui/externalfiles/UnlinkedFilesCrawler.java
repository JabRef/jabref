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
     * @param unlinkedPDFFileFilter contains a BibDatabaseContext which is used to determine whether the file is linked
     *
     * @return FileNodeViewModel containing the data of the current directory and all subdirectories
     * @throws IOException if directory is not a directory or empty
     */
    FileNodeViewModel searchDirectory(Path directory, UnlinkedPDFFileFilter unlinkedPDFFileFilter) throws IOException {
        // Return null if the directory is not valid.
        if ((directory == null) || !Files.isDirectory(directory)) {
            throw new IOException(String.format("Invalid directory for searching: %s", directory));
        }

        FileNodeViewModel fileNodeViewModelForCurrentDirectory = new FileNodeViewModel(directory);

        // Map from isDirectory (true/false) to full path
        // Result: Contains only files not matching the filter (i.e., PDFs not linked and files not ignored)
        // Filters:
        //   1. UnlinkedPDFFileFilter
        //   2. GitIgnoreFilter
        ChainedFilters filters = new ChainedFilters(unlinkedPDFFileFilter, new GitIgnoreFileFilter(directory));
        Map<Boolean, List<Path>> directoryAndFilePartition;
        try (Stream<Path> filesStream = StreamSupport.stream(Files.newDirectoryStream(directory, filters).spliterator(), false)) {
            directoryAndFilePartition = filesStream.collect(Collectors.partitioningBy(Files::isDirectory));
        } catch (IOException e) {
            LOGGER.error("Error while searching files", e);
            return fileNodeViewModelForCurrentDirectory;
        }
        List<Path> subDirectories = directoryAndFilePartition.get(true);
        List<Path> files = directoryAndFilePartition.get(false);

        // at this point, only unlinked PDFs AND unignored files are contained

        // initially, we find no files at all
        int fileCountOfSubdirectories = 0;

        // now we crawl into the found subdirectories first (!)
        for (Path subDirectory : subDirectories) {
            FileNodeViewModel subRoot = searchDirectory(subDirectory, unlinkedPDFFileFilter);
            if (!subRoot.getChildren().isEmpty()) {
                fileCountOfSubdirectories += subRoot.getFileCount();
                fileNodeViewModelForCurrentDirectory.getChildren().add(subRoot);
            }
        }
        // now we have the data of all subdirectories
        // it is stored in fileNodeViewModelForCurrentDirectory.getChildren()

        // now we handle the files in the current directory

        // filter files according to last edited date.
        // Note that we do not use the "StreamSupport.stream" filtering functionality, because refactoring the code to that would lead to more code
        List<Path> resultingFiles = new ArrayList<>();
        for (Path path : files) {
            if (FileFilterUtils.filterByDate(path, dateFilter)) {
                resultingFiles.add(path);
            }
        }

        // sort files according to last edited date.
        resultingFiles = FileFilterUtils.sortByDate(resultingFiles, sorter);

        // the count of all files is the count of the found files in current directory plus the count of all files in the subdirectories
        fileNodeViewModelForCurrentDirectory.setFileCount(resultingFiles.size() + fileCountOfSubdirectories);

        // create and add FileNodeViewModel to the FileNodeViewModel for the current directory
        fileNodeViewModelForCurrentDirectory.getChildren().addAll(resultingFiles.stream()
                .map(FileNodeViewModel::new)
                .collect(Collectors.toList()));

        return fileNodeViewModelForCurrentDirectory;
    }
}

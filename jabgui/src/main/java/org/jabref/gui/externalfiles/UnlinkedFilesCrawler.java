package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Util class for searching files on the file system which are not linked to a provided [org.jabref.model.database.BibDatabase].
///
/// The result is used to determine whether to link the files to an existing related entry or to create a new entry, according to the user's choice.
///
/// Related: [org.jabref.gui.externalfiles.AutoSetFileLinksUtil#findAssociatedNotLinkedFiles]
public class UnlinkedFilesCrawler extends BackgroundTask<UnlinkedFilesSearchResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkedFilesCrawler.class);

    private final Path directory;
    private final Filter<Path> fileFilter;
    private final DateRange dateFilter;
    private final ExternalFileSorter sorter;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final AutoLinkPreferences autoLinkPreferences;

    public UnlinkedFilesCrawler(Path directory,
                                Filter<Path> fileFilter,
                                DateRange dateFilter,
                                ExternalFileSorter sorter,
                                BibDatabaseContext databaseContext,
                                FilePreferences filePreferences,
                                ExternalApplicationsPreferences externalApplicationsPreferences,
                                AutoLinkPreferences autoLinkPreferences) {
        this.directory = directory;
        this.fileFilter = fileFilter;
        this.dateFilter = dateFilter;
        this.sorter = sorter;
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.autoLinkPreferences = autoLinkPreferences;
    }

    /// [impl->req~jabgui.externalfiles.unlinked-files.search.non-blocking-results~1]
    @Override
    public UnlinkedFilesSearchResult call() throws IOException {
        UnlinkedPDFFileFilter unlinkedPDFFileFilter = new UnlinkedPDFFileFilter(fileFilter, databaseContext, filePreferences);
        FileNodeViewModel treeRoot = searchDirectory(directory, unlinkedPDFFileFilter);
        return new UnlinkedFilesSearchResult(treeRoot, findRelatedEntriesByFile());
    }

    private Map<Path, List<BibEntry>> findRelatedEntriesByFile() {
        Map<Path, List<BibEntry>> relatedEntriesByFile = new HashMap<>();
        AutoSetFileLinksUtil autoSetFileLinksUtil = new AutoSetFileLinksUtil(
                databaseContext,
                externalApplicationsPreferences,
                filePreferences,
                autoLinkPreferences);

        List<Path> fileDirectories = databaseContext.getFileDirectories(filePreferences);
        for (BibEntry entry : databaseContext.getDatabase().getEntries()) {
            if (isCancelled()) {
                return Map.of();
            }
            try {
                Collection<LinkedFile> associatedFiles = autoSetFileLinksUtil.findAssociatedNotLinkedFiles(entry);
                Set<Path> associatedPaths = new HashSet<>();
                for (LinkedFile associatedFile : associatedFiles) {
                    // The links are relative and deduplicated, so a name present in several file directories must be expanded again
                    FileUtil.findListOfFiles(associatedFile.getLink(), fileDirectories).stream()
                            .map(UnlinkedFilesSearchResult::normalizePath)
                            .forEach(associatedPaths::add);
                }
                for (Path associatedPath : associatedPaths) {
                    relatedEntriesByFile.computeIfAbsent(associatedPath, ignored -> new ArrayList<>()).add(entry);
                }
            } catch (IOException e) {
                LOGGER.warn("Error finding related files for entry {}", entry.getCitationKey(), e);
            }
        }

        return relatedEntriesByFile.entrySet().stream()
                                   .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    /// Searches recursively all files in the specified directory.
    ///
    /// All files matched by the given [UnlinkedPDFFileFilter] are taken into the resulting tree.
    ///
    /// The result will be a tree structure of nodes of the type [javafx.scene.control.CheckBoxTreeItem].
    ///
    /// The user objects that are attached to the nodes is the [FileNodeViewModel], which wraps the [java.io.File]-Object.
    ///
    /// For ensuring the capability to cancel the work of this recursive method, the first position in the integer array
    /// 'state' must be set to 1, to keep the recursion running. When the states value changes, the method will resolve
    /// its recursion and return what it has saved so far.
    ///
    /// The files are filtered according to the [DateRange] filter value
    /// and then sorted according to the [ExternalFileSorter] value.
    ///
    /// @param unlinkedPDFFileFilter contains a BibDatabaseContext which is used to determine whether the file is linked
    /// @return FileNodeViewModel containing the data of the current directory and all subdirectories
    /// @throws IOException if directory is not a directory or empty
    FileNodeViewModel searchDirectory(Path directory, UnlinkedPDFFileFilter unlinkedPDFFileFilter) throws IOException {
        // Return null if the directory is not valid.
        if ((directory == null) || !Files.isDirectory(directory)) {
            throw new IOException("Invalid directory for searching: %s".formatted(directory));
        }

        FileNodeViewModel fileNodeViewModelForCurrentDirectory = new FileNodeViewModel(directory);

        // Map from isDirectory (true/false) to full path
        // Result: Contains only files not matching the filter (i.e., PDFs not linked and files not ignored)
        // Filters:
        //   1. UnlinkedPDFFileFilter
        //   2. GitIgnoreFilter
        ChainedFilters filters = new ChainedFilters(List.of(unlinkedPDFFileFilter, new GitIgnoreFileFilter(directory)));
        Map<Boolean, List<Path>> directoryAndFilePartition;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory, filters);
             Stream<Path> filesStream = StreamSupport.stream(dirStream.spliterator(), false)) {
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
                                                                                .toList());

        return fileNodeViewModelForCurrentDirectory;
    }
}

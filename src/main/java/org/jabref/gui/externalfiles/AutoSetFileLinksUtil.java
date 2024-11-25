package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSetFileLinksUtil {

    public static class LinkFilesResult {
        private final List<BibEntry> changedEntries = new ArrayList<>();
        private final List<IOException> fileExceptions = new ArrayList<>();

        protected void addBibEntry(BibEntry bibEntry) {
            changedEntries.add(bibEntry);
        }

        protected void addFileException(IOException exception) {
            fileExceptions.add(exception);
        }

        public List<BibEntry> getChangedEntries() {
            return changedEntries;
        }

        public List<IOException> getFileExceptions() {
            return fileExceptions;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSetFileLinksUtil.class);

    private final List<Path> directories;
    private final AutoLinkPreferences autoLinkPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;

    public AutoSetFileLinksUtil(BibDatabaseContext databaseContext,
                                ExternalApplicationsPreferences externalApplicationsPreferences,
                                FilePreferences filePreferences,
                                AutoLinkPreferences autoLinkPreferences) {
        this(databaseContext.getFileDirectories(filePreferences), externalApplicationsPreferences, filePreferences, autoLinkPreferences);
    }

    private AutoSetFileLinksUtil(List<Path> directories, ExternalApplicationsPreferences externalApplicationsPreferences, FilePreferences filePreferences, AutoLinkPreferences autoLinkPreferences) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.filePreferences = filePreferences;
    }

    // If file that is already linked to an existing entry is moved, the file should be relinked to the entry
    private void relinkFileIfMoved(List<LinkedFile> newLinkedFiles, BibEntry entry) {
        // Get the list of files linked to a specific entry
        List<LinkedFile> allLinkedFiles = entry.getField(StandardField.FILE).map(FileFieldParser::parse).orElse(Collections.emptyList());
        // newLinkedFiles refer to the new file path, while allLinkedFiles refer to both the old and new file paths
        if (newLinkedFiles.size() == 1 && allLinkedFiles.size() == 2) {
            String newPath = FileFieldWriter.getStringRepresentation(newLinkedFiles.getFirst());
            // If the first item in allLinkedFiles is an invalid file path and leads to no existing file, reset the value of the File field with the new path
            if (allLinkedFiles.getFirst().findIn(directories).isEmpty()) {
                entry.setField(StandardField.FILE, newPath);
            }
        }
    }

    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, BiConsumer<LinkedFile, BibEntry> onAddLinkedFile) {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            List<LinkedFile> linkedFiles = new ArrayList<>();

            try {
                linkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                result.addFileException(e);
                LOGGER.error("Problem finding files", e);
            }

            for (LinkedFile linkedFile : linkedFiles) {
                // store undo information
                onAddLinkedFile.accept(linkedFile, entry);
            }

            relinkFileIfMoved(linkedFiles, entry);

            result.addBibEntry(entry);
        }
        return result;
    }

    public List<LinkedFile> findAssociatedNotLinkedFiles(BibEntry entry) throws IOException {
        List<LinkedFile> linkedFiles = new ArrayList<>();

        List<String> extensions = externalApplicationsPreferences.getExternalFileTypes().stream().map(ExternalFileType::getExtension).toList();

        LOGGER.debug("Searching for extensions {} in directories {}", extensions, directories);

        // Run the search operation
        FileFinder fileFinder = FileFinders.constructFromConfiguration(autoLinkPreferences);
        List<Path> result = fileFinder.findAssociatedFiles(entry, directories, extensions);

        // Collect the found files that are not yet linked
        for (Path foundFile : result) {
            boolean fileAlreadyLinked = entry.getFiles().stream()
                                             .map(file -> file.findIn(directories))
                                             .anyMatch(file -> {
                                                 try {
                                                     return file.isPresent() && Files.isSameFile(file.get(), foundFile);
                                                 } catch (IOException e) {
                                                     LOGGER.error("Problem with isSameFile", e);
                                                 }
                                                 return false;
                                             });

            if (!fileAlreadyLinked) {
                Optional<ExternalFileType> type = FileUtil.getFileExtension(foundFile)
                                                            .map(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, externalApplicationsPreferences))
                                                            .orElse(Optional.of(new UnknownExternalFileType("")));

                String strType = type.isPresent() ? type.get().getName() : "";
                Path relativeFilePath = FileUtil.relativize(foundFile, directories);
                LinkedFile linkedFile = new LinkedFile("", relativeFilePath, strType);
                linkedFiles.add(linkedFile);
                LOGGER.debug("Found file {} for entry {}", linkedFile, entry.getCitationKey());
            }
        }

        return linkedFiles;
    }
}

package org.jabref.gui.externalfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSetFileLinksUtil {

    record RelinkedResults(List<LinkedFile> relinkedFiles, List<IOException> exceptions) { }

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
    private final FilePreferences filePreferences;

    public AutoSetFileLinksUtil(BibDatabaseContext databaseContext, FilePreferences filePreferences, AutoLinkPreferences autoLinkPreferences) {
        this(databaseContext.getFileDirectories(filePreferences), filePreferences, autoLinkPreferences);
    }

    private AutoSetFileLinksUtil(List<Path> directories, FilePreferences filePreferences, AutoLinkPreferences autoLinkPreferences) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.filePreferences = filePreferences;
    }

    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, NamedCompound ce) {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            List<LinkedFile> linkedFiles = new ArrayList<>();

            try {
                linkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                result.addFileException(e);
                LOGGER.error("Problem finding files", e);
            }

            if (ce != null) {
                boolean changed = false;

                for (LinkedFile linkedFile : linkedFiles) {
                    // store undo information
                    String newVal = FileFieldWriter.getStringRepresentation(linkedFile);
                    String oldVal = entry.getField(StandardField.FILE).orElse(null);
                    UndoableFieldChange fieldChange = new UndoableFieldChange(entry, StandardField.FILE, oldVal, newVal);
                    ce.addEdit(fieldChange);
                    changed = true;

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        entry.addFile(linkedFile);
                    });
                }
                if (changed) {
                    result.addBibEntry(entry);
                }
                // Run Relinking Process
                RelinkedResults relink = relinkingFiles(entry.getFiles());
                entry.setFiles(relink.relinkedFiles);
                if (!relink.relinkedFiles().isEmpty()) {
                    result.addBibEntry(entry);
                }
                for (IOException e : (relink.exceptions)) {
                    result.addFileException(e);
                }
            }
        }
        return result;
    }

    public List<LinkedFile> findAssociatedNotLinkedFiles(BibEntry entry) throws IOException {
        List<LinkedFile> linkedFiles = new ArrayList<>();

        List<String> extensions = filePreferences.getExternalFileTypes().stream().map(ExternalFileType::getExtension).collect(Collectors.toList());

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
                                                            .map(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, filePreferences))
                                                            .orElse(Optional.of(new UnknownExternalFileType("")));

                String strType = type.isPresent() ? type.get().getName() : "";
                Path relativeFilePath = FileUtil.relativize(foundFile, directories);
                LinkedFile linkedFile = new LinkedFile("", relativeFilePath, strType);
                linkedFiles.add(linkedFile);
            }
        }
        return linkedFiles;
    }

    public RelinkedResults relinkingFiles(List<LinkedFile> listlinked) {
        List<LinkedFile> changedFiles = new ArrayList<>();
        List<IOException> exceptions = new ArrayList<>();

        for (LinkedFile file : listlinked) {
            Path path = Paths.get(file.getLink());
            if (!Files.exists(path)) {
                Path filePath = Path.of(file.getLink());
                String directoryPath = filePath.getParent().getParent().toString();
                File directory = new File(directoryPath);

                String fileNameString = filePath.getFileName().toString();

                List<String> fileLocations = new ArrayList<>();

                searchFileInDirectoryAndSubdirectories(directory, fileNameString, fileLocations);
                if (!fileLocations.isEmpty()) {
                    file.setLink(fileLocations.get(0));
                    changedFiles.add(file);
                } else {
                    exceptions.add(new IOException("couldn't find file: " + file.getLink()));
                }
            }
        }
        return new RelinkedResults(changedFiles, exceptions);
    }

    public static void searchFileInDirectoryAndSubdirectories(File directory, String targetFileName, List<String> fileLocations) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFileInDirectoryAndSubdirectories(file, targetFileName, fileLocations);
                } else if (file.getName().equals(targetFileName)) {
                    fileLocations.add(file.getAbsolutePath());
                }
            }
        }
    }
}

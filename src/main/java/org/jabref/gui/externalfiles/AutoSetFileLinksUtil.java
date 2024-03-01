package org.jabref.gui.externalfiles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
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

    record RelinkedResults(List<LinkedFile> relinkedFiles, List<String> exceptions) { }

    public static class LinkFilesResult {
        private final List<BibEntry> changedEntries = new ArrayList<>();
        private final List<String> fileExceptions = new ArrayList<String>();

        protected void addBibEntry(BibEntry bibEntry) {
            changedEntries.add(bibEntry);
        }

        protected void addFileException(String exception) {
            fileExceptions.add(exception);
        }

        public List<BibEntry> getChangedEntries() {
            return changedEntries;
        }

        public List<String> getFileExceptions() {
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

    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, NamedCompound ce) throws FileNotFoundException {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            List<LinkedFile> linkedFiles = new ArrayList<>();

            try {
                linkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                result.addFileException(String.valueOf(e));
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
                for (String e : (relink.exceptions)) {
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

    public RelinkedResults relinkingFiles(List<LinkedFile> listlinked) throws FileNotFoundException {
        List<LinkedFile> changedFiles = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();

        for (LinkedFile file : listlinked) {
            Path path = Path.of(file.getLink());
            if (!Files.exists(path)) {
                Path filePath = Path.of(file.getLink());
                // May need to put some limit since it can cause considerable performance problems.
                String directoryPath = filePath.getParent().getParent().toString();

                String fileNameString = filePath.getFileName().toString();

                List<Path> fileLocations = searchFileInDirectoryAndSubdirectories(Path.of(directoryPath), fileNameString);
                if (!fileLocations.isEmpty()) {
                    // File locations is a list but as stated in the link below, it is a rare case. But can be solved if time allows.
                    // https://github.com/JabRef/jabref/issues/9798#issuecomment-1524155132
                    file.setLink(fileLocations.get(0).toString());
                    changedFiles.add(file);
                } else {
                    exceptions.add(fileNameString);
                    throw new FileNotFoundException();
                }
            }
        }
        return new RelinkedResults(changedFiles, exceptions);
    }

    public List<Path> searchFileInDirectoryAndSubdirectories(Path directory, String targetFileName) {
        List<Path> paths = new ArrayList<>();
        try {
            Files.walk(directory, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
                 .filter(path -> Files.isRegularFile(path))
                 .filter(path -> path.getFileName().toString().equals(targetFileName))
                 .forEach(paths::add);
        } catch (IOException e) {
            // Handle any exceptions here
        }
        List<Path> output = new ArrayList<>();
        for (Path p : paths) {
            output.add(p);
        }
        return output;
    }
}

package org.jabref.gui.externalfiles;

import java.io.IOException;
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
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSetFileLinksUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSetFileLinksUtil.class);
    private List<Path> directories;
    private AutoLinkPreferences autoLinkPreferences;
    private ExternalFileTypes externalFileTypes;

    public AutoSetFileLinksUtil(BibDatabaseContext databaseContext, FilePreferences filePreferences, AutoLinkPreferences autoLinkPreferences, ExternalFileTypes externalFileTypes) {
        this(databaseContext.getFileDirectories(filePreferences), autoLinkPreferences, externalFileTypes);
    }

    private AutoSetFileLinksUtil(List<Path> directories, AutoLinkPreferences autoLinkPreferences, ExternalFileTypes externalFileTypes) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.externalFileTypes = externalFileTypes;
    }

    public List<BibEntry> linkAssociatedFiles(List<BibEntry> entries, NamedCompound ce) {
        List<BibEntry> changedEntries = new ArrayList<>();
        for (BibEntry entry : entries) {

            List<LinkedFile> linkedFiles = new ArrayList<>();
            try {
                linkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                LOGGER.error("Problem finding files", e);
            }

            if (ce != null) {
                for (LinkedFile linkedFile : linkedFiles) {
                    // store undo information
                    String newVal = FileFieldWriter.getStringRepresentation(linkedFile);

                    String oldVal = entry.getField(StandardField.FILE).orElse(null);

                    UndoableFieldChange fieldChange = new UndoableFieldChange(entry, StandardField.FILE, oldVal, newVal);
                    ce.addEdit(fieldChange);

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        entry.addFile(linkedFile);
                    });
                }

                changedEntries.add(entry);
            }
        }
        return changedEntries;
    }

    public List<LinkedFile> findAssociatedNotLinkedFiles(BibEntry entry) throws IOException {
        List<LinkedFile> linkedFiles = new ArrayList<>();

        List<String> extensions = externalFileTypes.getExternalFileTypeSelection().stream().map(ExternalFileType::getExtension).collect(Collectors.toList());

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
                Optional<ExternalFileType> type = FileHelper.getFileExtension(foundFile)
                                                            .map(externalFileTypes::getExternalFileTypeByExt)
                                                            .orElse(Optional.of(new UnknownExternalFileType("")));

                String strType = type.isPresent() ? type.get().getName() : "";
                Path relativeFilePath = FileUtil.relativize(foundFile, directories);
                LinkedFile linkedFile = new LinkedFile("", relativeFilePath, strType);
                linkedFiles.add(linkedFile);
            }
        }

        return linkedFiles;
    }
}

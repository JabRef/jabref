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
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.util.FileHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSetFileLinksUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSetLinks.class);
    private List<Path> directories;
    private AutoLinkPreferences autoLinkPreferences;
    private ExternalFileTypes externalFileTypes;

    public AutoSetFileLinksUtil(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences, AutoLinkPreferences autoLinkPreferences, ExternalFileTypes externalFileTypes) {
        this(databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences), autoLinkPreferences, externalFileTypes);
    }

    public AutoSetFileLinksUtil(List<Path> directories, AutoLinkPreferences autoLinkPreferences, ExternalFileTypes externalFileTypes) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.externalFileTypes = externalFileTypes;
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
                String relativeFilePath = FileUtil.shortenFileName(foundFile, directories).toString();
                LinkedFile linkedFile = new LinkedFile("", relativeFilePath, strType);
                linkedFiles.add(linkedFile);
            }
        }

        return linkedFiles;
    }
}

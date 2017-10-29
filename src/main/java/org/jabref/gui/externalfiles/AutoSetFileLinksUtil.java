package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;

public class AutoSetFileLinksUtil {

    public Map<BibEntry, LinkedFile> findassociatedNotLinkedFiles(BibEntry entry, BibDatabaseContext databaseContext) {
        return findassociatedNotLinkedFiles(Arrays.asList(entry), databaseContext);
    }

    public Map<BibEntry, LinkedFile> findassociatedNotLinkedFiles(List<BibEntry> entries, BibDatabaseContext databaseContext) {
        Map<BibEntry, LinkedFile> linkedFiles = new HashMap<>();

        List<Path> dirs = databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
        List<String> extensions = ExternalFileTypes.getInstance().getExternalFileTypeSelection().stream().map(ExternalFileType::getExtension).collect(Collectors.toList());

        // Run the search operation:
        FileFinder fileFinder = FileFinders.constructFromConfiguration(Globals.prefs.getAutoLinkPreferences());
        Map<BibEntry, List<Path>> result = fileFinder.findAssociatedFiles(entries, dirs, extensions);

        // Iterate over the entries:
        for (Entry<BibEntry, List<Path>> entryFilePair : result.entrySet()) {

            for (Path foundFile : entryFilePair.getValue()) {
                boolean existingSameFile = entryFilePair.getKey().getFiles().stream()
                        .map(file -> file.findIn(dirs))
                        .anyMatch(file -> {
                            try {
                                return file.isPresent() && Files.isSameFile(file.get(), foundFile);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            return false;
                        });
                if (!existingSameFile) {

                    Optional<ExternalFileType> type = FileHelper.getFileExtension(foundFile)
                            .map(ExternalFileTypes.getInstance()::getExternalFileTypeByExt)
                            .orElse(Optional.of(new UnknownExternalFileType("")));

                    String strType = type.isPresent() ? type.get().getName() : "";

                    LinkedFile linkedFile = new LinkedFile("", foundFile.toString(), strType);
                    linkedFiles.put(entryFilePair.getKey(), linkedFile);

                }
            }

        }
        return linkedFiles;
    }
}

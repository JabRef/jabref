package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;

public class AutoSetFileLinksUtil {

    public FieldChange findassociatedNotLinkedFiles(BibEntry entry, BibDatabaseContext databaseContext) {
        return findassociatedNotLinkedFiles(Arrays.asList(entry), databaseContext);
    }

    public FieldChange findassociatedNotLinkedFiles(List<BibEntry> entries, BibDatabaseContext databaseContext) {

        List<Path> dirs = databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
        List<String> extensions = ExternalFileTypes.getInstance().getExternalFileTypeSelection().stream().map(ExternalFileType::getExtension).collect(Collectors.toList());

        // Run the search operation:
        FileFinder fileFinder = FileFinders.constructFromConfiguration(Globals.prefs.getAutoLinkPreferences());
        Map<BibEntry, List<Path>> result = fileFinder.findAssociatedFiles(entries, dirs, extensions);

        // Iterate over the entries:
        for (Entry<BibEntry, List<Path>> entryFilePair : result.entrySet()) {
            Optional<String> oldVal = entryFilePair.getKey().getField(FieldName.FILE);

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
                            .map(extension -> ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension))
                            .orElse(Optional.of(new UnknownExternalFileType("")));

                    String strType = type.isPresent() ? type.get().getName() : "";

                    LinkedFile linkedFile = new LinkedFile("", foundFile.toString(), strType);
                    String newVal = FileFieldWriter.getStringRepresentation(linkedFile);

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        entryFilePair.getKey().addFile(linkedFile);
                    });

                    return new FieldChange(entryFilePair.getKey(), FieldName.FILE, oldVal.orElse(null), newVal);
                }
            }

        }
        return new FieldChange(null, FieldName.FILE, null, null);
    }
}

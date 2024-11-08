package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalFilesEntryLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFilesEntryLinker.class);

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final NotificationService notificationService;
    private final Supplier<BibDatabaseContext> bibDatabaseContextSupplier;

    /**
     * @param stateManager required for currently active BibDatabaseContext
     */
    public ExternalFilesEntryLinker(ExternalApplicationsPreferences externalApplicationsPreferences, FilePreferences filePreferences, NotificationService notificationService, StateManager stateManager) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.filePreferences = filePreferences;
        this.bibDatabaseContextSupplier = () -> stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
        this.notificationService = notificationService;
    }

    public void linkFilesToEntry(BibEntry entry, List<Path> files) {
        List<LinkedFile> existingFiles = entry.getFiles();
        List<LinkedFile> linkedFiles = files.stream().flatMap(file -> {
            String typeName = FileUtil.getFileExtension(file)
                                      .map(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences).orElse(new UnknownExternalFileType(ext)).getName())
                                      .orElse("");
            Path relativePath = FileUtil.relativize(file, bibDatabaseContextSupplier.get(), filePreferences);
            LinkedFile linkedFile = new LinkedFile("", relativePath, typeName);

            String link = linkedFile.getLink();
            boolean alreadyLinked = existingFiles.stream().anyMatch(existingFile -> existingFile.getLink().equals(link));
            if (alreadyLinked) {
                notificationService.notify(Localization.lang("File '%0' already linked", link));
                return Stream.empty();
            } else {
                return Stream.of(linkedFile);
            }
        }).toList();
        entry.addFiles(linkedFiles);
    }

    /**
     * <ul>
     *     <li>Move files to file directory</li>
     *     <li>Use configured file directory pattern</li>
     *     <li>Rename file to configured pattern (and skip renaming if file already exists)</li>
     *     <li>Avoid overwriting files - by adding " {number}" after the file name</li>
     * </ul>
     */
    public void coveOrMoveFilesSteps(BibEntry entry, List<Path> files, boolean shouldMove) {
        List<LinkedFile> existingFiles = entry.getFiles();
        List<LinkedFile> linkedFiles = new ArrayList<>(files.size());
        // "old school" loop to enable logging properly
        for (Path file : files) {
            String typeName = FileUtil.getFileExtension(file)
                                      .map(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences).orElse(new UnknownExternalFileType(ext)).getName())
                                      .orElse("");
            LinkedFile linkedFile = new LinkedFile("", file, typeName);
            LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, bibDatabaseContextSupplier.get(), filePreferences);
            try {
                linkedFileHandler.copyOrMoveToDefaultDirectory(shouldMove, true);
            } catch (IOException exception) {
                LOGGER.error("Error while copying/moving file {}", file, exception);
            }

            String link = linkedFile.getLink();
            boolean alreadyLinked = existingFiles.stream().anyMatch(existingFile -> existingFile.getLink().equals(link));
            if (alreadyLinked) {
                notificationService.notify(Localization.lang("File '%0' already linked", link));
            } else {
                linkedFiles.add(linkedFile);
            }
        }
        entry.addFiles(linkedFiles);
    }
}

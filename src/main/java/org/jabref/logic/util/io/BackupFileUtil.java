package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.OS;

import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupFileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupFileUtil.class);

    private BackupFileUtil() {
    }

    public static Path getAppDataBackupDir() {
        Path directory = Path.of(AppDirsFactory.getInstance().getUserDataDir(
                                     OS.APP_DIR_APP_NAME,
                                     new BuildInfo().version.toString(),
                                     OS.APP_DIR_APP_AUTHOR))
                             .resolve("backups");
        return directory;
    }

    /**
     * Determines the path of the backup file (using the given extension)
     *
     * <p>
     *     As default, a directory inside the user temporary dir is used.<br>
     *     In case a AUTOSAVE backup is requested, a timestamp is added
     * </p>
     * <p>
     *     <em>SIDE EFFECT</em>: Creates the directory.
     *     In case that fails, the return path of the .bak file is set to be next to the .bib file
     * </p>
     * <p>
     *     Note that this backup is different from the <code>.sav</code> file generated by {@link org.jabref.logic.autosaveandbackup.BackupManager}
     *     (and configured in the preferences as "make backups")
     * </p>
     */
    public static Path getPathForNewBackupFileAndCreateDirectory(Path targetFile, BackupFileType fileType) {
        String extension = "." + fileType.getExtensions().get(0);
        String timeSuffix = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH.mm.ss"));

        // We choose the data directory, because a ".bak" file should survive cache cleanups
        Path directory = getAppDataBackupDir();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            Path result = FileUtil.addExtension(targetFile, extension);
            LOGGER.warn("Could not create bib writing directory {}, using {} as file", directory, result, e);
            return result;
        }
        String baseFileName = getUniqueFilePrefix(targetFile) + "--" + targetFile.getFileName() + "--" + timeSuffix;
        Path fileName = FileUtil.addExtension(Path.of(baseFileName), extension);
        return directory.resolve(fileName);
    }

    /**
     * Finds the latest backup (.sav). If it does not exist, an empty optional is returned
     *
     * @param targetFile the full path of the file to backup
     */
    public static Optional<Path> getPathOfLatestExisingBackupFile(Path targetFile, BackupFileType fileType) {
        // The code is similar to "getPathForNewBackupFileAndCreateDirectory"

        String extension = "." + fileType.getExtensions().get(0);

        Path directory = getAppDataBackupDir();
        if (Files.notExists(directory)) {
            // In case there is no app directory, we search in the directory of the bib file
            Path result = FileUtil.addExtension(targetFile, extension);
            if (Files.exists(result)) {
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }

        // Search the directory for the latest file
        final String prefix = getUniqueFilePrefix(targetFile) + "--" + targetFile.getFileName();
        Optional<Path> mostRecentFile;
        try {
            mostRecentFile = Files.list(directory)
                                         // just list the .sav belonging to the given targetFile
                                         .filter(p -> p.getFileName().toString().startsWith(prefix))
                                         .sorted()
                                         .reduce((first, second) -> second);
        } catch (IOException e) {
            LOGGER.error("Could not determine most recent file", e);
            return Optional.empty();
        }
        return mostRecentFile;
    }

    /**
     * <p>
     * Determines a unique file prefix.
     * </p>
     * <p>
     *     When creating a backup file, the backup file should belong to the original file.
     *     Just adding ".bak" suffix to the filename, does not work in all cases:
     *     It may be possible that the user has opened "paper.bib" twice.
     *     Thus, we need to create a unique prefix to distinguish these files.
     * </p>
     */
    public static String getUniqueFilePrefix(Path targetFile) {
        // Idea: use the hash code and convert it to hex
        // Thereby, use positive values only and use length 4
        int positiveCode = Math.abs(targetFile.hashCode());
        byte[] array = ByteBuffer.allocate(4).putInt(positiveCode).array();
        return HexFormat.of().formatHex(array);
    }
}

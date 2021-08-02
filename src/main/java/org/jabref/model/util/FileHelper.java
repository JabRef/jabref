package org.jabref.model.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.parser.AutoDetectParser;

public class FileHelper {

    /**
     * Returns the extension of a file or Optional.empty() if the file does not have one (no . in name).
     *
     * @param file
     * @return The extension, trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(Path file) {
        return getFileExtension(file.toString());
    }

    /**
     * Returns the extension of a file name or Optional.empty() if the file does not have one (no "." in name).
     *
     * @param fileName
     * @return The extension (without leading dot), trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(String fileName) {
        Metadata metadata = new Metadata();
        metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

        if (isUrl(fileName)) {
            try (InputStream is = new URL(fileName).openStream()) {
                return detectExtension(is, metadata);
            } catch (IOException | MimeTypeException e) {
                return Optional.empty();
            }
        }

        int dotPosition = fileName.lastIndexOf('.');
        if ((dotPosition > 0) && (dotPosition < (fileName.length() - 1))) {
            return Optional.of(fileName.substring(dotPosition + 1).trim().toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private static Optional<String> detectExtension(InputStream is, Metadata metaData) throws IOException, MimeTypeException {
        BufferedInputStream bis = new BufferedInputStream(is);
        AutoDetectParser parser = new AutoDetectParser();
        Detector detector = parser.getDetector();
        MediaType mediaType = detector.detect(bis, metaData);
        MimeType mimeType = TikaConfig.getDefaultConfig().getMimeRepository().forName(mediaType.toString());
        String extension = mimeType.getExtension();

        if (extension.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(extension.substring(1));
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns an empty optional if the file does not
     * exist.<br/>
     * <p>
     * Uses <ul>
     * <li>the default directory associated with the extension of the file</li>
     * <li>the standard file directory</li>
     * <li>the directory of the BIB file</li>
     * </ul>
     *
     * @param databaseContext The database this file belongs to.
     * @param fileName        The filename, may also be a relative path to the file
     */
    public static Optional<Path> find(final BibDatabaseContext databaseContext, String fileName, FilePreferences filePreferences) {
        return find(fileName, databaseContext.getFileDirectories(filePreferences));
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * an empty optional if the file does not exist.
     * <p>
     * Will look in each of the given directories starting from the beginning and
     * returning the first found file to match if any.
     */
    public static Optional<Path> find(String fileName, List<Path> directories) {
        return directories.stream()
                          .flatMap(directory -> find(fileName, directory).stream())
                          .findFirst();
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * an empty optional if the file does not exist.
     */
    public static Optional<Path> find(String fileName, Path directory) {
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(directory);

        // Explicitly check for an empty String, as File.exists returns true on that empty path, because it maps to the default jar location
        // if we then call toAbsoluteDir, it would always return the jar-location folder. This is not what we want here
        if (fileName.isEmpty()) {
            return Optional.of(directory);
        }

        Path resolvedFile = directory.resolve(fileName);
        if (Files.exists(resolvedFile)) {
            return Optional.of(resolvedFile);
        } else {
            return Optional.empty();
        }
    }

    private static boolean isUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Path relativize(Path path, List<Path> fileDirectories) {
        if (!path.isAbsolute()) {
            return path;
        }

        for (Path directory : fileDirectories) {
            if (path.startsWith(directory)) {
                return directory.relativize(path);
            }
        }
        return path;
    }
}

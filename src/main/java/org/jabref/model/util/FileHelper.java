package org.jabref.model.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.FilePreferences;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
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
        metadata.add(Metadata.RESOURCE_NAME_KEY, fileName);

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
     * @param name     The filename, may also be a relative path to the file
     */
    public static Optional<Path> expandFilename(final BibDatabaseContext databaseContext, String name,
                                                FilePreferences filePreferences) {
        Optional<String> extension = getFileExtension(name);
        // Find the default directory for this field type, if any:
        List<String> directories = databaseContext.getFileDirectories(extension.orElse(null), filePreferences);
        // Include the standard "file" directory:
        List<String> fileDir = databaseContext.getFileDirectories(filePreferences);

        List<String> searchDirectories = new ArrayList<>();
        for (String dir : directories) {
            if (!searchDirectories.contains(dir)) {
                searchDirectories.add(dir);
            }
        }
        for (String aFileDir : fileDir) {
            if (!searchDirectories.contains(aFileDir)) {
                searchDirectories.add(aFileDir);
            }
        }

        return expandFilename(name, searchDirectories);
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     * <p>
     * Will look in each of the given dirs starting from the beginning and
     * returning the first found file to match if any.
     *
     * @deprecated use {@link #expandFilenameAsPath(String, List)} instead
     */
    @Deprecated
    public static Optional<Path> expandFilename(String name, List<String> directories) {
        for (String dir : directories) {
            Optional<Path> result = expandFilename(name, Paths.get(dir));
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    public static Optional<Path> expandFilenameAsPath(String name, List<Path> directories) {
        for (Path directory : directories) {
            Optional<Path> result = expandFilename(name, directory);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * an empty optional if the file does not exist.
     */
    private static Optional<Path> expandFilename(String filename, Path directory) {
        Objects.requireNonNull(filename);
        Objects.requireNonNull(directory);

        Path file = Paths.get(filename);
        //Explicitly check for an empty String, as File.exists returns true on that empty path, because it maps to the default jar location
        // if we then call toAbsoluteDir, it would always return the jar-location folder. This is not what we want here
        if (filename.isEmpty()) {
            return Optional.of(directory);
        }

        Path resolvedFile = directory.resolve(file);
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
}

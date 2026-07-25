package org.jabref.logic.ai.ingestion.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileHasher {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHasher.class);

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    private FileHasher() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Computes the SHA-256 hash of a file.
    ///
    /// @param path the path to the file
    /// @return Optional containing the hex-encoded hash of the file, or empty if an error occurred
    public static Optional<String> computeHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            try (InputStream inputStream = Files.newInputStream(path)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return Optional.of(bytesToHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.error("Could not compute hash for file \"{}\"", path, e);
            return Optional.empty();
        }
    }

    /// Converts a byte array to a hexadecimal string.
    ///
    /// @param bytes the byte array
    /// @return the hex-encoded string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte theByte : bytes) {
            stringBuilder.append("%02x".formatted(theByte));
        }
        return stringBuilder.toString();
    }
}

package org.jabref.gui.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISBN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Provides functions for downloading and retrieving book covers for entries.
public class BookCoverFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookCoverFetcher.class);

    private static final Pattern URL_JSON_PATTERN = Pattern.compile("^\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*$");

    private static final String URL_FETCHER_URL = "https://bookcover.longitood.com/bookcover/";
    private static final String IMAGE_FALLBACK_URL = "https://covers.openlibrary.org/b/isbn/";
    private static final String IMAGE_FALLBACK_SUFFIX = "-L.jpg";
    private static final Integer IMAGE_DOWNLOAD_COOLDOWN_HOURS = 24;

    private static final CustomExternalFileType NOT_AVAILABLE_FILE_TYPE = new CustomExternalFileType("", "not-available", "", "", "", IconTheme.JabRefIcons.FILE);

    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    public BookCoverFetcher(ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
    }

    public Optional<Path> getDownloadedCoverForEntry(BibEntry entry) {
        return entry.getISBN().flatMap(isbn -> findExistingImage("isbn-" + isbn.asString(), Directories.getCoverDirectory()));
    }

    public void downloadCoversForEntry(BibEntry entry) {
        entry.getISBN().ifPresent(isbn -> downloadCoverForISBN(isbn, Directories.getCoverDirectory()));
    }

    private void downloadCoverForISBN(ISBN isbn, Path directory) {
        final String name = "isbn-" + isbn.asString();
        if (findExistingImage(name, directory).isEmpty()) {
            Optional<Duration> timeSincePreviousAttempt = timeSincePreviousAttempt(name, directory);
            if (!timeSincePreviousAttempt.isEmpty() && timeSincePreviousAttempt.get().toHours() < IMAGE_DOWNLOAD_COOLDOWN_HOURS) {
                LOGGER.info("Skipped download attempt for {}, attempted less than 24 hours ago", name);
                return;
            }
            final String url = getImageUrl(isbn);
            downloadCoverImage(url, name, directory);
        }
    }

    private Optional<Duration> timeSincePreviousAttempt(String name, Path directory) {
        Optional<Path> notAvailablePathOptional = resolveNameWithType(directory, name, NOT_AVAILABLE_FILE_TYPE);
        if (notAvailablePathOptional.isEmpty()) {
            LOGGER.warn("Could not find not available file path for: {}", name);
            return Optional.empty();
        }
        Path notAvailablePath = notAvailablePathOptional.get();
        if (Files.exists(notAvailablePath)) {
            try {
                FileTime lastModifiedTimeStamp = Files.getLastModifiedTime(notAvailablePath);
                Duration timeSinceLastModification = Duration.between(lastModifiedTimeStamp.toInstant(), Instant.now());
                return Optional.of(timeSinceLastModification);
            } catch (IOException e) {
                LOGGER.warn("Could not read last modified time", e);
            }
        }
        return Optional.empty();
    }

    private void downloadCoverImage(String url, final String name, final Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER.error("Could not access cover image directories", e);
            return;
        }

        LOGGER.info("Downloading cover image file from {}", url);

        URLDownload download;
        try {
            download = getURLDownload(url);
        } catch (MalformedURLException e) {
            LOGGER.error("Error while downloading cover image file", e);
            return;
        }

        ExternalFileType inferredFileType = download
                .getMimeType().flatMap(mime -> ExternalFileTypes.getExternalFileTypeByMimeType(mime, externalApplicationsPreferences))
                .filter(fileType -> fileType.getMimeType().startsWith("image/"))
                .or(() -> FileUtil.getFileNameFromUrl(url).flatMap(FileUtil::getFileExtension)
                                  .flatMap(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences))
                                  .filter(fileType -> fileType.getMimeType().startsWith("image/")))
                .orElse(StandardExternalFileType.JPG);

        Optional<Path> destinationOptional = resolveNameWithType(directory, name, inferredFileType);
        if (destinationOptional.isEmpty()) {
            LOGGER.warn("Skipping cover download: Could not resolve valid path for name {}", name);
            return;
        }
        Path destination = destinationOptional.get();
        try {
            download.toFile(destination);
            deleteNotAvailableFileIfExists(name, directory);
        } catch (FetcherClientException | FetcherServerException e) {
            LOGGER.info("Remote book cover does not exist or server returned an error for URL: {}", url);
            LOGGER.info("Flagging book cover as not available");
            flagAsNotAvailable(name, directory);
        } catch (FetcherException e) {
            LOGGER.error("Error while downloading or saving cover image file", e);
        }
    }

    protected URLDownload getURLDownload(String url) throws MalformedURLException {
        return new URLDownload(url);
    }

    private void flagAsNotAvailable(final String name, final Path directory) {
        Optional<Path> destinationOptional = resolveNameWithType(directory, name, NOT_AVAILABLE_FILE_TYPE);
        if (destinationOptional.isEmpty()) {
            LOGGER.warn("Skipping flagging as not available: Could not resolve valid path for name {}", name);
            return;
        }
        Path destination = destinationOptional.get();
        if (Files.exists(destination)) {
            try {
                Instant now = Instant.now();
                Files.setLastModifiedTime(destination, FileTime.from(now));
            } catch (IOException e) {
                LOGGER.error("Could not update last modified time of .not-available file", e);
            }
        } else {
            try {
                Files.createFile(destination);
            } catch (IOException e) {
                LOGGER.error("Could not create .not-available file", e);
            }
        }
    }

    private void deleteNotAvailableFileIfExists(final String name, final Path directory) {
        Optional<Path> destinationOptional = resolveNameWithType(directory, name, NOT_AVAILABLE_FILE_TYPE);
        if (destinationOptional.isEmpty()) {
            return;
        }
        Path destination = destinationOptional.get();
        try {
            Files.deleteIfExists(destination);
        } catch (IOException e) {
            LOGGER.error("Could not delete .not-available file", e);
        }
    }

    private Optional<Path> findExistingImage(final String name, final Path directory) {
        return externalApplicationsPreferences.getExternalFileTypes().stream()
                                              .filter(fileType -> fileType.getMimeType().startsWith("image/"))
                                              .flatMap(fileType -> resolveNameWithType(directory, name, fileType).stream())
                                              .filter(Files::exists).findFirst();
    }

    private static Optional<Path> resolveNameWithType(Path directory, String name, ExternalFileType fileType) {
        try {
            return Optional.of(directory.resolve(FileUtil.getValidFileName(name + "." + fileType.getExtension())));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    private String getImageUrl(ISBN isbn) {
        if (isbn.isIsbn13()) {
            String url = URL_FETCHER_URL + isbn.asString();
            try {
                LOGGER.info("Downloading book cover url from {}", url);

                URLDownload download = getURLDownload(url);
                String json = download.asString();
                Matcher matches = URL_JSON_PATTERN.matcher(json);

                if (matches.find()) {
                    String coverUrlString = matches.group(1);
                    if (coverUrlString != null) {
                        return coverUrlString;
                    }
                }
            } catch (FetcherException | MalformedURLException e) {
                LOGGER.error("Error while querying cover url, using fallback", e);
            }
        }
        return IMAGE_FALLBACK_URL + isbn.asString() + IMAGE_FALLBACK_SUFFIX;
    }
}

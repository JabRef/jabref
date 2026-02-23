package org.jabref.gui.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.importer.FetcherException;
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

    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    public BookCoverFetcher(ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
    }

    public Optional<Path> getDownloadedCoverForEntry(BibEntry entry) {
        return entry.getISBN().flatMap(isbn -> findExistingImage("isbn-" + isbn.asString(), Directories.getCoverDirectory()));
    }

    public boolean downloadCoversForEntry(BibEntry entry) {
        return entry.getISBN().map(isbn -> downloadCoverForISBN(isbn, Directories.getCoverDirectory())).orElse(false);
    }

    private boolean downloadCoverForISBN(ISBN isbn, Path directory) {
        final String name = "isbn-" + isbn.asString();
        if (findExistingImage(name, directory).isPresent()) {
            return false;
        }

        Path notAvailableFile = directory.resolve(name + ".not-available");
        if (Files.exists(notAvailableFile)) {
            try {
                long lastModified = Files.getLastModifiedTime(notAvailableFile).toMillis();
                if (System.currentTimeMillis() - lastModified < 24 * 60 * 60 * 1000L) {
                    LOGGER.debug("Skipping cover download for {} — marked not-available within last 24h", isbn.asString());
                    return false;
                }
            } catch (IOException e) {
                LOGGER.warn("Could not read .not-available file timestamp for {}", isbn.asString(), e);
            }
        }

        final String url = getImageUrl(isbn);
        try {
            boolean success = downloadCoverImage(url, name, directory);
            if (success) {
                // Clean up any stale not-available flag when we finally get a cover
                try {
                    Files.deleteIfExists(notAvailableFile);
                } catch (IOException e) {
                    LOGGER.warn("Could not delete .not-available file for {}", isbn.asString(), e);
                }
            }
            return success;
        } catch (org.jabref.logic.importer.FetcherClientException e) {
            // Only cache a 404 (cover doesn't exist) — 429, 403, etc. are transient and should be retried
            int statusCode = e.getHttpResponse().map(r -> r.statusCode()).orElse(-1);
            if (statusCode != java.net.HttpURLConnection.HTTP_NOT_FOUND) {
                LOGGER.warn("Non-404 client error ({}) fetching cover for ISBN {}, will retry next time", statusCode, isbn.asString(), e);
                return false;
            }
            LOGGER.info("No cover found for ISBN {} (HTTP 404). Will skip for 24h.", isbn.asString(), e);
            try {
                if (!Files.exists(notAvailableFile)) {
                    Files.createDirectories(directory);
                    Files.createFile(notAvailableFile);
                } else {
                    Files.setLastModifiedTime(notAvailableFile,
                                              java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
                }
            } catch (IOException ioException) {
                LOGGER.warn("Could not write .not-available file for {}", isbn.asString(), ioException);
            }
            return false;
        } catch (FetcherException e) {
            // Transient network or server error — do NOT cache, allow retry on next open
            LOGGER.warn("Transient error fetching cover for ISBN {}, will retry next time", isbn.asString(), e);
            return false;
        }
    }

    private boolean downloadCoverImage(String url, final String name, final Path directory) throws FetcherException {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER.error("Could not access cover image directories", e);
            return false;
        }

        LOGGER.info("Downloading cover image file from {}", url);

        URLDownload download;
        try {
            download = new URLDownload(url);
        } catch (MalformedURLException e) {
            LOGGER.error("Error while downloading cover image file", e);
            return false;
        }

        ExternalFileType inferredFileType = download
                                                    .getMimeType().flatMap(mime -> ExternalFileTypes.getExternalFileTypeByMimeType(mime, externalApplicationsPreferences))
                                                    .filter(fileType -> fileType.getMimeType().startsWith("image/"))
                                                    .or(() -> FileUtil.getFileNameFromUrl(url).flatMap(FileUtil::getFileExtension)
                                                                      .flatMap(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences))
                                                                      .filter(fileType -> fileType.getMimeType().startsWith("image/")))
                                                    .orElse(StandardExternalFileType.JPG);

        Optional<Path> destination = resolveNameWithType(directory, name, inferredFileType);
        if (destination.isEmpty()) {
            return false;
        }

        download.toFile(destination.get());

        return true;
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

    private static String getImageUrl(ISBN isbn) {
        if (isbn.isIsbn13()) {
            String url = URL_FETCHER_URL + isbn.asString();
            try {
                LOGGER.info("Downloading book cover url from {}", url);

                URLDownload download = new URLDownload(url);
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

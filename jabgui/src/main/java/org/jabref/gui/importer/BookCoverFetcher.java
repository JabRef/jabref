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

/**
 * Provides functions for downloading and retrieving book covers for entries.
 */
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

    public void downloadCoversForEntry(BibEntry entry) {
        entry.getISBN().ifPresent(isbn -> downloadCoverForISBN(isbn, Directories.getCoverDirectory()));
    }

    private void downloadCoverForISBN(ISBN isbn, Path directory) {
        final String name = "isbn-" + isbn.asString();
        if (findExistingImage(name, directory).isEmpty()) {
            final String url = getSourceForIsbn(isbn);
            downloadCoverImage(url, name, directory);
        }
    }

    private void downloadCoverImage(String url, final String name, final Path directory) {
        Optional<String> extension = FileUtil.getFileNameFromUrl(url).flatMap(FileUtil::getFileExtension);

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER.error("Could not access cover image directories", e);
            return;
        }

        try {
            LOGGER.info("Downloading cover image file from {}", url);

            URLDownload download = new URLDownload(url);
            Optional<String> mimeIfAvailable = download.getMimeType();

            Optional<ExternalFileType> inferredFromMime = mimeIfAvailable.flatMap(mime -> ExternalFileTypes.getExternalFileTypeByMimeType(mime, externalApplicationsPreferences))
                                                                         .filter(fileType -> fileType.getMimeType().startsWith("image/"));
            Optional<ExternalFileType> inferredFromExtension = extension.flatMap(ext -> ExternalFileTypes.getExternalFileTypeByExt(ext, externalApplicationsPreferences))
                                                                        .filter(fileType -> fileType.getMimeType().startsWith("image/"));
            final ExternalFileType inferredFileType = inferredFromMime.orElse(inferredFromExtension.orElse(StandardExternalFileType.JPG));

            Optional<Path> destination = resolveNameWithType(directory, name, inferredFileType);
            if (destination.isPresent()) {
                download.toFile(destination.get());
            }
        } catch (FetcherException | MalformedURLException e) {
            LOGGER.error("Error while downloading cover image file", e);
            return;
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

    private static String getSourceForIsbn(ISBN isbn) {
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

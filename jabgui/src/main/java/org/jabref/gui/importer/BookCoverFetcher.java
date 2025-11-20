package org.jabref.gui.importer;

import java.net.MalformedURLException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.identifier.ISBN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functions for downloading book covers for new entries.
 */
public class BookCoverFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookCoverFetcher.class);

    private static final Pattern URL_JSON_PATTERN = Pattern.compile("^\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*$");

    private static final String URL_FETCHER_URL = "https://bookcover.longitood.com/bookcover/";
    private static final String IMAGE_FALLBACK_URL = "https://covers.openlibrary.org/b/isbn/";
    private static final String IMAGE_FALLBACK_SUFFIX = "-L.jpg";

    public static Optional<BibEntry> withAttachedCoverFileIfExists(Optional<BibEntry> possible, BibDatabaseContext databaseContext, FilePreferences filePreferences, ExternalApplicationsPreferences externalApplicationsPreferences) {
        if (possible.isPresent() && filePreferences.shouldDownloadCovers()) {
            BibEntry entry = possible.get();
            Optional<ISBN> isbn = entry.getISBN();
            if (isbn.isPresent()) {
                final String url = getCoverImageURLForIsbn(isbn.get());
                final Optional<Path> directory = databaseContext.getFirstExistingFileDir(filePreferences);

                // Cannot use pattern for name, as auto-generated citation keys aren't available where function is used (org.jabref.gui.newentry.NewEntryViewModel#withCoversAttached)
                final String name = "isbn-" + isbn.get().asString();

                Optional<LinkedFile> file = tryToDownloadLinkedFile(externalApplicationsPreferences, url, directory, filePreferences.coversDownloadLocation().trim(), name);
                if (file.isPresent()) {
                    entry.addFile(file.get());
                }
            }
            possible = Optional.of(entry);
        }
        return possible;
    }

    private static String getCoverImageURLForIsbn(ISBN isbn) {
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

    private static Optional<LinkedFile> tryToDownloadLinkedFile(ExternalApplicationsPreferences externalApplicationsPreferences, String url, Optional<Path> directory, String location, String name) {
        Optional<Path> subdirectory = resolveRealSubdirectory(directory, location);
        if (subdirectory.isPresent()) {
            Optional<String> extension = FileUtil.getFileExtension(FileUtil.getFileNameFromUrl(url));
            Path destination = subdirectory.get().resolve(extension.map(x -> name + "." + x).orElse(name));

            String link = directory.get().relativize(destination).toString();
            
            Optional<String> mime = Optional.empty();

            if (Files.exists(destination)) {
                try {
                    String possiblyNullMimeType = Files.probeContentType();
                    if (possiblyNullMimeType != null) {
                        mime = Optional.of(possiblyNullMimeType)
                    }
                } catch (IOException e) {
                    LOGGER.error("File said it existed, but probeContentType failed", e);
                }
            } else {
                try {
                    LOGGER.info("Downloading cover image file from {}", url);

                    URLDownload download = new URLDownload(url);
                    mime = download.getMimeType();
                    download.toFile(destination);
                } catch (FetcherException | MalformedURLException e) {
                    LOGGER.error("Error while downloading cover image file, Storing as URL in file field", e);
                    return Optional.of(new LinkedFile("[cover]", url, ""));
                }
            }
            
            String type = inferFileType(externalApplicationsPreferences, mime, extension);
            return Optional.of(new LinkedFile("[cover]", link, type, url));
        } else {
            LOGGER.warn("File directory not available while downloading cover image {}. Storing as URL in file field.", url);
            return Optional.of(new LinkedFile("[cover]", url, ""));
        }
    }

    private static Optional<Path> resolveRealSubdirectory(Optional<Path> directory, String location) {
        if ("".equals(location)) {
            return directory;
        }
        if (directory.isPresent()) {
            try {
                final Path subdirectory = directory.get().resolve(location);
                Files.createDirectories(subdirectory);
                if (Files.exists(subdirectory)) {
                    return Optional.of(subdirectory);
                }
            } catch (IOException | InvalidPathException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static String inferFileType(ExternalApplicationsPreferences externalApplicationsPreferences, Optional<String> mime, Optional<String> extension) {
        Optional<ExternalFileType> suggested = Optional.empty();
        if (mime.isPresent()) {
            suggested = ExternalFileTypes.getExternalFileTypeByMimeType(mime.get(), externalApplicationsPreferences);
        }
        if (suggested.isEmpty() && extension.isPresent()) {
            Optional<ExternalFileType> suggested = ExternalFileTypes.getExternalFileTypeByExt(extension.get(), externalApplicationsPreferences);
        }
        return suggested.map(t -> t.getName()).orElse("");
    }
}

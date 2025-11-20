package org.jabref.gui.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public Optional<Path> getDownloadedCoverForEntry(BibEntry entry, String location) {
        return entry.getISBN().flatMap(isbn -> findExistingImage("isbn-" + isbn.asString(), Path.of(location)));
    }

    public void downloadCoversForEntry(BibEntry entry, String location) {
        entry.getISBN().ifPresent(isbn -> downloadCoverForISBN(isbn, Path.of(location)));
    }

    private void downloadCoverForISBN(ISBN isbn, Path directory) {
        final String name = "isbn-" + isbn.asString();
        if (findExistingImage(name, directory).isEmpty()) {
            final String url = getSourceForIsbn(isbn);
            downloadCoverImage(url, name, directory);
        }
    }

    private Optional<Path> findExistingImage(String name, Path directory) {
        for (ExternalFileType filetype : externalApplicationsPreferences.getExternalFileTypes()) {
            if (filetype.getMimeType().startsWith("image/")) {
                Path path = directory.resolve(FileUtil.getValidFileName(name + "." + filetype.getExtension()));
                if (Files.exists(path)) {
                    return Optional.of(path);
                }
            }
        }
        return Optional.empty();
    }

    private void downloadCoverImage(String url, String name, Path directory) {
        Optional<String> extension = FileUtil.getFileExtension(FileUtil.getFileNameFromUrl(url));

        try {
            LOGGER.info("Downloading cover image file from {}", url);

            URLDownload download = new URLDownload(url);
            Optional<String> mime = download.getMimeType();

            inferExtensionOfImage(mime, extension).map(x -> directory.resolve(FileUtil.getValidFileName(name + "." + x))).ifPresent(p -> download.toFile(p));
        } catch (FetcherException | MalformedURLException e) {
            LOGGER.error("Error while downloading cover image file", e);
        }
    }

    private Optional<String> inferExtensionOfImage(Optional<String> mime, Optional<String> extension) {
        return mime.map(m -> ExternalFileTypes.getExternalFileTypeByMimeType(m, externalApplicationsPreferences))
                   .orElse(extension.flatMap(x -> ExternalFileTypes.getExternalFileTypeByExt(x, externalApplicationsPreferences)))
                   .filter(t -> t.getMimeType().startsWith("image/")).map(t -> t.getExtension());
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

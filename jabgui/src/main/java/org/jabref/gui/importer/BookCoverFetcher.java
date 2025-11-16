package org.jabref.gui.importer;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.hc.core5.net.URIBuilder;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.logic.FilePreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.LinkedFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileUtil;

import kong.unirest.core.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches web resources for bibliographic information.
 */
public class BookCoverFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookCoverFetcher.class);

    private static final Pattern BOOK_COVER_PATTERN = Pattern.compile("^\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*$");

    public static Optional<BibEntry> withAttachedCoverFileIfExists(Optional<BibEntry> possible, BibDatabaseContext databaseContext, FilePreferences filePreferences, ExternalApplicationsPreferences externalApplicationsPreferences) {
        if (possible.isPresent()) {
            BibEntry entry = possible.get();
            Optional<ISBN> isbn = entry.getISBN();
            if (isbn.isPresent()) {
                final String url = getCoverImageURLForIsbn(isbn.get());
                final Path directory = databaseContext.getFirstExistingFileDir(filePreferences).orElse(filePreferences.getWorkingDirectory());

                // Cannot use pattern for name, as auto-generated citation keys aren't available where function is used
                final String name = "isbn-"+isbn.get().asString();

                Optional<LinkedFile> file = tryToDownloadLinkedFile(externalApplicationsPreferences, directory, url, name);
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
            String url = "https://bookcover.longitood.com/bookcover/" + isbn.asString();
            try {
                LOGGER.info("Downloading book cover url from {}", url);

                URLDownload download = new URLDownload(url);
                download.canBeReached();

                String json = download.asString();
                Matcher matches = BOOK_COVER_PATTERN.matcher(json);

                if (matches.find()) {
                    String coverUrlString = matches.group(1);
                    if (coverUrlString != null) {
                        return coverUrlString;
                    }
                }
            } catch (MalformedURLException | FetcherException e) {
                LOGGER.error("Error while querying cover url, using fallback", e);
            }
        }
        return "https://covers.openlibrary.org/b/isbn/" + isbn.asString() + "-L.jpg";
    }

    private static Optional<LinkedFile> tryToDownloadLinkedFile(ExternalApplicationsPreferences externalApplicationsPreferences, Path directory, String url, String name) {
        File covers = directory.resolve("covers").toFile();
        covers.mkdirs();

        if (covers.exists()) {
        	final Optional<String> extension = FileUtil.getFileExtension(FileUtil.getFileNameFromUrl(url));
            final Path destination = directory.resolve("covers").resolve(extension.map(x -> name + "." + x).orElse(name));
            final String link = directory.relativize(destination).toString();
            
            if (destination.toFile().exists()) {
                return Optional.of(new LinkedFile("[cover]", link, inferFileTypeFromExtension(externalApplicationsPreferences, extension), url));

            } else try {
                LOGGER.info("Downloading cover image file from {}", url);

                URLDownload download = new URLDownload(url);
                download.canBeReached();
                
                final String type = inferFileType(externalApplicationsPreferences, download.getMimeType(), extension);
                download.toFile(destination);
                return Optional.of(new LinkedFile("[cover]", link, type, url));

            } catch (UnirestException | FetcherException | MalformedURLException e) {
                LOGGER.error("Error while downloading cover image file", e);
            }
        } else {
            LOGGER.warn("File directory not available while downloading cover image {}. Storing as URL in file field.", url);
            return Optional.of(new LinkedFile("[cover]", url, ""));
        }

        return Optional.empty();
    }

    private static String inferFileType(ExternalApplicationsPreferences externalApplicationsPreferences, Optional<String> mime, Optional<String> extension) {
        if (mime.isPresent()) {
        	Optional<ExternalFileType> suggested = ExternalFileTypes.getExternalFileTypeByMimeType(mime.get(), externalApplicationsPreferences);
            if (suggested.isPresent()) {
                return suggested.get().getName();
            }
        }
        return inferFileTypeFromExtension(externalApplicationsPreferences, extension);
    }

    private static String inferFileTypeFromExtension(ExternalApplicationsPreferences externalApplicationsPreferences, Optional<String> extension) {
        return extension.map(x -> ExternalFileTypes.getExternalFileTypeByExt(x, externalApplicationsPreferences).map(t -> t.getName()).orElse("")).orElse("");
    }
}
package org.jabref.logic.importer;

import java.net.URL;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.net.URLDownload;

/**
 * Searches web resources for bibliographic information.
 */
public interface WebFetcher {

    /**
     * Returns the localized name of this fetcher.
     * The title can be used to display the fetcher in the menu and in the side pane.
     *
     * @return the localized name
     */
    String getName();

    /**
     * Returns the help page for this fetcher.
     *
     * @return the {@link HelpFile} enum constant for the help page
     */
    default Optional<HelpFile> getHelpPage() {
        return Optional.empty(); // no help page by default
    }

    /**
     * Constructs an {@link URLDownload} object for downloading content based on the given URL. Overwrite, if you need to send additional headers for the download.
     */
    default URLDownload getUrlDownload(URL url) {
        return new URLDownload(url);
    }
}

package org.jabref.logic.importer;

import org.jabref.logic.help.HelpFile;

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
    default HelpFile getHelpPage() {
        return null; // no help page by default
    }
}

/**
 *
 */
package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.MrDLibImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responible to get the recommendations from MDL
 */
public class MrDLibFetcher implements EntryBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MrDLibFetcher.class);

    private static final String NAME = "MDL_FETCHER";
    private final String LANGUAGE;
    private final String VERSION;


    public MrDLibFetcher(String language, String version) {
        LANGUAGE = language;
        VERSION = version;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getLatexFreeField(FieldName.TITLE);
        if (title.isPresent()) {
            String response = makeServerRequest(title.get());
            MrDLibImporter importer = new MrDLibImporter();
            ParserResult parserResult = new ParserResult();
            try {
                if (importer.isRecognizedFormat(response)) {
                    parserResult = importer.importDatabase(response);
                } else {
                    // For displaying An ErrorMessage
                    BibEntry errorBibEntry = new BibEntry();
                    errorBibEntry.setField("html_representation",
                            Localization.lang("Error while fetching from %0", "Mr.DLib"));
                    BibDatabase errorBibDataBase = new BibDatabase();
                    errorBibDataBase.insertEntry(errorBibEntry);
                    parserResult = new ParserResult(errorBibDataBase);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new FetcherException("XML Parser IOException.");
            }
            return parserResult.getDatabase().getEntries();
        } else {
            // without a title there is no reason to ask MrDLib
            return new ArrayList<>(0);
        }
    }

    /**
     * Contact the server with the title of the selected item
     *
     * @param query: The query holds the title of the selected entry. Used to make a query to the MDL Server
     * @return Returns the server response. This is an XML document as a String.
     */
    private String makeServerRequest(String queryByTitle) throws FetcherException {
        try {
            URLDownload urlDownload = new URLDownload(constructQuery(queryByTitle));
            urlDownload.bypassSSLVerification();
            String response = urlDownload.asString();

            //Conversion of < and >
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
            return response;
        } catch (IOException e) {
            throw new FetcherException("Problem downloading", e);
        }
    }

    /**
     * Constructs the query based on title of the bibentry. Adds statistical stuff to the url.
     *
     * @param query: the title of the bib entry.
     * @return the string used to make the query at mdl server
     */
    private String constructQuery(String queryWithTitle) {
        // The encoding does not work for / so we convert them by our own
        queryWithTitle = queryWithTitle.replaceAll("/", "convbckslsh");
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https");
        builder.setHost("api.mr-dlib.org");
        builder.setPath("/v1/documents/" + queryWithTitle + "/related_documents");
        builder.addParameter("partner_id", "jabref");
        builder.addParameter("app_id", "jabref_desktop");
        builder.addParameter("app_version", VERSION);
        builder.addParameter("app_lang", LANGUAGE);
        URI uri = null;
        try {
            uri = builder.build();
            return uri.toString();
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return "";
    }
}

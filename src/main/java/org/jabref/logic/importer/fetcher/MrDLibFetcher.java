package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.MrDLibImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.Version;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.MrDlibPreferences;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for getting the recommendations from Mr. DLib
 */
public class MrDLibFetcher implements EntryBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MrDLibFetcher.class);
    private static final String NAME = "MDL_FETCHER";
    private static final String MDL_JABREF_PARTNER_ID = "1";
    private static final String MDL_URL = "api.mr-dlib.org";
    private static final String DEFAULT_MRDLIB_ERROR_MESSAGE = Localization.lang("Error while fetching recommendations from Mr.DLib.");
    private final String LANGUAGE;
    private final Version VERSION;
    private String heading;
    private String description;
    private String recommendationSetId;
    private final MrDlibPreferences preferences;

    public MrDLibFetcher(String language, Version version, MrDlibPreferences preferences) {
        LANGUAGE = language;
        VERSION = version;
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getLatexFreeField(StandardField.TITLE);
        if (title.isPresent()) {
            String response = makeServerRequest(title.get());
            MrDLibImporter importer = new MrDLibImporter();
            ParserResult parserResult;
            try {
                if (importer.isRecognizedFormat(response)) {
                    parserResult = importer.importDatabase(response);
                    heading = importer.getRecommendationsHeading();
                    description = importer.getRecommendationsDescription();
                    recommendationSetId = importer.getRecommendationSetId();
                } else {
                    // For displaying An ErrorMessage
                    description = DEFAULT_MRDLIB_ERROR_MESSAGE;
                    BibDatabase errorBibDataBase = new BibDatabase();
                    parserResult = new ParserResult(errorBibDataBase);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new FetcherException("JSON Parser IOException.");
            }
            return parserResult.getDatabase().getEntries();
        } else {
            // without a title there is no reason to ask MrDLib
            return new ArrayList<>(0);
        }
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Contact the server with the title of the selected item
     *
     * @param queryByTitle the query holds the title of the selected entry. Used to make a query to the MDL Server
     * @return Returns the server response. This is an XML document as a String.
     */
    private String makeServerRequest(String queryByTitle) throws FetcherException {
        try {
            URLDownload urlDownload = new URLDownload(constructQuery(queryByTitle));
            String response = urlDownload.asString();

            // Conversion of < and >
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
            return response;
        } catch (IOException e) {
            throw new FetcherException("Problem downloading", e);
        }
    }

    /**
     * Constructs the query based on title of the BibEntry. Adds statistical stuff to the url.
     *
     * @param queryWithTitle the title of the bib entry.
     * @return the string used to make the query at mdl server
     */
    private String constructQuery(String queryWithTitle) {
        // The encoding does not work for / so we convert them by our own
        queryWithTitle = queryWithTitle.replaceAll("/", " ");
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http");
        builder.setHost(MDL_URL);
        builder.setPath("/v2/documents/" + queryWithTitle + "/related_documents");
        builder.addParameter("partner_id", MDL_JABREF_PARTNER_ID);
        builder.addParameter("app_id", "jabref_desktop");
        builder.addParameter("app_version", VERSION.getFullVersion());

        if (preferences.shouldSendLanguage()) {
            builder.addParameter("app_lang", LANGUAGE);
        }
        if (preferences.shouldSendOs()) {
            builder.addParameter("os", System.getProperty("os.name"));
        }
        if (preferences.shouldSendTimezone()) {
            builder.addParameter("timezone", Calendar.getInstance().getTimeZone().getID());
        }

        try {
            URI uri = builder.build();
            LOGGER.trace("Request: " + uri.toString());
            return uri.toString();
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return "";
    }
}

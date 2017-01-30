/**
 *
 */
package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.MrDLibImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

/**
 *  This class is responible to get the recommendations from MDL
 *
 */
public class MrDLibFetcher implements EntryBasedFetcher {

    private static final String NAME = "MDL_FETCHER";
    private static final Log LOGGER = LogFactory.getLog(MrDLibFetcher.class);
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
        String response = makeServerRequest(entry.getLatexFreeField(FieldName.TITLE).get());
        MrDLibImporter importer = new MrDLibImporter();
        ParserResult parserResult = new ParserResult();
        try {
            if (importer.isRecognizedFormat(new BufferedReader(new StringReader(response)))) {
                parserResult = importer.importDatabase(new BufferedReader(new StringReader(response)));
            } else {
                // For displaying An ErrorMessage
                BibEntry errorBibEntry = new BibEntry();
                errorBibEntry.setField("html_representation",
                        Localization.lang("Error_while_fetching_from_%0", "Mr.DLib"));
                BibDatabase errorBibDataBase = new BibDatabase();
                errorBibDataBase.insertEntry(errorBibEntry);
                parserResult = new ParserResult(errorBibDataBase);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new FetcherException("XML Parser IOException.");
        }
        return parserResult.getDatabase().getEntries();
    }

    /**
     * Contact the server with the title of the selected item
     * @param query: The query holds the title of the selected entry. Used to make a query to the MDL Server
     * @return Returns the server response. This is an XML document as a String.
     * @throws FetcherException
     */
    private String makeServerRequest(String queryByTitle) throws FetcherException {
        try {
            String response = new URLDownload(constructQuery(queryByTitle)).downloadToString(StandardCharsets.UTF_8);

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
     * @param query: the title of the bib entry.
     * @return the string used to make the query at mdl server
     */
    private String constructQuery(String queryByTitle) {
        queryByTitle = queryByTitle.replaceAll(":|'|\"|#|<|>|&|´|`|~|-", "");
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https");
        builder.setHost("api-dev.mr-dlib.org");
        builder.setPath("/v1/documents/" + queryByTitle + "/related_documents");
        builder.addParameter("partner_id", "jabref");
        builder.addParameter("app_id", "jabref_desktop");
        builder.addParameter("app_version", VERSION);
        builder.addParameter("app_lang", LANGUAGE);
        URI uri = null;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }
        System.out.println("Query: " + uri.toString());
        return uri.toString();
    }

}

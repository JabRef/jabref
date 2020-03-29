package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.APIKeyPreferences;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.WorldcatImporter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * EntryBasedFetcher that searches the Worldcat database
 * 
 * @see https://www.oclc.org/developer/develop/web-services/worldcat-search-api/bibliographic-resource.en.html
 */
public class WorldcatFetcher implements EntryBasedFetcher {

	public static String API_KEY;
	private static final String NAME = "Worldcat Fetcher";

	private static final String WORLDCAT_OPEN_SEARCH_URL = "http://www.worldcat.org/webservices/catalog/search/opensearch?wskey=" + API_KEY;

	public WorldcatFetcher (APIKeyPreferences apiKeyPreferences) {
		API_KEY = apiKeyPreferences.getWorldcatKey ();
	}

	@Override
	public String getName () {
		return NAME;
	}

	/**
	 * Create a open search query with specified title
	 * @param title the title to include in the query
	 * @return the earch query for the api
	 */
	private String getOpenSearchURL (String title) throws MalformedURLException { 
		String query = "&q=srw.ti+all+\"" + title + "\"";
		URL url = new URL (WORLDCAT_OPEN_SEARCH_URL + query);
		return url.toString ();
	}

	/**
	 * Make request to open search API of Worldcat, with specified title
	 * @param title the title of the search
	 * @return the body of the HTTP response
	 */
	private String makeOpenSearchRequest (String title) throws FetcherException { 
		try {
			URLDownload urlDownload = new URLDownload (getOpenSearchURL (title));
			URLDownload.bypassSSLVerification ();
			String resp = urlDownload.asString ();

			return resp;
		} catch (MalformedURLException e) {
			throw new FetcherException ("Bad url", e);
		} catch (IOException e) {
			throw new FetcherException ("Error with Open Search Request (Worldcat)", e);
		} 
	}

	@Override
	public List<BibEntry> performSearch (BibEntry entry) throws FetcherException {
		Optional<String> entryTitle = entry.getLatexFreeField (StandardField.TITLE);
		if (entryTitle.isPresent ()) {
			String xmlResponse = makeOpenSearchRequest (entryTitle.get ());
			WorldcatImporter importer = new WorldcatImporter (); 
			ParserResult parserResult;
			try { 
				if (importer.isRecognizedFormat (xmlResponse)) {
					parserResult = importer.importDatabase (xmlResponse);
				} else { 
					// For displaying An ErrorMessage
					BibDatabase errorBibDataBase = new BibDatabase ();
					parserResult = new ParserResult (errorBibDataBase);
				}
				return parserResult.getDatabase ().getEntries ();
			}
			catch (IOException e) {
				throw new FetcherException ("Could not perform search (Worldcat) ", e);
			}
		} else {
			return new ArrayList<>();
		}
	}

}
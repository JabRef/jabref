package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.WorldcatImporter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class WorldcatFetcher implements EntryBasedFetcher {

	private final static String NAME = "Worldcat Fetcher";
	public final static String API_KEY = "API-KEY";

	private final static String WORLDCAT_OPEN_SEARCH_URL = "http://www.worldcat.org/webservices/catalog/search/opensearch?wskey=" + API_KEY;

	@Override
	public String getName() {
		return NAME;
	}

	private String getOpenSearchURL(String title){
		String query = "&q=srw.ti+all+\"" + title.replaceAll(" ", "%20") + "\"";
		return WORLDCAT_OPEN_SEARCH_URL + query.replace("\"", "%22");
	}

	private String makeOpenSearchRequest(String title) throws FetcherException{
		try {
			URLDownload urlDownload = new URLDownload(getOpenSearchURL(title));
			URLDownload.bypassSSLVerification();
			String resp = urlDownload.asString();

			return resp;
		} catch (IOException e) {
			e.printStackTrace();
			throw new FetcherException("IO Exception", e);
		}
	}

	//Example http://www.worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+"The very best of Glenn"&wskey={built-in-api-key}
	@Override
	public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
		Optional<String> entryTitle = entry.getLatexFreeField(StandardField.TITLE);
		if(entryTitle.isPresent()){
			String xmlResponse = makeOpenSearchRequest(entryTitle.get());
			WorldcatImporter importer = new WorldcatImporter(); 
			ParserResult parserResult;
			try{
				if(importer.isRecognizedFormat(xmlResponse)){
					parserResult = importer.importDatabase(xmlResponse);
				} else{
					// For displaying An ErrorMessage
					BibDatabase errorBibDataBase = new BibDatabase();
					parserResult = new ParserResult(errorBibDataBase);
				}
				return parserResult.getDatabase().getEntries();
			}
			catch(IOException e){
				throw new FetcherException("IO Exception " + e.getMessage(), e);
			}
		} else {
			return new ArrayList<>();
		}
	}

}
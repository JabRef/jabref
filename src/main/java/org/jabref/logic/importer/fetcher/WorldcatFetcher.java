package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class WorldcatFetcher implements EntryBasedFetcher {

	private final static String NAME = "Worldcat Fetcher";
	private final static String API_KEY = "API-KEY";
	private final static String WORLDCAT_URL = "http://www.worldcat.org/webservices/catalog/search/worldcat/opensearch?wskey=" + API_KEY + "&";

	@Override
	public String getName() {
		return NAME;
	}

	private String getURL(String title){
		String query = "q=srw.ti+all+\"" + title.replaceAll(" ", "%20") + "\"";
		return WORLDCAT_URL + query.replace("\"", "%22");
	}

	private String makeRequest(String title) throws FetcherException{
		try {
			URLDownload urlDownload = new URLDownload(getURL(title));
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
			String xmlResponse = makeRequest(entryTitle.get());
			/*
				- Create importer (implements importer)
				- Check if recognized format
					- yes
						- set parserResult to importer.importDatabase
						- return pR.getDbase.getEntries()
					- no
						- return empty bibdatabase's entries
			*/
			return null;
		} else {
			return new ArrayList<>();
		}
	}

}
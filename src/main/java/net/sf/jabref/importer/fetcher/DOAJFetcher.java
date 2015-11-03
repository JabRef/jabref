/*  Copyright (C) 2015 Oscar Gustafsson.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fetcher;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.BibJSONConverter;
import net.sf.jabref.model.entry.BibtexEntry;


public class DOAJFetcher implements EntryFetcher {

    final String searchURL = "https://doaj.org/api/v1/search/articles/";
    private static final Log LOGGER = LogFactory.getLog(DOAJFetcher.class);


    public DOAJFetcher() {
        super();
    }

    @Override
    public void stopFetching() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = Unirest.get(searchURL + query + "?pageSize=100").header("accept", "application/json").asJson();
            JSONObject jo = jsonResponse.getBody().getObject();
            int pagesize = jo.getInt("pageSize");
            int hits = jo.getInt("total");
            for (int i = 0; i < (pagesize > hits ? hits : pagesize); i++) {
                JSONObject bibJsonEntry = jo.getJSONArray("results").getJSONObject(i).getJSONObject("bibjson");
                BibtexEntry entry = BibJSONConverter.BibJSONtoBibtex(bibJsonEntry);
                inspector.addEntry(entry);
            }
            return true;
        } catch (UnirestException e) {
            LOGGER.warn("Problem searching DOAJ", e);
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "DOAJ (Directory of Open Access Journals";
    }

    @Override
    public String getKeyName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHelpPage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }

}

/*  Copyright (C) 2012 JabRef contributors.
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref.imports.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;

import javax.swing.JPanel;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.CaseKeeper;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.ImportInspector;

/**
 * This class uses Manas Tungare's ISBN to BibTeX Converter to convert an ISBN to a BibTeX entry <br />
 * The online version of the converter is available at http://manas.tungare.name/software/isbn-to-bibtex/
 */
public class ISBNtoBibTeXFetcher implements EntryFetcher {
	
	private static final String URL_PATTERN = "http://manas.tungare.name/software/isbn-to-bibtex/isbn-service?isbn=%s"; 
        final CaseKeeper caseKeeper = new CaseKeeper();
   
	@Override
    public void stopFetching() {
		// nothing needed as the fetching is a single HTTP GET
    }

	@Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
		String q;
		try {
	        q = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	// this should never happen
        	status.setStatus(Globals.lang("Error"));
	        e.printStackTrace();
	        return false;
        }
		
        String urlString = String.format(URL_PATTERN, q);

        // Send the request
        URL url;
        URLConnection conn;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        InputStream source;
        try {
	        source = url.openStream();
        } catch (IOException e) {
	        e.printStackTrace();
	        return false;
        }
        
        String bibtexString = new Scanner(source).useDelimiter("\\A").next();
        if (bibtexString.startsWith("@comment")) {
        	// an error occured
        	// the error is nested in @comment{...}
        	String errorMsg = bibtexString.substring("@comment{".length());
        	errorMsg = errorMsg.substring(0, errorMsg.length()-1);
        	status.showMessage(errorMsg); // showMessage does not work -> NPE
        	return false;
        }
        
        BibtexEntry entry = BibtexParser.singleFromString(bibtexString);
        if(entry != null)  {
            // Optionally add curly brackets around key words to keep the case
            String title = (String)entry.getField("title");
            if (title != null) {
                if (Globals.prefs.getBoolean("useCaseKeeperOnSearch")) {
                    title = caseKeeper.format(title);
                }
                entry.setField("title", title);
            }
            
            inspector.addEntry(entry);
	    return true;
        } else {
            return false;
        }
    }

	@Override
    public String getTitle() {
	    return "ISBN to BibTeX";
    }

	@Override
    public String getKeyName() {
	    return "ISBNtoBibTeX";
    }

	@Override
    public URL getIcon() {
		// no special icon for this fetcher available.
		// Therefore, we return some kind of default icon
	    return GUIGlobals.getIconUrl("www");
    }

	@Override
    public String getHelpPage() {
	    return "ISBNtoBibTeXHelp.html";
    }

	@Override
    public JPanel getOptionsPanel() {
		// no additional options available
	    return null;
    }

}

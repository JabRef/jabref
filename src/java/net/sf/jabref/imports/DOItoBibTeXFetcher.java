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
package net.sf.jabref.imports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;


public class DOItoBibTeXFetcher implements EntryFetcher {
	
	private static final String URL_PATTERN = "http://dx.doi.org/%s"; 

    public void stopFetching() {
		// nothing needed as the fetching is a single HTTP GET
    }

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
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        URLConnection conn;
        try {
	        conn = url.openConnection();
        } catch (IOException e) {
	        e.printStackTrace();
	        return false;
        }
        
        conn.setRequestProperty("Accept", "text/bibliography; style=bibtex");
        
        
       String bibtexString;
        try {
	        bibtexString = getResults(conn);
        } catch (FileNotFoundException e) {
               status.showMessage(Globals.lang("Unknown DOI: '%0'.",
                        query),
                        Globals.lang("Get BibTeX entry from DOI"), JOptionPane.INFORMATION_MESSAGE);
	        return false;
        }
        catch (IOException e) {
	        e.printStackTrace();
	        return false;
        }
        
       
        
        BibtexEntry entry = BibtexParser.singleFromString(bibtexString);
        // Do not use the provided key
        // entry.setField(BibtexFields.KEY_FIELD,null);
        inspector.addEntry(entry);
	    return true;
    }

    public String getTitle() {
	    return "DOI to BibTeX";
    }

    public String getKeyName() {
	    return "DOItoBibTeX";
    }

    public URL getIcon() {
		// no special icon for this fetcher available.
		// Therefore, we return some kind of default icon
	    return GUIGlobals.getIconUrl("www");
    }

    public String getHelpPage() {
	    return "DOItoBibTeXHelp.html";
    }

    public JPanel getOptionsPanel() {
		// no additional options available
	    return null;
    }
        
     /**
     * Download the URL and return contents as a String.
     * @param source
     * @return
     * @throws IOException
     */
    public String getResults(URLConnection source) throws IOException {
        
        InputStream in = source.getInputStream();
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = in.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        return sb.toString();
    }


}

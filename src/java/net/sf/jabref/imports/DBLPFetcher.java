/*  Copyright (C) 2011 Sascha Hunold.
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
package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.OutputPrinter;

public class DBLPFetcher implements EntryFetcher {


    private final String URL_START = "http://www.dblp.org/search/api/";
    private final String URL_PART1 = "?q=";
    private final String URL_END   = "&h=1000&c=4&f=0&format=json";

	private volatile boolean shouldContinue = false;
	private String query;
	private final DBLPHelper helper = new DBLPHelper();


	@Override
	public void stopFetching() {
	    shouldContinue  = false;
	}

	@Override
	public boolean processQuery(String query, ImportInspector inspector,
			OutputPrinter status) {

		boolean res = false;
		this.query = query;

		shouldContinue = true;

		try {

			String address = makeSearchURL();
			//System.out.println(address);
			URL url = new URL(address);
	        String page = readFromURL(url);

	        //System.out.println(page);
	        String[] lines = page.split("\n");
	        List<String> bibtexUrlList = new ArrayList<String>();
	        for(String line : lines) {
	        	if( line.startsWith("\"url\"") ) {
	        		String addr = line.replace("\"url\":\"", "");
	        		addr = addr.substring(0, addr.length()-2);
	        		//System.out.println("key address: " + addr);
	        		bibtexUrlList.add(addr);
	        	}
	        }


	        int count = 1;
	        for(String urlStr : bibtexUrlList) {
	        	if( ! shouldContinue ) {
	        		break;
	        	}

	        	final URL bibUrl = new URL(urlStr);
		        String bibtexPage = readFromURL(bibUrl);
		        //System.out.println(bibtexPage);

		        List<BibtexEntry> bibtexList = helper.getBibTexFromPage(bibtexPage);

		        for(BibtexEntry bibtexEntry : bibtexList ) {
		        	inspector.addEntry(bibtexEntry);
		        	if( ! shouldContinue ) {
		        		break;
		        	}
		        }
	        	inspector.setProgress(count, bibtexUrlList.size());
	        	count++;
	        }

	        // everything went smooth
	        res = true;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			status.showMessage(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			status.showMessage(e.getMessage());
		} catch(DBLPParseException e) {
			e.printStackTrace();
			status.showMessage(e.getMessage());
		}

		return res;
	}


    private String readFromURL(final URL source) throws IOException {
        final InputStream in = source.openStream();
        final InputStreamReader ir = new InputStreamReader(in);
        final StringBuffer sbuf = new StringBuffer();

        char[] cbuf = new char[256];
        int read;
        while( (read = ir.read(cbuf)) != -1 ) {
        	sbuf.append(cbuf, 0, read);
        }
        return sbuf.toString();
    }

	private String makeSearchURL() {
        StringBuffer sb = new StringBuffer(URL_START).append(URL_PART1);
        String cleanedQuery = helper.cleanDBLPQuery(query);
        sb.append(cleanedQuery);
        sb.append(URL_END);
        return sb.toString();
	}

	@Override
	public String getTitle() {
		return "DBLP";
	}

	@Override
	public String getKeyName() {
		return "DBLP";
	}

	@Override
	public URL getIcon() {
	    return GUIGlobals.getIconUrl("www");
	}

	@Override
	public String getHelpPage() {
		return null;
	}

	@Override
	public JPanel getOptionsPanel() {
		return null;
	}

}

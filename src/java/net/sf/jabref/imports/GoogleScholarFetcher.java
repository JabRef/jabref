/*  Copyright (C) 2003-2011 JabRef contributors.
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

import net.sf.jabref.*;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.util.NameListNormalizer;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GoogleScholarFetcher implements EntryFetcher {

    private boolean hasRunConfig = false;
    private boolean clearKeys = true; // Should we clear the keys so new ones can be generated?
    protected static int MAX_ENTRIES_TO_LOAD = 20;
    final static String QUERY_MARKER = "___QUERY___";
    final static String URL_START = "http://scholar.google.com";
    final static String URL_SETTING = "http://scholar.google.com/scholar_settings";
    final static String URL_SETPREFS = "http://scholar.google.com/scholar_setprefs";
    final static String SEARCH_URL = URL_START+"/scholar?q="+QUERY_MARKER
            +"&amp;hl=en&amp;btnG=Search";

    final static Pattern BIBTEX_LINK_PATTERN = Pattern.compile("<a href=\"([^\"]*)\">[A-Za-z ]*BibTeX");


    //final static Pattern NEXT_PAGE_PATTERN = Pattern.compile(
    //        "<a href=\"([^\"]*)\"><span class=\"SPRITE_nav_next\"> </span><br><span style=\".*\">Next</span></a>");

    protected boolean stopFetching = false;

    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        stopFetching = false;
        try {
            if (!hasRunConfig) {
                runConfig();
                hasRunConfig = true;
            }
            List<String> citations = getCitations(query);
            int entriesAdded = 0;
            inspector.setProgress(2, citations.size()+2);
            int i=0;
            for (String citation : citations) {
                if (stopFetching)
                    break;

                BibtexEntry entry = downloadEntry(citation);
                inspector.setProgress((++i)+2, citations.size()+2);
                if (entry != null) {
                    inspector.addEntry(entry);
                    entriesAdded++;
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(Globals.lang("Error fetching from Google Scholar"));
            return false;
        }
    }

    public String getTitle() {
        return "Google Scholar";
    }

    public String getKeyName() {
        return "Google Scholar";
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getHelpPage() {
        return "GoogleScholarHelp.html";
    }

    public JPanel getOptionsPanel() {
        return null;
    }

    public void stopFetching() {
        stopFetching = true;
    }


    private void save(String filename, String content) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(content);
        out.close();
    }

    protected void runConfig() throws IOException {
        String urlQuery;
        try {
            URL url;
            URLDownload ud;
            url = new URL("http://scholar.google.com");
            ud = new URLDownload(url);
            ud.download();
            url = new URL(URL_SETTING);
            ud = new URLDownload(url);
            ud.download();
            //save("setting.html", ud.getStringContent());
            String settingsPage = ud.getStringContent();
            // Get the form items and their values from the page:
            HashMap<String,String> formItems = getFormElements(settingsPage);
            // Override the important ones:
            formItems.put("scis", "yes");
            formItems.put("scisf", "4");
            formItems.put("num", String.valueOf(MAX_ENTRIES_TO_LOAD));
            StringBuilder ub = new StringBuilder(URL_SETPREFS+"?");
            for (Iterator<String> i = formItems.keySet().iterator(); i.hasNext();) {
                String name = i.next();
                ub.append(name).append("=").append(formItems.get(name));
                if (i.hasNext())
                    ub.append("&");
            }
            ub.append("&submit=");
            // Download the URL to set preferences:
            URL url_setprefs = new URL(ub.toString());
            ud = new URLDownload(url_setprefs);
            ud.download();

        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param query
     *            The search term to query JStor for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    protected List<String> getCitations(String query) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            urlQuery = SEARCH_URL.replace(QUERY_MARKER, URLEncoder.encode(query, "UTF-8"));
            int count = 1;
            String nextPage = null;
            while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                    && (count < 2)) {
                urlQuery = nextPage;
                count++;
                if (stopFetching)
                    break;
            }
            return ids;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        URL url = new URL(urlQuery);
        URLDownload ud = new URLDownload(url);
        ud.download();
        String cont = ud.getStringContent();
        Matcher m = BIBTEX_LINK_PATTERN.matcher(cont);
        while (m.find()) {
            String link = m.group(1).replaceAll("&amp;", "&");
            ids.add(link);
        }

        /*m = NEXT_PAGE_PATTERN.matcher(cont);
        if (m.find()) {
            System.out.println("NEXT: "+URL_START+m.group(1).replaceAll("&amp;", "&"));
            return URL_START+m.group(1).replaceAll("&amp;", "&");
        }
        else*/
        return null;
    }

    protected BibtexEntry downloadEntry(String link) throws IOException {
        try {
            URL url = new URL(URL_START+link);
            URLDownload ud = new URLDownload(url);
            ud.download();
            String s = ud.getStringContent();
            BibtexParser bp = new BibtexParser(new StringReader(s));
            ParserResult pr = bp.parse();
            if ((pr != null) && (pr.getDatabase() != null)) {
                Collection<BibtexEntry> entries = pr.getDatabase().getEntries();
                if (entries.size() == 1) {
                    BibtexEntry entry = entries.iterator().next();
                    if (clearKeys)
                        entry.setField(BibtexFields.KEY_FIELD, null);
                    return entry;
                }
                else if (entries.size() == 0) {
                    System.out.println("No entry found! ("+link+")");
                    return null;
                }
                else {
                    System.out.println(entries.size()+" entries found! ("+link+")");
                    return null;
                }
            }
            else {
                System.out.println("Parser failed! ("+link+")");
                return null;

            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    static Pattern inputPattern = Pattern.compile("<input type=([^ ]+) name=([^ ]+) value=([^> ]+)");
    public static HashMap<String,String> getFormElements(String page) {
        Matcher m = inputPattern.matcher(page);
        HashMap<String,String> items = new HashMap<String, String>();
        while (m.find()) {
            String name = m.group(2);
            if ((name.length() > 2) && (name.charAt(0) == '"')
                    && (name.charAt(name.length()-1) == '"'))
                name = name.substring(1, name.length()-1);
            String value = m.group(3);
            if ((value.length() > 2) && (value.charAt(0) == '"')
                    && (value.charAt(value.length()-1) == '"'))
                value = value.substring(1, value.length()-1);
            items.put(name, value);
        }
        return items;
    }
}

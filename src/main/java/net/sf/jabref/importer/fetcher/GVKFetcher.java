/**
 * License: GPLv2, but Jan Frederik Maas agreed to change license upon request
 */
package net.sf.jabref.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Fetch or search from GVK http://gso.gbv.de/sru/DB=2.1/
 */
public class GVKFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(GVKFetcher.class);

    private final Map<String, String> searchKeys = new HashMap<>();


    public GVKFetcher() {
        searchKeys.put("all", "pica.all%3D");
        searchKeys.put("tit", "pica.tit%3D");
        searchKeys.put("per", "pica.per%3D");
        searchKeys.put("thm", "pica.thm%3D");
        searchKeys.put("slw", "pica.slw%3D");
        searchKeys.put("txt", "pica.txt%3D");
        searchKeys.put("num", "pica.num%3D");
        searchKeys.put("kon", "pica.kon%3D");
        searchKeys.put("ppn", "pica.ppn%3D");
        searchKeys.put("bkl", "pica.bkl%3D");
        searchKeys.put("erj", "pica.erj%3D");
    }

    /**
     * Necessary for JabRef
     */
    @Override
    public void stopFetching() {
        // not supported
    }

    @Override
    public String getHelpPage() {
        return "GVKHelp";
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public String getTitle() {
        return "GVK (Gemeinsamer Verbundkatalog)";
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {

        query = query.trim();

        String[] qterms = query.split("\\s");

        // Null abfangen!
        if (qterms.length == 0) {
            return false;
        }

        // Jeden einzelnen Suchbegriff URL-Encodieren
        for (int x = 0; x < qterms.length; x++) {
            try {
                qterms[x] = URLEncoder.encode(qterms[x], StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Unsupported encoding", e);
            }
        }

        String gvkQuery;
        if (searchKeys.containsKey(qterms[0])) {
            gvkQuery = processComplexQuery(qterms);
        } else {
            gvkQuery = "pica.all%3D";
            gvkQuery = gvkQuery.concat(qterms[0]);

            for (int x = 1; x < qterms.length; x++) {
                gvkQuery = gvkQuery.concat("%20");
                gvkQuery = gvkQuery.concat(qterms[x]);
            }
        }

        List<BibEntry> bibs = fetchGVK(gvkQuery);

        for (BibEntry entry : bibs) {
            dialog.addEntry(entry);
        }

        if (bibs.isEmpty()) {
            frame.showMessage(Localization.lang("No references found"));
        }

        return true;
    }

    private String processComplexQuery(String[] s) {
        String result = "";
        boolean lastWasKey = false;

        for (int x = 0; x < s.length; x++) {
            if (searchKeys.containsKey(s[x])) {
                if (x == 0) {
                    result = searchKeys.get(s[x]);
                } else {
                    result = result.concat("%20and%20" + searchKeys.get(s[x]));
                }
                lastWasKey = true;
            } else {
                if (!lastWasKey) {
                    result = result.concat("%20");
                }
                String encoded = s[x];
                encoded = encoded.replace(",", "%2C").replace("?", "%3F");

                result = result.concat(encoded);
                lastWasKey = false;
            }
        }
        return result;
    }

    private List<BibEntry> fetchGVK(String query) {
        List<BibEntry> result;

        String urlPrefix = "http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=";
        String urlSuffix = "&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1";

        String searchstring = urlPrefix + query + urlSuffix;
        LOGGER.debug(searchstring);
        try {
            URI uri = new URI(searchstring);
            URL url = uri.toURL();
            try (InputStream is = url.openStream()) {
                result = (new GVKParser()).parseEntries(is);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("URI malformed error", e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.error("GVK: An I/O exception occurred", e);
            return Collections.emptyList();
        } catch (ParserConfigurationException e) {
            LOGGER.error("GVK: An internal parser error occurred", e);
            return Collections.emptyList();
        } catch (SAXException e) {
            LOGGER.error("An internal parser error occurred", e);
            return Collections.emptyList();
        }

        return result;
    }

}

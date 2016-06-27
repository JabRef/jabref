package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.GoogleScholarFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;

public class GoogleScholarFetcherGUI extends GoogleScholarFetcher implements PreviewEntryFetcherGUI {

    @Override
    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status) {
        entryLinks.clear();
        setStopFetching(false);
        try {
            if (!isHasRunConfig()) {
                runConfig();
                setHasRunConfig(true);
            }
            Map<String, JLabel> citations = getCitations(query);
            for (Map.Entry<String, JLabel> linkEntry : citations.entrySet()) {
                preview.addEntry(linkEntry.getKey(), linkEntry.getValue());
            }

            return true;
        } catch (IOException e) {
            getLogger().warn("Error fetching from Google Scholar", e);
            status.showMessage(Localization.lang("Error while fetching from %0", "Google Scholar"));
            return false;
        }
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_GOOGLE_SCHOLAR;
    }

    /**
     * @param query The search term to query Google Scholar for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private Map<String, JLabel> getCitations(String query) throws IOException {
        String urlQuery;
        LinkedHashMap<String, JLabel> res = new LinkedHashMap<>();

        urlQuery = getSearchUrl().replace(getQueryMarker(), URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
        int count = 1;
        String nextPage;
        while (((nextPage = getCitationsFromUrl(urlQuery, res)) != null) && (count < 2)) {
            urlQuery = nextPage;
            count++;
            if (isStopFetching()) {
                break;
            }
        }
        return res;
    }

    private String getCitationsFromUrl(String urlQuery, Map<String, JLabel> ids) throws IOException {
        URL url = new URL(urlQuery);
        String cont = new URLDownload(url).downloadToString();
        Matcher m = getBibtexLinkPattern().matcher(cont);
        int lastRegionStart = 0;

        while (m.find()) {
            String link = m.group(1).replace("&amp;", "&");
            String partialText;
            String part = cont.substring(lastRegionStart, m.start());
            Matcher titleS = getTitleStartPattern().matcher(part);
            Matcher titleE = getTitleEndPattern().matcher(part);
            boolean fS = titleS.find();
            boolean fE = titleE.find();
            if (fS && fE) {
                if (titleS.end() < titleE.start()) {
                    partialText = part.substring(titleS.end(), titleE.start());
                } else {
                    partialText = part;
                }
            } else {
                partialText = link;
            }

            partialText = partialText.replace("[PDF]", "");
            JLabel preview = new JLabel("<html>" + partialText + "</html>");
            ids.put(link, preview);

            // See if we can extract the link Google Scholar puts on the entry's title.
            // That will be set as "url" for the entry if downloaded:
            Matcher linkMatcher = getLinkPattern().matcher(partialText);
            if (linkMatcher.find()) {
                entryLinks.put(link, linkMatcher.group(1));
            }

            lastRegionStart = m.end();
        }

        /*m = NEXT_PAGE_PATTERN.matcher(cont);
        if (m.find()) {
            System.out.println("NEXT: "+URL_START+m.group(1).replace("&amp;", "&"));
            return URL_START+m.group(1).replace("&amp;", "&");
        }
        else*/
        return null;
    }

}

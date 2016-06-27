/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.MedlineImporter;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Fetch or search from Pubmed http://www.ncbi.nlm.nih.gov/sites/entrez/
 *
 */
public class MedlineFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(MedlineFetcher.class);

    private static final Pattern ID_PATTERN = Pattern.compile("<Id>(\\d+)</Id>");
    private static final Pattern COUNT_PATTERN = Pattern.compile("<Count>(\\d+)<\\/Count>");
    private static final Pattern RET_MAX_PATTERN = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
    private static final Pattern RET_START_PATTERN = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");
    private static final Pattern PART1_PATTERN = Pattern.compile(", ");
    private static final Pattern PART2_PATTERN = Pattern.compile(",");

    private boolean shouldContinue;

    /**
     * How many entries to query in one request
     */
    private static final int PACING = 20;


    protected static int getPacing() {
        return PACING;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected static Pattern getIdPattern() {
        return ID_PATTERN;
    }

    protected static Pattern getCountPattern() {
        return COUNT_PATTERN;
    }

    protected static Pattern getRetMaxPattern() {
        return RET_MAX_PATTERN;
    }

    protected static Pattern getRetStartPattern() {
        return RET_START_PATTERN;
    }

    protected static Pattern getPart1Pattern() {
        return PART1_PATTERN;
    }

    protected static Pattern getPart2Pattern() {
        return PART2_PATTERN;
    }

    protected boolean isShouldContinue() {
        return shouldContinue;
    }

    protected void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public String getTitle() {
        return "Medline";
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     *
     * @param id One or several ids, separated by ","
     *
     * @return Will return an empty list on error.
     */
    protected static List<BibEntry> fetchMedline(String id, OutputPrinter status) {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id="
                + id;
        try {
            URL url = new URL(baseUrl);
            URLConnection data = url.openConnection();
            ParserResult result = new MedlineImporter()
                    .importDatabase(new BufferedReader(new InputStreamReader(data.getInputStream())));
            if (result.hasWarnings()) {
                status.showMessage(result.getErrorMessage());
            }
            return result.getDatabase().getEntries();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }


    protected static class SearchResult {

        public int count;

        public int retmax;

        public int retstart;

        public String ids = "";

        public void addID(String id) {
            if (ids.isEmpty()) {
                ids = id;
            } else {
                ids += "," + id;
            }
        }
    }

}

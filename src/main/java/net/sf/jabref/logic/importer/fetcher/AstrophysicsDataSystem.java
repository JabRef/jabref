/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.SearchBasedParserFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the SAO/NASA Astrophysics Data System (http://www.adsabs.harvard.edu/)
 *
 * Search query-based: http://adsabs.harvard.edu/basic_search.html
 */
public class AstrophysicsDataSystem implements SearchBasedParserFetcher {

    private static String API_URL = "http://adsabs.harvard.edu/cgi-bin/nph-basic_connect";
    private final ImportFormatPreferences preferences;

    public AstrophysicsDataSystem(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "SAO/NASA Astrophysics Data System";
    }

    @Override
    public HelpFile getHelpPage() {
        return null;
    }

    @Override
    public URL getQueryURL(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("qsearch", query);
        uriBuilder.addParameter("data_type", "BIBTEXPLUS");
        uriBuilder.addParameter("start_nr", String.valueOf(1));
        uriBuilder.addParameter("nr_to_return", String.valueOf(200));
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(FieldName.ABSTRACT, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.TITLE, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);
        new FieldFormatterCleanup("adsnote", new ClearFormatter()).cleanup(entry);
        new FieldFormatterCleanup("adsurl", new ClearFormatter()).cleanup(entry);
    }
}

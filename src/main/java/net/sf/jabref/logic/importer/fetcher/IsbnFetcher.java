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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN.
 */
public class IsbnFetcher implements IdBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(IsbnFetcher.class);
    private static final String URL_PATTERN = "http://www.ebook.de/de/tools/isbn2bibtex?";

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        ISBN isbn = new ISBN(identifier);
        Optional<BibEntry> result = Optional.empty();

        if (isbn.isValidChecksum() && isbn.isValidFormat()) {

            try {
                //Build the URL. In this case: http://www.ebook.de/de/tools/isbn2bibtex?isbn=identifier
                URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
                uriBuilder.addParameter("isbn", identifier);
                URL url = uriBuilder.build().toURL();

                //Downloads the source code of the site and then creates a .bib file out of the String
                URLDownload urlDownload = new URLDownload(url);
                String bibtexString = urlDownload.downloadToString(StandardCharsets.UTF_8);
                BibEntry entry = BibtexParser.singleFromString(bibtexString);

                entry.clearField(FieldName.URL);

                //Removes every non-digit character in the PAGETOTAL field.
                if (entry.hasField(FieldName.PAGETOTAL)) {
                    String pagetotal = entry.getFieldOptional(FieldName.PAGETOTAL).get();
                    entry.setField(FieldName.PAGETOTAL, pagetotal.replaceAll("[\\D]", ""));
                }

                result = Optional.ofNullable(entry);

            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Bad URL when fetching ISBN info", e);
                throw new FetcherException("Bad URL when fetching ISBN info", e);
            }
        }
        return result;
    }
}

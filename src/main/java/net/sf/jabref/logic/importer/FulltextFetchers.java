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

package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.gui.importer.fetcher.CrossRef;
import net.sf.jabref.logic.importer.fetcher.ACS;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.DoiResolution;
import net.sf.jabref.logic.importer.fetcher.GoogleScholar;
import net.sf.jabref.logic.importer.fetcher.IEEE;
import net.sf.jabref.logic.importer.fetcher.ScienceDirect;
import net.sf.jabref.logic.importer.fetcher.SpringerLink;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FulltextFetchers {
    private static final Log LOGGER = LogFactory.getLog(FulltextFetchers.class);

    private final List<FulltextFetcher> finders = new ArrayList<>();

    public FulltextFetchers() {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new DoiResolution());
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        finders.add(new ArXiv());
        finders.add(new IEEE());
        // Meta search
        finders.add(new GoogleScholar());
    }

    public FulltextFetchers(List<FulltextFetcher> fetcher) {
        finders.addAll(fetcher);
    }

    public Optional<URL> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = (BibEntry) entry.clone();
        Optional<String> doi = clonedEntry.getFieldOptional(FieldName.DOI);

        if (!doi.isPresent() || !DOI.build(doi.get()).isPresent()) {
            CrossRef.findDOI(clonedEntry).ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
        }

        for (FulltextFetcher finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(clonedEntry);

                if (result.isPresent() && MimeTypeDetector.isPdfContentType(result.get().toString())) {
                    return result;
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
        }
        return Optional.empty();
    }
}

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

package net.sf.jabref.logic.fulltext;

import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import net.sf.jabref.gui.help.HelpFile;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.EntryFetcher;
import net.sf.jabref.importer.fetcher.SearchBasedFetcher;
import net.sf.jabref.logic.fetcher.FetcherException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper around {@link SearchBasedFetcher} which implements the old {@link EntryFetcher} interface.
 */
public class SearchBasedEntryFetcher implements EntryFetcher{

    private static final Log LOGGER = LogFactory.getLog(SearchBasedEntryFetcher.class);
    private final SearchBasedFetcher fetcher;

    public SearchBasedEntryFetcher(SearchBasedFetcher fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher);
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {

        status.setStatus(Localization.lang("Processing %0", query));
        try {
            List<BibEntry> matches = fetcher.performSearch(query);
            matches.forEach(inspector::addEntry);
            return !matches.isEmpty();
        } catch (FetcherException e) {
            status.setStatus(Localization.lang("Error while fetching from %0", fetcher.getName()));
            LOGGER.error("Error while fetching from" + fetcher.getName(), e);
        }

        return false;
    }

    @Override
    public String getTitle() {
        return fetcher.getName();
    }

    @Override
    public HelpFile getHelpPage() {
        return fetcher.getHelpPage();
    }

    @Override
    public JPanel getOptionsPanel() {
        // not supported
        return null;
    }

    @Override
    public void stopFetching() {
        // not supported
    }
}

package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;

/// Fetcher for <https://unpaywall.org/>
///
/// Currently only used for storing an "API key" to be able to cope with URLs appearing at web server answers such as `Paper or abstract available at https://api.unpaywall.org/v2/10.47397/tb/44-3/tb138kopp-jabref?email=<INSERT_YOUR_EMAIL>`
public class UnpaywallFetcher implements SearchBasedFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Unpaywall";

    @Override
    public List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException {
        return List.of();
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}

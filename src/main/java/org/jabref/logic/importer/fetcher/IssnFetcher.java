package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnFetcher implements IdBasedFetcher, IdFetcher<ISSN> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssnFetcher.class);

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {

        if(StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        return Optional.empty();

    }

    @Override
    public Optional<ISSN> findIdentifier(BibEntry entry) throws FetcherException {
        return Optional.empty();
    }

    @Override
    public String getIdentifierName() {
        return getName();
    }

    @Override
    public String getName() {
        return "ISSN";
    }

}

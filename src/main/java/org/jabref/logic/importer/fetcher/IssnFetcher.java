package org.jabref.logic.importer.fetcher;

import java.net.URI;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.model.entry.identifier.Identifier;

public class IssnFetcher implements Identifier, IdBasedFetcher, IdFetcher<ISSN> {
    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        return Optional.empty();
    }

    @Override
    public Optional<ISSN> findIdentifier(BibEntry entry) throws FetcherException {
        return Optional.empty();
    }

    @Override
    public String getIdentifierName() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getNormalized() {
        return null;
    }

    @Override
    public Field getDefaultField() {
        return null;
    }

    @Override
    public Optional<URI> getExternalURI() {
        return Optional.empty();
    }
}

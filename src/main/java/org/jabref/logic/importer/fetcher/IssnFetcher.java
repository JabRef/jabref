package org.jabref.logic.importer.fetcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.strings.StringUtil;

public class IssnFetcher implements Identifier, IdBasedFetcher, IdFetcher<ISSN> {

    private static ISSN issnIdentifier;
    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if(StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }
        Optional<BibEntry> bibEntry = Optional.empty();

        issnIdentifier = new ISSN(identifier);
        return bibEntry; //temporary, thinking about the solution

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

    @Override
    public String getNormalized() {
        return issnIdentifier.getNormalized();
    }

    @Override
    public Field getDefaultField() {
        return StandardField.ISSN;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://doaj.org/api/v1/search/articles/" + getNormalized()));
        } catch (
                URISyntaxException e) {
            return Optional.empty();
        }
    }
}

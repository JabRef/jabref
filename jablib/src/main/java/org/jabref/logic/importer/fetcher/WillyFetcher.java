package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

public class WillyFetcher implements FulltextFetcher {

    private static final String PDF_URL = "https://onlinelibrary.wiley.com/doi/pdf/";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(URLUtil.create(PDF_URL + doi.get().asString()));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.SOURCE;
    }
}

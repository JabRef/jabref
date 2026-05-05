package org.jabref.toolkit.converter;

import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;

public class CitationFetcherTypeConverter extends CaseInsensitiveEnumConverter<CitationFetcherType> {
    public CitationFetcherTypeConverter() {
        super(CitationFetcherType.class);
    }
}

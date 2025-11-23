package org.jabref.toolkit.converter;

import org.jabref.toolkit.arguments.Provider;

public class ProviderConverter extends CaseInsensitiveEnumConverter<Provider> {
    public ProviderConverter() {
        super(Provider.class);
    }
}

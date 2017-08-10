package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.FieldName;

public class ArXivIdentifier implements Identifier {

    private final String identifier;

    ArXivIdentifier(String identifier) {
        this.identifier = Objects.requireNonNull(identifier).trim();
    }

    public static Optional<ArXivIdentifier> parse(String value) {
        String identifier = value.replaceAll("(?i)arxiv:", "");
        return Optional.of(new ArXivIdentifier(identifier));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArXivIdentifier that = (ArXivIdentifier) o;

        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String getDefaultField() {
        return FieldName.EPRINT;
    }

    @Override
    public String getNormalized() {
        return identifier;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://arxiv.org/abs/" + identifier));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}

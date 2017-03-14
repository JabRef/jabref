package org.jabref.model.entry.identifier;

import java.util.Objects;
import java.util.Optional;

public class ArXivIdentifier {

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

    public String getNormalized() {
        return identifier;
    }
}

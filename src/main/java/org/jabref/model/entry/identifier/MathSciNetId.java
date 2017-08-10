package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.FieldName;
import org.jabref.model.strings.StringUtil;

/**
 * Article identifier for MathSciNet (also sometimes called "MRNumber")
 */
public class MathSciNetId implements Identifier {

    private String identifier;

    public MathSciNetId(String identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    public static Optional<MathSciNetId> parse(String mrNumberRaw) {
        // Take everything before whitespace or open bracket, so something like `619693 (82j:58046)` gets parsed correctly
        String identifier = StringUtil.tokenizeToList(mrNumberRaw, " (").get(0).trim();
        return Optional.of(new MathSciNetId(identifier));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MathSciNetId that = (MathSciNetId) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return identifier;
    }

    /**
     * Get URL in online database.
     */
    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("http://www.ams.org/mathscinet-getitem?mr=" + identifier));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    public String getDefaultField() {
        return FieldName.MR_NUMBER;
    }

    @Override
    public String getNormalized() {
        return identifier;
    }
}

package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SWHID implements Identifier {

    public static final String SWHID_PREFIX = "https://archive.softwareheritage.org/";

    private static final String SWHID_REGEX =
            "^(?:https?://archive\\.softwareheritage\\.org/)?(swh:1:(?:cnt|dir|rel|rev|snp):[0-9a-fA-F]{40}(?:;[a-zA-Z0-9_]+=[^;\\s]+)*)$";

    private static final Pattern SWHID_PATTERN = Pattern.compile(SWHID_REGEX);

    private final String swhid;

    public SWHID(String swhid) {
        this.swhid = swhid.trim();
    }

    public static Optional<SWHID> parse(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = SWHID_PATTERN.matcher(value.trim());
        if (matcher.matches()) {
            return Optional.of(new SWHID(matcher.group(1)));
        }
        return Optional.empty();
    }

    public static boolean isValid(String value) {
        return parse(value).isPresent();
    }

    @Override
    public String asString() {
        return swhid;
    }

    @Override
    public Field getDefaultField() {
        return BiblatexSoftwareField.SWHID;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI(SWHID_PREFIX + swhid));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SWHID other)) {
            return false;
        }
        return swhid.equalsIgnoreCase(other.swhid);
    }

    @Override
    public int hashCode() {
        return swhid.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "SWHID{" + "swhid='" + swhid + '\'' + '}';
    }
}

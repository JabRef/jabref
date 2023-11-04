package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

/**
 * Archival Resource Key (ARK) identifiers are URLs that support long-term access to information. They are similar to DOIs
 * only that we don't know of any service that can extract bibliography information from ARKs. For this reason, if an ARK
 * is used in an eprint field, it will only be possible to open the ARK in the browner; no lookup
 * or extraction of biography information will be possible.
 * <br><br>
 * <p>
 * ARK identifiers are typically prefixed with "ark:/". Even though the prefix does not help identify the resource, it appears
 * to be the standard because many websites that feature ARKs, such as archive.org, include it. For the convenience of our users
 * and to provide them with ability to copy/paste the ark as is, we support arks with or without the prefix.
 * </p>
 */
public class ARK extends EprintIdentifier {
    private final String ark;

    private ARK(String ark) {
        this.ark = ark;
    }

    public static Optional<ARK> parse(String value) {
        // TODO: Validate ARK
        return Optional.of(new ARK(value));
    }

    @Override
    public String getNormalized() {
        String cleanARK = ark.strip();
        return cleanARK.replaceFirst("^ark:/", "");
    }

    @Override
    public Optional<URI> getExternalURI() {
        return Optional.of(URI.create("https://n2t.net/ark:/" + getNormalized()));
    }
}

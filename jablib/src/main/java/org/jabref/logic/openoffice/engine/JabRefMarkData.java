package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Objects;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Immutable identity stored in a canonical mark name. Key order, including repeated keys, is
/// storage order; per-citation page info is stored separately by uid and storage index.
/// Numbers and rendered text are deliberately not part of this value.
@NullMarked
public final class JabRefMarkData {

    private final List<String> citationKeys;
    private final String uniqueId;
    private final CitationType citationType;

    /// The caller supplies stable citation keys and a document-unique id; neither is generated here.
    ///
    /// @throws MarkCodecException if the keys or uid cannot be represented in a canonical name
    public JabRefMarkData(List<String> citationKeys, String uniqueId, CitationType citationType) throws MarkCodecException {
        List<String> keys = List.copyOf(citationKeys);
        MarkCodec.validate(keys, uniqueId);
        this.citationKeys = keys;
        this.uniqueId = uniqueId;
        this.citationType = citationType;
    }

    public List<String> citationKeys() {
        return citationKeys;
    }

    public String uniqueId() {
        return uniqueId;
    }

    public CitationType citationType() {
        return citationType;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object instanceof JabRefMarkData other
                && citationKeys.equals(other.citationKeys)
                && uniqueId.equals(other.uniqueId)
                && citationType == other.citationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(citationKeys, uniqueId, citationType);
    }

    @Override
    public String toString() {
        return MarkCodec.encode(this);
    }
}

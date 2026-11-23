package org.jabref.logic.openoffice.engine;

import java.util.Set;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

/// Structural features of the selected style and reference-mark mode, not rendering options.
/// Stored citation types must be preserved even when a renderer uses a supported approximation.
@NullMarked
public record StyleCapabilities(Set<CitationType> supportedTypes,
                                boolean hasBibliography,
                                boolean supportsPageInfo,
                                boolean supportsGroupEditing) {
    public StyleCapabilities {
        supportedTypes = Set.copyOf(supportedTypes);
    }

    /// Use at insert time before storing the type. Renderers also use this approximation without
    /// changing types already stored in the document, so switching families is reversible.
    public CitationType coerceForInsert(CitationType requested) throws RenderException {
        if (supportedTypes.contains(requested)) {
            return requested;
        }
        if (supportedTypes.contains(CitationType.AUTHORYEAR_PAR)) {
            return CitationType.AUTHORYEAR_PAR;
        }
        throw new RenderException("The renderer does not support the requested citation type or a normal citation fallback");
    }
}

package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Map;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroupId;

import org.jspecify.annotations.NullMarked;

/// Prepared formatted text; applying it to a document is a separate operation.
/// Marker keys must be the request's original group-id instances: [CitationGroupId] uses identity
/// equality. Writer order comes from the request, not from iteration over the marker map.
/// `refreshCitedOnPages` requests the existing JStyle cross-reference refresh after bibliography
/// writing; all page-reference markup is already included in the rendered bibliography.
@NullMarked
public record RenderResult(Map<CitationGroupId, OOText> markers,
                           List<OOText> bibliography,
                           List<String> unresolvedKeys,
                           boolean refreshCitedOnPages) {
    public RenderResult(Map<CitationGroupId, OOText> markers, List<OOText> bibliography, List<String> unresolvedKeys) {
        this(markers, bibliography, unresolvedKeys, false);
    }

    public RenderResult {
        markers = Map.copyOf(markers);
        bibliography = List.copyOf(bibliography);
        unresolvedKeys = List.copyOf(unresolvedKeys);
    }
}

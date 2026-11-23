package org.jabref.logic.openoffice.engine;

import org.jabref.logic.openoffice.style.BstCitationFormat;

import org.jspecify.annotations.NullMarked;

/// Explicit per-render options. The selected style belongs to the renderer, never to global preferences.
/// CSL and JStyle renderers ignore the BST-specific citation format. Header/body formats are Writer
/// paragraph-style names. JStyle keeps its own bibliography body style; an empty title selects its
/// style-defined title, while CSL/BST omit the header. Excluding the bibliography does not skip the
/// set-wide computations needed to render correct citation markers.
@NullMarked
public record RenderOptions(BstCitationFormat bstCitationFormat,
                            boolean includeBibliography,
                            String bibliographyTitle,
                            String bibliographyHeaderFormat,
                            String bibliographyBodyFormat,
                            boolean alwaysAddCitedOnPages) {
    public RenderOptions(BstCitationFormat bstCitationFormat) {
        this(bstCitationFormat, true, "", "Bibliography Heading", "Text body", false);
    }
}

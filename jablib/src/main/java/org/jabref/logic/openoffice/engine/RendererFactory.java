package org.jabref.logic.openoffice.engine;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;

/// The single style-family dispatch boundary. Renderers receive explicit dependencies and never
/// discover the selected style through global preferences or a document adapter.
@NullMarked
public final class RendererFactory {

    private final BibEntryTypesManager entryTypesManager;
    private final String pandocPath;

    public RendererFactory(BibEntryTypesManager entryTypesManager, String pandocPath) {
        this.entryTypesManager = entryTypesManager;
        this.pandocPath = pandocPath;
    }

    public CitationRenderer create(OOStyle style) throws RenderException {
        return switch (style) {
            case JStyle jStyle ->
                    new JStyleRenderer(jStyle);
            case CitationStyle citationStyle ->
                    new CslRenderer(citationStyle, entryTypesManager);
            case BstStyle bstStyle ->
                    new BstRenderer(bstStyle, pandocPath);
        };
    }
}

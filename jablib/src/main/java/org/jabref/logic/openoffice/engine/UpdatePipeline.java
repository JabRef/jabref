package org.jabref.logic.openoffice.engine;

import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoCrossRef;
import org.jabref.logic.openoffice.uno.UnoStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.openoffice.style.CitationGroup;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;

/// Shared render-before-write pipeline. There is no incremental insertion or persisted numbering path.
@NullMarked
public final class UpdatePipeline {

    private final XTextDocument document;
    private final OpenOfficePreferences preferences;
    private final MarkWriter writer;
    private final RendererFactory renderers;
    private final BibliographySection bibliography = new BibliographySection();
    private final DocumentStateStore states = new DocumentStateStore();

    public UpdatePipeline(XTextDocument document, OpenOfficePreferences preferences, MarkWriter writer, RendererFactory renderers) {
        this.document = document;
        this.preferences = preferences;
        this.writer = writer;
        this.renderers = renderers;
    }

    public RenderResult render(OOStyle style, List<CitationGroup> groups, List<BibDatabase> databases, boolean includeBibliography)
            throws RenderException, WrappedTargetException {
        CitationRenderer renderer = renderers.create(style);
        checkPreconditions(style);
        RenderOptions options = new RenderOptions(preferences.getBstCitationFormat(),
                includeBibliography && renderer.capabilities().hasBibliography(),
                style instanceof JStyle ? "" : preferences.getCslBibliographyTitle(),
                preferences.getCslBibliographyHeaderFormat(), preferences.getCslBibliographyBodyFormat(),
                preferences.getAlwaysAddCitedOnPages());
        return renderer.render(new RenderRequest(groups, databases, options));
    }

    private void checkPreconditions(OOStyle style) throws WrappedTargetException, RenderException {
        if (style instanceof JStyle jStyle) {
            checkParagraphStyle(jStyle.getReferenceParagraphFormat());
            checkParagraphStyle(jStyle.getReferenceHeaderParagraphFormat());
            if (jStyle.isFormatCitations() && !jStyle.getCitationCharacterFormat().isEmpty()
                    && UnoStyle.getCharacterStyle(document, jStyle.getCitationCharacterFormat()).isEmpty()) {
                throw MissingWriterStyleException.character(jStyle.getCitationCharacterFormat());
            }
        }
    }

    private void checkParagraphStyle(String name) throws WrappedTargetException, RenderException {
        if (!name.isEmpty() && UnoStyle.getParagraphStyle(document, name).isEmpty()) {
            throw MissingWriterStyleException.paragraph(name);
        }
    }

    public void apply(OOStyle style, MarkStore store, List<CitationGroup> groups, RenderResult result, boolean rebuildBibliography)
            throws NoDocumentException, CreationException, WrappedTargetException, NotRemoveableException,
            IllegalTypeException, PropertyVetoException, RenderException {
        writer.apply(store, groups, result.markers());
        if (rebuildBibliography && renderers.create(style).capabilities().hasBibliography()) {
            bibliography.rebuild(document, result.bibliography(), style instanceof JStyle);
        }
        DocumentStateStore.State state = switch (style) {
            case JStyle jStyle ->
                    new DocumentStateStore.State(DocumentStateStore.Family.JSTYLE, jStyle.getPath());
            case CitationStyle csl ->
                    new DocumentStateStore.State(DocumentStateStore.Family.CSL, csl.getStyleId());
            case BstStyle bst ->
                    new DocumentStateStore.State(DocumentStateStore.Family.BST, bst.getPath());
            default ->
                    throw new CreationException("Unsupported citation style");
        };
        if (style instanceof CitationStyle csl && !ZoteroDocumentPreferences.writeCitationStyle(document, csl)) {
            throw new CreationException("Could not update document CSL preferences");
        }
        states.store(document, state);
        store.removeOrphanPageInfos(document);
        if (result.refreshCitedOnPages()) {
            UnoCrossRef.refresh(document);
        }
    }
}

package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;

/// Resolves bundled and external CSL styles from document metadata without rendering or changing the document.
@NullMarked
final class CslStyleResolver {

    private final Supplier<List<CitationStyle>> cslStyles;

    CslStyleResolver(Supplier<List<CitationStyle>> cslStyles) {
        this.cslStyles = cslStyles;
    }

    Optional<CitationStyle> resolve(XTextDocument document) throws WrappedTargetException {
        Optional<DocumentStyleStore.State> stored = new DocumentStyleStore().read(document);
        if (stored.isPresent()) {
            return stored.filter(state -> state.family() == DocumentStyleStore.Family.CSL)
                         .flatMap(state -> cslStyles.get().stream()
                                                    .filter(style -> state.styleId().equals(style.getPath()) || state.styleId().equals(style.getStyleId()))
                                                    .findFirst());
        }
        return ZoteroDocumentPreferences.findCitationStyle(document, cslStyles.get());
    }
}

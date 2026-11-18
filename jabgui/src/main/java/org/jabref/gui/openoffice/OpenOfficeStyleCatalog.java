package org.jabref.gui.openoffice;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;

/// Reloads JStyles and infers bundled and external CSL styles without rendering or changing document metadata.
@NullMarked
final class OpenOfficeStyleCatalog {

    private final JStyleLoader jStyleLoader;
    private final Supplier<List<CitationStyle>> cslStyles;

    OpenOfficeStyleCatalog(JStyleLoader jStyleLoader, Supplier<List<CitationStyle>> cslStyles) {
        this.jStyleLoader = jStyleLoader;
        this.cslStyles = cslStyles;
    }

    Optional<CitationStyle> inferCslStyle(XTextDocument document) throws WrappedTargetException {
        Optional<DocumentStyleStore.State> stored = new DocumentStyleStore().read(document);
        if (stored.isPresent()) {
            return stored.filter(state -> state.family() == DocumentStyleStore.Family.CSL)
                         .flatMap(state -> cslStyles.get().stream()
                                                    .filter(style -> state.styleId().equals(style.getPath()) || state.styleId().equals(style.getStyleId()))
                                                    .findFirst());
        }
        return ZoteroDocumentPreferences.findCitationStyle(document, cslStyles.get());
    }

    OOStyle reload(OOStyle selected) throws IOException {
        Optional<JStyle> jStyle = jStyleLoader.getStyles().stream().filter(selected::equals).findFirst();
        if (jStyle.isEmpty()) {
            return selected;
        }
        JStyle reloaded = jStyle.orElseThrow();
        reloaded.ensureUpToDate();
        return reloaded;
    }
}

package org.jabref.gui.openoffice;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;

/// Runtime registry of bundled and external JStyle, CSL, and BST styles, distinct from the generated CSL catalog.
/// Restores explicit document style identities and infers CSL styles without rendering or changing document metadata.
@NullMarked
final class OpenOfficeStyleCatalog {

    private final JStyleLoader jStyleLoader;
    private final BstStyleLoader bstStyleLoader;
    private final Supplier<List<CitationStyle>> cslStyles;

    OpenOfficeStyleCatalog(JStyleLoader jStyleLoader, Supplier<List<CitationStyle>> cslStyles, BstStyleLoader bstStyleLoader) {
        this.jStyleLoader = jStyleLoader;
        this.bstStyleLoader = bstStyleLoader;
        this.cslStyles = cslStyles;
    }

    Optional<OOStyle> restore(XTextDocument document, boolean zoteroMode) throws WrappedTargetException {
        if (zoteroMode) {
            return ZoteroDocumentPreferences.findCitationStyle(document, cslStyles.get()).map(style -> style);
        }
        return new DocumentStateStore().read(document).flatMap(this::find);
    }

    Optional<CitationStyle> inferCslStyle(XTextDocument document) throws WrappedTargetException {
        Optional<DocumentStateStore.State> stored = new DocumentStateStore().read(document);
        if (stored.isPresent()) {
            return stored.filter(state -> state.family() == DocumentStateStore.Family.CSL)
                         .flatMap(state -> cslStyles.get().stream()
                                                    .filter(style -> state.styleId().equals(style.getPath()) || state.styleId().equals(style.getStyleId()))
                                                    .findFirst());
        }
        return ZoteroDocumentPreferences.findCitationStyle(document, cslStyles.get());
    }

    Optional<OOStyle> find(DocumentStateStore.State state) {
        Stream<StyleOption> jStyles = jStyleLoader.getStyles().stream()
                                                  .map(style -> new StyleOption(DocumentStateStore.Family.JSTYLE, style, Set.of(style.getPath())));
        Stream<StyleOption> cslStyles = this.cslStyles.get().stream()
                                                      .map(style -> new StyleOption(DocumentStateStore.Family.CSL, style,
                                                              Set.copyOf(List.of(style.getPath(), style.getStyleId()))));
        Stream<StyleOption> bstStyles = bstStyleLoader.getStyles().stream()
                                                      .map(style -> new StyleOption(DocumentStateStore.Family.BST, style, Set.of(style.getPath())));
        return Stream.of(jStyles, cslStyles, bstStyles).flatMap(stream -> stream)
                     .filter(option -> option.family() == state.family() && option.identifiers().contains(state.styleId()))
                     .map(StyleOption::style)
                     .findFirst();
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

    @NullMarked
    private record StyleOption(DocumentStateStore.Family family, OOStyle style, Set<String> identifiers) {
    }
}

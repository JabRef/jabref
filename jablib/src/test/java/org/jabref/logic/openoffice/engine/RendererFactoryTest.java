package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;

import static org.jabref.logic.openoffice.engine.RendererTestData.SIMPLE_BST;
import static org.jabref.logic.openoffice.engine.RendererTestData.numericStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@NullMarked
class RendererFactoryTest {

    @Test
    void selectsConcreteRenderersWithExplicitDependencies(@TempDir Path temporaryDirectory) throws IOException, RenderException {
        RendererFactory factory = new RendererFactory(new BibEntryTypesManager(), "pandoc");
        Path bst = temporaryDirectory.resolve("test.bst");
        Files.writeString(bst, SIMPLE_BST);
        JStyle jstyle = new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class));

        assertEquals(List.of(JStyleRenderer.class, CslRenderer.class, BstRenderer.class),
                List.of(factory.create(jstyle).getClass(), factory.create(numericStyle(false, false)).getClass(), factory.create(BstStyle.loadExternal(bst)).getClass()));
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void insertionCoercesUnsupportedBstTypesToNormal(CitationType type) throws RenderException {
        StyleCapabilities capabilities = new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false);

        assertEquals(CitationType.AUTHORYEAR_PAR, capabilities.coerceForInsert(type));
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void insertionPreservesSupportedTypes(CitationType type) throws RenderException {
        StyleCapabilities capabilities = new StyleCapabilities(Set.of(CitationType.values()), true, true, true);

        assertEquals(type, capabilities.coerceForInsert(type));
    }

    @Test
    void missingNormalFallbackFailsWithCheckedError() {
        StyleCapabilities capabilities = new StyleCapabilities(Set.of(), false, false, false);

        assertThrows(RenderException.class, () -> capabilities.coerceForInsert(CitationType.AUTHORYEAR_PAR));
    }

    @Test
    void styleHierarchyPermitsOnlyTheThreeRendererFamilies() {
        assertEquals(Set.of(CitationStyle.class, JStyle.class, BstStyle.class), Set.of(OOStyle.class.getPermittedSubclasses()));
    }

    @Test
    void cslConstructionNeedsNeitherWriterNorGlobalPreferences() throws RenderException {
        assertInstanceOf(CslRenderer.class, new RendererFactory(new BibEntryTypesManager(), "unused-pandoc-path").create(numericStyle(false, false)));
    }
}

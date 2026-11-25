package org.jabref.languageserver.util.definition;

import org.jabref.languageserver.util.LspParserHandler;

import org.eclipse.lsp4j.Position;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class LatexDefinitionProviderTest {

    @Test
    void citationKeyAfterSpaceIsTrimmed() {
        String content = "\\cite{Corti_2009, Cooper_2007}";
        LatexDefinitionProvider provider = new LatexDefinitionProvider(new LspParserHandler());
        assertEquals("Cooper_2007", provider.getCitationKeyAtPosition(content, new Position(0, content.indexOf("Coop"))).orElseThrow());
    }

    @Test
    void noKeyOnSeparatorOrWhitespace() {
        String content = "\\cite{Corti_2009, Cooper_2007}";
        LatexDefinitionProvider provider = new LatexDefinitionProvider(new LspParserHandler());
        assertTrue(provider.getCitationKeyAtPosition(content, new Position(0, content.indexOf(","))).isEmpty());
        assertTrue(provider.getCitationKeyAtPosition(content, new Position(0, content.indexOf(" "))).isEmpty());
    }
}

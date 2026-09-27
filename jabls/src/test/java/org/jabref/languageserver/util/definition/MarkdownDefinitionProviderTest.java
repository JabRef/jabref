package org.jabref.languageserver.util.definition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntryPreferences;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkdownDefinitionProviderTest {

    private static final String MARKDOWN = """
            ---
            bibliography: literature.bib
            ---

            Starting literature: [@Nygard2011; @Kopp2018adr]
            """;

    private final LspParserHandler parserHandler = new LspParserHandler();
    private final MarkdownDefinitionProvider provider = new MarkdownDefinitionProvider(parserHandler);
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.filePreferences()).thenReturn(mock(FilePreferences.class));

        Files.writeString(tempDir.resolve("literature.bib"), """
                @Misc{Nygard2011,
                  author = {Michael Nygard},
                  title  = {Documenting Architecture Decisions},
                  year   = {2011},
                }

                @InProceedings{Kopp2018adr,
                  author    = {Oliver Kopp and Anita Armbruster and Olaf Zimmermann},
                  title     = {Markdown Architectural Decision Records: Format and Tool Support},
                  year      = {2018},
                }
                """);
        parserHandler.loadBibliographiesFromFrontMatter(tempDir.resolve("topics.md").toUri().toString(), MARKDOWN, importFormatPreferences);
    }

    @Test
    void hoverOnSecondKeyOfMultiCitation() {
        int column = "Starting literature: [@Nygard2011; @Kopp".length();
        Hover hover = provider.provideHover(MARKDOWN, new Position(4, column)).orElseThrow();
        assertEquals("""
                **Kopp2018adr**

                Oliver Kopp and Anita Armbruster and Olaf Zimmermann

                *Markdown Architectural Decision Records: Format and Tool Support* (2018)""", hover.getContents().getRight().getValue());
    }

    @Test
    void hoverOnFirstKeyOfMultiCitation() {
        int column = "Starting literature: [@Nyg".length();
        Hover hover = provider.provideHover(MARKDOWN, new Position(4, column)).orElseThrow();
        assertTrue(hover.getContents().getRight().getValue().startsWith("**Nygard2011**"));
    }

    @Test
    void noHoverOutsideCitation() {
        assertTrue(provider.provideHover(MARKDOWN, new Position(4, 3)).isEmpty());
    }
}

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
            bibliography: Chocolate.bib
            ---

            Starting literature: [@Corti_2009; @Cooper_2007]
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

        Files.writeString(tempDir.resolve("Chocolate.bib"), """
                @Article{Corti_2009,
                  author       = {Corti, Roberto and Flammer, Andreas J. and Hollenberg, Norman K. and Lüscher, Thomas F.},
                  date         = {2009-03},
                  journaltitle = {Circulation},
                  title        = {Cocoa and Cardiovascular Health},
                }

                @Article{Cooper_2007,
                  author       = {Cooper, Karen A. and Donovan, Jennifer L. and Waterhouse, Andrew L. and Williamson, Gary},
                  date         = {2007-08},
                  journaltitle = {British Journal of Nutrition},
                  title        = {Cocoa and health: a decade of research},
                }
                """);
        parserHandler.loadBibliographiesFromFrontMatter(tempDir.resolve("topics.md").toUri().toString(), MARKDOWN, importFormatPreferences);
    }

    @Test
    void hoverOnSecondKeyOfMultiCitation() {
        int column = "Starting literature: [@Corti_2009; @Coop".length();
        Hover hover = provider.provideHover(MARKDOWN, new Position(4, column)).orElseThrow();
        assertEquals("""
                **Cooper_2007**

                Cooper, Karen A. and Donovan, Jennifer L. and Waterhouse, Andrew L. and Williamson, Gary

                *Cocoa and health: a decade of research* (2007)""", hover.getContents().getRight().getValue());
    }

    @Test
    void hoverOnFirstKeyOfMultiCitation() {
        int column = "Starting literature: [@Cor".length();
        Hover hover = provider.provideHover(MARKDOWN, new Position(4, column)).orElseThrow();
        assertTrue(hover.getContents().getRight().getValue().startsWith("**Corti_2009**"));
    }

    @Test
    void noHoverOutsideCitation() {
        assertTrue(provider.provideHover(MARKDOWN, new Position(4, 3)).isEmpty());
    }
}

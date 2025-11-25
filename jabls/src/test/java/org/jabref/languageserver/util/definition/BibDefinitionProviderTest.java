package org.jabref.languageserver.util.definition;

import java.io.IOException;
import java.util.List;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibDefinitionProviderTest {

    private LspParserHandler lspParserHandler = new LspParserHandler();
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.filePreferences()).thenReturn(mock(FilePreferences.class));
        when(importFormatPreferences.filePreferences().getUserAndHost()).thenReturn("MockedUser-mockedhost");
    }

    @Test
    void provideDefinition() throws JabRefException, IOException {
        ParserResult parserResult = lspParserHandler.parserResultFromString(
                "some-uri",
                """
                        @Article{Cooper_2007,
                                author       = {Cooper, Karen A. and Donovan, Jennifer L. and Waterhouse, Andrew L. and Williamson, Gary},
                                date         = {2007-08},
                                journaltitle = {British Journal of Nutrition},
                                title        = {Cocoa and health: a decade of research},
                                doi          = {10.1017/s0007114507795296},
                                issn         = {1475-2662},
                                number       = {1},
                                pages        = {1--11},
                                volume       = {99},
                                file         = {:C\\:/Users/Philip/Downloads/corti-et-al-2009-cocoa-and-cardiovascular-health.pdf:PDF;:corti-et-al-2009-cocoa-and-cardiovascular-health.pdf:PDF},
                                publisher    = {Cambridge University Press (CUP)}
                                ,
                        }
                        """,
                importFormatPreferences);
        List<LinkedFile> files = parserResult.getDatabaseContext().getEntries().getFirst().getFiles();
        assertEquals(2, files.size());
        assertNotNull(files.getLast().getLink());
    }
}

package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OpenAlexParserTest {

    @Test
    void parsesResultsArray() throws Exception {
        String json = """
          {"results":[
            {"type":"article","title":"T1","publication_year":2023,"doi":"10.1/abc",
             "id":"https://openalex.org/W1",
             "authorships":[{"author":{"display_name":"A1"}},{"author":{"display_name":"A2"}}],
             "biblio":{"volume":"10","issue":"2","first_page":"1","last_page":"10"},
             "concepts":[{"display_name":"ML"},{"display_name":"NLP"}]
            }]}
        """;

        Parser p = new OpenAlex().getParser();
        List<BibEntry> out = p.parseEntries(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(out, "Output list should not be null");
        assertEquals(1, out.size(), "Should parse exactly one entry");

        BibEntry e = out.get(0);
        assertNotNull(e.getType(), "Entry type should not be null");
        assertEquals("article", e.getType().getName());

        assertEquals(Optional.of("T1"), e.getField(StandardField.TITLE));
        assertEquals(Optional.of("10.1/abc"), e.getField(StandardField.DOI));
        assertEquals(Optional.of("https://openalex.org/W1"), e.getField(StandardField.URL));
        assertEquals(Optional.of("A1 and A2"), e.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("1--10"), e.getField(StandardField.PAGES));
        assertEquals(Optional.of("ML, NLP"), e.getField(StandardField.KEYWORDS));
    }

    @Test
    void parsesSingleWorkObject() throws Exception {
        String json = """
          {"type":"article","title":"Only","publication_year":2022,"id":"https://openalex.org/W2"}
        """;

        Parser p = new OpenAlex().getParser();
        List<BibEntry> out = p.parseEntries(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(out, "Output list should not be null");
        assertEquals(1, out.size(), "Should parse exactly one entry");

        assertEquals(Optional.of("Only"), out.get(0).getField(StandardField.TITLE));
    }

    @Test
    void parsesDoiWithoutYear() throws Exception {
        // Regression test: ensure DOI field is correctly set even without publication_year
        String json = """
          {"type":"article","title":"T2","doi":"10.1/xyz","id":"https://openalex.org/W3"}
        """;

        Parser p = new OpenAlex().getParser();
        List<BibEntry> out = p.parseEntries(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(out, "Output list should not be null");
        assertEquals(1, out.size(), "Should parse exactly one entry");

        assertEquals(Optional.of("10.1/xyz"), out.get(0).getField(StandardField.DOI));
    }
}

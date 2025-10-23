package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAlex_ParserTest {

    @Test
    void parsesResultsArray() throws Exception {
        String json =
            "{ \"results\": [" +
            "  {\"type\":\"article\",\"title\":\"T1\",\"publication_year\":2023,\"doi\":\"10.1/abc\"," +
            "   \"id\":\"https://openalex.org/W1\"," +
            "   \"authorships\":[{\"author\":{\"display_name\":\"A1\"}},{\"author\":{\"display_name\":\"A2\"}}]," +
            "   \"biblio\":{\"volume\":\"10\",\"issue\":\"2\",\"first_page\":\"1\",\"last_page\":\"10\"}," +
            "   \"concepts\":[{\"display_name\":\"ML\"},{\"display_name\":\"NLP\"}]" +
            "  }]}";

        Parser p = new OpenAlex().getParser();
        List<BibEntry> out = p.parseEntries(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(out).hasSize(1);
        BibEntry e = out.get(0);
        assertThat(e.getType().getName()).isEqualTo("article");
        assertThat(e.getField(StandardField.TITLE)).hasValue("T1");
        assertThat(e.getField(StandardField.DOI)).hasValue("10.1/abc");
        assertThat(e.getField(StandardField.URL)).hasValue("https://openalex.org/W1");
        assertThat(e.getField(StandardField.AUTHOR)).hasValue("A1 and A2");
        assertThat(e.getField(StandardField.PAGES)).hasValue("1--10");
        assertThat(e.getField(StandardField.KEYWORDS)).hasValue("ML, NLP");
    }

    @Test
    void parsesSingleWorkObject() throws Exception {
        String json = "{\"type\":\"article\",\"title\":\"Only\",\"publication_year\":2022,\"id\":\"https://openalex.org/W2\"}";
        Parser p = new OpenAlex().getParser();
        List<BibEntry> out = p.parseEntries(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getField(StandardField.TITLE)).hasValue("Only");
    }
}



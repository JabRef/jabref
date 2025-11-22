package org.jabref.logic.importer.relatedwork;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gold fixture model used for evaluating citation-context extraction.
 */
public final class RelatedWorkFixture {

    public static final class Expectation {
        public final String firstAuthorSurname;
        public final String year;
        public final String snippetContains;

        @JsonCreator
        public Expectation(@JsonProperty("firstAuthorSurname") String firstAuthorSurname,
                           @JsonProperty("year") String year,
                           @JsonProperty("snippetContains") String snippetContains) {
            this.firstAuthorSurname = firstAuthorSurname;
            this.year = year;
            this.snippetContains = snippetContains;
        }

        public String canonicalKey() {
            String author = (firstAuthorSurname == null)
                            ? "unknown"
                            : firstAuthorSurname.toLowerCase(Locale.ROOT);
            String yr = (year == null) ? "unknown" : year.trim();
            return author + "-" + yr;
        }
    }

    public final String id;
    public final String relatedWorkText;
    public final List<Expectation> expectations;

    @JsonCreator
    public RelatedWorkFixture(@JsonProperty("id") String id,
                              @JsonProperty("relatedWorkText") String relatedWorkText,
                              @JsonProperty("expectations") List<Expectation> expectations) {
        this.id = id;
        this.relatedWorkText = relatedWorkText;
        this.expectations = expectations;
    }

    /**
     * Load a fixture from a JSON file.
     */
    public static RelatedWorkFixture load(Path jsonPath) throws IOException {
        try (Reader r = Files.newBufferedReader(jsonPath)) {
            return new ObjectMapper().readValue(r, RelatedWorkFixture.class);
        }
    }
}

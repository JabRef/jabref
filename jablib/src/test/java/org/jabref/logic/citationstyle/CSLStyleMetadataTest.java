package org.jabref.logic.citationstyle;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class CSLStyleMetadataTest {

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void catalogAndRuntimeReadTheSameMetadata(boolean citationSort, boolean bibliographySort) {
        String source = """
                <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                  <info><title>Example</title><title-short>Ex</title-short><id>urn:example</id>
                    <category citation-format="numeric"/><category field="medicine"/>
                  </info>
                  <citation>%s<layout><text variable="citation-number"/></layout></citation>
                  <bibliography hanging-indent="true">%s<layout><text variable="title"/></layout></bibliography>
                </style>
                """.formatted(citationSort ? "<sort><key variable=\"citation-number\"/></sort>" : "",
                bibliographySort ? "<sort><key variable=\"title\"/></sort>" : "");
        CSLStyleMetadata expected = new CSLStyleMetadata("urn:example", "in-text", "Example", "Ex", true, true, bibliographySort, true);

        assertEquals(Optional.of(expected), CSLStyleMetadata.parseStyleInfo("example.csl", source));
        assertEquals(Optional.of(new CSLStyleUtils.StyleInfo("urn:example", "in-text", "Example", "Ex", true, true, bibliographySort, true)),
                CSLStyleUtils.parseStyleInfo("example.csl", source));
    }

    @Test
    void citationOnlyStyleHasNoBibliographyCapabilities() {
        String source = "<style class=\"note\"><info><title>Notes</title></info><citation><layout/></citation></style>";

        assertEquals(Optional.of(new CSLStyleMetadata("", "note", "Notes", "", false, false, false, false)),
                CSLStyleMetadata.parseStyleInfo("notes.csl", source));
    }

    @Test
    void stylesWithoutCitationsAreNotAddedToCatalog() {
        assertEquals(Optional.empty(), CSLStyleMetadata.parseStyleInfo("bibliography-only.csl", "<style><bibliography/></style>"));
    }

    @Test
    void malformedXmlIsRejectedWithoutLoadingStyles() {
        assertEquals(Optional.empty(), CSLStyleMetadata.parseStyleInfo("broken.csl", "<style><citation>"));
    }
}

package org.jabref.logic.biblog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.biblog.BibWarning;
import org.jabref.model.biblog.SeverityType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibtexLogParserTest {
    private BibtexLogParser parser;

    @BeforeEach
    void setup() {
        parser = new BibtexLogParser();
    }

    @Test
    void parsesWarningsFromResourceFileTest() throws IOException {
        Path blgFile = Path.of("src/test/resources/org/jabref/logic/blg/Chocolate.blg");
        List<BibWarning> warnings = parser.parseBiblog(blgFile);
        assertEquals(List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Tan_2021"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Tan_2021")
        ), warnings);
    }

    @ParameterizedTest
    @MethodSource("biblatexValidationWarningsProvider")
    void parsesBiblatexValidationWarnings(String warningLine, Optional<BibWarning> expectedWarning) {
        assertEquals(expectedWarning, parser.parseWarningLine(warningLine));
    }

    private static Stream<Arguments> biblatexValidationWarningsProvider() {
        return Stream.of(
                Arguments.of("[1124] Biber.pm:131> WARN - Datamodel: article entry 'Corti_2009' (chocolate.bib): Invalid field 'publisher' for entrytype 'article'",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Invalid field 'publisher' for entrytype 'article'", "publisher", "Corti_2009"))),

                Arguments.of("[1126] Biber.pm:131> WARN - Datamodel: article entry 'Parker_2006' (Chocolate.bib): Missing mandatory field - one of 'date, year' must be defined",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Missing mandatory field - one of 'date, year' must be defined", "date", "Parker_2006"))),

                Arguments.of("[1127] Biber.pm:131> WARN - Datamodel: article entry 'Corti_2009' (Chocolate.bib): Missing mandatory field 'author'",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Missing mandatory field 'author'", "author", "Corti_2009"))),

                Arguments.of("[1128] Biber.pm:131> WARN - Datamodel: article entry 'Cooper_2007' (Chocolate.bib): Invalid ISSN in value of field 'issn'",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Invalid ISSN in value of field 'issn'", "issn", "Cooper_2007"))),

                Arguments.of("[1129] Biber.pm:131> WARN - Datamodel: article entry 'Katz_2011' (Chocolate.bib): Invalid value of field 'volume' must be datatype 'integer' - ignoring field",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Invalid value of field 'volume' must be datatype 'integer' - ignoring field", "volume", "Katz_2011"))),

                Arguments.of("WARN - Datamodel: article entry 'Keen_2001' (Chocolate.bib): Invalid field 'publisher' for entrytype 'article'",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Invalid field 'publisher' for entrytype 'article'", "publisher", "Keen_2001"))),

                Arguments.of("WARN - Datamodel: article entry 'Macht_2007' (Chocolate.bib): Field 'groups' invalid in data model - ignoring",
                        Optional.of(new BibWarning(SeverityType.WARNING, "Field 'groups' invalid in data model - ignoring", "groups", "Macht_2007"))),

                Arguments.of("This is not a valid warning line", Optional.empty()),
                Arguments.of("", Optional.empty())
        );
    }
}

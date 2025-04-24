package org.jabref.logic.biblog;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.biblog.BibWarning;
import org.jabref.model.biblog.SeverityType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibtexLogParserTest {
    private BibtexLogParser parser;

    @BeforeEach
    void setup() {
        parser = new BibtexLogParser();
    }

    @Test
    void parsesWarningsFromResourceFileTest() throws Exception {
        Path blgFile = Path.of("src/test/resources/org/jabref/logic/blg/Chocolate.blg");
        List<BibWarning> warnings = parser.parseBiblog(blgFile);
        assertEquals(List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Tan_2021"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Tan_2021")
        ), warnings);
    }
}

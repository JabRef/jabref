package org.jabref.logic.importer.plaincitation;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeveralCitationsParsingTest {
    static SeveralPlainCitationParser parser = new SeveralPlainCitationParser(new RuleBasedPlainCitationParser());

    @Test
    void correctlySplitTexts() {
        String part1 = "abc";
        String part2 = "123";
        String part3 = "xyz";

        String input = part1 + "\n\n" + part2 + "\n\n" + part3;

        List<String> output = parser.splitCitations(input).toList();

        assertEquals(List.of(part1, part2, part3), output);
    }
}

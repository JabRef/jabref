package org.jabref.logic.citationkeypattern;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Execution(ExecutionMode.CONCURRENT)
class AbstractCitationKeyPatternTest {

    @Test
    void AbstractCitationKeyPatternParse() throws Exception {
        AbstractCitationKeyPattern pattern = mock(AbstractCitationKeyPattern.class, Mockito.CALLS_REAL_METHODS);

        pattern.setDefaultValue("[field1]spacer1[field2]spacer2[field3]");
        List<String> expectedPattern = List.of(
            "[field1]spacer1[field2]spacer2[field3]",
            "[",
            "field1",
            "]",
            "spacer1",
            "[",
            "field2",
            "]",
            "spacer2",
            "[",
            "field3",
            "]"
        );
        assertEquals(expectedPattern, pattern.getDefaultValue());
    }

    @Test
    void AbstractCitationKeyPatternParseEmptySpacer() throws Exception {
        AbstractCitationKeyPattern pattern = mock(AbstractCitationKeyPattern.class, Mockito.CALLS_REAL_METHODS);

        pattern.setDefaultValue("[field1][field2]spacer2[field3]");
        List<String> expectedPattern = List.of(
            "[field1][field2]spacer2[field3]",
            "[",
            "field1",
            "]",
            "[",
            "field2",
            "]",
            "spacer2",
            "[",
            "field3",
            "]"
        );
        assertEquals(expectedPattern, pattern.getDefaultValue());
    }
}

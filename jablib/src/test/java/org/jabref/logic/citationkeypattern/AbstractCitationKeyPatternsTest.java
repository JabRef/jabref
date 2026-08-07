package org.jabref.logic.citationkeypattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Execution(ExecutionMode.CONCURRENT)
class AbstractCitationKeyPatternsTest {

    @Test
    void AbstractCitationKeyPatternParse() {
        AbstractCitationKeyPatterns pattern = mock(AbstractCitationKeyPatterns.class, Mockito.CALLS_REAL_METHODS);

        pattern.setDefaultValue("[field1]spacer1[field2]spacer2[field3]");
        CitationKeyPattern expectedPattern = new CitationKeyPattern("[field1]spacer1[field2]spacer2[field3]");
        assertEquals(expectedPattern, pattern.getDefaultValue());
    }

    @Test
    void AbstractCitationKeyPatternParseEmptySpacer() {
        AbstractCitationKeyPatterns pattern = mock(AbstractCitationKeyPatterns.class, Mockito.CALLS_REAL_METHODS);

        pattern.setDefaultValue("[field1][field2]spacer2[field3]");
        CitationKeyPattern expectedPattern = new CitationKeyPattern("[field1][field2]spacer2[field3]");
        assertEquals(expectedPattern, pattern.getDefaultValue());
    }
}

package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SSRNTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                // Basic string
                Arguments.of(false, "4904445"),
                Arguments.of(false, "  4904445   "),

                // URLs
                Arguments.of(true, "https://ssrn.com/abstract=4904445"),
                Arguments.of(true, "https://papers.ssrn.com/sol3/papers.cfm?abstract_id=4904445"),
                Arguments.of(true, "  https://ssrn.com/abstract=4904445    "),
                Arguments.of(true, "  https://papers.ssrn.com/sol3/papers.cfm?abstract_id=4904445     "),
                Arguments.of(true, "http://ssrn.com/abstract=4904445")
        );
    }

    /**
     * @param findInText if the input should be found when passing through "find in text"
     * @param input the input to be checked
     */
    @ParameterizedTest
    @MethodSource("provideTestData")
    public void acceptCorrectSSRNAbstracts(boolean findInText, String input) {
        assertEquals("4904445", new SSRN(input).asString());
        Optional<SSRN> parsed = SSRN.parse(input);

        if (findInText) {
            assertTrue(parsed.isPresent());
            assertEquals("4904445", parsed.get().asString());
        } else {
            assertTrue(parsed.isEmpty());
        }
    }

    @Test
    public void findInText() {
        Optional<SSRN> parsed = SSRN.parse("The example paper (https://ssrn.com/abstract=4904445) should be found within this text");
        assertTrue(parsed.isPresent());
        assertEquals("4904445", parsed.get().asString());
    }

    @Test
    public void identifierNormalisation() {
        assertEquals("123456", new SSRN(123456).asString());
    }

    @Test
    public void identifierExternalUrl() {
        SSRN ssrnIdentifier = new SSRN(123456);
        URI uri = URI.create("https://ssrn.com/abstract=123456");
        assertEquals(Optional.of(uri), ssrnIdentifier.getExternalURI());
    }
}

package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorOrgSciTest {

    LayoutFormatter authorOrgNatFormatter = new AuthorOrgSci();
    LayoutFormatter authorOrgNatFormatterComposite = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());

    @ParameterizedTest
    @MethodSource("formatTests")
    void paramLayoutFormatTest(String expectedString, String inputString, boolean removeSpacesBetweenAbbreviations) {
        if (!removeSpacesBetweenAbbreviations) {
            assertEquals(expectedString, authorOrgNatFormatter.format(inputString));
        } else {
            assertEquals(expectedString, authorOrgNatFormatterComposite.format(inputString));
        }
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                // OrgSci Formatting Tests
                Arguments.of("Flynn, J., S. Gartska", "John Flynn and Sabine Gartska", false),
                Arguments.of("Garvin, D. A.", "David A. Garvin", false),
                Arguments.of("Makridakis, S., S. C. Wheelwright, V. E. McGee", "Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee", false),

                // Composite OrgSci Formatting Tests
                Arguments.of("Flynn, J., S. Gartska", "John Flynn and Sabine Gartska", true),
                Arguments.of("Garvin, D.A.", "David A. Garvin", true),
                Arguments.of("Makridakis, S., S.C. Wheelwright, V.E. McGee", "Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee", true)
        );
    }
}

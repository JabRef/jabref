package org.jabref.logic.importer.util;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.identifier.ISBN;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsbnExtractorTest {

    private final IsbnExtractor extractor = new IsbnExtractor();

    private List<String> extract(String text) {
        return extractor.extract(text).stream().map(ISBN::asString).toList();
    }

    static Stream<Arguments> foundIsbns() {
        return Stream.of(
                // ISBN-13s first, then ISBN-10s
                Arguments.of("ISBN-10: 0-596-52068-9\nISBN-13: 978-0-596-52068-7", List.of("9780596520687", "0596520689")),
                // order of appearance, not numeric order
                Arguments.of("ISBN 978-0-596-52068-7\nISBN 978-0-13-468599-1", List.of("9780596520687", "9780134685991")),
                Arguments.of("ISBN 0-596-52068-9\nISBN 0-13-162959-X", List.of("0596520689", "013162959X")),
                // the same ISBN written in different ways is returned once
                Arguments.of("ISBN 978-0-596-52068-7 and 9780596520687 and 978 0 596 52068 7", List.of("9780596520687")),
                // X check digit, upper and lower case
                Arguments.of("ISBN 0-13-162959-X", List.of("013162959X")),
                Arguments.of("ISBN 0-13-162959-x", List.of("013162959X")),
                // ISBN wrapped over two lines
                Arguments.of("ISBN 978-0-596-\n52068-7", List.of("9780596520687")),
                Arguments.of("ISBN 978-0-596-\r\n52068-7", List.of("9780596520687")),
                // label variants for ISBN-10
                Arguments.of("ISBN10: 0596520689", List.of("0596520689")),
                Arguments.of("ISBN-10 0-596-52068-9", List.of("0596520689")),
                Arguments.of("isbn 0 596 52068 9", List.of("0596520689")),
                Arguments.of("ISBN:\n0-596-52068-9", List.of("0596520689")),
                // an ISBN-13 label also starts a list, so an ISBN-10 after the ISBN-13 is found
                Arguments.of("ISBN-13: 978-0-596-52068-7, 0-596-52068-9", List.of("9780596520687", "0596520689")),
                Arguments.of("ISBN13 978-0-596-52068-7 and 0-596-52068-9", List.of("9780596520687", "0596520689")),
                // digits after the label that look like "10" or "13" are part of the number
                Arguments.of("ISBN 1305116550", List.of("1305116550")),
                Arguments.of("ISBN 101234567X", List.of("101234567X")),
                // a short note in parentheses between label and ISBN-10
                Arguments.of("ISBN (paperback): 0-13-468599-7", List.of("0134685997")),
                Arguments.of("ISBN-10 (hardcover) 0-13-468599-7", List.of("0134685997")),
                // lists of ISBN-10s after a label
                Arguments.of("ISBN 0-596-52068-9, 0-306-40615-2", List.of("0596520689", "0306406152")),
                Arguments.of("ISBN 0-596-52068-9 and 0-306-40615-2", List.of("0596520689", "0306406152")),
                Arguments.of("ISBN 0-596-52068-9 (print), 0-306-40615-2 (ebook)", List.of("0596520689", "0306406152")),
                Arguments.of("ISBN 0-596-52068-9, 0-306-40615-2, 0-13-468599-7", List.of("0596520689", "0306406152", "0134685997")),
                Arguments.of("ISBN 978-0-596-52068-7, 0-596-52068-9", List.of("9780596520687", "0596520689")),
                // an ISBN-13 in the middle of a list does not end the list
                Arguments.of("ISBN 0-596-52068-9, 978-0-13-468599-1, 0-306-40615-2", List.of("9780134685991", "0596520689", "0306406152")),
                // a typo in one ISBN of a list is skipped and does not hide the next one
                Arguments.of("ISBN 978-0-596-52068-0, 0-596-52068-9", List.of("0596520689")),
                Arguments.of("ISBN 0-596-52068-0, 0-306-40615-2", List.of("0306406152")),
                Arguments.of("ISBN 0-596-52068-9, 0-596-52068-0, 0-306-40615-2", List.of("0596520689", "0306406152")),
                // an ISBN-13 needs no label
                Arguments.of("Order number 978-0-596-52068-7", List.of("9780596520687"))
        );
    }

    /// Documents what is deliberately not found.
    static Stream<Arguments> knownLimitations() {
        return Stream.of(
                // no separator between the numbers
                Arguments.of("ISBN 0-596-52068-9\n0-306-40615-2", List.of("0596520689")),
                // note in parentheses is too long
                Arguments.of("ISBN (see the preface for more details): 0-13-468599-7", List.of())
        );
    }

    @ParameterizedTest
    @MethodSource({"foundIsbns", "knownLimitations"})
    void extractsExpectedIsbns(String text, List<String> expected) {
        assertEquals(expected, extract(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "ISBN 978-0-596-52068-0",
            "ISBN 0-596-52068-0",
            "ISBN 97805965206879",
            "Order number 0-596-52068-9",
            "Print run 2023 2024 12",
            "(c) 2019-2020-12",
            ", 0-596-52068-9",
            "2023, 0-596-52068-9",
            "Edition 3, 0-596-52068-9",
            // the label ends in a digit, so the number is cut out of a longer run of digits
            "ISBN-100596520689"
    })
    void findsNothing(String text) {
        assertEquals(List.of(), extract(text));
    }
}

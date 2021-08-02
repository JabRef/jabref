package org.jabref.logic.importer.util;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileFieldParserTest {

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        new LinkedFile("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf"),
                        List.of("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf")
                ),
                Arguments.of(
                        new LinkedFile("arXiv Fulltext PDF", "https/://arxiv.org/pdf/1109.0517.pdf", "application/pdf"),
                        List.of("arXiv Fulltext PDF", "https\\://arxiv.org/pdf/1109.0517.pdf", "application/pdf")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void check(LinkedFile expected, List<String> input) {
        // we need to convert the unmodifiable list to a modifiable because of the side effect of "convert"
        assertEquals(expected, FileFieldParser.convert(new ArrayList<>(input)));
    }

    private static Stream<Arguments> stringsToParseTestData() throws Exception {
        return Stream.of(
                // null string
                Arguments.of(
                        Collections.emptyList(),
                        null
                ),

                // empty string
                Arguments.of(
                        Collections.emptyList(),
                        ""
                ),

                // correct input
                Arguments.of(
                        Collections.singletonList(new LinkedFile("Desc", Path.of("File.PDF"), "PDF")),
                        "Desc:File.PDF:PDF"
                ),

                // parseCorrectOnlineInput
                Arguments.of(
                        Collections.singletonList(new LinkedFile(new URL("http://arxiv.org/pdf/2010.08497v1"), "PDF")),
                        ":http\\://arxiv.org/pdf/2010.08497v1:PDF"
                ),

                // parseFaultyOnlineInput
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", "htt://arxiv.org/pdf/2010.08497v1", "PDF")),
                        ":htt\\://arxiv.org/pdf/2010.08497v1:PDF"
                ),

                // parseFaultyArxivOnlineInput
                Arguments.of(
                        Collections.singletonList(new LinkedFile("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf")),
                        "arXiv Fulltext PDF:https\\://arxiv.org/pdf/1109.0517.pdf:application/pdf"
                ),

                // ignoreMissingDescription
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", Path.of("wei2005ahp.pdf"), "PDF")),
                        ":wei2005ahp.pdf:PDF"
                ),

                // interpretLinkAsOnlyMandatoryField: single
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", Path.of("wei2005ahp.pdf"), "")),
                        "wei2005ahp.pdf"
                ),

                // interpretLinkAsOnlyMandatoryField: multiple
                Arguments.of(
                        List.of(
                                new LinkedFile("", Path.of("wei2005ahp.pdf"), ""),
                                new LinkedFile("", Path.of("other.pdf"), "")
                        ),
                        "wei2005ahp.pdf;other.pdf"
                ),

                // escapedCharactersInDescription
                Arguments.of(
                        Collections.singletonList(new LinkedFile("test:;", Path.of("wei2005ahp.pdf"), "PDF")),
                        "test\\:\\;:wei2005ahp.pdf:PDF"
                ),

                // handleXmlCharacters
                Arguments.of(
                        Collections.singletonList(new LinkedFile("test&#44;st:;", Path.of("wei2005ahp.pdf"), "PDF")),
                        "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF"
                ),

                // handleEscapedFilePath
                Arguments.of(
                        Collections.singletonList(new LinkedFile("desc", Path.of("C:\\test.pdf"), "PDF")),
                        "desc:C\\:\\\\test.pdf:PDF"
                ),

                // subsetOfFieldsResultsInFileLink: description only
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                        "file.pdf::"
                ),

                // subsetOfFieldsResultsInFileLink: file only
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                        ":file.pdf"
                ),

                // subsetOfFieldsResultsInFileLink: type only
                Arguments.of(
                        Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                        "::file.pdf"
                ),

                // tooManySeparators
                Arguments.of(
                        Collections.singletonList(new LinkedFile("desc", Path.of("file.pdf"), "PDF")),
                        "desc:file.pdf:PDF:asdf"
                ),

                // url
                Arguments.of(
                         Collections.singletonList(new LinkedFile(new URL("https://books.google.de/"), "")),
                         "https://books.google.de/"
                ),

                // url as file
                Arguments.of(
                             Collections.singletonList(new LinkedFile("", new URL("http://ceur-ws.org/Vol-438"), "URL")),
                             ":http\\://ceur-ws.org/Vol-438:URL"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("stringsToParseTestData")
    public void testParse(List<LinkedFile> expected, String input) {
        assertEquals(expected, FileFieldParser.parse(input));
    }
}

package org.jabref.logic.importer.util;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileFieldParserTest {

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        LinkedFile.of("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf"),
                        List.of("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf")
                ),
                Arguments.of(
                        LinkedFile.of("arXiv Fulltext PDF", "https/://arxiv.org/pdf/1109.0517.pdf", "application/pdf"),
                        List.of("arXiv Fulltext PDF", "https\\://arxiv.org/pdf/1109.0517.pdf", "application/pdf")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void check(LinkedFile expected, List<String> input) {
        // we need to convert the unmodifiable list to a modifiable because of the side effect of "convert"
        assertEquals(expected, FileFieldParser.convert(new ArrayList<>(input)));
    }

    private static Stream<Arguments> stringsToParseTest() throws MalformedURLException {
        return Stream.of(
                // null string
                Arguments.of(
                        List.of(),
                        null
                ),

                // empty string
                Arguments.of(
                        List.of(),
                        ""
                ),

                // URL starting with www. (without protocol)
                Arguments.of(
                        List.of(LinkedFile.of("A test", URLUtil.create("https://www.yahoo.com/abc/cde.htm"), "URL")),
                        "A test:www.yahoo.com/abc/cde.htm:URL"
                ),

                // correct input
                Arguments.of(
                        List.of(LinkedFile.of("Desc", Path.of("File.PDF"), "PDF")),
                        "Desc:File.PDF:PDF"
                ),

                // Mendeley input
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("C:/Users/XXXXXX/AppData/Local/Mendeley Ltd./Mendeley Desktop/Downloaded/Brown - 2017 - Physical test methods for elastomers.pdf"), "pdf")),
                        ":C$\\backslash$:/Users/XXXXXX/AppData/Local/Mendeley Ltd./Mendeley Desktop/Downloaded/Brown - 2017 - Physical test methods for elastomers.pdf:pdf"
                ),

                // parseCorrectOnlineInput
                Arguments.of(
                        List.of(LinkedFile.of(URLUtil.create("http://arxiv.org/pdf/2010.08497v1"), "PDF")),
                        ":http\\://arxiv.org/pdf/2010.08497v1:PDF"
                ),

                // parseFaultyOnlineInput
                Arguments.of(
                        List.of(LinkedFile.of("", "htt://arxiv.org/pdf/2010.08497v1", "PDF")),
                        ":htt\\://arxiv.org/pdf/2010.08497v1:PDF"
                ),

                // parseFaultyArxivOnlineInput
                Arguments.of(
                        List.of(LinkedFile.of("arXiv Fulltext PDF", "https://arxiv.org/pdf/1109.0517.pdf", "application/pdf")),
                        "arXiv Fulltext PDF:https\\://arxiv.org/pdf/1109.0517.pdf:application/pdf"
                ),

                // ignoreMissingDescription
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("wei2005ahp.pdf"), "PDF")),
                        ":wei2005ahp.pdf:PDF"
                ),

                // interpretLinkAsOnlyMandatoryField: single
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("wei2005ahp.pdf"), "")),
                        "wei2005ahp.pdf"
                ),

                // interpretLinkAsOnlyMandatoryField: multiple
                Arguments.of(
                        List.of(
                                LinkedFile.of("", Path.of("wei2005ahp.pdf"), ""),
                                LinkedFile.of("", Path.of("other.pdf"), "")
                        ),
                        "wei2005ahp.pdf;other.pdf"
                ),

                // escapedCharactersInDescription
                Arguments.of(
                        List.of(LinkedFile.of("test:;", Path.of("wei2005ahp.pdf"), "PDF")),
                        "test\\:\\;:wei2005ahp.pdf:PDF"
                ),

                // handleXmlCharacters
                Arguments.of(
                        List.of(LinkedFile.of("test&#44;st:;", Path.of("wei2005ahp.pdf"), "PDF")),
                        "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF"
                ),

                // handleEscapedFilePath
                Arguments.of(
                        List.of(LinkedFile.of("desc", Path.of("C:\\test.pdf"), "PDF")),
                        "desc:C\\:\\\\test.pdf:PDF"
                ),

                // handleNonEscapedFilePath
                Arguments.of(
                        List.of(LinkedFile.of("desc", Path.of("C:\\test.pdf"), "PDF")),
                        "desc:C:\\test.pdf:PDF"
                ),

                // Source: https://github.com/JabRef/jabref/issues/8991#issuecomment-1214131042
                Arguments.of(
                        List.of(LinkedFile.of("Boyd2012.pdf", Path.of("C:\\Users\\Literature_database\\Boyd2012.pdf"), "PDF")),
                        "Boyd2012.pdf:C\\:\\\\Users\\\\Literature_database\\\\Boyd2012.pdf:PDF"
                ),

                // subsetOfFieldsResultsInFileLink: description only
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("file.pdf"), "")),
                        "file.pdf::"
                ),

                // subsetOfFieldsResultsInFileLink: file only
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("file.pdf"), "")),
                        ":file.pdf"
                ),

                // subsetOfFieldsResultsInFileLink: type only
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("file.pdf"), "")),
                        "::file.pdf"
                ),

                // tooManySeparators
                Arguments.of(
                        List.of(LinkedFile.of("desc", Path.of("file.pdf"), "PDF", "qwer")),
                        "desc:file.pdf:PDF:qwer:asdf:uiop"
                ),

                // www inside filename
                Arguments.of(
                        List.of(LinkedFile.of("", Path.of("/home/www.google.de.pdf"), "")),
                        ":/home/www.google.de.pdf"
                ),

                // url
                Arguments.of(
                        List.of(LinkedFile.of(URLUtil.create("https://books.google.de/"), "")),
                        "https://books.google.de/"
                ),

                // url with www
                Arguments.of(
                        List.of(LinkedFile.of(URLUtil.create("https://www.google.de/"), "")),
                        "https://www.google.de/"
                ),

                // url as file
                Arguments.of(
                        List.of(LinkedFile.of("", URLUtil.create("http://ceur-ws.org/Vol-438"), "URL")),
                        ":http\\://ceur-ws.org/Vol-438:URL"
                ),
                // url as file with desc
                Arguments.of(
                        List.of(LinkedFile.of("desc", URLUtil.create("http://ceur-ws.org/Vol-438"), "URL")),
                        "desc:http\\://ceur-ws.org/Vol-438:URL"
                ),
                // link with source url
                Arguments.of(
                        List.of(LinkedFile.of("arXiv Fulltext PDF", "matheus.ea explicit.pdf", "PDF", "https://arxiv.org/pdf/1109.0517.pdf")),
                        "arXiv Fulltext PDF:matheus.ea explicit.pdf:PDF:https\\://arxiv.org/pdf/1109.0517.pdf"
                ),
                // link without description and with source url
                Arguments.of(
                        List.of(LinkedFile.of("", "matheus.ea explicit.pdf", "PDF", "https://arxiv.org/pdf/1109.0517.pdf")),
                        ":matheus.ea explicit.pdf:PDF:https\\://arxiv.org/pdf/1109.0517.pdf"
                ),
                // no link but with source url
                Arguments.of(
                        List.of(LinkedFile.of("arXiv Fulltext PDF", "", "PDF", "https://arxiv.org/pdf/1109.0517.pdf")),
                        "arXiv Fulltext PDF::PDF:https\\://arxiv.org/pdf/1109.0517.pdf"
                ),
                // No description or file type but with sourceURL
                Arguments.of(
                        List.of(LinkedFile.of("", "matheus.ea explicit.pdf", "", "https://arxiv.org/pdf/1109.0517.pdf")),
                        ":matheus.ea explicit.pdf::https\\://arxiv.org/pdf/1109.0517.pdf"
                ),
                // Absolute path
                Arguments.of(
                        List.of(LinkedFile.of("", "A:\\Zotero\\storage\\test.pdf", "")),
                        ":A:\\Zotero\\storage\\test.pdf"
                ),
                // zotero absolute path
                Arguments.of(
                        List.of(LinkedFile.of("", "A:\\Zotero\\storage\\test.pdf", "")),
                        "A:\\Zotero\\storage\\test.pdf"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void stringsToParseTest(List<LinkedFile> expected, String input) {
        assertEquals(expected, FileFieldParser.parse(input));
    }
}

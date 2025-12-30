package org.jabref.logic.importer.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.exporter.MetaDataSerializerTest;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataParserTest {

    @ParameterizedTest
    @CsvSource({
            "C:\\temp\\test,                 C:\\temp\\test",
            "\\\\servername\\path\\to\\file, \\\\\\\\servername\\\\path\\\\to\\\\file",
            "\\\\servername\\path\\to\\file, \\\\servername\\path\\to\\file",
            "//servername/path/to/file,      //servername/path/to/file",
            ".\\pdfs,                        .\\pdfs,",
            ".\\pdfs,                        .\\\\pdfs,",
            ".,                              .",
    })
    void parseDirectory(String expected, String input) {
        assertEquals(expected, MetaDataParser.parseDirectory(input));
    }

    /**
     * In case of any change, copy the content to {@link MetaDataSerializerTest#serializeCustomizedEntryType()}
     */
    public static Stream<Arguments> parseCustomizedEntryType() {
        return Stream.of(
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(new UnknownField("Test1"), new UnknownField("Test2")),
                        "jabref-entrytype: test: req[Test1;Test2] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(new UnknownField("tEST"), new UnknownField("tEsT2")),
                        "jabref-entrytype: test: req[tEST;tEsT2] opt[]"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseCustomizedEntryType(BibEntryTypeBuilder expected, String source) {
        assertEquals(Optional.of(expected.build()), MetaDataParser.parseCustomEntryType(source));
    }

    @Test
    void saveActions() throws ParseException {
        Map<String, String> data = Map.of("saveActions", "enabled;title[lower_case]");
        MetaDataParser metaDataParser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = metaDataParser.parse(new MetaData(), data, ',', "userAndHost");

        MetaData expected = new MetaData();
        FieldFormatterCleanupActions fieldFormatterCleanupActions = new FieldFormatterCleanupActions(true, List.of(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())));
        expected.setSaveActions(fieldFormatterCleanupActions);
        assertEquals(expected, parsed);
    }

    @Test
    void parsesUserSpecificBlgPathSuccessfully() throws ParseException {
        String user = "testUser";
        String rawKey = "blgFilePath-" + user;
        String rawValue = "/home/user/test.blg;";

        MetaDataParser parser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = parser.parse(Map.of(rawKey, rawValue), ',', "userAndHost");

        assertEquals(Optional.of(Path.of("/home/user/test.blg")), parsed.getBlgFilePath(user));
    }

    @Test
    void parsesLatexFileDirectoryForUserHostSuccessfully() throws ParseException {
        String user = "testUser";
        String host = "testHost";
        String userHost = user + "-" + host;
        String rawKey = "fileDirectoryLatex-" + userHost;
        String rawValue = "/home/user/latex;";

        MetaDataParser parser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = parser.parse(Map.of(rawKey, rawValue), ',', "userAndHost");

        assertEquals(Optional.of(Path.of("/home/user/latex")), parsed.getLatexFileDirectory(userHost));
    }

    @Test
    void parsesMultipleLatexFileDirectoriesSuccessfully() throws ParseException {
        String userHost1 = "user1-host1";
        String userHost2 = "user2-host2";

        Map<String, String> data = Map.of(
                "fileDirectoryLatex-" + userHost1, "/path/for/host1;",
                "fileDirectoryLatex-" + userHost2, "/path/for/host2;"
        );

        MetaDataParser parser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = parser.parse(data, ',', "userAndHost");

        assertEquals(Optional.of(Path.of("/path/for/host1")), parsed.getLatexFileDirectory(userHost1));
        assertEquals(Optional.of(Path.of("/path/for/host2")), parsed.getLatexFileDirectory(userHost2));
    }

    @Test
    void parsesWindowsPathsInLatexFileDirectoryCorrectly() throws ParseException {
        String userHost = "user-host";
        String rawKey = "fileDirectoryLatex-" + userHost;
        String rawValue = "C:\\\\Path\\\\To\\\\Latex;";

        MetaDataParser parser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = parser.parse(Map.of(rawKey, rawValue), ',', "userAndHost");

        assertEquals(Optional.of(Path.of("C:\\Path\\To\\Latex")), parsed.getLatexFileDirectory(userHost));
    }
}

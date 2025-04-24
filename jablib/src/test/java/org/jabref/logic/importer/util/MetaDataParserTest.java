package org.jabref.logic.importer.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.MetaDataSerializerTest;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
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
                                .withRequiredFields(UnknownField.fromDisplayName("Test1"), UnknownField.fromDisplayName("Test2")),
                        "jabref-entrytype: test: req[Test1;Test2] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(UnknownField.fromDisplayName("tEST"), UnknownField.fromDisplayName("tEsT2")),
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
    void saveActions() throws Exception {
        Map<String, String> data = Map.of("saveActions", "enabled;title[lower_case]");
        MetaDataParser metaDataParser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = metaDataParser.parse(new MetaData(), data, ',');

        MetaData expected = new MetaData();
        FieldFormatterCleanups fieldFormatterCleanups = new FieldFormatterCleanups(true, List.of(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())));
        expected.setSaveActions(fieldFormatterCleanups);
        assertEquals(expected, parsed);
    }

    /**
     * Verifies that a user-specific .blg path (e.g. blgFilePath-testUser) is correctly parsed from metadata.
     * Ensures that the trailing semicolon (used as separator in .bib metadata) is handled and stripped properly.
     */
    @Test
    void parsesUserSpecificBlgPathSuccessfully() throws Exception {
        String user = "testUser";
        String rawKey = "blgFilePath-" + user;
        String rawValue = "/home/user/test.blg;";

        MetaDataParser parser = new MetaDataParser(new DummyFileUpdateMonitor());
        MetaData parsed = parser.parse(Map.of(rawKey, rawValue), ',');

        assertEquals(Optional.of(Path.of("/home/user/test.blg")), parsed.getBlgFilePath(user));
    }
}

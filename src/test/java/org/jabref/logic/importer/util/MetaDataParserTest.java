package org.jabref.logic.importer.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.MetaDataSerializerTest;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.FieldProperty;
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
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE),
                        "v2-jabref-entrytype: test: req[author;title] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(StandardField.AUTHOR)
                                .withImportantFields(StandardField.TITLE),
                        "v2-jabref-entrytype: test: req[author] opt[title]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(UnknownField.fromDisplayName("Test1"), UnknownField.fromDisplayName("Test2")),
                        "v2-jabref-entrytype: test: req[Test1;Test2] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(UnknownField.fromDisplayName("tEST"), UnknownField.fromDisplayName("tEsT2")),
                        "v2-jabref-entrytype: test: req[tEST;tEsT2] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("person"))
                                .withRequiredFields(new UnknownField("Name", FieldProperty.PERSON_NAMES))
                                .withImportantFields(
                                        new UnknownField("Googlescholar", FieldProperty.EXTERNAL),
                                        new UnknownField("Orcid", FieldProperty.EXTERNAL)
                                ),
                        "v2-jabref-entrytype: person: req[Name|PERSON_NAMES] opt[Googlescholar|EXTERNAL;Orcid|EXTERNAL]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(new UnknownField("custom1", "custom1", FieldProperty.MULTILINE_TEXT, FieldProperty.EXTERNAL)),
                        "v2-jabref-entrytype: test: req[custom1|MULTILINE_TEXT,EXTERNAL] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(new UnknownField("custom2", "custom2")),
                        "v2-jabref-entrytype: test: req[custom2] opt[]"
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
}

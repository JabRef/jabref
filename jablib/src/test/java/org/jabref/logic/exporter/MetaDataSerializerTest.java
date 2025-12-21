package org.jabref.logic.exporter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.os.OS;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
public class MetaDataSerializerTest {

    private static final EntryType CUSTOM_TYPE = new UnknownEntryType("customType");

    private MetaData metaData;
    private GlobalCitationKeyPatterns pattern;
    private BibEntryType newCustomType;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
        pattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                List.of(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT)),
                Set.of());
    }

    @Test
    void serializeNewMetadataReturnsEmptyMap() {
        assertEquals(Map.of(), MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    void serializeSingleSaveAction() {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                List.of(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + OS.NEWLINE + "title[lower_case]" + OS.NEWLINE + ";");
        assertEquals(expectedSerialization, MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    void serializeSingleContentSelectors() {
        List<String> values = List.of(
                "approved",
                "captured",
                "received",
                "status");

        metaData.addContentSelector(new ContentSelector(StandardField.PUBSTATE, values));

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("selector_pubstate", "approved;captured;received;status;");
        assertEquals(expectedSerialization, MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    void parsingEmptyOrFieldsReturnsEmptyCollections() {
        String serialized = MetaDataSerializer.serializeCustomEntryTypes(newCustomType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);
        assertEquals(Set.of(), type.get().getRequiredFields());
    }

    @Test
    void parsingEmptyOptionalFieldsFieldsReturnsEmptyCollections() {
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                Set.of(),
                Set.of(new OrFields(StandardField.AUTHOR)));

        String serialized = MetaDataSerializer.serializeCustomEntryTypes(newCustomType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);
        assertEquals(Set.of(), type.get().getOptionalFields());
    }

    /**
     * Code clone of {@link org.jabref.logic.importer.util.MetaDataParserTest#parseCustomizedEntryType()}
     */
    public static Stream<Arguments> serializeCustomizedEntryType() {
        return Stream.of(
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE),
                        "jabref-entrytype: test: req[author;title] opt[]"
                ),
                Arguments.of(
                        new BibEntryTypeBuilder()
                                .withType(new UnknownEntryType("test"))
                                .withRequiredFields(StandardField.AUTHOR)
                                .withImportantFields(StandardField.TITLE),
                        "jabref-entrytype: test: req[author] opt[title]"
                ),
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
    void serializeCustomizedEntryType(BibEntryTypeBuilder bibEntryTypeBuilder, String expected) {
        assertEquals(expected, MetaDataSerializer.serializeCustomEntryTypes(bibEntryTypeBuilder.build()));
    }

    /**
     * Ensures that user-specific .blg path is correctly serialized
     * to the form: blgFilePath-{username}=/path/to/file.blg;
     */
    @Test
    void serializeUserSpecificBlgPath() {
        String user = "testUser";
        Path blgPath = Path.of("/home/user/test.blg");
        metaData.setBlgFilePath(user, blgPath);

        Map<String, String> serialized = MetaDataSerializer.getSerializedStringMap(metaData, pattern);

        // On Windows, the path separator is a backslash, which is escaped in JabRef (see org.jabref.logic.exporter.MetaDataSerializer.serializeMetaData)
        assertEquals(blgPath.toString().replace("\\", "\\\\") + ";", serialized.get("blgFilePath-" + user));
    }
}

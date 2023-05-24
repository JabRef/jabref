package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataSerializerTest {

    private static final EntryType CUSTOM_TYPE = new UnknownEntryType("customType");

    private MetaData metaData;
    private GlobalCitationKeyPattern pattern;
    private BibEntryType newCustomType;

    @BeforeEach
    public void setUp() {
        metaData = new MetaData();
        pattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                List.of(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT)),
                Collections.emptySet());
    }

    @Test
    public void serializeNewMetadataReturnsEmptyMap() {
        assertEquals(Collections.emptyMap(), MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    public void serializeSingleSaveAction() {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + OS.NEWLINE + "title[lower_case]" + OS.NEWLINE + ";");
        assertEquals(expectedSerialization, MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    public void serializeSingleContentSelectors() {
        List<String> values = new ArrayList<>(4);
        values.add("approved");
        values.add("captured");
        values.add("received");
        values.add("status");

        metaData.addContentSelector(new ContentSelector(StandardField.PUBSTATE, values));

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("selector_pubstate", "approved;captured;received;status;");
        assertEquals(expectedSerialization, MetaDataSerializer.getSerializedStringMap(metaData, pattern));
    }

    @Test
    void testParsingEmptyOrFieldsReturnsEmptyCollections() {
        String serialized = MetaDataSerializer.serializeCustomEntryTypes(newCustomType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);
        assertEquals(Collections.emptySet(), type.get().getRequiredFields());
    }

    @Test
    void testParsingEmptyOptionalFieldsFieldsReturnsEmptyCollections() {
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                Collections.emptySet(),
                Collections.singleton(new OrFields(StandardField.AUTHOR)));

        String serialized = MetaDataSerializer.serializeCustomEntryTypes(newCustomType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);
        assertEquals(Collections.emptySet(), type.get().getOptionalFields());
    }
}

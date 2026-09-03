package org.jabref.logic.exporter;

import java.net.URISyntaxException;
import java.nio.file.Path;

import org.jabref.logic.importer.fileformat.HayagrivaMapping;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HayagrivaEntryWriterTest {

    private final HayagrivaEntryWriter writer = new HayagrivaEntryWriter();
    private final YAMLMapper mapper = new YAMLMapper();

    private ObjectNode parseEntryNode(String yaml) {
        return (ObjectNode) mapper.readTree(yaml).get("key");
    }

    /// The upstream fixture exercises every construct the mapping knows: merging an unchanged
    /// entry back into its node must not touch the node.
    @Test
    void mergingUnchangedFixtureEntriesIsIdentity() throws URISyntaxException {
        Path fixture = Path.of(HayagrivaEntryWriterTest.class.getResource("/org/jabref/logic/importer/fileformat/basic.yml").toURI());
        JsonNode root = mapper.readTree(fixture.toFile());

        ObjectNode merged = mapper.createObjectNode();
        root.properties().forEach(property -> {
            ObjectNode node = (ObjectNode) property.getValue();
            merged.set(property.getKey(), writer.mergeIntoNode(HayagrivaMapping.toBibEntry(property.getKey(), node), node.deepCopy()));
        });

        assertEquals(root, merged);
    }

    @Test
    void mergePreservesUnknownKeysWhileUpdatingChangedField() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    tongus: 2
                    note: old note
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.NOTE, "new note");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    tongus: 2
                    note: new note
                """), node);
    }

    @Test
    void mergeKeepsStructuredTitleFormWhenTitleChanges() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title:
                        value: Long Original Title
                        short: Short Title
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.TITLE, "Corrected Title");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title:
                        value: Corrected Title
                        short: Short Title
                """), node);
    }

    @Test
    void mergeKeepsStructuredPersonFormWhenUnchanged() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    author:
                        - name: "Mädje"
                          given-name: "Laurenz"
                          alias: "laurmaedje"
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.NOTE, "unrelated change");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    author:
                        - name: "Mädje"
                          given-name: "Laurenz"
                          alias: "laurmaedje"
                    note: unrelated change
                """), node);
    }

    @Test
    void mergeRemovesKeyOfClearedField() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    note: to be removed
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.clearField(StandardField.NOTE);

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                """), node);
    }

    @Test
    void mergeKeepsEquivalentTypeString() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: post
                    title: Some Title
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.NOTE, "unrelated change");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: post
                    title: Some Title
                    note: unrelated change
                """), node);
    }

    @Test
    void mergePreservesForeignSerialNumberSchemes() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    serial-number:
                        isbn: "978-0747551003"
                        serial: "3"
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.DOI, "10.1000/example");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    serial-number:
                        isbn: "978-0747551003"
                        serial: "3"
                        doi: 10.1000/example
                """), node);
    }

    @Test
    void mergePreservesBareSerialNumberAsExplicitSerialWhenIdentifierIsAdded() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: reference
                    title: Some Title
                    serial-number: RFC 2845
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.DOI, "10.1000/example");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: reference
                    title: Some Title
                    serial-number:
                        serial: RFC 2845
                        doi: 10.1000/example
                """), node);
    }

    @Test
    void mergeRemovesSerialFallbackWhenNumberIsCleared() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: reference
                    title: Some Title
                    serial-number: RFC 2845
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.clearField(StandardField.NUMBER);

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: reference
                    title: Some Title
                """), node);
    }

    @Test
    void mergeWritesCommentAndPerUserComment() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.COMMENT, "shared comment");
        entry.setField(new UserSpecificCommentField("koppor"), "per-user comment");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    comment: shared comment
                    comment-koppor: per-user comment
                """), node);
    }

    @Test
    void mergeRemovesClearedComments() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    comment: to be removed
                    comment-koppor: also to be removed
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.clearField(StandardField.COMMENT);
        entry.clearField(new UserSpecificCommentField("koppor"));

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                """), node);
    }

    @Test
    void freshWriteSynthesizesDateFromYearAndMonth() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.YEAR, "1020")
                .withField(StandardField.MONTH, "may");

        ObjectNode node = writer.toEntryNode(entry);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    date: "1020-05"
                """), node);
    }

    @Test
    void mergeKeepsDateWhenYearAliasIsUnchanged() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    date: 2020
                """);
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.YEAR, "2020");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    date: 2020
                """), node);
    }

    @Test
    void freshWriteRelocatesJournalDetailsIntoParent() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.JOURNAL, "Some Journal")
                .withField(StandardField.VOLUME, "13")
                .withField(StandardField.NUMBER, "3");

        ObjectNode node = writer.toEntryNode(entry);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    parent:
                        type: periodical
                        title: Some Journal
                        volume: "13"
                        issue: "3"
                """), node);
    }

    @Test
    void freshWriteNestsSeriesUnderBooktitleParent() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.BOOKTITLE, "Some Proceedings")
                .withField(StandardField.SERIES, "LNCS");

        ObjectNode node = writer.toEntryNode(entry);

        assertEquals(parseEntryNode("""
                key:
                    type: conference
                    title: Some Title
                    parent:
                        type: proceedings
                        title: Some Proceedings
                        parent:
                            type: book
                            title: LNCS
                """), node);
    }

    @Test
    void mergeLeavesParentUntouchedWhenSourceFieldsUnchanged() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    parent:
                        type: periodical
                        title:
                            value: Physical Review B
                            verbatim: true
                        volume: 102
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.NOTE, "unrelated change");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    parent:
                        type: periodical
                        title:
                            value: Physical Review B
                            verbatim: true
                        volume: 102
                    note: unrelated change
                """), node);
    }

    /// A top-level `volume` wins over the parent's on import, so leaving it in place is what
    /// keeps the entry stable; removing it would surface the parent's different value.
    @Test
    void mergeKeepsUnchangedTopLevelVolumeNextToPeriodicalParent() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    volume: 1-2
                    parent:
                        type: periodical
                        title: Some Journal
                        volume: 61-62
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.setField(StandardField.NOTE, "unrelated change");

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    volume: 1-2
                    parent:
                        type: periodical
                        title: Some Journal
                        volume: 61-62
                    note: unrelated change
                """), node);
    }

    @Test
    void mergeMovesUnchangedJournalDetailsToTopLevelWhenJournalIsCleared() {
        ObjectNode node = parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    parent:
                        type: periodical
                        title: Some Journal
                        volume: "13"
                        issue: "3"
                """);
        BibEntry entry = HayagrivaMapping.toBibEntry("key", node);
        entry.clearField(StandardField.JOURNAL);

        writer.mergeIntoNode(entry, node);

        assertEquals(parseEntryNode("""
                key:
                    type: article
                    title: Some Title
                    volume: "13"
                    issue: "3"
                """), node);
    }
}

package org.jabref.logic.exporter;

import org.jabref.logic.importer.fileformat.HayagrivaMapping;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HayagrivaEntryWriterTest {

    private final HayagrivaEntryWriter writer = new HayagrivaEntryWriter();
    private final YAMLMapper mapper = new YAMLMapper();

    private ObjectNode parseEntryNode(String yaml) {
        return (ObjectNode) mapper.readTree(yaml).get("key");
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

        assertEquals("new note", node.get("note").asString());
        assertEquals(2, node.get("tongus").asInt());
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

        assertEquals("Corrected Title", node.get("title").get("value").asString());
        assertEquals("Short Title", node.get("title").get("short").asString());
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

        assertEquals("laurmaedje", node.get("author").get(0).get("alias").asString());
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

        assertFalse(node.has("note"));
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

        assertEquals("post", node.get("type").asString());
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

        assertEquals("10.1000/example", node.get("serial-number").get("doi").asString());
        assertEquals("978-0747551003", node.get("serial-number").get("isbn").asString());
        assertEquals("3", node.get("serial-number").get("serial").asString());
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

        assertEquals("RFC 2845", node.get("serial-number").get("serial").asString());
        assertEquals("10.1000/example", node.get("serial-number").get("doi").asString());
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

        assertFalse(node.has("serial-number"));
    }

    @Test
    void freshWriteRelocatesJournalDetailsIntoParent() {
        BibEntry entry = new BibEntry(org.jabref.model.entry.types.StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.JOURNAL, "Some Journal")
                .withField(StandardField.VOLUME, "13")
                .withField(StandardField.NUMBER, "3");

        ObjectNode node = writer.toEntryNode(entry);

        assertFalse(node.has("volume"));
        assertFalse(node.has("issue"));
        assertEquals("13", node.get("parent").get("volume").asString());
        assertEquals("3", node.get("parent").get("issue").asString());
        assertEquals("Some Journal", node.get("parent").get("title").asString());
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

        assertTrue(node.get("parent").get("title").get("verbatim").asBoolean());
    }
}

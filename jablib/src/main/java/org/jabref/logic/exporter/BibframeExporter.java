package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;

/// Exports the Work and Instance subset of BIBFRAME 2.0 as striped RDF/XML.
// [impl->req~export.bibframe.rdfxml~1]
@NullMarked
public class BibframeExporter extends Exporter {
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String BF = "http://id.loc.gov/ontologies/bibframe/";
    private static final String BFLC = "http://id.loc.gov/ontologies/bflc/";
    private static final String RESOURCE_BASE = "https://jabref.org/bibframe/sha256/";

    public BibframeExporter() {
        super("bibframe", "BIBFRAME 2.0 RDF/XML", StandardFileType.RDF);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws SaveException {
        try (Writer output = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            if (entries.isEmpty()) {
                return;
            }
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            try {
                writer.writeStartDocument("UTF-8", "1.0");
                start(writer, "rdf", "RDF", RDF);
                writer.writeNamespace("rdf", RDF);
                writer.writeNamespace("rdfs", RDFS);
                writer.writeNamespace("bf", BF);
                writer.writeNamespace("bflc", BFLC);

                Map<String, Integer> occurrences = new HashMap<>();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                for (BibEntry entry : entries) {
                    String hash = contentHash(entry, digest);
                    int occurrence = occurrences.merge(hash, 1, Integer::sum);
                    String stem = RESOURCE_BASE + hash + (occurrence == 1 ? "" : "-" + occurrence);
                    writeEntry(writer, entry, stem + "#Work", stem + "#Instance");
                }
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } finally {
                writer.close();
            }
        } catch (IOException | NoSuchAlgorithmException | XMLStreamException e) {
            throw new SaveException(e);
        }
    }

    private static String contentHash(BibEntry entry, MessageDigest digest) {
        StringJoiner content = new StringJoiner("");
        appendCanonical(content, "type", entry.getType().getName());
        entry.getFieldMap().entrySet().stream()
             .filter(field -> InternalField.KEY_FIELD != field.getKey())
             .sorted(Comparator.comparing(field -> field.getKey().getName()))
             .forEach(field -> appendCanonical(content, field.getKey().getName(), field.getValue()));
        return HexFormat.of().formatHex(digest.digest(content.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static void appendCanonical(StringJoiner content, String name, String value) {
        content.add(name.length() + ":" + name + value.length() + ":" + value);
    }

    private static void writeEntry(XMLStreamWriter writer, BibEntry entry, String workUri, String instanceUri) throws XMLStreamException {
        start(writer, "bf", "Work", BF);
        attribute(writer, "rdf", RDF, "about", workUri);
        resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Text");
        if (entry.getType() == StandardEntryType.Book) {
            resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Monograph");
        }
        writeContributors(writer, entry, StandardField.AUTHOR, "aut");
        writeContributors(writer, entry, StandardField.EDITOR, "edt");
        writeTitle(writer, entry);
        Optional<String> language = entry.getField(StandardField.LANGUAGE);
        if (language.isPresent()) {
            resource(writer, "bf", "language", BF, "rdf", RDF,
                    "http://id.loc.gov/vocabulary/languages/" + language.orElseThrow());
        }
        writeSummary(writer, entry.getField(StandardField.ABSTRACT));
        writeHost(writer, entry);
        resource(writer, "bf", "hasInstance", BF, "rdf", RDF, instanceUri);
        writer.writeEndElement();

        start(writer, "bf", "Instance", BF);
        attribute(writer, "rdf", RDF, "about", instanceUri);
        resource(writer, "bf", "issuance", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/issuance/mono");
        if (entry.getField(StandardField.URL).isPresent()) {
            resource(writer, "bf", "media", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/mediaTypes/c");
        }
        writeTitle(writer, entry);
        writePublication(writer, entry);
        writeIdentifier(writer, entry, StandardField.ISBN, "Isbn");
        writeIdentifier(writer, entry, StandardField.DOI, "Doi");
        if (entry.getType() != StandardEntryType.Article) {
            writeIdentifier(writer, entry, StandardField.ISSN, "Issn");
        }
        Optional<String> url = entry.getField(StandardField.URL);
        if (url.isPresent()) {
            resource(writer, "bf", "electronicLocator", BF, "rdf", RDF, url.orElseThrow());
        }
        resource(writer, "bf", "instanceOf", BF, "rdf", RDF, workUri);
        writer.writeEndElement();
    }

    private static void writeTitle(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        Optional<String> title = entry.getField(StandardField.TITLE);
        if (title.isEmpty()) {
            return;
        }
        start(writer, "bf", "title", BF);
        start(writer, "bf", "Title", BF);
        literal(writer, "bf", "mainTitle", BF, title.orElseThrow());
        optionalLiteral(writer, "bf", "subtitle", BF, entry.getField(StandardField.SUBTITLE));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeContributors(XMLStreamWriter writer, BibEntry entry, StandardField field, String role) throws XMLStreamException {
        if (entry.getField(field).isEmpty()) {
            return;
        }
        for (var author : AuthorList.parse(entry.getField(field).orElseThrow()).getAuthors()) {
            start(writer, "bf", "contribution", BF);
            start(writer, "bf", "Contribution", BF);
            if (field == StandardField.AUTHOR) {
                resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "PrimaryContribution");
            }
            start(writer, "bf", "agent", BF);
            start(writer, "bf", "Agent", BF);
            resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Person");
            literal(writer, "rdfs", "label", RDFS, author.getFamilyGiven(false));
            writer.writeEndElement();
            writer.writeEndElement();
            start(writer, "bf", "role", BF);
            start(writer, "bf", "Role", BF);
            literal(writer, "bf", "code", BF, role);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private static void writeSummary(XMLStreamWriter writer, Optional<String> abstractText) throws XMLStreamException {
        if (abstractText.isPresent()) {
            start(writer, "bf", "summary", BF);
            start(writer, "bf", "Summary", BF);
            literal(writer, "rdfs", "label", RDFS, abstractText.orElseThrow());
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private static void writePublication(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        Optional<String> place = entry.getField(StandardField.ADDRESS);
        Optional<String> publisher = entry.getField(StandardField.PUBLISHER);
        Optional<String> date = entry.getField(StandardField.YEAR).or(() -> entry.getField(StandardField.DATE));
        if (place.isEmpty() && publisher.isEmpty() && date.isEmpty()) {
            return;
        }
        start(writer, "bf", "provisionActivity", BF);
        start(writer, "bf", "ProvisionActivity", BF);
        resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Publication");
        optionalLiteral(writer, "bflc", "simplePlace", BFLC, place);
        optionalLiteral(writer, "bflc", "simpleAgent", BFLC, publisher);
        optionalLiteral(writer, "bflc", "simpleDate", BFLC, date);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeIdentifier(XMLStreamWriter writer, BibEntry entry, StandardField field, String type) throws XMLStreamException {
        if (entry.getField(field).isEmpty()) {
            return;
        }
        start(writer, "bf", "identifiedBy", BF);
        start(writer, "bf", type, BF);
        literal(writer, "rdf", "value", RDF, entry.getField(field).orElseThrow());
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeHost(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        if (entry.getType() != StandardEntryType.Article || entry.getField(StandardField.JOURNAL).isEmpty()) {
            return;
        }
        start(writer, "bf", "relation", BF);
        start(writer, "bf", "Relation", BF);
        resource(writer, "bf", "relationship", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/relationship/partof");
        start(writer, "bf", "associatedResource", BF);
        start(writer, "bf", "Work", BF);
        start(writer, "bf", "title", BF);
        start(writer, "bf", "Title", BF);
        literal(writer, "bf", "mainTitle", BF, entry.getField(StandardField.JOURNAL).orElseThrow());
        writer.writeEndElement();
        writer.writeEndElement();
        writeIdentifier(writer, entry, StandardField.ISSN, "Issn");
        if (entry.getField(StandardField.PAGES).isPresent()) {
            start(writer, "bf", "hasInstance", BF);
            start(writer, "bf", "Instance", BF);
            literal(writer, "bf", "part", BF, "pages:" + entry.getField(StandardField.PAGES).orElseThrow());
            writer.writeEndElement();
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void start(XMLStreamWriter writer, String prefix, String localName, String namespace) throws XMLStreamException {
        writer.writeStartElement(prefix, localName, namespace);
    }

    private static void literal(XMLStreamWriter writer, String prefix, String localName, String namespace, String value) throws XMLStreamException {
        start(writer, prefix, localName, namespace);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    private static void optionalLiteral(XMLStreamWriter writer, String prefix, String localName, String namespace,
                                        Optional<String> value) throws XMLStreamException {
        if (value.isPresent()) {
            literal(writer, prefix, localName, namespace, value.orElseThrow());
        }
    }

    private static void resource(XMLStreamWriter writer, String prefix, String localName, String namespace,
                                 String attributePrefix, String attributeNamespace, String value) throws XMLStreamException {
        start(writer, prefix, localName, namespace);
        attribute(writer, attributePrefix, attributeNamespace, "resource", value);
        writer.writeEndElement();
    }

    private static void attribute(XMLStreamWriter writer, String prefix, String namespace, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(prefix, namespace, localName, value);
    }
}

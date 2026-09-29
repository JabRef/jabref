package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Exports the Work and Instance subset of BIBFRAME 2.0 as striped RDF/XML.
/// See the [BIBFRAME ontology](https://github.com/lcnetdev/bibframe-ontology/blob/main/bibframe.rdf).
// [impl->req~export.bibframe.rdfxml~1]
@NullMarked
public class BibframeExporter extends Exporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibframeExporter.class);
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String BF = "http://id.loc.gov/ontologies/bibframe/";
    private static final String BFLC = "http://id.loc.gov/ontologies/bflc/";
    private static final String RESOURCE_BASE = "https://jabref.org/bibframe/sha256/";
    private static final List<StandardField> EXPORTED_FIELDS = List.of(StandardField.TITLE, StandardField.SUBTITLE,
            StandardField.AUTHOR, StandardField.EDITOR, StandardField.LANGUAGE, StandardField.ABSTRACT,
            StandardField.ADDRESS, StandardField.PUBLISHER, StandardField.ISBN, StandardField.ISSN,
            StandardField.DOI, StandardField.URL);
    private static final Map<String, String> MARC_LANGUAGE_ALIASES = Map.ofEntries(
            Map.entry("deu", "ger"), Map.entry("fra", "fre"), Map.entry("nld", "dut"),
            Map.entry("zho", "chi"), Map.entry("ces", "cze"), Map.entry("ell", "gre"),
            Map.entry("ron", "rum"), Map.entry("slk", "slo"), Map.entry("hye", "arm"),
            Map.entry("eus", "baq"), Map.entry("isl", "ice"), Map.entry("mkd", "mac"),
            Map.entry("mri", "mao"), Map.entry("msa", "may"), Map.entry("mya", "bur"),
            Map.entry("fas", "per"), Map.entry("sqi", "alb"), Map.entry("bod", "tib"),
            Map.entry("cym", "wel"), Map.entry("kat", "geo"));

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
            LOGGER.debug("Could not export BIBFRAME RDF/XML", e);
            throw new SaveException(e);
        }
    }

    private static String contentHash(BibEntry entry, MessageDigest digest) {
        StringJoiner content = new StringJoiner("");
        appendCanonical(content, "type", entry.getType().getName());
        EXPORTED_FIELDS.forEach(field -> entry.getField(field)
                                              .ifPresent(value -> appendCanonical(content, field.getName(), value)));
        entry.getField(StandardField.YEAR).or(() -> entry.getField(StandardField.DATE))
             .ifPresent(value -> appendCanonical(content, "publicationDate", value));
        if (entry.getType() == StandardEntryType.Article) {
            entry.getField(StandardField.JOURNAL).ifPresent(value -> appendCanonical(content, "journal", value));
            entry.getField(StandardField.PAGES).ifPresent(value -> appendCanonical(content, "pages", value));
        }
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
        writeLanguage(writer, entry.getField(StandardField.LANGUAGE));
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
        if (entry.getType() != StandardEntryType.Article || entry.getField(StandardField.JOURNAL).isEmpty()) {
            writeIdentifier(writer, entry, StandardField.ISSN, "Issn");
        }
        Optional<String> url = entry.getField(StandardField.URL);
        if (url.isPresent()) {
            resource(writer, "bf", "electronicLocator", BF, "rdf", RDF, url.orElseThrow());
        }
        resource(writer, "bf", "instanceOf", BF, "rdf", RDF, workUri);
        writer.writeEndElement();
    }

    private static void writeLanguage(XMLStreamWriter writer, Optional<String> language) throws XMLStreamException {
        if (language.isEmpty()) {
            return;
        }
        Optional<String> code = languageCode(language.orElseThrow());
        if (code.isPresent()) {
            resource(writer, "bf", "language", BF, "rdf", RDF,
                    "http://id.loc.gov/vocabulary/languages/" + code.orElseThrow());
            return;
        }
        start(writer, "bf", "language", BF);
        start(writer, "bf", "Language", BF);
        literal(writer, "rdfs", "label", RDFS, language.orElseThrow());
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static Optional<String> languageCode(String language) {
        String normalized = language.trim();
        Optional<String> marcCode = MARC_LANGUAGE_ALIASES.values().stream()
                                                         .filter(code -> code.equalsIgnoreCase(normalized)).findFirst();
        if (marcCode.isPresent()) {
            return marcCode;
        }
        return Arrays.stream(Locale.getISOLanguages()).map(Locale::of)
                     .filter(locale -> normalized.equalsIgnoreCase(locale.getLanguage())
                             || normalized.equalsIgnoreCase(locale.getISO3Language())
                             || normalized.equalsIgnoreCase(locale.getDisplayLanguage(Locale.ENGLISH))
                             || normalized.equalsIgnoreCase(locale.getDisplayLanguage(Locale.GERMAN)))
                     .map(Locale::getISO3Language)
                     .map(code -> MARC_LANGUAGE_ALIASES.getOrDefault(code, code))
                     .findFirst();
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
        if (date.filter(value -> value.length() == 4 && value.chars().allMatch(Character::isDigit)).isPresent()) {
            start(writer, "bf", "date", BF);
            attribute(writer, "rdf", RDF, "datatype", "http://id.loc.gov/datatypes/edtf");
            writer.writeCharacters(date.orElseThrow());
            writer.writeEndElement();
        }
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
        if (entry.getType() != StandardEntryType.Article) {
            return;
        }
        start(writer, "bf", "relation", BF);
        start(writer, "bf", "Relation", BF);
        resource(writer, "bf", "relationship", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/relationship/partof");
        start(writer, "bf", "associatedResource", BF);
        start(writer, "bf", "Work", BF);
        resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Serial");
        if (entry.getField(StandardField.JOURNAL).isPresent()) {
            start(writer, "bf", "title", BF);
            start(writer, "bf", "Title", BF);
            literal(writer, "bf", "mainTitle", BF, entry.getField(StandardField.JOURNAL).orElseThrow());
            writer.writeEndElement();
            writer.writeEndElement();
        }
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

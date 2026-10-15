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
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.LatexToUnicodeAdapter;

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
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final List<StandardField> EXPORTED_FIELDS = List.of(StandardField.TITLE, StandardField.SUBTITLE,
            StandardField.AUTHOR, StandardField.EDITOR, StandardField.LANGUAGE, StandardField.ABSTRACT,
            StandardField.ADDRESS, StandardField.PUBLISHER, StandardField.ISBN, StandardField.ISSN,
            StandardField.DOI, StandardField.URL, StandardField.EDITION, StandardField.SERIES,
            StandardField.PAGETOTAL);
    private static final Map<String, String> MARC_LANGUAGE_ALIASES = Map.ofEntries(
            Map.entry("deu", "ger"), Map.entry("fra", "fre"), Map.entry("nld", "dut"),
            Map.entry("zho", "chi"), Map.entry("ces", "cze"), Map.entry("ell", "gre"),
            Map.entry("ron", "rum"), Map.entry("slk", "slo"), Map.entry("hye", "arm"),
            Map.entry("eus", "baq"), Map.entry("isl", "ice"), Map.entry("mkd", "mac"),
            Map.entry("mri", "mao"), Map.entry("msa", "may"), Map.entry("mya", "bur"),
            Map.entry("fas", "per"), Map.entry("sqi", "alb"), Map.entry("bod", "tib"),
            Map.entry("cym", "wel"), Map.entry("kat", "geo")); // spellchecker:disable-line

    public BibframeExporter() {
        super("bibframe", "BIBFRAME 2.0 RDF/XML", StandardFileType.RDF);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws SaveException {
        try (Writer output = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            XMLStreamWriter writer;
            synchronized (XML_OUTPUT_FACTORY) {
                writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(output);
            }
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
        entry.getFiles().stream().filter(file -> isOnlineUrl(file.getLink())).forEach(file -> {
            appendCanonical(content, "file", file.getLink());
            appendCanonical(content, "fileDescription", file.getDescription());
            appendCanonical(content, "fileType", file.getFileType());
        });
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
        List<LinkedFile> onlineFiles = entry.getFiles().stream()
                                            .filter(file -> isOnlineUrl(file.getLink())).toList();
        start(writer, "bf", "Work", BF);
        attribute(writer, "rdf", RDF, "about", workUri);
        resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Text");
        if (entry.getType() == StandardEntryType.Book) {
            resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Monograph");
        }
        writeContributors(writer, entry, StandardField.AUTHOR, "aut");
        writeContributors(writer, entry, StandardField.EDITOR, "edt");
        writeTitle(writer, entry);
        writeLanguage(writer, entry);
        writeSummary(writer, entry);
        writeHost(writer, entry);
        for (int index = 0; index < onlineFiles.size(); index++) {
            resource(writer, "bf", "hasInstance", BF, "rdf", RDF, instanceUri + "-File-" + index);
        }
        resource(writer, "bf", "hasInstance", BF, "rdf", RDF, instanceUri);
        writer.writeEndElement();

        start(writer, "bf", "Instance", BF);
        attribute(writer, "rdf", RDF, "about", instanceUri);
        if (entry.getType() == StandardEntryType.Book) {
            resource(writer, "bf", "issuance", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/issuance/mono");
        }
        String url = entry.getField(StandardField.URL).orElse("");
        if (!url.isBlank()) {
            resource(writer, "bf", "media", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/mediaTypes/c");
        }
        writeTitle(writer, entry);
        writePublication(writer, entry);
        optionalLiteral(writer, "bf", "editionStatement", BF, entry, StandardField.EDITION);
        optionalLiteral(writer, "bf", "seriesStatement", BF, entry, StandardField.SERIES);
        String pageTotal = entry.getField(StandardField.PAGETOTAL).orElse("");
        if (!pageTotal.isBlank()) {
            start(writer, "bf", "extent", BF);
            start(writer, "bf", "Extent", BF);
            literal(writer, "rdfs", "label", RDFS, pageTotal + " pages");
            writer.writeEndElement();
            writer.writeEndElement();
        }
        writeIdentifier(writer, entry, StandardField.ISBN, "Isbn");
        writeIdentifier(writer, entry, StandardField.DOI, "Doi");
        if (entry.getType() != StandardEntryType.Article || entry.getField(StandardField.JOURNAL).isEmpty()) {
            writeIdentifier(writer, entry, StandardField.ISSN, "Issn");
        }
        if (!url.isBlank()) {
            resource(writer, "bf", "electronicLocator", BF, "rdf", RDF, url);
        }
        resource(writer, "bf", "instanceOf", BF, "rdf", RDF, workUri);
        writer.writeEndElement();

        writeLinkedFiles(writer, onlineFiles, workUri, instanceUri);
    }

    private static void writeLanguage(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        String language = entry.getField(StandardField.LANGUAGE).orElse("");
        if (language.isBlank()) {
            return;
        }
        String code = languageCode(language).orElse("");
        if (!code.isBlank()) {
            resource(writer, "bf", "language", BF, "rdf", RDF,
                    "http://id.loc.gov/vocabulary/languages/" + code);
            return;
        }
        start(writer, "bf", "language", BF);
        start(writer, "bf", "Language", BF);
        literal(writer, "rdfs", "label", RDFS, language);
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
        String title = entry.getField(StandardField.TITLE).orElse("");
        if (title.isBlank()) {
            return;
        }
        start(writer, "bf", "title", BF);
        start(writer, "bf", "Title", BF);
        literal(writer, "bf", "mainTitle", BF, title);
        optionalLiteral(writer, "bf", "subtitle", BF, entry, StandardField.SUBTITLE);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeContributors(XMLStreamWriter writer, BibEntry entry, StandardField field, String role) throws XMLStreamException {
        String nameField = entry.getField(field).orElse("");
        if (nameField.isBlank()) {
            return;
        }
        var authors = AuthorList.parse(nameField).getAuthors();
        // AuthorList drops the trailing hyphen from open-ended authority dates such as "1963-".
        List<String> names = authors.size() == 1 && nameField.endsWith("-")
                             ? List.of(Author.FORMATTER.format(nameField))
                             : authors.stream().map(author -> author.getFamilyGiven(false)).toList();
        for (String name : names) {
            start(writer, "bf", "contribution", BF);
            start(writer, "bf", "Contribution", BF);
            if (field == StandardField.AUTHOR) {
                resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "PrimaryContribution");
            }
            start(writer, "bf", "agent", BF);
            start(writer, "bf", "Agent", BF);
            resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Person");
            literal(writer, "rdfs", "label", RDFS, name);
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

    private static void writeSummary(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        String abstractText = entry.getField(StandardField.ABSTRACT).orElse("");
        if (!abstractText.isBlank()) {
            start(writer, "bf", "summary", BF);
            start(writer, "bf", "Summary", BF);
            literal(writer, "rdfs", "label", RDFS, abstractText);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private static void writePublication(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        String place = entry.getField(StandardField.ADDRESS).orElse("");
        String publisher = entry.getField(StandardField.PUBLISHER).orElse("");
        String date = entry.getField(StandardField.YEAR).or(() -> entry.getField(StandardField.DATE)).orElse("");
        if (place.isBlank() && publisher.isBlank() && date.isBlank()) {
            return;
        }
        start(writer, "bf", "provisionActivity", BF);
        start(writer, "bf", "ProvisionActivity", BF);
        resource(writer, "rdf", "type", RDF, "rdf", RDF, BF + "Publication");
        optionalLiteral(writer, "bflc", "simplePlace", BFLC, entry, StandardField.ADDRESS);
        optionalLiteral(writer, "bflc", "simpleAgent", BFLC, entry, StandardField.PUBLISHER);
        if (!date.isBlank()) {
            literal(writer, "bflc", "simpleDate", BFLC, date);
        }
        if (date.length() == 4 && date.chars().allMatch(Character::isDigit)) {
            start(writer, "bf", "date", BF);
            attribute(writer, "rdf", RDF, "datatype", "http://id.loc.gov/datatypes/edtf");
            writer.writeCharacters(date);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeIdentifier(XMLStreamWriter writer, BibEntry entry, StandardField field, String type) throws XMLStreamException {
        String value = entry.getField(field).orElse("");
        if (value.isBlank()) {
            return;
        }
        start(writer, "bf", "identifiedBy", BF);
        start(writer, "bf", type, BF);
        literal(writer, "rdf", "value", RDF, value);
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
        String journal = entry.getField(StandardField.JOURNAL).orElse("");
        if (!journal.isBlank()) {
            start(writer, "bf", "title", BF);
            start(writer, "bf", "Title", BF);
            literal(writer, "bf", "mainTitle", BF, journal);
            writer.writeEndElement();
            writer.writeEndElement();
        }
        writeIdentifier(writer, entry, StandardField.ISSN, "Issn");
        String pages = entry.getField(StandardField.PAGES).orElse("");
        if (!pages.isBlank()) {
            start(writer, "bf", "hasInstance", BF);
            start(writer, "bf", "Instance", BF);
            literal(writer, "bf", "part", BF, "pages:" + pages);
            writer.writeEndElement();
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeLinkedFiles(XMLStreamWriter writer, List<LinkedFile> onlineFiles, String workUri, String instanceUri) throws XMLStreamException {
        for (int index = 0; index < onlineFiles.size(); index++) {
            LinkedFile file = onlineFiles.get(index);
            start(writer, "bf", "Instance", BF);
            attribute(writer, "rdf", RDF, "about", instanceUri + "-File-" + index);
            resource(writer, "rdf", "type", RDF, "rdf", RDF, BFLC + "SecondaryInstance");
            resource(writer, "bf", "media", BF, "rdf", RDF, "http://id.loc.gov/vocabulary/mediaTypes/c");
            String title = file.getDescription();
            if (!title.isBlank()) {
                start(writer, "bf", "title", BF);
                start(writer, "bf", "Title", BF);
                literal(writer, "bf", "mainTitle", BF, title);
                writer.writeEndElement();
                writer.writeEndElement();
            }
            resource(writer, "bf", "electronicLocator", BF, "rdf", RDF, file.getLink());
            resource(writer, "bf", "instanceOf", BF, "rdf", RDF, workUri);
            writer.writeEndElement();
        }
    }

    private static boolean isOnlineUrl(String uri) {
        return uri.startsWith("https://") || uri.startsWith("http://");
    }

    private static void start(XMLStreamWriter writer, String prefix, String localName, String namespace) throws XMLStreamException {
        writer.writeStartElement(prefix, localName, namespace);
    }

    private static void literal(XMLStreamWriter writer, String prefix, String localName, String namespace, String value) throws XMLStreamException {
        start(writer, prefix, localName, namespace);
        writer.writeCharacters(LatexToUnicodeAdapter.format(value));
        writer.writeEndElement();
    }

    private static void optionalLiteral(XMLStreamWriter writer, String prefix, String localName, String namespace,
                                        BibEntry entry, StandardField field) throws XMLStreamException {
        String value = entry.getField(field).orElse("");
        if (!value.isBlank()) {
            literal(writer, prefix, localName, namespace, value);
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

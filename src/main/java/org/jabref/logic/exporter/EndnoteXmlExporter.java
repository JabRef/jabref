package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.BibEntryPreferences;

public class EndnoteXmlExporter extends Exporter {

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private record EndNoteType(String name, Integer number) {
    }

    private static final Map<EntryType, EndNoteType> ENTRY_TYPE_MAPPING = new HashMap<>();

    static {
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Article, new EndNoteType("Journal Article", 1));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Book, new EndNoteType("Book", 2));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InBook, new EndNoteType("Book Section", 3));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InCollection, new EndNoteType("Book Section", 4));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Proceedings, new EndNoteType("Conference Proceedings", 5));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.MastersThesis, new EndNoteType("Thesis", 6));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.PhdThesis, new EndNoteType("Thesis", 7));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.TechReport, new EndNoteType("Report", 8));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Unpublished, new EndNoteType("Manuscript", 9));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InProceedings, new EndNoteType("Conference Paper", 10));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Conference, new EndNoteType("Conference", 11));
        ENTRY_TYPE_MAPPING.put(IEEETranEntryType.Patent, new EndNoteType("Patent", 12));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Online, new EndNoteType("Web Page", 13));
        ENTRY_TYPE_MAPPING.put(IEEETranEntryType.Electronic, new EndNoteType("Electronic Article", 14));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Misc, new EndNoteType("Generic", 15));
    }

    // Contains the mapping of all fields not explicitly handled by mapX methods
    // We need a fixed order here, so we use a SequencedMap
    private static final SequencedMap<Field, String> STANDARD_FIELD_MAPPING = new LinkedHashMap<>();

    static {
        STANDARD_FIELD_MAPPING.put(StandardField.PAGES, "pages");
        STANDARD_FIELD_MAPPING.put(StandardField.VOLUME, "volume");
        STANDARD_FIELD_MAPPING.put(StandardField.PUBLISHER, "publisher");
        STANDARD_FIELD_MAPPING.put(StandardField.ISBN, "isbn");
        STANDARD_FIELD_MAPPING.put(StandardField.DOI, "electronic-resource-num");
        STANDARD_FIELD_MAPPING.put(StandardField.ABSTRACT, "abstract");
        STANDARD_FIELD_MAPPING.put(StandardField.BOOKTITLE, "secondary-title");
        STANDARD_FIELD_MAPPING.put(StandardField.EDITION, "edition");
        STANDARD_FIELD_MAPPING.put(StandardField.SERIES, "tertiary-title");
        STANDARD_FIELD_MAPPING.put(StandardField.NUMBER, "number");
        STANDARD_FIELD_MAPPING.put(StandardField.ISSUE, "issue");
        STANDARD_FIELD_MAPPING.put(StandardField.LOCATION, "pub-location");
        STANDARD_FIELD_MAPPING.put(StandardField.CHAPTER, "section");
        STANDARD_FIELD_MAPPING.put(StandardField.HOWPUBLISHED, "work-type");
        STANDARD_FIELD_MAPPING.put(StandardField.ISSN, "issn");
        STANDARD_FIELD_MAPPING.put(StandardField.ADDRESS, "auth-address");
        STANDARD_FIELD_MAPPING.put(StandardField.PAGETOTAL, "page-total");
        STANDARD_FIELD_MAPPING.put(StandardField.NOTE, "notes");
        STANDARD_FIELD_MAPPING.put(StandardField.LABEL, "label");
        STANDARD_FIELD_MAPPING.put(StandardField.LANGUAGE, "language");
        STANDARD_FIELD_MAPPING.put(StandardField.KEY, "foreign-keys");
        STANDARD_FIELD_MAPPING.put(new UnknownField("accession-num"), "accession-num");
    }

    private static final EndNoteType DEFAULT_TYPE = new EndNoteType("Generic", 15);

    private final BibEntryPreferences bibEntryPreferences;

    public EndnoteXmlExporter(BibEntryPreferences bibEntryPreferences) {
        super("endnote", "EndNote XML", StandardFileType.XML);
        this.bibEntryPreferences = bibEntryPreferences;
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws IOException, XMLStreamException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        XMLStreamWriter writer = OUTPUT_FACTORY.createXMLStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8.name());

        try {
            writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            writer.writeStartElement("xml");
            writer.writeStartElement("records");

            for (BibEntry entry : entries) {
                writeRecord(writer, entry, databaseContext);
            }

            writer.writeEndElement(); // records
            writer.writeEndElement(); // xml
            writer.writeEndDocument();
        } finally {
            writer.close();
        }
    }

    private void writeRecord(XMLStreamWriter writer, BibEntry entry, BibDatabaseContext databaseContext) throws XMLStreamException {
        writer.writeStartElement("record");

        writeEntryType(writer, entry);
        writeMetaInformation(writer, databaseContext);
        writeAuthorAndEditor(writer, entry);
        writeTitle(writer, entry);
        writeJournalTitle(writer, entry);
        writeKeywords(writer, databaseContext.getDatabase(), entry);
        writeDates(writer, entry);
        writeUrls(writer, entry);

        for (Map.Entry<Field, String> fieldMapping : STANDARD_FIELD_MAPPING.entrySet()) {
            Field field = fieldMapping.getKey();
            String xmlElement = fieldMapping.getValue();

            entry.getField(field).ifPresent(value -> {
                try {
                    writer.writeStartElement(xmlElement);
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        writer.writeEndElement(); // record
    }

    private void writeEntryType(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        EntryType entryType = entry.getType();
        EndNoteType endNoteType = ENTRY_TYPE_MAPPING.getOrDefault(entryType, DEFAULT_TYPE);
        writer.writeStartElement("ref-type");
        writer.writeAttribute("name", endNoteType.name());
        writer.writeCharacters(endNoteType.number().toString());
        writer.writeEndElement();
    }

    private void writeMetaInformation(XMLStreamWriter writer, BibDatabaseContext databaseContext) throws XMLStreamException {
        writer.writeStartElement("database");
        writer.writeAttribute("name", "MyLibrary");
        String name = databaseContext.getDatabasePath().map(Path::getFileName).map(Path::toString).orElse("MyLibrary");
        writer.writeCharacters(name);
        writer.writeEndElement();

        writer.writeStartElement("source-app");
        writer.writeAttribute("name", "JabRef");
        writer.writeCharacters("JabRef");
        writer.writeEndElement();
    }

    private void writeAuthorAndEditor(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        writer.writeStartElement("contributors");
        entry.getField(StandardField.AUTHOR).ifPresent(authors -> {
            try {
                addPersons(writer, authors, "authors");
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        entry.getField(StandardField.EDITOR).ifPresent(editors -> {
            try {
                addPersons(writer, editors, "secondary-authors");
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        writer.writeEndElement(); // contributors
    }

    private void addPersons(XMLStreamWriter writer, String authors, String wrapTagName) throws XMLStreamException {
        writer.writeStartElement(wrapTagName);
        AuthorList parsedPersons = AuthorList.parse(authors).latexFree();
        for (Author person : parsedPersons) {
            writer.writeStartElement("author");
            writer.writeCharacters(person.getFamilyGiven(false));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeTitle(XMLStreamWriter writer, BibEntry entry) {
        entry.getFieldOrAlias(StandardField.TITLE).ifPresent(title -> {
            try {
                writer.writeStartElement("titles");
                writer.writeStartElement("title");
                writer.writeCharacters(title);
                writer.writeEndElement();

                entry.getField(new UnknownField("alt-title")).ifPresent(altTitle -> {
                    try {
                        writer.writeStartElement("alt-title");
                        writer.writeCharacters(altTitle);
                        writer.writeEndElement();
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                });

                entry.getField(StandardField.BOOKTITLE).ifPresent(secondaryTitle -> {
                    try {
                        writer.writeStartElement("secondary-title");
                        writer.writeCharacters(secondaryTitle);
                        writer.writeEndElement();
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                });

                writer.writeEndElement(); // titles
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeJournalTitle(XMLStreamWriter writer, BibEntry entry) {
        entry.getFieldOrAlias(StandardField.JOURNAL).ifPresent(journalTitle -> {
            try {
                writer.writeStartElement("periodical");
                writer.writeStartElement("full-title");
                writer.writeCharacters(journalTitle);
                writer.writeEndElement();
                writer.writeEndElement(); // periodical
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeKeywords(XMLStreamWriter writer, BibDatabase bibDatabase, BibEntry entry) {
        entry.getFieldOrAlias(StandardField.KEYWORDS).ifPresent(keywords -> {
            try {
                writer.writeStartElement("keywords");
                entry.getResolvedKeywords(bibEntryPreferences.getKeywordSeparator(), bibDatabase).forEach(keyword -> {
                    try {
                        writer.writeStartElement("keyword");
                        // Hierarchical keywords are separated by the '>' character. See {@link } for details.
                        writer.writeCharacters(keyword.get());
                        writer.writeEndElement();
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                });
                writer.writeEndElement(); // keywords
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeUrls(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        writer.writeStartElement("urls");

        entry.getFieldOrAlias(StandardField.FILE).ifPresent(fileField -> {
            try {
                writer.writeStartElement("pdf-urls");
                writer.writeStartElement("url");
                writer.writeCharacters(fileField);
                writer.writeEndElement();
                writer.writeEndElement(); // pdf-urls
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });

        entry.getFieldOrAlias(StandardField.URL).ifPresent(url -> {
            try {
                writer.writeStartElement("web-urls");
                writer.writeStartElement("url");
                writer.writeCharacters(url);
                writer.writeEndElement();
                writer.writeEndElement(); // web-urls
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });

        writer.writeEndElement(); // urls
    }

    private void writeDates(XMLStreamWriter writer, BibEntry entry) throws XMLStreamException {
        writer.writeStartElement("dates");
        entry.getFieldOrAlias(StandardField.YEAR).ifPresent(year -> {
            try {
                writer.writeStartElement("year");
                writer.writeCharacters(year);
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        entry.getFieldOrAlias(StandardField.MONTH).ifPresent(month -> {
            try {
                writer.writeStartElement("month");
                writer.writeCharacters(month);
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        entry.getFieldOrAlias(StandardField.DAY).ifPresent(day -> {
            try {
                writer.writeStartElement("day");
                writer.writeCharacters(day);
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        // We need to use getField here - getFieldOrAlias for Date tries to convert year, month, and day to a date, which we do not want
        entry.getField(StandardField.DATE).ifPresent(date -> {
            try {
                writer.writeStartElement("pub-dates");
                writer.writeStartElement("date");
                writer.writeCharacters(date);
                writer.writeEndElement();
                writer.writeEndElement(); // pub-dates
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
        writer.writeEndElement(); // dates
    }
}

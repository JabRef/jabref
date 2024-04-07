package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class EndnoteXmlExporter extends Exporter {

    private static final String ENDNOTE_XML_VERSION = "20.1";
    private static final String ENDNOTE_NAMESPACE_URI = "http://www.endnote.com/ns/ENdnx";
    private static final Map<EntryType, String> EXPORT_ITEM_TYPE = Map.ofEntries(
            Map.entry(StandardEntryType.Article, "Journal Article"),
            Map.entry(StandardEntryType.Book, "Book"),
            Map.entry(StandardEntryType.InBook, "Book Section"),
            Map.entry(StandardEntryType.InProceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.InCollection, "Book Section"),
            Map.entry(StandardEntryType.MastersThesis, "Thesis"),
            Map.entry(StandardEntryType.PhdThesis, "Thesis"),
            Map.entry(StandardEntryType.Proceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.TechReport, "Report"),
            Map.entry(StandardEntryType.Unpublished, "Manuscript"),
            Map.entry(IEEETranEntryType.Patent, "Patent"),
            Map.entry(StandardEntryType.Online, "Web Page"),
            Map.entry(IEEETranEntryType.Electronic, "Electronic Article"),
            Map.entry(StandardEntryType.Article, "Newspaper Article")
    );
    private static final Map<EntryType, String> EXPORT_REF_NUMBER = EXPORT_ITEM_TYPE.entrySet().stream()
                                                                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> Integer.toString(EXPORT_ITEM_TYPE.entrySet().stream()
                                                                                                                                                                           .map(Map.Entry::getValue)
                                                                                                                                                                           .collect(Collectors.toList())
                                                                                                                                                                           .indexOf(entry.getValue()) + 1)));

    public EndnoteXmlExporter() {
        super("endnote", "EndNote XML", StandardFileType.XML);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            XMLOutputFactory xof = XMLOutputFactory.newFactory();
            XMLStreamWriter xml = xof.createXMLStreamWriter(writer);
            xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            xml.writeStartElement("xml");
            xml.writeStartElement("records");

            for (BibEntry entry : entries) {
                writeEntry(entry, xml);
            }

            xml.writeEndElement(); // records
            xml.writeEndElement(); // xml
            xml.writeEndDocument();
            xml.close();
        } catch (
                IOException |
                XMLStreamException e) {
            throw new SaveException(e);
        }
    }

    private void writeEntry(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("record");

        writeField(xml, "database", "endnote.enl");
        writeField(xml, "source-app", "JabRef", Map.of("name", "JabRef", "version", ENDNOTE_XML_VERSION));
        writeField(xml, "ref-type", EXPORT_REF_NUMBER.getOrDefault(entry.getType(), "Generic"), Map.of("name", EXPORT_ITEM_TYPE.getOrDefault(entry.getType(), "Generic")));

        writeContributors(entry, xml);
        writeTitles(entry, xml);
        writeField(xml, "periodical", entry.getField(StandardField.JOURNAL).orElse(""));
        writeField(xml, "pages", entry.getField(StandardField.PAGES).orElse(""));
        writeField(xml, "volume", entry.getField(StandardField.VOLUME).orElse(""));
        writeField(xml, "number", entry.getField(StandardField.NUMBER).orElse(""));
        writeField(xml, "issue", entry.getField(StandardField.ISSUE).orElse(""));
        writeField(xml, "keywords", entry.getField(StandardField.KEYWORDS).orElse(""));
        writeDates(entry, xml);
        writeField(xml, "pub-location", entry.getField(StandardField.ADDRESS).orElse(""));
        writeField(xml, "publisher", entry.getField(StandardField.PUBLISHER).orElse(""));
        writeField(xml, "isbn", entry.getField(StandardField.ISBN).orElse(""));
        writeField(xml, "electronic-resource-num", entry.getField(StandardField.DOI).orElse(""));
        writeField(xml, "abstract", entry.getField(StandardField.ABSTRACT).orElse(""));
        writeField(xml, "label", entry.getCitationKey().orElse(""));
        writeField(xml, "notes", entry.getField(StandardField.NOTE).orElse(""));
        writeUrls(entry, xml);

        xml.writeEndElement(); // record
    }

    private void writeField(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }

    private void writeField(XMLStreamWriter xml, String name, String value, Map<String, String> attributes) throws XMLStreamException {
        xml.writeStartElement(name);
        attributes.forEach((attr, attrValue) -> {
            try {
                xml.writeAttribute(attr, attrValue);
            } catch (
                    XMLStreamException e) {
                // Ignore exception and continue
            }
        });
        xml.writeCharacters(value);
        xml.writeEndElement();
    }

    private void writeContributors(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("contributors");
        writeAuthors(entry, xml, StandardField.AUTHOR, "authors");
        writeAuthors(entry, xml, StandardField.EDITOR, "secondary-authors");
        xml.writeEndElement(); // contributors
    }

    private void writeAuthors(BibEntry entry, XMLStreamWriter xml, Field field, String elementName) throws XMLStreamException {
        entry.getField(field).ifPresent(authors -> {
            try {
                xml.writeStartElement(elementName);
                for (String author : authors.split("and")) {
                    xml.writeStartElement("author");
                    xml.writeCharacters(author.trim());
                    xml.writeEndElement(); // author
                }
                xml.writeEndElement(); // elementName
            } catch (
                    XMLStreamException e) {
                // Ignore exception and continue
            }
        });
    }

    private void writeTitles(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("titles");
        writeField(xml, "title", entry.getField(StandardField.TITLE).orElse(""));
        writeField(xml, "secondary-title", entry.getField(StandardField.BOOKTITLE).orElse(""));
        writeField(xml, "short-title", entry.getField(StandardField.SHORTTITLE).orElse(""));
        xml.writeEndElement(); // titles
    }

    private void writeDates(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("dates");
        entry.getField(StandardField.YEAR).ifPresent(year -> {
            try {
                xml.writeStartElement("year");
                xml.writeCharacters(year);
                xml.writeEndElement(); // year
            } catch (
                    XMLStreamException e) {
                // Ignore exception and continue
            }
        });
        xml.writeEndElement(); // dates
    }

    private void writeUrls(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("urls");
        entry.getField(StandardField.URL).ifPresent(url -> {
            try {
                xml.writeStartElement("web-urls");
                xml.writeStartElement("url");
                xml.writeCharacters(url);
                xml.writeEndElement(); // url
                xml.writeEndElement(); // web-urls
            } catch (
                    XMLStreamException e) {
                // Ignore exception and continue
            }
        });
        xml.writeEndElement(); // urls
    }
}

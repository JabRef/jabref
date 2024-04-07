package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
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
            Map.entry(StandardEntryType.InCollection, "Book Section"),
            Map.entry(StandardEntryType.InProceedings, "Conference Paper"),
            Map.entry(StandardEntryType.Conference, "Conference"),
            Map.entry(StandardEntryType.MastersThesis, "Thesis"),
            Map.entry(StandardEntryType.PhdThesis, "Thesis"),
            Map.entry(StandardEntryType.Proceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.TechReport, "Report"),
            Map.entry(StandardEntryType.Unpublished, "Manuscript"),
            Map.entry(IEEETranEntryType.Patent, "Patent"),
            Map.entry(StandardEntryType.Online, "Web Page"),
            Map.entry(IEEETranEntryType.Electronic, "Electronic Article"),
            Map.entry(StandardEntryType.Misc, "Generic")
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
        } catch (IOException | XMLStreamException e) {
            throw new SaveException(e);
        }
    }

    private void writeEntry(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("record");

        // Write the necessary fields and elements
        writeField(xml, "database", "endnote.enl", Map.of("name", "My EndNote Library.enl", "path", "/path/to/My EndNote Library.enl"));
        writeField(xml, "source-app", "JabRef", Map.of("name", "JabRef", "version", ENDNOTE_XML_VERSION));
        writeField(xml, "rec-number", String.valueOf(entry.getId()), null);

        xml.writeStartElement("foreign-keys");
        xml.writeStartElement("key");
        xml.writeAttribute("app", "EN");
        xml.writeCharacters(String.valueOf(entry.getId()));
        xml.writeEndElement(); // key
        xml.writeEndElement(); // foreign-keys

        writeField(xml, "ref-type", EXPORT_REF_NUMBER.getOrDefault(entry.getType(), "Generic"), Map.of("name", EXPORT_ITEM_TYPE.getOrDefault(entry.getType(), "Generic")));

        writeContributors(entry, xml);
        writeField(xml, "titles", null, Map.of(), entry.getField(StandardField.TITLE).orElse(""), "title");
        writeField(xml, "periodical", null, Map.of(), entry.getField(StandardField.JOURNAL).orElse(""), "full-title");
        writeField(xml, "tertiary-title", entry.getField(StandardField.BOOKTITLE).orElse(""), null);
        writeField(xml, "pages", entry.getField(StandardField.PAGES).orElse(""), null);
        writeField(xml, "volume", entry.getField(StandardField.VOLUME).orElse(""), null);
        writeField(xml, "number", entry.getField(StandardField.NUMBER).orElse(""), null);
        writeField(xml, "dates", null, Map.of(), entry.getField(StandardField.YEAR).orElse(""), "year");
        writeField(xml, "publisher", entry.getField(StandardField.PUBLISHER).orElse(""), null);
        writeField(xml, "isbn", entry.getField(StandardField.ISBN).orElse(""), null);
        writeField(xml, "abstract", entry.getField(StandardField.ABSTRACT).orElse(""), null);
        writeField(xml, "notes", entry.getField(StandardField.NOTE).orElse(""), null);
        writeField(xml, "urls", null, Map.of(), entry.getField(StandardField.URL).orElse(""), "web-urls");
        writeField(xml, "electronic-resource-num", entry.getField(StandardField.DOI).orElse(""), null);

        xml.writeEndElement(); // record
    }

    private void writeField(XMLStreamWriter xml, String name, String value, Map<String, String> attributes) throws XMLStreamException {
        if (value != null && !value.isEmpty()) {
            xml.writeStartElement(name);
            if (attributes != null) {
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    xml.writeAttribute(attribute.getKey(), attribute.getValue());
                }
            }
            xml.writeStartElement("style");
            xml.writeAttribute("face", "normal");
            xml.writeAttribute("font", "default");
            xml.writeAttribute("size", "100%");
            xml.writeCharacters(value);
            xml.writeEndElement(); // style
            xml.writeEndElement(); // name
        }
    }

    private void writeField(XMLStreamWriter xml, String name, String value, Map<String, String> attributes, String childValue, String childElementName) throws XMLStreamException {
        xml.writeStartElement(name);
        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                xml.writeAttribute(attribute.getKey(), attribute.getValue());
            }
        }
        if (childValue != null && !childValue.isEmpty()) {
            xml.writeStartElement(childElementName);
            xml.writeStartElement("style");
            xml.writeAttribute("face", "normal");
            xml.writeAttribute("font", "default");
            xml.writeAttribute("size", "100%");
            xml.writeCharacters(childValue);
            xml.writeEndElement(); // style
            xml.writeEndElement(); // childElementName
        }
        xml.writeEndElement(); // name
    }

    private void writeContributors(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        Optional<String> authors = entry.getField(StandardField.AUTHOR);
        if (authors.isPresent()) {
            xml.writeStartElement("contributors");
            xml.writeStartElement("authors");
            for (String author : authors.get().split("and")) {
                xml.writeStartElement("author");
                xml.writeStartElement("style");
                xml.writeAttribute("face", "normal");
                xml.writeAttribute("font", "default");
                xml.writeAttribute("size", "100%");
                xml.writeCharacters(author.trim());
                xml.writeEndElement(); // style
                xml.writeEndElement(); // author
            }
            xml.writeEndElement(); // authors
            xml.writeEndElement(); // contributors
        }
    }

    private void writeDates(BibEntry entry, XMLStreamWriter xml) throws XMLStreamException {
        Optional<String> year = entry.getField(StandardField.YEAR);
        if (year.isPresent()) {
            xml.writeStartElement("dates");
            xml.writeStartElement("year");
            xml.writeCharacters(year.get());
            xml.writeEndElement(); // year
            xml.writeEndElement(); // dates
        }
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
            } catch (XMLStreamException e) {
                // Ignore exception and continue
            }
        });
        entry.getField(StandardField.FILE).ifPresent(file -> {
            try {
                xml.writeStartElement("file-urls");
                xml.writeStartElement("url");
                xml.writeCharacters(file);
                xml.writeEndElement(); // url
                xml.writeEndElement(); // file-urls
            } catch (XMLStreamException e) {
                // Ignore exception and continue
            }
        });
        xml.writeEndElement(); // urls
    }
}

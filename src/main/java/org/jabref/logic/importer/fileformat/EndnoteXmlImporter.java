package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndnoteXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndnoteXmlImporter.class);
    private static final Map<EntryType, String> ENTRY_TYPE_MAPPING = Map.ofEntries(
            Map.entry(StandardEntryType.Article, "Journal Article"),
            Map.entry(StandardEntryType.Book, "Book"),
            Map.entry(StandardEntryType.InBook, "Book Section"),
            Map.entry(StandardEntryType.InCollection, "Book Section"),
            Map.entry(StandardEntryType.Proceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.MastersThesis, "Thesis"),
            Map.entry(StandardEntryType.PhdThesis, "Thesis"),
            Map.entry(StandardEntryType.TechReport, "Report"),
            Map.entry(StandardEntryType.Unpublished, "Manuscript"),
            Map.entry(StandardEntryType.InProceedings, "Conference Paper"),
            Map.entry(StandardEntryType.Conference, "Conference"),
            Map.entry(IEEETranEntryType.Patent, "Patent"),
            Map.entry(StandardEntryType.Online, "Web Page"),
            Map.entry(IEEETranEntryType.Electronic, "Electronic Article"),
            Map.entry(StandardEntryType.Misc, "Generic")
    );

    private static final Map<Field, String> FIELD_MAPPING = Map.ofEntries(
            Map.entry(StandardField.TITLE, "title"),
            Map.entry(StandardField.AUTHOR, "authors"),
            Map.entry(StandardField.EDITOR, "secondary-authors"),
            Map.entry(StandardField.YEAR, "year"),
            Map.entry(StandardField.MONTH, "pub-dates"),
            Map.entry(StandardField.JOURNAL, "full-title"),
            Map.entry(StandardField.JOURNALTITLE, "full-title"),
            Map.entry(StandardField.BOOKTITLE, "secondary-title"),
            Map.entry(StandardField.EDITION, "edition"),
            Map.entry(StandardField.SERIES, "tertiary-title"),
            Map.entry(StandardField.VOLUME, "volume"),
            Map.entry(StandardField.NUMBER, "number"),
            Map.entry(StandardField.ISSUE, "issue"),
            Map.entry(StandardField.PAGES, "pages"),
            Map.entry(StandardField.PUBLISHER, "publisher"),
            Map.entry(StandardField.ADDRESS, "pub-location"),
            Map.entry(StandardField.CHAPTER, "section"),
            Map.entry(StandardField.HOWPUBLISHED, "work-type"),
            Map.entry(StandardField.INSTITUTION, "publisher"),
            Map.entry(StandardField.ORGANIZATION, "publisher"),
            Map.entry(StandardField.SCHOOL, "publisher"),
            Map.entry(StandardField.ISBN, "isbn"),
            Map.entry(StandardField.ISSN, "isbn"),
            Map.entry(StandardField.DOI, "electronic-resource-num"),
            Map.entry(StandardField.URL, "web-urls"),
            Map.entry(StandardField.FILE, "pdf-urls"),
            Map.entry(StandardField.ABSTRACT, "abstract"),
            Map.entry(StandardField.KEYWORDS, "keywords")
    );
    private final ImportFormatPreferences preferences;
    private final XMLInputFactory xmlInputFactory;

    public EndnoteXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
        xmlInputFactory = XMLInputFactory.newInstance();
    }

    @Override
    public String getName() {
        return "EndNote XML";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public String getId() {
        return "endnote";
    }

    @Override
    public String getDescription() {
        return "Importer for the EndNote XML format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase(Locale.ENGLISH).contains("<records>")) {
                return true;
            }
            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                reader.next();
                if (isStartElement(reader, "record")) {
                    BibEntry entry = parseRecord(reader);
                    bibItems.add(entry);
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
    }

    private BibEntry parseRecord(XMLStreamReader reader) throws XMLStreamException {
        BibEntry entry = new BibEntry();

        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "record")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "ref-type" -> {
                        String refType = reader.getAttributeValue(null, "name");
                        EntryType entryType = ENTRY_TYPE_MAPPING.entrySet().stream()
                                                                .filter(e -> e.getValue().equals(refType))
                                                                .map(Map.Entry::getKey)
                                                                .findFirst()
                                                                .orElse(StandardEntryType.Misc);
                        entry.setType(entryType);
                    }
                    case "contributors" -> {
                        parseContributors(reader, entry);
                    }
                    default -> {
                        Field field = FIELD_MAPPING.entrySet().stream()
                                                   .filter(e -> e.getValue().equals(elementName))
                                                   .map(Map.Entry::getKey)
                                                   .findFirst()
                                                   .orElse(null);
                        if (field != null) {
                            entry.setField(field, reader.getElementText());
                        }
                    }
                }
            }
        }

        return entry;
    }

    private void parseContributors(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "contributors")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "authors" -> {
                        entry.setField(StandardField.AUTHOR, reader.getElementText());
                    }
                    case "secondary-authors" -> {
                        entry.setField(StandardField.EDITOR, reader.getElementText());
                    }
                }
            }
        }
    }

    private boolean isStartElement(XMLStreamReader reader, String elementName) {
        return isStartElement(reader) && reader.getName().getLocalPart().equals(elementName);
    }

    private boolean isStartElement(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.START_ELEMENT;
    }

    private boolean isEndElement(XMLStreamReader reader, String elementName) {
        return isEndElement(reader) && reader.getName().getLocalPart().equals(elementName);
    }

    private boolean isEndElement(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.END_ELEMENT;
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            return importDatabase(
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }
}

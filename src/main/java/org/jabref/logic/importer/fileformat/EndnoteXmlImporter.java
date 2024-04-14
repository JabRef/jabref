package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndnoteXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndnoteXmlImporter.class);
    private static final Map<String, EntryType> ENTRY_TYPE_MAPPING = Map.ofEntries(
            Map.entry("Journal Article", StandardEntryType.Article),
            Map.entry("Book", StandardEntryType.Book),
            Map.entry("Book Section", StandardEntryType.InBook),
            Map.entry("Conference Proceedings", StandardEntryType.Proceedings),
            Map.entry("Thesis", StandardEntryType.Thesis),
            Map.entry("Report", StandardEntryType.Report),
            Map.entry("Manuscript", StandardEntryType.Unpublished),
            Map.entry("Conference Paper", StandardEntryType.InProceedings),
            Map.entry("Conference", StandardEntryType.Conference),
            Map.entry("Patent", IEEETranEntryType.Patent),
            Map.entry("Web Page", StandardEntryType.Online),
            Map.entry("Electronic Article", IEEETranEntryType.Electronic),
            Map.entry("Generic", StandardEntryType.Misc)
    );

    private static final Map<String, Field> FIELD_MAPPING = Map.ofEntries(
            Map.entry("title", StandardField.TITLE),
            Map.entry("authors", StandardField.AUTHOR),
            Map.entry("secondary-authors", StandardField.EDITOR),
            Map.entry("pages", StandardField.PAGES),
            Map.entry("volume", StandardField.VOLUME),
            Map.entry("keywords", StandardField.KEYWORDS),
            Map.entry("year", StandardField.YEAR),
            Map.entry("pub-dates", StandardField.DATE),
            Map.entry("publisher", StandardField.PUBLISHER),
            Map.entry("isbn", StandardField.ISBN),
            Map.entry("electronic-resource-num", StandardField.DOI),
            Map.entry("abstract", StandardField.ABSTRACT),
            Map.entry("web-urls", StandardField.URL),
            Map.entry("pdf-urls", StandardField.FILE),
            Map.entry("full-title", StandardField.JOURNAL),
            Map.entry("secondary-title", StandardField.BOOKTITLE),
            Map.entry("edition", StandardField.EDITION),
            Map.entry("tertiary-title", StandardField.SERIES),
            Map.entry("number", StandardField.NUMBER),
            Map.entry("issue", StandardField.ISSUE),
            Map.entry("pub-location", StandardField.ADDRESS),
            Map.entry("section", StandardField.CHAPTER),
            Map.entry("work-type", StandardField.HOWPUBLISHED)
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
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("<xml>") && line.trim().contains("<records>")) {
                return true;
            }
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
            LOGGER.debug("Could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
    }

    private BibEntry parseRecord(XMLStreamReader reader) throws XMLStreamException {
        BibEntry entry = new BibEntry();
        Map<Field, String> fields = new HashMap<>();
        KeywordList keywordList = new KeywordList();
        List<LinkedFile> linkedFiles = new ArrayList<>();

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
                        EntryType entryType = ENTRY_TYPE_MAPPING.get(refType);
                        if (entryType != null) {
                            entry.setType(entryType);
                        }
                    }
                    case "contributors" -> {
                        parseContributors(reader, fields);
                    }
                    case "titles" -> {
                        parseTitles(reader, fields);
                    }
                    case "keywords" -> {
                        parseKeywords(reader, keywordList);
                    }
                    case "urls" -> {
                        parseUrls(reader, fields, linkedFiles);
                    }
                    default -> {
                        Field field = FIELD_MAPPING.get(elementName);
                        if (field != null) {
                            fields.put(field, reader.getElementText());
                        } else {
                            fields.put(new UnknownField(elementName), reader.getElementText());
                        }
                    }
                }
            }
        }

        entry.setField(fields);
        entry.putKeywords(keywordList, preferences.bibEntryPreferences().getKeywordSeparator());
        entry.setFiles(linkedFiles);
        return entry;
    }

    private void parseContributors(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "contributors")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "authors" -> {
                        fields.put(StandardField.AUTHOR, parseAuthors(reader));
                    }
                    case "secondary-authors" -> {
                        fields.put(StandardField.EDITOR, parseAuthors(reader));
                    }
                }
            }
        }
    }

    private String parseAuthors(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder authors = new StringBuilder();
        while (reader.hasNext()) {
            reader.next();
            if (isStartElement(reader, "author")) {
                authors.append(reader.getElementText());
                authors.append(" and ");
            } else if (isEndElement(reader) && reader.getName().getLocalPart().equals("authors")) {
                break;
            }
        }
        if (authors.length() > 0) {
            authors.delete(authors.length() - 5, authors.length()); // Remove trailing " and "
        }
        return authors.toString();
    }

    private void parseTitles(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "titles")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "title" -> {
                        fields.put(StandardField.TITLE, reader.getElementText());
                    }
                    case "secondary-title" -> {
                        fields.put(StandardField.JOURNAL, reader.getElementText());
                    }
                }
            }
        }
    }

    private void parseKeywords(XMLStreamReader reader, KeywordList keywordList) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "keywords")) {
                break;
            }

            if (isStartElement(reader, "keyword")) {
                keywordList.add(reader.getElementText());
            }
        }
    }

    private void parseUrls(XMLStreamReader reader, Map<Field, String> fields, List<LinkedFile> linkedFiles) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "urls")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "web-urls" -> {
                        fields.put(StandardField.URL, reader.getElementText());
                    }
                    case "pdf-urls" -> {
                        String urlText = reader.getElementText();
                        try {
                            linkedFiles.add(new LinkedFile(new URL(urlText), "PDF"));
                        } catch (MalformedURLException e) {
                            LOGGER.info("Unable to parse URL: {}", urlText);
                        }
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
            return importDatabase(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }
}

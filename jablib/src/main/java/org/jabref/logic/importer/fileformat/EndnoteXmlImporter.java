package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;
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
            Map.entry(StandardField.BOOKTITLE, "secondary-title"),
            Map.entry(StandardField.EDITION, "edition"),
            Map.entry(StandardField.SERIES, "tertiary-title"),
            Map.entry(StandardField.VOLUME, "volume"),
            Map.entry(StandardField.NUMBER, "number"),
            Map.entry(StandardField.ISSUE, "issue"),
            Map.entry(StandardField.PAGES, "pages"),
            Map.entry(StandardField.LOCATION, "pub-location"),
            Map.entry(StandardField.CHAPTER, "section"),
            Map.entry(StandardField.HOWPUBLISHED, "work-type"),
            Map.entry(StandardField.PUBLISHER, "publisher"),
            Map.entry(StandardField.ISBN, "isbn"),
            Map.entry(StandardField.ISSN, "issn"),
            Map.entry(StandardField.DOI, "electronic-resource-num"),
            Map.entry(StandardField.URL, "web-urls"),
            Map.entry(StandardField.FILE, "pdf-urls"),
            Map.entry(StandardField.ABSTRACT, "abstract"),
            Map.entry(StandardField.KEYWORDS, "keywords"),
            Map.entry(StandardField.PAGETOTAL, "page-total"),
            Map.entry(StandardField.NOTE, "notes"),
            //  Map.entry(StandardField.LABEL, "label"), // We omit this field
            Map.entry(StandardField.LANGUAGE, "language"),
            // Map.entry(StandardField.KEY, "foreign-keys"),  // We omit this field
            Map.entry(StandardField.ADDRESS, "auth-address")
    );
    private static final UnknownField FIELD_ALT_TITLE = new UnknownField("alt-title");

    private final ImportFormatPreferences preferences;

    private final XMLInputFactory xmlInputFactory;

    public EndnoteXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
        xmlInputFactory = XMLInputFactory.newInstance();

        // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
        // not suported by aalto-xml
        // xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // required for reading Unicode characters such as &#xf6;
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
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
        return Localization.lang("Importer for the EndNote XML format.");
    }

    @Override
    public boolean isRecognizedFormat(@NonNull BufferedReader reader) throws IOException {
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
    public ParserResult importDatabase(@NonNull BufferedReader input) throws IOException {
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
                    case "contributors" ->
                            parseContributors(reader, entry);
                    case "titles" ->
                            parseTitles(reader, entry);
                    case "periodical" ->
                            parsePeriodical(reader, entry);
                    case "keywords" ->
                            parseKeywords(reader, entry);
                    case "urls" ->
                            parseUrls(reader, entry);
                    case "dates" ->
                            parseDates(reader, entry);
                    // TODO: Left for future work -- test files need to be adpated
                    // case "accession-num" -> {
                    //    String accessionNumber = parseElementContent(reader, "accession-num");
                    //    entry.setField(new UnknownField("accession-num"), accessionNumber);
                    // }
                    default -> {
                        Field field = FIELD_MAPPING.entrySet().stream()
                                                   .filter(e -> e.getValue().equals(elementName))
                                                   .map(Map.Entry::getKey)
                                                   .findFirst()
                                                   .orElse(null);
                        if (field != null) {
                            String value = parseElementContent(reader, elementName);
                            entry.setField(field, value);
                        }
                    }
                }
            }
        }

        // Cleanup: Remove alt-title if it matches the journal
        String journalOrBooktitle = entry.getField(StandardField.JOURNAL).or(() -> entry.getField(StandardField.BOOKTITLE)).orElse("");
        if (entry.hasField(FIELD_ALT_TITLE)) {
            String altTitle = entry.getField(FIELD_ALT_TITLE).orElse("");
            if (journalOrBooktitle.equals(altTitle)) {
                entry.clearField(FIELD_ALT_TITLE);
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
            extractPersons(reader, "authors", entry, StandardField.AUTHOR);
            extractPersons(reader, "secondary-authors", entry, StandardField.EDITOR);
        }
    }

    private void extractPersons(XMLStreamReader reader, String elementName, BibEntry entry, StandardField author) throws XMLStreamException {
        if (isStartElement(reader, elementName)) {
            StringJoiner persons = new StringJoiner(" and ");
            while (reader.hasNext()) {
                reader.next();
                if (isEndElement(reader, elementName)) {
                    break;
                }
                if (isStartElement(reader, "author")) {
                    String person = parseElementContent(reader, "author");
                    if (!person.isEmpty()) {
                        persons.add(person);
                    }
                }
            }
            entry.setField(author, persons.toString());
        }
    }

    private void parseTitles(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "titles")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "title" -> {
                        String title = parseElementContent(reader, "title");
                        entry.setField(StandardField.TITLE, title);
                    }
                    case "secondary-title" -> {
                        String secondaryTitle = parseElementContent(reader, "secondary-title");
                        if (entry.getType().equals(StandardEntryType.Article)) {
                            entry.setField(StandardField.JOURNAL, secondaryTitle);
                        } else {
                            entry.setField(StandardField.BOOKTITLE, secondaryTitle);
                        }
                    }
                    case "alt-title" -> {
                        String altTitle = parseElementContent(reader, "alt-title");
                        entry.setField(FIELD_ALT_TITLE, altTitle);
                    }
                }
            }
        }
    }

    private void parsePeriodical(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "periodical")) {
                break;
            }

            if (isStartElement(reader)) {
                parseJournalOrBookTitle(reader, entry);
            }
        }
    }

    private void parseJournalOrBookTitle(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        String elementName = reader.getName().getLocalPart();
        switch (elementName) {
            case "full-title",
                 "abbr-2",
                 "abbr-1",
                 "abbr-3" -> {
                String title = parseElementContent(reader, elementName);
                if (entry.getType().equals(StandardEntryType.Article)) {
                    entry.setField(StandardField.JOURNAL, title);
                } else {
                    entry.setField(StandardField.BOOKTITLE, title);
                }
            }
        }
    }

    private void parseKeywords(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        KeywordList keywordList = new KeywordList();
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "keywords")) {
                break;
            }

            if (isStartElement(reader, "keyword")) {
                String keyword = parseElementContent(reader, "keyword");
                if (!keyword.isEmpty()) {
                    keywordList.add(keyword);
                }
            }
        }
        if (!keywordList.isEmpty()) {
            entry.putKeywords(keywordList, preferences.bibEntryPreferences().getKeywordSeparator());
        }
    }

    private void parseUrls(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "urls")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "web-urls" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isEndElement(reader, "web-urls")) {
                                break;
                            }
                            if (isStartElement(reader, "url")) {
                                String url = parseElementContent(reader, "url");
                                entry.setField(StandardField.URL, url);
                            }
                        }
                    }
                    case "pdf-urls" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isEndElement(reader, "pdf-urls")) {
                                break;
                            }
                            if (isStartElement(reader, "url")) {
                                String file = parseElementContent(reader, "url");
                                entry.setField(StandardField.FILE, file);
                            }
                        }
                    }
                    case "related-urls" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isEndElement(reader, "related-urls")) {
                                break;
                            }
                            if (isStartElement(reader, "url")) {
                                String url = clean(parseElementContent(reader, "url"));
                                entry.setField(StandardField.URL, url);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseDates(XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, "dates")) {
                break;
            }

            if (isStartElement(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "year",
                         "month",
                         "day" -> {
                        String date = parseElementContent(reader, elementName);
                        entry.setField(StandardField.fromName(elementName).get(), date);
                    }
                    case "pub-dates" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isEndElement(reader, "pub-dates")) {
                                break;
                            }
                            if (isStartElement(reader, "date")) {
                                String pubDate = parseElementContent(reader, "date");
                                entry.setField(StandardField.DATE, pubDate);
                            }
                        }
                    }
                }
            }
        }
    }

    private String parseElementContent(XMLStreamReader reader, String elementName) throws XMLStreamException {
        StringBuilder content = new StringBuilder();
        while (reader.hasNext()) {
            reader.next();
            if (isEndElement(reader, elementName)) {
                break;
            }
            if (isStartElement(reader, "style")) {
                content.append(reader.getElementText()).append(" ");
            } else if (reader.getEventType() == XMLEvent.CHARACTERS) {
                content.append(reader.getText());
            }
        }
        return clean(content.toString());
    }

    private String clean(String input) {
        return input.trim().replaceAll("\\s+", " ");
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
            LOGGER.error("Could not import file", e);
        }
        return List.of();
    }
}

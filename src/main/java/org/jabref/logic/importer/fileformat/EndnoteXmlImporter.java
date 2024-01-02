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
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the Endnote XML format.
 * <p>
 * Based on dtd scheme downloaded from Article #122577 in http://kbportal.thomson.com.
 */
public class EndnoteXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndnoteXmlImporter.class);
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

    private static String join(List<String> list, String string) {
        return Joiner.on(string).join(list);
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
                if (isStartXMLEvent(reader)) {
                    String elementName = reader.getName().getLocalPart();
                    if ("record".equals(elementName)) {
                         parseRecord(reader, bibItems, elementName);
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
    }

    private void parseRecord(XMLStreamReader reader, List<BibEntry> bibItems, String startElement)
        throws XMLStreamException {

        Map<Field, String> fields = new HashMap<>();
        EntryType entryType = StandardEntryType.Article;

        KeywordList keywordList = new KeywordList();
        List<LinkedFile> linkedFiles = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "ref-type" -> {
                        String type = reader.getAttributeValue(null, "name");
                        entryType = convertRefNameToType(type);
                    }
                    case "contributors" -> {
                        handleAuthorList(reader, fields, elementName);
                    }
                    case "titles" -> {
                        handleTitles(reader, fields, elementName);
                    }
                    case "pages" -> {
                        parseStyleContent(reader, fields, StandardField.PAGES, elementName);
                    }
                    case "volume" -> {
                        parseStyleContent(reader, fields, StandardField.VOLUME, elementName);
                    }
                    case "number" -> {
                        parseStyleContent(reader, fields, StandardField.NUMBER, elementName);
                    }
                    case "dates" -> {
                        parseYear(reader, fields);
                    }
                    case "notes" -> {
                        parseStyleContent(reader, fields, StandardField.NOTE, elementName);
                    }
                    case "urls" -> {
                       handleUrlList(reader, fields, linkedFiles);
                    }
                    case "keywords" -> {
                        handleKeywordsList(reader, keywordList, elementName);
                    }
                    case "abstract" -> {
                        parseStyleContent(reader, fields, StandardField.ABSTRACT, elementName);
                    }
                    case "isbn" -> {
                        parseStyleContent(reader, fields, StandardField.ISBN, elementName);
                    }
                    case "electronic-resource-num" -> {
                        parseStyleContent(reader, fields, StandardField.DOI, elementName);
                    }
                    case "publisher" -> {
                        parseStyleContent(reader, fields, StandardField.PUBLISHER, elementName);
                    }
                    case "label" -> {
                        parseStyleContent(reader, fields, new UnknownField("endnote-label"), elementName);
                    }
                }
            }
            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        BibEntry entry = new BibEntry(entryType);
        entry.putKeywords(keywordList, preferences.bibEntryPreferences().getKeywordSeparator());

        entry.setField(fields);
        entry.setFiles(linkedFiles);
        bibItems.add(entry);
    }

    private static EntryType convertRefNameToType(String refName) {
        return switch (refName.toLowerCase().trim()) {
            case "artwork", "generic" -> StandardEntryType.Misc;
            case "electronic article" -> IEEETranEntryType.Electronic;
            case "book section" -> StandardEntryType.InBook;
            case "book" -> StandardEntryType.Book;
            case "report" -> StandardEntryType.Report;
            // case "journal article" -> StandardEntryType.Article;
            default -> StandardEntryType.Article;
        };
    }

    private void handleAuthorList(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
        List<String> authorNames = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "author" -> {
                        parseAuthor(reader, authorNames);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
        fields.put(StandardField.AUTHOR, join(authorNames, " and "));
    }

    private void parseAuthor(XMLStreamReader reader, List<String> authorNames) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "style" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            authorNames.add(reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && "author".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void parseStyleContent(XMLStreamReader reader, Map<Field, String> fields, Field field, String elementName) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String tag = reader.getName().getLocalPart();
                if ("style".equals(tag)) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        if ("abstract".equals(elementName) || "electronic-resource-num".equals(elementName) || "notes".equals(elementName)) {
                            putIfValueNotNull(fields, field, reader.getText().trim());
                        } else if ("isbn".equals(elementName) || "secondary-title".equals(elementName)) {
                            putIfValueNotNull(fields, field, clean(reader.getText()));
                        } else {
                            putIfValueNotNull(fields, field, reader.getText());
                        }
                    }
                }
            }
            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(elementName)) {
                break;
            }
        }
    }

    private void parseYear(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "style" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.YEAR, reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && "year".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void handleKeywordsList(XMLStreamReader reader, KeywordList keywordList, String startElement) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "keyword" -> {
                        parseKeyword(reader, keywordList);
                    }
                }
            }
            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
    }

    private void parseKeyword(XMLStreamReader reader, KeywordList keywordList) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "style" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            if (reader.getText() != null) {
                                keywordList.add(reader.getText());
                            }
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && "keyword".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void handleTitles(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "title" -> {
                        List<String> titleStyleContent = new ArrayList<>();
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if ("style".equals(tag)) {
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        if (reader.getText() != null) {
                                            titleStyleContent.add((reader.getText()));
                                        }
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(elementName)) {
                                break;
                            }
                        }
                        putIfValueNotNull(fields, StandardField.TITLE, clean(join(titleStyleContent, "")));
                    }
                    case "secondary-title" -> {
                        parseStyleContent(reader, fields, StandardField.JOURNAL, elementName);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }
    }

    private void handleUrlList(XMLStreamReader reader, Map<Field, String> fields, List<LinkedFile> linkedFiles) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "related-urls" -> {
                        parseRelatedUrls(reader, fields);
                    }
                    case "pdf-urls" -> {
                        parsePdfUrls(reader, fields, linkedFiles);
                    }
                }
            }

            if (isEndXMLEvent(reader) && "urls".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void parseRelatedUrls(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                if ("style".equals(elementName)) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        putIfValueNotNull(fields, StandardField.URL, reader.getText());
                    }
                }
            } else if (isCharacterXMLEvent(reader)) {
                String value = clean(reader.getText());
                if (value.length() > 0) {
                    putIfValueNotNull(fields, StandardField.URL, clean(value));
                }
            }

            if (isEndXMLEvent(reader) && "related-urls".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void parsePdfUrls(XMLStreamReader reader, Map<Field, String> fields, List<LinkedFile> linkedFiles) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                if ("url".equals(elementName)) {
                    reader.next();
                    if (isStartXMLEvent(reader)) {
                        String tagName = reader.getName().getLocalPart();
                        if ("style".equals(tagName)) {
                            reader.next();
                            if (isCharacterXMLEvent(reader)) {
                                try {
                                    linkedFiles.add(new LinkedFile(new URL(reader.getText()), "PDF"));
                                } catch (
                                        MalformedURLException e) {
                                    LOGGER.info("Unable to parse {}", reader.getText());
                                }
                            }
                        }
                    }
                }
            }
            if (isCharacterXMLEvent(reader)) {
                try {
                    linkedFiles.add(new LinkedFile(new URL(reader.getText()), "PDF"));
                } catch (
                        MalformedURLException e) {
                    LOGGER.info("Unable to parse {}", reader.getText());
                }
            }
            if (isEndXMLEvent(reader) && "pdf-urls".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private String clean(String input) {
        return StringUtil.unifyLineBreaks(input, " ")
                         .trim()
                         .replaceAll(" +", " ");
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    private boolean isCharacterXMLEvent(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.CHARACTERS;
    }

    private boolean isStartXMLEvent(XMLStreamReader reader) {
        return reader.getEventType() == XMLEvent.START_ELEMENT;
    }

    private boolean isEndXMLEvent(XMLStreamReader reader) {
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






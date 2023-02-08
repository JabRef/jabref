package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.mods.Identifier;
import org.jabref.logic.importer.fileformat.mods.Name;
import org.jabref.logic.importer.fileformat.mods.RecordInfo;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the MODS format.<br>
 * More details about the format can be found here <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>. <br>
 * The newest xml schema can also be found here <a href="www.loc.gov/standards/mods/mods-schemas.html.">www.loc.gov/standards/mods/mods-schemas.html.</a>.
 */
public class ModsImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModsImporter.class);
    private static final Pattern MODS_PATTERN = Pattern.compile("<mods .*>");

    private final String keywordSeparator;

    public ModsImporter(ImportFormatPreferences importFormatPreferences) {
        keywordSeparator = importFormatPreferences.bibEntryPreferences().getKeywordSeparator() + " ";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.lines().anyMatch(line -> MODS_PATTERN.matcher(line).find());
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        List<BibEntry> bibItems = new ArrayList<>();

        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);

            parseModsCollection(bibItems, reader);
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }

        return new ParserResult(bibItems);
    }

    private void parseModsCollection(List<BibEntry> bibItems, XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader) && reader.getName().getLocalPart().equals("mods")) {
                BibEntry entry = new BibEntry();
                Map<Field, String> fields = new HashMap<>();

                String id = reader.getAttributeValue(null, "ID");
                if (id != null) {
                    entry.setCitationKey(id);
                }

                parseModsGroup(fields, reader, entry);

                entry.setField(fields);
                bibItems.add(entry);
            }
        }
    }

    private void parseModsGroup(Map<Field, String> fields, XMLStreamReader reader, BibEntry entry) throws XMLStreamException {
        // These elements (subject, keywords and authors) can appear more than once,
        // so they are collected in lists
        List<String> notes = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        List<String> authors = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                // check which MODS group has started
                switch (elementName) {
                    case "abstract" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.ABSTRACT, reader.getText());
                        }
                    }
                    case "genre" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            entry.setType(EntryTypeFactory.parse(mapGenre(reader.getText())));
                        }
                    }
                    case "language" -> {
                        parseLanguage(reader, fields);
                    }
                    case "location" -> {
                        parseLocationAndUrl(reader, fields);
                    }
                    case "identifier" -> {
                        String type = reader.getAttributeValue(null, "type");
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            parseIdentifier(fields, new Identifier(type, reader.getText()), entry);
                        }
                    }
                    case "note" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            notes.add(reader.getText());
                        }
                    }
                    case "recordInfo" -> {
                        parseRecordInfo(reader, fields);
                    }
                    case "titleInfo" -> {
                        parseTitle(reader, fields);
                    }
                    case "subject" -> {
                        parseSubject(reader, fields, keywords);
                    }
                    case "originInfo" -> {
                        parseOriginInfo(reader, fields);
                    }
                    case "name" -> {
                        parseName(reader, fields, authors);
                    }
                    case "relatedItem" -> {
                        parseRelatedItem(reader, fields);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("mods")) {
                break;
            }
        }

        putIfListIsNotEmpty(fields, notes, StandardField.NOTE, ", ");
        putIfListIsNotEmpty(fields, keywords, StandardField.KEYWORDS, this.keywordSeparator);
        putIfListIsNotEmpty(fields, authors, StandardField.AUTHOR, " and ");
    }

    /**
     * Parses information from the RelatedModsGroup. It has the same elements as ModsGroup.
     * But information like volume, issue and the pages appear here instead of in the ModsGroup.
     * Also, if there appears a title field, then this indicates that is the name of the journal
     * which the article belongs to.
     */
    private void parseRelatedItem(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                switch (reader.getName().getLocalPart()) {
                    case "title" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.JOURNAL, reader.getText());
                        }
                    }
                    case "detail" -> {
                        handleDetail(reader, fields);
                    }
                    case "extent" -> {
                        handleExtent(reader, fields);
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("relatedItem")) {
                break;
            }
        }
    }

    private void handleExtent(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        String total = "";
        String startPage = "";
        String endPage = "";

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                reader.next();
                switch (elementName) {
                    case "total" -> {
                        if (isCharacterXMLEvent(reader)) {
                            total = reader.getText();
                        }
                    }
                    case "start" -> {
                        if (isCharacterXMLEvent(reader)) {
                            startPage = reader.getText();
                        }
                    }
                    case "end" -> {
                        if (isCharacterXMLEvent(reader)) {
                            endPage = reader.getText();
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("extent")) {
                break;
            }
        }

        if (!total.isBlank()) {
            putIfValueNotNull(fields, StandardField.PAGES, total);
        } else if (!startPage.isBlank()) {
            putIfValueNotNull(fields, StandardField.PAGES, startPage);
            if (!endPage.isBlank()) {
                // if end appears, then there has to be a start page appeared, so get it and put it together with
                // the end page
                fields.put(StandardField.PAGES, startPage + "-" + endPage);
            }
        }
    }

    private void handleDetail(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "type");
        Set<String> detailElementSet = Set.of("number", "caption", "title");

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                if (detailElementSet.contains(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        putIfValueNotNull(fields, FieldFactory.parseField(type), reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("detail")) {
                break;
            }
        }
    }

    private void parseName(XMLStreamReader reader, Map<Field, String> fields, List<String> authors) throws XMLStreamException {
        List<Name> names = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                if (reader.getName().getLocalPart().equals("affiliation")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        putIfValueNotNull(fields, new UnknownField("affiliation"), reader.getText());
                    }
                } else if (reader.getName().getLocalPart().equals("namePart")) {
                    String type = reader.getAttributeValue(null, "type");
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        names.add(new Name(reader.getText(), type));
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("name")) {
                break;
            }
        }

        handleAuthorsInNamePart(names, authors);
    }

    private void parseOriginInfo(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        List<String> places = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "issuance" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("issuance"), reader.getText());
                        }
                    }
                    case "placeTerm" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            appendIfValueNotNullOrBlank(places, reader.getText());
                        }
                    }
                    case "publisher" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.PUBLISHER, reader.getText());
                        }
                    }
                    case "edition" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.EDITION, reader.getText());
                        }
                    }
                    case "dateIssued", "dateCreated", "dateCaptured", "dateModified" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putDate(fields, elementName, reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("originInfo")) {
                break;
            }
        }

        putIfListIsNotEmpty(fields, places, StandardField.ADDRESS, ", ");
    }

    private void parseSubject(XMLStreamReader reader, Map<Field, String> fields, List<String> keywords) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                switch (reader.getName().getLocalPart()) {
                    case "topic" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            keywords.add(reader.getText().trim());
                        }
                    }
                    case "city" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("city"), reader.getText());
                        }
                    }
                    case "country" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, new UnknownField("country"), reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("subject")) {
                break;
            }
        }
    }

    private void parseRecordInfo(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        RecordInfo recordInfoDefinition = new RecordInfo();
        List<String> recordContents = recordInfoDefinition.recordContents();
        List<String> languages = recordInfoDefinition.languages();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                if (RecordInfo.elementNameSet.contains(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        recordContents.add(0, reader.getText());
                    }
                } else if (reader.getName().getLocalPart().equals("languageTerm")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        languages.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("recordInfo")) {
                break;
            }
        }

        for (String recordContent : recordContents) {
            putIfValueNotNull(fields, new UnknownField("source"), recordContent);
        }
        putIfListIsNotEmpty(fields, languages, StandardField.LANGUAGE, ", ");
    }

    private void parseLanguage(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader) && reader.getName().getLocalPart().equals("languageTerm")) {
                reader.next();
                if (isCharacterXMLEvent(reader)) {
                    putIfValueNotNull(fields, StandardField.LANGUAGE, reader.getText());
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("language")) {
                break;
            }
        }
    }

    private void parseTitle(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader) && reader.getName().getLocalPart().equals("title")) {
                reader.next();
                if (isCharacterXMLEvent(reader)) {
                    putIfValueNotNull(fields, StandardField.TITLE, reader.getText());
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("titleInfo")) {
                break;
            }
        }
    }

    private void parseLocationAndUrl(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        List<String> locations = new ArrayList<>();
        List<String> urls = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                if (reader.getName().getLocalPart().equals("physicalLocation")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        locations.add(reader.getText());
                    }
                } else if (reader.getName().getLocalPart().equals("url")) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        urls.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("location")) {
                break;
            }
        }

        putIfListIsNotEmpty(fields, locations, StandardField.LOCATION, ", ");
        putIfListIsNotEmpty(fields, urls, StandardField.URL, ", ");
    }

    private String mapGenre(String genre) {
        return switch (genre.toLowerCase(Locale.ROOT)) {
            case "conference publication" -> "proceedings";
            case "database" -> "dataset";
            case "yearbook", "handbook" -> "book";
            case "law report or digest", "technical report", "reporting" -> "report";
            default -> genre;
        };
    }

    private void parseIdentifier(Map<Field, String> fields, Identifier identifier, BibEntry entry) {
        String type = identifier.type();
        if ("citekey".equals(type) && entry.getCitationKey().isEmpty()) {
            entry.setCitationKey(identifier.value());
        } else if (!"local".equals(type) && !"citekey".equals(type)) {
            // put all identifiers (doi, issn, isbn,...) except of local and citekey
            putIfValueNotNull(fields, FieldFactory.parseField(identifier.type()), identifier.value());
        }
    }

    private void putDate(Map<Field, String> fields, String elementName, String date) {
        if (date != null) {
            switch (elementName) {
                case "dateIssued" -> {
                    Optional<Date> optionalParsedDate = Date.parse(date);
                    optionalParsedDate
                            .ifPresent(parsedDate -> fields.put(StandardField.DATE, parsedDate.getNormalized()));

                    optionalParsedDate.flatMap(Date::getYear)
                            .ifPresent(year -> fields.put(StandardField.YEAR, year.toString()));

                    optionalParsedDate.flatMap(Date::getMonth)
                            .ifPresent(month -> fields.put(StandardField.MONTH, month.getJabRefFormat()));
                }
                case "dateCreated" -> {
                    // If there was no year in date issued, then take the year from date created
                    fields.computeIfAbsent(StandardField.YEAR, k -> date.substring(0, 4));
                    fields.put(new UnknownField("created"), date);
                }
                case "dateCaptured" -> {
                    fields.put(new UnknownField("captured"), date);
                }
                case "dateModified" -> {
                    fields.put(new UnknownField("modified"), date);
                }
            }
        }
    }

    private void putIfListIsNotEmpty(Map<Field, String> fields, List<String> list, Field key, String separator) {
        if (!list.isEmpty()) {
            fields.put(key, list.stream().collect(Collectors.joining(separator)));
        }
    }

    private void handleAuthorsInNamePart(List<Name> names, List<String> authors) {
        List<String> foreName = new ArrayList<>();
        String familyName = "";
        String author = "";

        for (Name name : names) {
            String type = name.type(); // date, family, given, termsOfAddress

            if ((type == null) && (name.value() != null)) {
                String namePartValue = name.value();
                namePartValue = namePartValue.replaceAll(",$", "");
                authors.add(namePartValue);
            } else if ("family".equals(type) && (name.value() != null)) {
                // family should come first, so if family appears we can set the author then comes before
                // we have to check if forename and family name are not empty in case it's the first author
                if (!foreName.isEmpty() && !familyName.isEmpty()) {
                    // now set and add the old author
                    author = familyName + ", " + Joiner.on(" ").join(foreName);
                    authors.add(author);
                    // remove old forenames
                    foreName.clear();
                } else if (foreName.isEmpty() && !familyName.isEmpty()) {
                    authors.add(familyName);
                }
                familyName = name.value();
            } else if ("given".equals(type) && (name.value() != null)) {
                foreName.add(name.value());
            }
        }

        // last author is not added, so do it here
        if (!foreName.isEmpty() && !familyName.isEmpty()) {
            author = familyName + ", " + Joiner.on(" ").join(foreName);
            authors.add(author.trim());
            foreName.clear();
        } else if (foreName.isEmpty() && !familyName.isEmpty()) {
            authors.add(familyName.trim());
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    private void appendIfValueNotNullOrBlank(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value);
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
    public String getName() {
        return "MODS";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public String getDescription() {
        return "Importer for the MODS format";
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

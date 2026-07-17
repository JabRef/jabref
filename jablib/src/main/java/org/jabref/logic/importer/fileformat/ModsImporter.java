package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

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
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Importer for the MODS format.<br>
/// More details about the format can be found here <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>. <br>
/// The newest xml schema can also be found here <a href="www.loc.gov/standards/mods/mods-schemas.html.">www.loc.gov/standards/mods/mods-schemas.html.</a>.
public class ModsImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModsImporter.class);
    private static final Pattern MODS_PATTERN = Pattern.compile("<mods(?:\\s|>)");

    private final String keywordSeparator;
    private final XMLInputFactory xmlInputFactory;

    public ModsImporter(ImportFormatPreferences importFormatPreferences) {
        keywordSeparator = importFormatPreferences.bibEntryPreferences().getKeywordSeparator() + " ";
        xmlInputFactory = XMLInputFactory.newInstance();
        // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
        // Not supported by aalto-xml
        // xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        // xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }

    @Override
    public boolean isRecognizedFormat(@NonNull Reader input) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(input);
        return bufferedReader.lines().anyMatch(line -> MODS_PATTERN.matcher(line).find());
    }

    @Override
    public ParserResult importDatabase(@NonNull BufferedReader input) throws IOException {
        List<BibEntry> bibItems = new ArrayList<>();
        try {
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
            if (isStartXMLEvent(reader) && "mods".equals(reader.getName().getLocalPart())) {
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
        TypeHints typeHints = new TypeHints();

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
                            typeHints.mainGenres.add(reader.getText());
                        }
                    }
                    case "typeOfResource" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            typeHints.mainResource = reader.getText();
                        }
                    }
                    case "language" ->
                            parseLanguage(reader, fields);
                    case "location" ->
                            parseLocationAndUrl(reader, fields);
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
                    case "recordInfo" ->
                            parseRecordInfo(reader, fields);
                    case "titleInfo" ->
                            parseTitle(reader, fields);
                    case "subject" ->
                            parseSubject(reader, fields, keywords);
                    case "originInfo" ->
                            parseOriginInfo(reader, fields, typeHints, false);
                    case "name" ->
                            parseName(reader, fields, authors);
                    case "relatedItem" ->
                            parseRelatedItem(reader, fields, typeHints);
                }
            }

            if (isEndXMLEvent(reader) && "mods".equals(reader.getName().getLocalPart())) {
                break;
            }
        }

        putIfListIsNotEmpty(fields, notes, StandardField.NOTE, ", ");
        putIfListIsNotEmpty(fields, keywords, StandardField.KEYWORDS, this.keywordSeparator);
        putIfListIsNotEmpty(fields, authors, StandardField.AUTHOR, " and ");

        inferEntryType(typeHints)
                .ifPresent(entry::setType);

        putRelatedItemTitle(fields, typeHints, entry.getType());
    }

    /// Parses information from the RelatedModsGroup. It has the same elements as ModsGroup.
    /// But information like volume, issue and the pages appear here instead of in the ModsGroup.
    /// Also, if there appears a title field, then this indicates that is the name of the journal
    /// which the article belongs to.
    private void parseRelatedItem(XMLStreamReader reader, Map<Field, String> fields, TypeHints typeHints) throws XMLStreamException {
        String relatedItemType = reader.getAttributeValue(null, "type");
        boolean hostRelatedItem = "host".equals(relatedItemType);

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                switch (reader.getName().getLocalPart()) {
                    case "title" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            if (hostRelatedItem) {
                                typeHints.hostTitle = reader.getText();
                            } else {
                                putIfValueNotNull(fields, StandardField.JOURNAL, reader.getText());
                            }
                        }
                    }
                    case "genre" -> {
                        reader.next();
                        if (hostRelatedItem && isCharacterXMLEvent(reader)) {
                            typeHints.hostGenres.add(reader.getText());
                        }
                    }
                    case "originInfo" ->
                            parseOriginInfo(reader, fields, typeHints, hostRelatedItem);
                    case "detail" ->
                            handleDetail(reader, fields);
                    case "extent" ->
                            handleExtent(reader, fields);
                }
            }

            if (isEndXMLEvent(reader) && "relatedItem".equals(reader.getName().getLocalPart())) {
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

            if (isEndXMLEvent(reader) && "extent".equals(reader.getName().getLocalPart())) {
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

            if (isEndXMLEvent(reader) && "detail".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void parseName(XMLStreamReader reader, Map<Field, String> fields, List<String> authors) throws XMLStreamException {
        List<Name> names = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                if ("affiliation".equals(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        putIfValueNotNull(fields, new UnknownField("affiliation"), reader.getText());
                    }
                } else if ("namePart".equals(reader.getName().getLocalPart())) {
                    String type = reader.getAttributeValue(null, "type");
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        names.add(new Name(reader.getText(), type));
                    }
                }
            }

            if (isEndXMLEvent(reader) && "name".equals(reader.getName().getLocalPart())) {
                break;
            }
        }

        handleAuthorsInNamePart(names, authors);
    }

    private void parseOriginInfo(XMLStreamReader reader, Map<Field, String> fields, TypeHints typeHints, boolean hostOriginInfo) throws XMLStreamException {
        List<String> places = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "issuance" -> {
                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            if (hostOriginInfo) {
                                typeHints.hostIssuance = reader.getText();
                            } else {
                                typeHints.mainIssuance = reader.getText();
                            }
                        }
                    }
                    case "placeTerm" -> {
                        reader.next();
                        if (!hostOriginInfo && isCharacterXMLEvent(reader)) {
                            appendIfValueNotNullOrBlank(places, reader.getText());
                        }
                    }
                    case "publisher" -> {
                        reader.next();
                        if (!hostOriginInfo && isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.PUBLISHER, reader.getText());
                        }
                    }
                    case "edition" -> {
                        reader.next();
                        if (!hostOriginInfo && isCharacterXMLEvent(reader)) {
                            putIfValueNotNull(fields, StandardField.EDITION, reader.getText());
                        }
                    }
                    case "dateIssued",
                         "dateCreated",
                         "dateCaptured",
                         "dateModified" -> {
                        reader.next();
                        if (!hostOriginInfo && isCharacterXMLEvent(reader)) {
                            putDate(fields, elementName, reader.getText());
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && "originInfo".equals(reader.getName().getLocalPart())) {
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

            if (isEndXMLEvent(reader) && "subject".equals(reader.getName().getLocalPart())) {
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
                        recordContents.addFirst(reader.getText());
                    }
                } else if ("languageTerm".equals(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        languages.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && "recordInfo".equals(reader.getName().getLocalPart())) {
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

            if (isStartXMLEvent(reader) && "languageTerm".equals(reader.getName().getLocalPart())) {
                reader.next();
                if (isCharacterXMLEvent(reader)) {
                    putIfValueNotNull(fields, StandardField.LANGUAGE, reader.getText());
                }
            }

            if (isEndXMLEvent(reader) && "language".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
    }

    private void parseTitle(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();

            if (isStartXMLEvent(reader) && "title".equals(reader.getName().getLocalPart())) {
                reader.next();
                if (isCharacterXMLEvent(reader)) {
                    putIfValueNotNull(fields, StandardField.TITLE, reader.getText());
                }
            }

            if (isEndXMLEvent(reader) && "titleInfo".equals(reader.getName().getLocalPart())) {
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
                if ("physicalLocation".equals(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        locations.add(reader.getText());
                    }
                } else if ("url".equals(reader.getName().getLocalPart())) {
                    reader.next();
                    if (isCharacterXMLEvent(reader)) {
                        urls.add(reader.getText());
                    }
                }
            }

            if (isEndXMLEvent(reader) && "location".equals(reader.getName().getLocalPart())) {
                break;
            }
        }

        putIfListIsNotEmpty(fields, locations, StandardField.LOCATION, ", ");
        putIfListIsNotEmpty(fields, urls, StandardField.URL, ", ");
    }

    private String mapGenre(String genre) {
        return switch (genre.toLowerCase(Locale.ROOT)) {
            case "academic journal",
                 "article" ->
                    "article";
            case "book" ->
                    "book";
            case "conference publication" ->
                    "proceedings";
            case "database" ->
                    "dataset";
            case "yearbook",
                 "handbook" ->
                    "book";
            case "law report or digest",
                 "technical report",
                 "reporting" ->
                    "report";
            default ->
                    genre;
        };
    }

    private Optional<EntryType> inferEntryType(TypeHints typeHints) {
        Optional<EntryType> explicitMainType = typeHints.mainGenres.stream()
                                                                   .map(this::mapGenre)
                                                                   .map(this::parseKnownEntryType)
                                                                   .flatMap(Optional::stream)
                                                                   .findFirst();
        if (explicitMainType.isPresent()) {
            return explicitMainType;
        }

        Optional<EntryType> hostType = inferHostEntryType(typeHints);
        if (hostType.isPresent()) {
            return hostType;
        }

        if ("text".equalsIgnoreCase(typeHints.mainResource) && "monographic".equalsIgnoreCase(typeHints.mainIssuance)) {
            return Optional.of(StandardEntryType.Book);
        }

        return Optional.empty();
    }

    private Optional<EntryType> inferHostEntryType(TypeHints typeHints) {
        if (typeHints.hostGenres.stream().anyMatch("conference publication"::equalsIgnoreCase)) {
            return Optional.of(StandardEntryType.InProceedings);
        }

        if (typeHints.hostGenres.stream().anyMatch("book"::equalsIgnoreCase)
                || ("text".equalsIgnoreCase(typeHints.mainResource) && "monographic".equalsIgnoreCase(typeHints.hostIssuance))) {
            return Optional.of(StandardEntryType.InBook);
        }

        if (typeHints.hostGenres.stream().anyMatch("multivolume monograph"::equalsIgnoreCase)) {
            return Optional.of(StandardEntryType.InCollection);
        }

        if (typeHints.hostGenres.stream().anyMatch(genre -> "periodical".equalsIgnoreCase(genre) || "academic journal".equalsIgnoreCase(genre))) {
            return Optional.of(StandardEntryType.Article);
        }

        return Optional.empty();
    }

    private Optional<EntryType> parseKnownEntryType(String typeName) {
        EntryType parsedType = EntryTypeFactory.parse(typeName);
        if (parsedType instanceof StandardEntryType) {
            return Optional.of(parsedType);
        }

        return Optional.empty();
    }

    private void putRelatedItemTitle(Map<Field, String> fields, TypeHints typeHints, EntryType entryType) {
        if (typeHints.hostTitle == null) {
            return;
        }

        if (StandardEntryType.InBook == entryType
                || StandardEntryType.InCollection == entryType
                || StandardEntryType.InProceedings == entryType) {
            putIfValueNotNull(fields, StandardField.BOOKTITLE, typeHints.hostTitle);
        } else {
            putIfValueNotNull(fields, StandardField.JOURNAL, typeHints.hostTitle);
        }
    }

    private static final class TypeHints {
        private final List<String> mainGenres = new ArrayList<>();
        private final List<String> hostGenres = new ArrayList<>();
        private @Nullable String mainResource;
        private @Nullable String mainIssuance;
        private @Nullable String hostIssuance;
        private @Nullable String hostTitle;
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
            Optional<Date> optionalParsedDate = Date.parse(date);
            switch (elementName) {
                case "dateIssued" -> {
                    optionalParsedDate
                            .ifPresent(parsedDate -> fields.put(StandardField.DATE, parsedDate.getNormalized()));

                    optionalParsedDate.flatMap(Date::getYear)
                                      .ifPresent(year -> fields.put(StandardField.YEAR, year.toString()));

                    optionalParsedDate.flatMap(Date::getMonth)
                                      .ifPresent(month -> fields.put(StandardField.MONTH, month.getJabRefFormat()));
                }
                case "dateCreated" -> {
                    // If there was no year in date issued, then take the year from date created
                    fields.computeIfAbsent(StandardField.YEAR, _ -> date.substring(0, 4));
                    fields.put(new UnknownField("created"), date);
                }
                case "dateCaptured" ->
                        optionalParsedDate
                                .ifPresent(parsedDate -> fields.put(StandardField.CREATIONDATE, parsedDate.getNormalized()));
                case "dateModified" ->
                        optionalParsedDate
                                .ifPresent(parsedDate -> fields.put(StandardField.MODIFICATIONDATE, parsedDate.getNormalized()));
            }
        }
    }

    private void putIfListIsNotEmpty(Map<Field, String> fields, List<String> list, Field key, String separator) {
        if (!list.isEmpty()) {
            fields.put(key, String.join(separator, list));
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
                    author = familyName + ", " + String.join(" ", foreName);
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
            author = familyName + ", " + String.join(" ", foreName);
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
    public String getId() {
        return "mods";
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
        return Localization.lang("Importer for the MODS format.");
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            return importDatabase(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return List.of();
    }
}

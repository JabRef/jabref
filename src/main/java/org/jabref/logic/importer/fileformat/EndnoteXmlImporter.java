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
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
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
//    private Unmarshaller unmarshaller;

    public EndnoteXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
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
//            Object unmarshalledObject = unmarshallRoot(input);
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            // required for reading Unicode characters such as &#xf6;
            xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);



            while (reader.hasNext()){
//                System.out.println(reader);
                reader.next();
                if (isStartXMLEvent(reader)){
                    String elementName = reader.getName().getLocalPart();
                    System.out.println(elementName);
                    if (elementName.equals("record")){
                        parseRecord(reader,bibItems,elementName);
                    }
                }
            }
//            return ParserResult.fromErrorMessage("testing");
//            if (unmarshalledObject instanceof Xml) {
//                // Check whether we have an article set, an article, a book article or a book article set
//                Xml root = (Xml) unmarshalledObject;
//                List<BibEntry> bibEntries = root
//                        .getRecords().getRecord()
//                        .stream()
//                        .map(this::parseRecord)
//                        .collect(Collectors.toList());
//
//                return new ParserResult(bibEntries);
//            } else {
//                return ParserResult.fromErrorMessage("File does not start with xml tag.");
//            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return new ParserResult(bibItems);
    }

    private void parseRecord(XMLStreamReader reader, List<BibEntry> bibItems, String startElement)
            throws XMLStreamException{
        Map<Field, String> fields = new HashMap<>();
        EntryType entryType = StandardEntryType.Article;

        while (reader.hasNext()){
            reader.next();
            if (isStartXMLEvent(reader)){
                String elementName = reader.getName().getLocalPart();
                switch(elementName){
                    case "ref-type" -> {
                        String type = reader.getAttributeValue(null,"name");
                        entryType = convertRefNameToType(type);
                    }
                    case "contributors" -> {
                        //parseContributors
                        handleAuthorList(reader,fields,startElement);
                    }
                    case "titles" -> {
                        //parseTitles
                    }

                    case "pages" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.PAGES,reader.getText());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("pages")) {
                                break;
                            }
                        }
                    }
                    case "volume" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.VOLUME,reader.getText());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("volume")) {
                                break;
                            }
                        }
                    }
                    case "number" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.NUMBER,reader.getText());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("number")) {
                                break;
                            }
                        }
                    }
                    case "dates" -> {
                        // parseDate
                    }

                    case "notes" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.NOTE,reader.getText().trim());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("notes")) {
                                break;
                            }
                        }
                    }
                    case "urls" -> {
                        handleUrlList(reader, fields, startElement);
                        System.out.println("urls");
                    }
                    case "abstract" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.ABSTRACT,reader.getText().trim());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("abstract")) {
                                break;
                            }
                        }
                    }
                    case "isbn" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.ISBN,clean(reader.getText()));
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("isbn")) {
                                break;
                            }
                        }
                    }
                    case "electronic-resource-num" -> {
                        //parse DOI
                    }
                    case "publisher" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,StandardField.PUBLISHER,reader.getText());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("publisher")) {
                                break;
                            }
                        }
                    }
                    case "label" -> {
                        while (reader.hasNext()) {
                            reader.next();
                            if (isStartXMLEvent(reader)) {
                                String tag = reader.getName().getLocalPart();
                                if (tag.equals("style")){
                                    reader.next();
                                    if (isCharacterXMLEvent(reader)) {
                                        putIfValueNotNull(fields,new UnknownField("endnote-label"),reader.getText());
                                    }
                                }
                            }
                            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals("label")) {
                                break;
                            }
                        }
                    }

                }
            }
            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

        BibEntry entry = new BibEntry(entryType);
        entry.setField(fields);
        for (Map.Entry<Field,String> f: fields.entrySet()){
            System.out.println(f.getKey().getName() + " : " + f.getValue());
        }
        bibItems.add(entry);

    }
    private void handleUrlList(XMLStreamReader reader, Map<Field, String> fields, String startElement) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                switch (elementName) {
                    case "related-urls" -> {
                        parseRelatedUrls(reader, fields);
                        System.out.println("related-urls");
                    }
                    case "pdf-urls" -> {
                        reader.next();
                        if(isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.FILE, reader.getText());
                            System.out.println("pdf-urls");
                        }
                    }
                }
            }

            if (isEndXMLEvent(reader) && reader.getName().getLocalPart().equals(startElement)) {
                break;
            }
        }

    }
    private void parseRelatedUrls(XMLStreamReader reader, Map<Field, String> fields) throws XMLStreamException {

        while (reader.hasNext()) {
            reader.next();
            if (isStartXMLEvent(reader)) {
                String elementName = reader.getName().getLocalPart();
                System.out.println("elementName" + elementName);
                if (elementName.equals("style")) {
                    reader.next();
//                    if ("style".equals(reader.getName().getLocalPart())) {
//                        reader.next();
                        if (isCharacterXMLEvent(reader)) {
                            fields.put(StandardField.URL, reader.getText());
                        }
//                    }
                }
            }

            if (isEndXMLEvent(reader) && "related-urls".equals(reader.getName().getLocalPart())) {
                break;
            }
        }
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
                        System.out.println("test");
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
                System.out.println(elementName + " Tst");
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
    public List<BibEntry> parseEntries(InputStream inputStream) {
        try {
            return importDatabase(
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Collections.emptyList();
    }
}

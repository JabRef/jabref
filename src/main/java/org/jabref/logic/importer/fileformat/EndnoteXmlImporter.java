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
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

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
                        // parseRecord;
                    }
                }
            }
            return ParserResult.fromErrorMessage("testing");
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

//        return new ParseResult(bibItems);
    }
    private void parseRecord(XMLStreamReader reader, List<BibEntry> bibItems, String startElement)
        throws XMLStreamException{
        Map<Field, String> fields = new HashMap<>();

        while (reader.hasNext()){
            reader.next();
            if (isStartXMLEvent(reader)){
                String elementName = reader.getName().getLocalPart();
                switch(elementName){
                    case "ref-type" -> {
                        reader.getAttributeValue(null,"name");
//                        putIfValueNotNull(fields, StandardField.);
                    }
                    case "contributors" -> {
                        //parseContributors
                    }
                    case "titles" -> {
                        //parseTitles
                    }

                    case "pages" -> {

                    }
                    case "volume" -> {

                    }
                    case "number" -> {

                    }
                    case "dates" -> {
                        // parseDate
                    }
                    case "notes" -> {

                    }
                    case "urls" -> {
                        //parseUrls

                    }
                    case "abstract" -> {

                    }
                    case "isbn" -> {

                    }
                    case "electronic-resource-num" -> {
                        //parse DOI
                    }
                    case "publisher" -> {

                    }
                    case "label" -> {

                    }

                }
            }
        }

    }
//
//    private Object unmarshallRoot(BufferedReader reader) throws XMLStreamException, JAXBException {
//        initUnmarshaller();
//
//        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
//        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
//
//        // Go to the root element
//        while (!xmlStreamReader.isStartElement()) {
//            xmlStreamReader.next();
//        }
//
//        return unmarshaller.unmarshal(xmlStreamReader);
//    }
//
//    private void initUnmarshaller() throws JAXBException {
//        if (unmarshaller == null) {
//            // Lazy init because this is expensive
//            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.endnote");
//            unmarshaller = context.createUnmarshaller();
//        }
//    }
//
//    private static EntryType convertRefNameToType(String refName) {
//        return switch (refName.toLowerCase().trim()) {
//            case "artwork", "generic" -> StandardEntryType.Misc;
//            case "electronic article" -> IEEETranEntryType.Electronic;
//            case "book section" -> StandardEntryType.InBook;
//            case "book" -> StandardEntryType.Book;
//            case "report" -> StandardEntryType.Report;
//            // case "journal article" -> StandardEntryType.Article;
//            default -> StandardEntryType.Article;
//        };
//    }
//
//    private BibEntry parseRecord(Record endNoteRecord) {
//        BibEntry entry = new BibEntry();
//
//        entry.setType(getType(endNoteRecord));
//        Optional.ofNullable(getAuthors(endNoteRecord))
//                .ifPresent(value -> entry.setField(StandardField.AUTHOR, value));
//        Optional.ofNullable(endNoteRecord.getTitles())
//                .map(Titles::getTitle)
//                .map(Title::getStyle)
//                .map(this::mergeStyleContents)
//                .ifPresent(value -> entry.setField(StandardField.TITLE, clean(value)));
//        Optional.ofNullable(endNoteRecord.getTitles())
//                .map(Titles::getSecondaryTitle)
//                .map(SecondaryTitle::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.JOURNAL, clean(value)));
//        Optional.ofNullable(endNoteRecord.getPages())
//                .map(Pages::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.PAGES, value));
//        Optional.ofNullable(endNoteRecord.getNumber())
//                .map(Number::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.NUMBER, value));
//        Optional.ofNullable(endNoteRecord.getVolume())
//                .map(Volume::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.VOLUME, value));
//        Optional.ofNullable(endNoteRecord.getDates())
//                .map(Dates::getYear)
//                .map(Year::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.YEAR, value));
//        Optional.ofNullable(endNoteRecord.getNotes())
//                .map(Notes::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.NOTE, value.trim()));
//        getUrl(endNoteRecord)
//                .ifPresent(value -> entry.setField(StandardField.URL, value));
//        entry.putKeywords(getKeywords(endNoteRecord), preferences.bibEntryPreferences().getKeywordSeparator());
//        Optional.ofNullable(endNoteRecord.getAbstract())
//                .map(Abstract::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.ABSTRACT, value.trim()));
//        entry.setFiles(getLinkedFiles(endNoteRecord));
//        Optional.ofNullable(endNoteRecord.getIsbn())
//                .map(Isbn::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.ISBN, clean(value)));
//        Optional.ofNullable(endNoteRecord.getElectronicResourceNum())
//                .map(ElectronicResourceNum::getStyle)
//                .map(Style::getContent)
//                .ifPresent(doi -> entry.setField(StandardField.DOI, doi.trim()));
//        Optional.ofNullable(endNoteRecord.getPublisher())
//                .map(Publisher::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(StandardField.PUBLISHER, value));
//        Optional.ofNullable(endNoteRecord.getLabel())
//                .map(Label::getStyle)
//                .map(Style::getContent)
//                .ifPresent(value -> entry.setField(new UnknownField("endnote-label"), value));
//
//        return entry;
//    }
//
//    private EntryType getType(Record endNoteRecord) {
//        return Optional.ofNullable(endNoteRecord.getRefType())
//                       .map(RefType::getName)
//                       .map(EndnoteXmlImporter::convertRefNameToType)
//                       .orElse(StandardEntryType.Article);
//    }
//
//    private List<LinkedFile> getLinkedFiles(Record endNoteRecord) {
//        Optional<PdfUrls> urls = Optional.ofNullable(endNoteRecord.getUrls())
//                                         .map(Urls::getPdfUrls);
//        return OptionalUtil.toStream(urls)
//                           .flatMap(pdfUrls -> pdfUrls.getUrl().stream())
//                           .flatMap(url -> OptionalUtil.toStream(getUrlValue(url)))
//                           .map(url -> {
//                               try {
//                                   return new LinkedFile(new URL(url), "PDF");
//                               } catch (
//                                       MalformedURLException e) {
//                                   LOGGER.info("Unable to parse {}", url);
//                                   return null;
//                               }
//                           })
//                           .collect(Collectors.toList());
//    }
//
//    private Optional<String> getUrl(Record endNoteRecord) {
//        Optional<RelatedUrls> urls = Optional.ofNullable(endNoteRecord.getUrls())
//                                             .map(Urls::getRelatedUrls);
//        return OptionalUtil.toStream(urls)
//                           .flatMap(url -> url.getUrl().stream())
//                           .flatMap(url -> OptionalUtil.toStream(getUrlValue(url)))
//                           .findFirst();
//    }
//
//    private String mergeStyleContents(List<Style> styles) {
//        return styles.stream().map(Style::getContent).collect(Collectors.joining());
//    }
//
//    private Optional<String> getUrlValue(Url url) {
//        Optional<List<Object>> urlContent = Optional.ofNullable(url).map(Url::getContent);
//        List<Object> list = urlContent.orElse(Collections.emptyList());
//        Optional<String> ret;
//        if (list.size() == 0) {
//            return Optional.empty();
//        } else {
//            boolean isStyleExist = false;
//            int style_index = -1;
//            for (int i = 0; i < list.size(); i++) {
//                if (list.get(i) instanceof Style) {
//                    isStyleExist = true;
//                    style_index = i;
//                }
//            }
//            if (!isStyleExist) {
//                ret = Optional.ofNullable((String) list.get(0))
//                        .map(this::clean);
//            } else {
//                ret = Optional.ofNullable((Style) list.get(style_index))
//                        .map(Style::getContent)
//                        .map(this::clean);
//            }
//        }
//        return ret;
//    }
//
//    private List<String> getKeywords(Record endNoteRecord) {
//        Keywords keywords = endNoteRecord.getKeywords();
//        if (keywords != null) {
//            return keywords.getKeyword()
//                           .stream()
//                           .map(Keyword::getStyle)
//                           .filter(Objects::nonNull)
//                           .map(Style::getContent)
//                           .collect(Collectors.toList());
//        } else {
//            return Collections.emptyList();
//        }
//    }
//
//    private String getAuthors(Record endNoteRecord) {
//        Optional<Authors> authors = Optional.ofNullable(endNoteRecord.getContributors())
//                                            .map(Contributors::getAuthors);
//        return OptionalUtil.toStream(authors)
//                           .flatMap(value -> value.getAuthor().stream())
//                           .map(author -> author.getStyle().getContent())
//                           .collect(Collectors.joining(" and "));
//    }

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

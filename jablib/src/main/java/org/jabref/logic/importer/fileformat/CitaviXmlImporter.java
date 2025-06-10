package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.Persons.Person;
import org.jabref.logic.importer.fileformat.citavi.KnowledgeItem;
import org.jabref.logic.importer.fileformat.citavi.Reference;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private static final byte UUID_LENGTH = 36;
    private static final byte UUID_SEMICOLON_OFFSET_INDEX = 37;
    private static final EnumSet<QuotationTypeMapping> QUOTATION_TYPES = EnumSet.allOf(QuotationTypeMapping.class);
    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final NormalizePagesFormatter pagesFormatter = new NormalizePagesFormatter();

    private final Map<String, Author> knownPersons = new HashMap<>();
    private final Map<String, Keyword> knownKeywords = new HashMap<>();
    private final Map<String, String> knownPublishers = new HashMap<>();
    private final List<Reference> references = new ArrayList<>();
    private final List<KnowledgeItem> knowledgeItems = new ArrayList<>();
    private final XMLInputFactory xmlInputFactory;
    private Map<String, String> refIdWithAuthors = new HashMap<>();
    private Map<String, String> refIdWithEditors = new HashMap<>();
    private Map<String, String> refIdWithKeywords = new HashMap<>();
    private Map<String, String> refIdWithPublishers = new HashMap<>();

    private CitaviExchangeData.Persons persons;
    private CitaviExchangeData.Keywords keywords;
    private CitaviExchangeData.Publishers publishers;

    private CitaviExchangeData.ReferenceAuthors refAuthors;
    private CitaviExchangeData.ReferenceEditors refEditors;
    private CitaviExchangeData.ReferenceKeywords refKeywords;
    private CitaviExchangeData.ReferencePublishers refPublishers;

    private Unmarshaller unmarshaller;

    public CitaviXmlImporter() {
        xmlInputFactory = XMLInputFactory.newFactory();
        // prevent xxe (https://rules.sonarsource.com/java/RSPEC-2755)
        xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // required for reading Unicode characters such as &#xf6;
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    @Override
    public String getName() {
        return "Citavi XML";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.CITAVI;
    }

    @Override
    public String getId() {
        return "citavi";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Citavi XML format.");
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        return false;
    }

    @Override
    public boolean isRecognizedFormat(Path filePath) throws IOException {
        try (BufferedReader reader = getReaderFromZip(filePath)) {
            String str;
            int i = 0;
            while (((str = reader.readLine()) != null) && (i < 50)) {
                if (str.toLowerCase(Locale.ROOT).contains("citaviexchangedata")) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        Objects.requireNonNull(filePath);

        try (BufferedReader reader = getReaderFromZip(filePath)) {
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

            if (xmlStreamReader.hasNext()) {
                xmlStreamReader.next();
                if (xmlStreamReader.isStartElement()) {
                    String elementName = xmlStreamReader.getLocalName();
                    if ("CitaviExchangeData".equals(elementName)) {
                        parseCitaviData(xmlStreamReader);
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        List<BibEntry> bibItems = new ArrayList<>();

        return new ParserResult(bibItems);
    }

    private void parseCitaviData(XMLStreamReader reader) throws XMLStreamException {
        // TODO: Persons, Keywords, Publishers, KnowledgeItems, ReferenceAuthors, ReferenceKeywords, ReferencePublishers, ReferenceEditors
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String startElementName = reader.getLocalName();
                    switch (startElementName) {
                        case "Persons" -> parsePersons(reader);
                        case "Keywords" -> parseKeywords(reader);
                        case "Publishers" -> parsePublishers(reader);
                        case "References" -> parseReferences(reader);
                        case "KnowledgeItems" -> parseKnowledgeItems(reader);
                        default -> consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    String endElementName = reader.getLocalName();
                    if ("CitaviExchangeData".equals(endElementName)) {
                        return;
                    }
                }
            }
        }
    }

    private void parsePersons(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    if ("Person".equals(reader.getLocalName())) {
                        parsePerson(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Persons".equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private void parsePerson(XMLStreamReader reader) throws XMLStreamException {
        String id = reader.getAttributeValue(null, "id");
        String firstName = null;
        String lastName = null;

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if (elementName.equals("FirstName")) {
                        firstName = reader.getElementText();
                    } else if (elementName.equals("LastName")) {
                        lastName = reader.getElementText();
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Person".equals(reader.getLocalName())) {
                        Author author = new Author(firstName, null, null, lastName, null);
                        knownPersons.put(id, author);
                        return;
                    }
                }
            }
        }
    }

    private void parseKeywords(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    if ("Keyword".equals(reader.getLocalName())) {
                        parseKeyword(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Keywords".equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private void parseKeyword(XMLStreamReader reader) throws XMLStreamException {
        String id = reader.getAttributeValue(null, "id");
        String keywordName = null;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if (elementName.equals("Name")) {
                        keywordName = reader.getElementText();
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Keyword".equals(reader.getLocalName())) {
                        Keyword keyword = new Keyword(keywordName);
                        knownKeywords.put(id, keyword);
                        return;
                    }
                }
            }
        }
    }

    private void parsePublishers(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if (elementName.equals("Publisher")) {
                        parsePublisher(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Publishers".equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private void parsePublisher(XMLStreamReader reader) throws XMLStreamException {
        String id = reader.getAttributeValue(null, "id");
        String publisherName = null;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if (elementName.equals("Name")) {
                        publisherName = reader.getElementText();
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Publisher".equals(reader.getLocalName())) {
                        knownPublishers.put(id, publisherName);
                        return;
                    }
                }
            }
        }
    }

    private void parseReferences(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if (elementName.equals("Reference")) {
                        parseReference(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("References".equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private void parseReference(XMLStreamReader reader) throws XMLStreamException {
        String id = reader.getAttributeValue(null, "id");
        String referenceType = null;
        String title = null;
        String year = null;
        String abstractText = null;
        String pageRange = null;
        String pageCount = null;
        String volume = null;
        String doi = null;
        String isbn = null;

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    switch (elementName) {
                        case "ReferenceType" -> referenceType = reader.getElementText();
                        case "Title" -> title = clean(reader.getElementText());
                        case "Year" -> year = clean(reader.getElementText());
                        case "Abstract" -> abstractText = clean(reader.getElementText());
                        case "PageRange" -> pageRange = reader.getElementText();
                        case "PageCount" -> pageCount = reader.getElementText();
                        case "Volume" -> volume = clean(reader.getElementText());
                        case "Doi" -> doi = clean(reader.getElementText());
                        case "Isbn" -> isbn = clean(reader.getElementText());
                        default -> consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("Reference".equals(reader.getLocalName())) {
                        references.add(new Reference(id, referenceType, title, year, abstractText, pageRange, pageCount, volume, doi, isbn));
                        return;
                    }
                }
            }
        }
    }

    private void parseKnowledgeItems(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    if ("KnowledgeItem".equals(reader.getLocalName())) {
                        parseKnowledgeItem(reader);
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("KnowledgeItems".equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private void parseKnowledgeItem(XMLStreamReader reader) throws XMLStreamException {
        String referenceId = null;
        String coreStatement = null;
        String text = null;
        String pageRangeNumber = null;
        String quotationType = null;
        String quotationIndex = null;

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    switch (elementName) {
                        case "ReferenceID" -> referenceId = reader.getElementText();
                        case "CoreStatement" -> coreStatement = reader.getElementText();
                        case "Text" -> text = reader.getElementText();
                        case "PageRangeNumber" -> pageRangeNumber = reader.getElementText();
                        case "QuotationType" -> quotationType = reader.getElementText();
                        case "QuotationIndex" -> quotationIndex = reader.getElementText();
                        default -> consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if ("KnowledgeItem".equals(reader.getLocalName())) {
                        knowledgeItems.add(new KnowledgeItem(referenceId, coreStatement, text, pageRangeNumber, quotationType, quotationIndex));
                        return;
                    }
                }
            }
        }
    }

    private static EntryType convertRefNameToType(String refName) {
        return switch (refName.toLowerCase().trim()) {
            case "artwork", "generic", "musicalbum", "audioorvideodocument", "movie" -> StandardEntryType.Misc;
            case "electronic article" -> IEEETranEntryType.Electronic;
            case "book section" -> StandardEntryType.InBook;
            case "book", "bookedited", "audiobook" -> StandardEntryType.Book;
            case "report" -> StandardEntryType.Report;
            // case "journal article" -> StandardEntryType.Article;
            default -> StandardEntryType.Article;
        };
    }

    private Map<String, String> buildPersonList(List<String> authorsOrEditors) {
        Map<String, String> refToPerson = new HashMap<>();

        for (String idStringsWithSemicolon : authorsOrEditors) {
            String refId = idStringsWithSemicolon.substring(0, UUID_LENGTH);
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX);

            String[] personIds = rest.split(";");

            List<Author> jabrefAuthors = new ArrayList<>();

            for (String personId : personIds) {
                // Store persons we already encountered, we can have the same author multiple times in the whole database
                knownPersons.computeIfAbsent(personId, k -> {
                    Optional<Person> person = persons.getPerson().stream().filter(p -> p.getId().equals(k)).findFirst();
                    return person.map(p -> new Author(p.getFirstName(), "", "", p.getLastName(), "")).orElse(null);
                });
                jabrefAuthors.add(knownPersons.get(personId));
            }
            String stringifiedAuthors = AuthorList.of(jabrefAuthors).getAsLastFirstNamesWithAnd(false);
            refToPerson.put(refId, stringifiedAuthors);
        }
        return refToPerson;
    }

    private Map<String, String> buildKeywordList(List<String> keywordsList) {
        Map<String, String> refToKeywords = new HashMap<>();

        for (String idStringsWithSemicolon : keywordsList) {
            String refId = idStringsWithSemicolon.substring(0, UUID_LENGTH);
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX);

            String[] keywordIds = rest.split(";");

            List<Keyword> jabrefKeywords = new ArrayList<>();

            for (String keywordId : keywordIds) {
                // store keywords already encountered
                knownKeywords.computeIfAbsent(keywordId, k -> {
                    Optional<CitaviExchangeData.Keywords.Keyword> keyword = keywords.getKeyword().stream().filter(p -> p.getId().equals(k)).findFirst();
                    return keyword.map(kword -> new Keyword(kword.getName())).orElse(null);
                });
                jabrefKeywords.add(knownKeywords.get(keywordId));
            }

            KeywordList list = new KeywordList(List.copyOf(jabrefKeywords));
            String stringifiedKeywords = list.toString();
            refToKeywords.put(refId, stringifiedKeywords);
        }
        return refToKeywords;
    }

    private Map<String, String> buildPublisherList(List<String> publishersList) {
        Map<String, String> refToPublishers = new HashMap<>();

        for (String idStringsWithSemicolon : publishersList) {
            String refId = idStringsWithSemicolon.substring(0, UUID_LENGTH);
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX);

            String[] publisherIds = rest.split(";");

            List<String> jabrefPublishers = new ArrayList<>();

            for (String pubId : publisherIds) {
                // store publishers already encountered
                knownPublishers.computeIfAbsent(pubId, k -> {
                    Optional<CitaviExchangeData.Publishers.Publisher> publisher = publishers.getPublisher().stream().filter(p -> p.getId().equals(k)).findFirst();
                    return publisher.map(CitaviExchangeData.Publishers.Publisher::getName).orElse(null);
                });
                jabrefPublishers.add(knownPublishers.get(pubId));
            }

            String stringifiedKeywords = String.join(",", jabrefPublishers);
            refToPublishers.put(refId, stringifiedKeywords);
        }
        return refToPublishers;
    }

    String cleanUpText(String text) {
        String result = removeSpacesBeforeLineBreak(text);
        result = result.replaceAll("(?<!\\\\)\\{", "\\\\{");
        result = result.replaceAll("(?<!\\\\)}", "\\\\}");
        return result;
    }

    private String removeSpacesBeforeLineBreak(String string) {
        return string.replaceAll(" +\r\n", "\r\n")
              .replaceAll(" +\n", "\n");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("CitaviXmlImporter does not support importDatabase(BufferedReader reader). "
                                                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) {
        try {
            return importDatabase(
                                  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return List.of();
    }

    private void consumeElement(XMLStreamReader reader) throws XMLStreamException {
        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    private String getPages(String pageRange, String pageCount) {
        if (pageCount != null && !pageCount.isEmpty()) {
            return pageCount;
        }
        if (pageRange != null && !pageRange.isEmpty()) {
            return pageRange;
        }
        return "";
    }

    private BufferedReader getReaderFromZip(Path filePath) throws IOException {
        Path newFile = Files.createTempFile("citavicontent", ".xml");
        newFile.toFile().deleteOnExit();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                zipEntry = zis.getNextEntry();
            }
        }

        // Citavi XML files sometimes contains BOM markers. We just discard them.
        // Solution inspired by https://stackoverflow.com/a/37445972/873282
        return new BufferedReader(
                new InputStreamReader(
                        new BOMInputStream.Builder()
                            .setInputStream(Files.newInputStream(newFile, StandardOpenOption.READ))
                            .setInclude(false)
                            .setByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE)
                            .get()));
    }

    private String clean(String input) {
        String result = StringUtil.unifyLineBreaks(input, " ")
                         .trim()
                         .replaceAll(" +", " ");
        return htmlToLatexFormatter.format(result);
    }

    enum QuotationTypeMapping {
        IMAGE_QUOTATION(0, "Image quotation"),
        DIRECT_QUOTATION(1, "Direct quotation"),
        INDIRECT_QUOTATION(2, "Indirect quotation"),
        SUMMARY(3, "Summary"),
        COMMENT(4, "Comment"),
        HIGHLIGHT(5, "Highlight"),
        HIGHLIGHT_RED(6, "Highlight in red");

        final int citaviType;
        final String name;

        QuotationTypeMapping(int citaviType, String name) {
            this.name = name;
            this.citaviType = citaviType;
        }

        String getName() {
            return name;
        }

        int getCitaviIndexType() {
            return citaviType;
        }
    }
}

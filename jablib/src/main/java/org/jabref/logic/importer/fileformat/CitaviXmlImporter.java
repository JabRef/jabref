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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.citavi.KnowledgeItem;
import org.jabref.logic.importer.fileformat.citavi.Reference;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.logic.util.strings.StringUtil;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private static final byte UUID_LENGTH = 36;
    private static final byte UUID_SEMICOLON_OFFSET_INDEX = 37;
    private static final int END_TAG_CHARACTER_COUNT = 5; // </os> or </ps>
    private static final EnumSet<QuotationTypeMapping> QUOTATION_TYPES = EnumSet.allOf(QuotationTypeMapping.class);
    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final NormalizePagesFormatter pagesFormatter = new NormalizePagesFormatter();

    private final Map<String, Author> knownPersons = new HashMap<>();
    private final Map<String, Keyword> knownKeywords = new HashMap<>();
    private final Map<String, String> knownPublishers = new HashMap<>();
    private final List<Reference> references = new ArrayList<>();
    private final List<KnowledgeItem> knowledgeItems = new ArrayList<>();
    private final XMLInputFactory xmlInputFactory;
    private final Map<String, List<String>> refIdWithAuthorIds = new HashMap<>();
    private final Map<String, List<String>> refIdWithEditorIds = new HashMap<>();
    private final Map<String, List<String>> refIdWithKeywordsIds = new HashMap<>();
    private final Map<String, List<String>> refIdWithPublisherIds = new HashMap<>();

    public CitaviXmlImporter() {
        xmlInputFactory = XMLInputFactory.newFactory();
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
    public boolean isRecognizedFormat(@NonNull BufferedReader reader) throws IOException {
        return false;
    }

    @Override
    public boolean isRecognizedFormat(@NonNull Path filePath) throws IOException {
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
    public ParserResult importDatabase(@NonNull Path filePath) throws IOException {
        try (BufferedReader reader = getReaderFromZip(filePath)) {
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

            if (xmlStreamReader.hasNext()) {
                xmlStreamReader.next();
                if (xmlStreamReader.isStartElement()) {
                    String elementName = xmlStreamReader.getLocalName();
                    if ("CitaviExchangeData".equals(elementName)) {
                        parseCitaviData(xmlStreamReader);
                        List<BibEntry> bibItems = buildBibItems();
                        return new ParserResult(bibItems);
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
        return ParserResult.fromErrorMessage("Could not find root element");
    }

    private void parseCitaviData(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String startElementName = reader.getLocalName();
                    switch (startElementName) {
                        case "Persons" ->
                                parsePersons(reader);
                        case "Keywords" ->
                                parseKeywords(reader);
                        case "Publishers" ->
                                parsePublishers(reader);
                        case "References" ->
                                parseReferences(reader);
                        case "KnowledgeItems" ->
                                parseKnowledgeItems(reader);
                        case "ReferenceAuthors" ->
                                parseReferenceIdLink(reader, "ReferenceAuthors", refIdWithAuthorIds);
                        case "ReferenceKeywords" ->
                                parseReferenceIdLink(reader, "ReferenceKeywords", refIdWithKeywordsIds);
                        case "ReferencePublishers" ->
                                parseReferenceIdLink(reader, "ReferencePublishers", refIdWithPublisherIds);
                        case "ReferenceEditors" ->
                                parseReferenceIdLink(reader, "ReferenceEditors", refIdWithEditorIds);
                        default ->
                                consumeElement(reader);
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
                    if ("FirstName".equals(elementName)) {
                        firstName = reader.getElementText();
                    } else if ("LastName".equals(elementName)) {
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
                    if ("Name".equals(elementName)) {
                        keywordName = reader.getElementText();
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (keywordName == null) {
                        LOGGER.error("No keyword name found for keyword with id {}. Please check if the keyword name is present in the XML file and if the keyword name is not empty.", id);
                        return;
                    }

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
                    if ("Publisher".equals(elementName)) {
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
                    if ("Name".equals(elementName)) {
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
                    if ("Reference".equals(elementName)) {
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
                        case "ReferenceType" ->
                                referenceType = reader.getElementText();
                        case "Title" ->
                                title = reader.getElementText();
                        case "Year" ->
                                year = reader.getElementText();
                        case "Abstract" ->
                                abstractText = reader.getElementText();
                        case "PageRange" ->
                                pageRange = reader.getElementText();
                        case "PageCount" ->
                                pageCount = reader.getElementText();
                        case "Volume" ->
                                volume = reader.getElementText();
                        case "Doi" ->
                                doi = reader.getElementText();
                        case "Isbn" ->
                                isbn = reader.getElementText();
                        default ->
                                consumeElement(reader);
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
                        case "ReferenceID" ->
                                referenceId = reader.getElementText();
                        case "CoreStatement" ->
                                coreStatement = reader.getElementText();
                        case "Text" ->
                                text = reader.getElementText();
                        case "PageRangeNumber" ->
                                pageRangeNumber = reader.getElementText();
                        case "QuotationType" ->
                                quotationType = reader.getElementText();
                        case "QuotationIndex" ->
                                quotationIndex = reader.getElementText();
                        default ->
                                consumeElement(reader);
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

    private void parseReferenceIdLink(XMLStreamReader reader, String startElement, Map<String, List<String>> targetMap) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String elementName = reader.getLocalName();
                    if ("OnetoN".equals(elementName)) {
                        String rawString = reader.getElementText();
                        if (rawString != null && rawString.length() > UUID_SEMICOLON_OFFSET_INDEX) {
                            String referenceId = rawString.substring(0, UUID_LENGTH);
                            String attributeIds = rawString.substring(UUID_SEMICOLON_OFFSET_INDEX);
                            List<String> attributeIdList = Arrays.asList(attributeIds.split(";"));
                            targetMap.put(referenceId, attributeIdList);
                        }
                    } else {
                        consumeElement(reader);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (startElement.equals(reader.getLocalName())) {
                        return;
                    }
                }
            }
        }
    }

    private List<BibEntry> buildBibItems() {
        List<BibEntry> bibItems = new ArrayList<>();

        Map<String, String> resolvedAuthorMap = resolvePersonMap(refIdWithAuthorIds, knownPersons);
        Map<String, String> resolvedEditorMap = resolvePersonMap(refIdWithEditorIds, knownPersons);
        Map<String, String> resolvedPublisherMap = resolvePublisherMap(refIdWithPublisherIds, knownPublishers);
        Map<String, String> resolvedKeywordMap = resolveKeywordMap(refIdWithKeywordsIds, knownKeywords);

        Map<String, List<KnowledgeItem>> knowledgeItemsByRefId = knowledgeItems.stream()
                                                                               .filter(item -> item.referenceId() != null && !item.referenceId().isEmpty())
                                                                               .collect(Collectors.groupingBy(KnowledgeItem::referenceId));

        for (Reference reference : references) {
            BibEntry entry = new BibEntry();
            setEntryFieldsFromReference(entry, reference);

            String authors = resolvedAuthorMap.get(reference.id());
            String editors = resolvedEditorMap.get(reference.id());
            String publishers = resolvedPublisherMap.get(reference.id());
            String keywords = resolvedKeywordMap.get(reference.id());

            Optional.ofNullable(authors)
                    .ifPresent(value -> entry.setField(StandardField.AUTHOR, clean(value)));
            Optional.ofNullable(editors)
                    .ifPresent(value -> entry.setField(StandardField.EDITOR, clean(value)));
            Optional.ofNullable(publishers)
                    .ifPresent(value -> entry.setField(StandardField.PUBLISHER, clean(value)));
            Optional.ofNullable(keywords)
                    .ifPresent(value -> entry.setField(StandardField.KEYWORDS, clean(value)));

            Optional.ofNullable(getKnowledgeItem(knowledgeItemsByRefId, reference))
                    .ifPresent(value -> entry.setField(StandardField.COMMENT, StringUtil.unifyLineBreaks(value, "\n")));

            bibItems.add(entry);
        }
        return bibItems;
    }

    private void setEntryFieldsFromReference(BibEntry entry, Reference reference) {
        entry.setType(getType(reference));

        Optional.ofNullable(reference.title())
                .ifPresent(value -> entry.setField(StandardField.TITLE, clean(value)));
        Optional.ofNullable(reference.abstractText())
                .ifPresent(value -> entry.setField(StandardField.ABSTRACT, clean(value)));
        Optional.ofNullable(reference.year())
                .ifPresent(value -> entry.setField(StandardField.YEAR, clean(value)));
        Optional.ofNullable(reference.doi())
                .ifPresent(value -> entry.setField(StandardField.DOI, clean(value)));
        Optional.ofNullable(reference.isbn())
                .ifPresent(value -> entry.setField(StandardField.ISBN, clean(value)));
        Optional.ofNullable(reference.volume())
                .ifPresent(value -> entry.setField(StandardField.VOLUME, clean(value)));

        String pages = clean(getPages(reference.pageRange(), reference.pageCount()));
        pages = pagesFormatter.format(pages);
        entry.setField(StandardField.PAGES, pages);
    }

    private Map<String, String> resolvePersonMap(Map<String, List<String>> referenceIdMap, Map<String, Author> personMap) {
        Map<String, String> resolvedPersonMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : referenceIdMap.entrySet()) {
            String referenceId = entry.getKey();
            List<String> personIds = entry.getValue();

            List<Author> authorsForThisReferenceId = personIds.stream()
                                                              .map(personMap::get)
                                                              .filter(Objects::nonNull)
                                                              .toList();

            if (!authorsForThisReferenceId.isEmpty()) {
                String stringifiedAuthors = AuthorList.of(authorsForThisReferenceId).getAsLastFirstNamesWithAnd(false);
                resolvedPersonMap.put(referenceId, stringifiedAuthors);
            }
        }
        return resolvedPersonMap;
    }

    private Map<String, String> resolvePublisherMap(Map<String, List<String>> referenceIdMap, Map<String, String> publisherMap) {
        Map<String, String> resolvedPublisherMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : referenceIdMap.entrySet()) {
            String referenceId = entry.getKey();
            List<String> publisherIds = entry.getValue();

            List<String> publisherList = publisherIds.stream().
                                                     map(publisherMap::get).
                                                     filter(Objects::nonNull).
                                                     toList();

            if (!publisherList.isEmpty()) {
                String stringifiedPublishers = String.join(",", publisherList);
                resolvedPublisherMap.put(referenceId, stringifiedPublishers);
            }
        }
        return resolvedPublisherMap;
    }

    private Map<String, String> resolveKeywordMap(Map<String, List<String>> referenceIdMap, Map<String, Keyword> keywordMap) {
        Map<String, String> resolvedKeywordMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : referenceIdMap.entrySet()) {
            String referenceId = entry.getKey();
            List<String> keywordIds = entry.getValue();

            List<Keyword> keywordList = keywordIds.stream().
                                                  map(keywordMap::get).
                                                  filter(Objects::nonNull).
                                                  toList();

            if (!keywordList.isEmpty()) {
                KeywordList list = new KeywordList(List.copyOf(keywordList));
                String stringifiedKeyword = list.toString();
                resolvedKeywordMap.put(referenceId, stringifiedKeyword);
            }
        }
        return resolvedKeywordMap;
    }

    private String getKnowledgeItem(Map<String, List<KnowledgeItem>> groupedKnowledgeItemMap, Reference reference) {
        StringJoiner comment = new StringJoiner("\n\n");

        List<KnowledgeItem> relevantKnowledgeItems = groupedKnowledgeItemMap.get(reference.id());

        if (relevantKnowledgeItems == null || relevantKnowledgeItems.isEmpty()) {
            return "";
        }
        for (KnowledgeItem knowledgeItem : relevantKnowledgeItems) {
            Optional.ofNullable(knowledgeItem.coreStatement())
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(t -> comment.add("# " + cleanUpText(t)));

            Optional.ofNullable(knowledgeItem.text())
                    .filter(Predicate.not(String::isEmpty))
                    .ifPresent(t -> comment.add(cleanUpText(t)));

            try {
                Optional<Integer> pages = Optional.ofNullable(knowledgeItem.pageRangeNumber())
                                                  .map(Integer::parseInt)
                                                  .filter(range -> range != -1);
                pages.ifPresent(p -> comment.add("page range: " + p));
            } catch (NumberFormatException e) {
                // If the string is not a number, we replicate behaviour by leaving the optional empty
            }

            try {
                Optional<String> quotationTypeDesc = Optional.ofNullable(knowledgeItem.quotationType())
                                                             .map(Short::parseShort)
                                                             .flatMap(type -> QUOTATION_TYPES.stream()
                                                                                             .filter(qt -> type == qt.getCitaviIndexType())
                                                                                             .map(QuotationTypeMapping::getName)
                                                                                             .findFirst());
                quotationTypeDesc.ifPresent(qt -> comment.add("quotation type: %s".formatted(qt)));
            } catch (NumberFormatException e) {
                // If the string is not a number, we replicate behaviour by leaving the optional empty
            }

            try {
                Optional<Short> quotationIndex = Optional.ofNullable(knowledgeItem.quotationIndex())
                                                         .map(Short::parseShort);
                quotationIndex.ifPresent(index -> comment.add("quotation index: %d".formatted(index)));
            } catch (NumberFormatException e) {
                // If the string is not a number, we replicate behaviour by leaving the optional empty
            }
        }

        return comment.toString();
    }

    private EntryType getType(Reference reference) {
        return Optional.ofNullable(reference.referenceType())
                       .map(CitaviXmlImporter::convertRefNameToType)
                       .orElse(StandardEntryType.Article);
    }

    private static EntryType convertRefNameToType(String refName) {
        return switch (refName.toLowerCase().trim()) {
            case "artwork",
                 "generic",
                 "musicalbum",
                 "audioorvideodocument",
                 "movie" ->
                    StandardEntryType.Misc;
            case "electronic article" ->
                    IEEETranEntryType.Electronic;
            case "book section" ->
                    StandardEntryType.InBook;
            case "book",
                 "bookedited",
                 "audiobook" ->
                    StandardEntryType.Book;
            case "report" ->
                    StandardEntryType.Report;
            default ->
                    StandardEntryType.Article;
        };
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
    public ParserResult importDatabase(@NonNull BufferedReader reader) throws IOException {
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

    /**
     * {@code PageRange} and {@code PageCount} tags contain text
     * with additional markers that need to be discarded.
     * <p>
     * Example {@code PageCount}:
     * {@snippet :
     *   <PageCount>
     *   <c>113</c> <in>true</in> <os>113</os> <ps>113</ps>
     *   </PageCount>
     *}
     * Contents of {@code PageCount} after parsing above example data:
     * {@snippet :
     *   <c>113</c> <in>true</in> <os>113</os> <ps>113</ps>
     *}
     * Content of "ps" tag is returned by {@code getPages}.
     * <p>
     * Example {@code PageRange}:
     * {@snippet :
     *   <PageRange>
     *   <![CDATA[
     *     <sp> <n>34165</n> <in>true</in> <os>34165</os> <ps>34165</ps> </sp>
     *     <ep> <n>34223</n> <in>true</in> <os>34223</os> <ps>34223</ps> </ep>
     *     <os>34165-223</os>
     *   ]]>
     *   </PageRange>
     *}
     * Contents of {@code PageRange} after parsing above example data:
     * {@snippet :
     *   <sp> <n>24</n> <in>true</in> <os>24</os> <ps>24</ps> </sp>
     *   <ep> <n>31</n> <in>true</in> <os>31</os> <ps>31</ps> </ep>
     *   <os>24-31</os>
     *}
     * Content of "os" tag is returned by {@code getPages}.
     */
    private String getPages(String pageRange, String pageCount) {
        String tmpStr = "";
        if ((pageCount != null) && (pageRange == null)) {
            tmpStr = pageCount;
        } else if ((pageCount == null) && (pageRange != null)) {
            tmpStr = pageRange;
        } else if (pageCount == null) {
            return tmpStr;
        }
        int count = 0;
        String pages = "";
        for (int i = tmpStr.length() - 1; i >= 0; i--) {
            if (count == 2) {
                pages = tmpStr.substring(i + 2, tmpStr.length() - END_TAG_CHARACTER_COUNT); // extract tag content, skipping first 2 chars ("s>") and trimming closing tag
                break;
            } else {
                if (tmpStr.charAt(i) == '>') {
                    count++;
                }
            }
        }
        return pages;
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

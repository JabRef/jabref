package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.KnowledgeItems;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.KnowledgeItems.KnowledgeItem;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.Persons.Person;
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
import org.jabref.model.strings.StringUtil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private static final byte UUID_LENGTH = 36;
    private static final byte UUID_SEMICOLON_OFFSET_INDEX = 37;
    private final NormalizePagesFormatter pagesFormatter = new NormalizePagesFormatter();

    private final Map<String, Author> knownPersons = new HashMap<>();
    private final Map<String, Keyword> knownKeywords = new HashMap<>();
    private final Map<String, String> knownPublishers = new HashMap<>();

    private Map<String, String> refIdWithAuthors = new HashMap<>();
    private Map<String, String> refIdWithEditors = new HashMap<>();
    private Map<String, String> refIdWithKeywords = new HashMap<>();
    private Map<String, String> refIdWithPublishers = new HashMap<>();

    private CitaviExchangeData.Persons persons;
    private CitaviExchangeData.Keywords keywords;
    private CitaviExchangeData.Publishers publishers;
    private KnowledgeItems knowledgeItems;

    private CitaviExchangeData.ReferenceAuthors refAuthors;
    private CitaviExchangeData.ReferenceEditors refEditors;
    private CitaviExchangeData.ReferenceKeywords refKeywords;
    private CitaviExchangeData.ReferencePublishers refPublishers;

    private Unmarshaller unmarshaller;

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
        return "Importer for the Citavi XML format.";
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

        try (BufferedReader reader = getReaderFromZip(filePath)) {
            Object unmarshalledObject = unmarshallRoot(reader);

            if (unmarshalledObject instanceof CitaviExchangeData) {
                // Check whether we have an article set, an article, a book article or a book article set
                CitaviExchangeData data = (CitaviExchangeData) unmarshalledObject;
                List<BibEntry> bibEntries = parseDataList(data);

                return new ParserResult(bibEntries);
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }
    }

    private List<BibEntry> parseDataList(CitaviExchangeData data) {
        List<BibEntry> bibEntries = new ArrayList<>();

        persons = data.getPersons();
        keywords = data.getKeywords();
        publishers = data.getPublishers();
        knowledgeItems = data.getKnowledgeItems();

        refAuthors = data.getReferenceAuthors();
        refEditors = data.getReferenceEditors();
        refKeywords = data.getReferenceKeywords();
        refPublishers = data.getReferencePublishers();

        if (refAuthors != null) {
            this.refIdWithAuthors = buildPersonList(refAuthors.getOnetoN());
        }
        if (refEditors != null) {
            this.refIdWithEditors = buildPersonList(refEditors.getOnetoN());
        }
        if (refKeywords != null) {
            this.refIdWithKeywords = buildKeywordList(refKeywords.getOnetoN());
        }
        if (refPublishers != null) {
            this.refIdWithPublishers = buildPublisherList(refPublishers.getOnetoN());
        }

        bibEntries = data
                         .getReferences().getReference()
                         .stream()
                         .map(this::parseData)
                         .collect(Collectors.toList());

        return bibEntries;
    }

    private BibEntry parseData(CitaviExchangeData.References.Reference data) {
        BibEntry entry = new BibEntry();

        entry.setType(getType(data));
        Optional.ofNullable(data.getTitle())
                .ifPresent(value -> entry.setField(StandardField.TITLE, clean(value)));
        Optional.ofNullable(data.getAbstract())
                .ifPresent(value -> entry.setField(StandardField.ABSTRACT, clean(value)));
        Optional.ofNullable(data.getYear())
                .ifPresent(value -> entry.setField(StandardField.YEAR, clean(value)));
        Optional.ofNullable(data.getDoi())
                .ifPresent(value -> entry.setField(StandardField.DOI, clean(value)));
        Optional.ofNullable(data.getIsbn())
                .ifPresent(value -> entry.setField(StandardField.ISBN, clean(value)));

        String pages = clean(getPages(data));
        // Cleans also unicode minus signs
        pages = pagesFormatter.format(pages);
        entry.setField(StandardField.PAGES, pages);

        Optional.ofNullable(data.getVolume())
                .ifPresent(value -> entry.setField(StandardField.VOLUME, clean(value)));
        Optional.ofNullable(getAuthorName(data))
                .ifPresent(value -> entry.setField(StandardField.AUTHOR, value));
        Optional.ofNullable(getEditorName(data))
                .ifPresent(value -> entry.setField(StandardField.EDITOR, value));
        Optional.ofNullable(getKeywords(data))
                .ifPresent(value -> entry.setField(StandardField.KEYWORDS, value));
        Optional.ofNullable(getPublisher(data))
                .ifPresent(value -> entry.setField(StandardField.PUBLISHER, value));
        Optional.ofNullable(getKnowledgeItem(data))
                .ifPresent(value -> entry.setField(StandardField.COMMENT, value));
        return entry;
    }

    private EntryType getType(CitaviExchangeData.References.Reference data) {
        return Optional.ofNullable(data.getReferenceType())
                       .map(CitaviXmlImporter::convertRefNameToType)
                       .orElse(StandardEntryType.Article);
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

    private String getPages(CitaviExchangeData.References.Reference data) {
        String tmpStr = "";
        if ((data.getPageCount() != null) && (data.getPageRange() == null)) {
            tmpStr = data.getPageCount();
        } else if ((data.getPageCount() == null) && (data.getPageRange() != null)) {
            tmpStr = data.getPageRange();
        } else if ((data.getPageCount() == null) && (data.getPageRange() == null)) {
            return tmpStr;
        }
        int count = 0;
        String pages = "";
        for (int i = tmpStr.length() - 1; i >= 0; i--) {
            if (count == 2) {
                pages = tmpStr.substring(i + 2, (tmpStr.length() - 1 - 5) + 1);
                break;
            } else {
                if (tmpStr.charAt(i) == '>') {
                    count++;
                }
            }
        }
        return pages;
    }

    private String getAuthorName(CitaviExchangeData.References.Reference data) {
        if (refAuthors == null) {
            return null;
        }

        return this.refIdWithAuthors.get(data.getId());
    }

    private Map<String, String> buildPersonList(List<String> authorsOrEditors) {
        Map<String, String> refToPerson = new HashMap<>();

        for (String idStringsWithSemicolon : authorsOrEditors) {
            String refId = idStringsWithSemicolon.substring(0, UUID_LENGTH);
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX, idStringsWithSemicolon.length());

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
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX, idStringsWithSemicolon.length());

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
            String rest = idStringsWithSemicolon.substring(UUID_SEMICOLON_OFFSET_INDEX, idStringsWithSemicolon.length());

            String[] publisherIds = rest.split(";");

            List<String> jabrefPublishers = new ArrayList<>();

            for (String pubId : publisherIds) {
                // store publishers already encountered
                knownPublishers.computeIfAbsent(pubId, k -> {
                    Optional<CitaviExchangeData.Publishers.Publisher> publisher = publishers.getPublisher().stream().filter(p -> p.getId().equals(k)).findFirst();
                    return publisher.map(p -> new String(p.getName())).orElse(null);
                });
                jabrefPublishers.add(knownPublishers.get(pubId));
            }

            String stringifiedKeywords = String.join(",", jabrefPublishers);
            refToPublishers.put(refId, stringifiedKeywords);
        }
        return refToPublishers;
    }

    private String getEditorName(CitaviExchangeData.References.Reference data) {
        if (refEditors == null) {
            return null;
        }
        return this.refIdWithEditors.get(data.getId());
    }

    private String getKeywords(CitaviExchangeData.References.Reference data) {
        if (refKeywords == null) {
            return null;
        }
        return this.refIdWithKeywords.get(data.getId());
    }

    private String getPublisher(CitaviExchangeData.References.Reference data) {
        if (refPublishers == null) {
            return null;
        }
        return this.refIdWithPublishers.get(data.getId());
    }

    private String getKnowledgeItem(CitaviExchangeData.References.Reference data) {
        Optional<KnowledgeItem> knowledgeItem = knowledgeItems.getKnowledgeItem().stream().filter(p -> data.getId().equals(p.getReferenceID())).findFirst();

        StringBuilder comment = new StringBuilder();
        Optional<String> title = knowledgeItem.map(item -> item.getCoreStatement());
        title.ifPresent(t -> comment.append("#").append(t).append("\n\n"));
        Optional<String> text = knowledgeItem.map(item -> item.getText());
        text.ifPresent(t -> comment.append(t).append("\n"));
        Optional<String> pages = knowledgeItem.map(item -> item.getPageRange());
        pages.ifPresent(p -> comment.append(p));

        return comment.toString();
    }

    private void initUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            // Lazy init because this is expensive
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.citavi");
            unmarshaller = context.createUnmarshaller();
        }
    }

    private Object unmarshallRoot(BufferedReader reader) throws XMLStreamException, JAXBException {
        initUnmarshaller();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

        // Go to the root element
        while (!xmlStreamReader.isStartElement()) {
            xmlStreamReader.next();
        }

        return unmarshaller.unmarshal(xmlStreamReader);
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("CitaviXmlImporter does not support importDatabase(BufferedReader reader)."
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
        return Collections.emptyList();
    }

    private BufferedReader getReaderFromZip(Path filePath) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();

        Path newFile = Files.createTempFile("citavicontent", ".xml");

        while (zipEntry != null) {
            Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();

        InputStream stream = Files.newInputStream(newFile, StandardOpenOption.READ);

        // check and delete the utf-8 BOM bytes
        InputStream newStream = checkForUtf8BOMAndDiscardIfAny(stream);

        // clean up the temp file
        Files.delete(newFile);

        return new BufferedReader(new InputStreamReader(newStream, StandardCharsets.UTF_8));
    }

    private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF))) {
                pushbackInputStream.unread(bom);
            }
        }
        return pushbackInputStream;
    }

    private String clean(String input) {
        return StringUtil.unifyLineBreaks(input, " ")
                         .trim()
                         .replaceAll(" +", " ");
    }
}

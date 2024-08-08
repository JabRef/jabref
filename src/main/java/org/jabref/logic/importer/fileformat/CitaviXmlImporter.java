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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
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
    private final XMLInputFactory xmlInputFactory;
    private Map<String, String> refIdWithAuthors = new HashMap<>();
    private Map<String, String> refIdWithEditors = new HashMap<>();
    private Map<String, String> refIdWithKeywords = new HashMap<>();
    private Map<String, String> refIdWithPublishers = new HashMap<>();

    private List<Author> persons;
    private List<Keyword> keywords;
    private List<String> publishers;

    private String refAuthors;
    private String refEditors;
    private String refKeywords;
    private String refPublishers;

    private XmlMapper mapper;

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
        List<BibEntry> bibEntries = null;

        try (BufferedReader reader = getReaderFromZip(filePath)) {
            Object mapperObject = mapperRoot(reader);

            if (mapperObject instanceof XMLStreamReader data) {
                while (data.hasNext()){
                    data.next();
                    bibEntries.addAll(parseDataList(data));
                }
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
        }

        return new ParserResult(bibEntries);
    }

    private List<BibEntry> parseDataList(XMLStreamReader reader) {
        List<BibEntry> bibEntries = null;
        String lastName = "";
        String foreName = "";

        try {
            while (reader.hasNext()) {
                reader.next();
                String elementName = reader.getName().getLocalPart();

                if (elementName == null) {
                    break;
                }
                else {
                    if (elementName.equals("Author")) {
                        this.refIdWithAuthors.put("PersonList", reader.getText());
                        bibEntries.add(new BibEntry(reader.getText()));
                    }
                    if (elementName.equals("LastName")) {
                        reader.next();
                        lastName = reader.getText();
                    }
                    if (elementName.equals("ForeName")){
                        reader.next();
                        foreName = reader.getText();
                        persons.add(new Author(lastName, "", "", foreName, ""));
                    }
                    if (elementName.equals("PublisherLocation"))       {
                        this.refIdWithEditors.put("", reader.getText());
                    }
                    if (elementName.equals("Keyword")) {
                        this.refIdWithKeywords.put("KeywordList", reader.getText());
                        keywords.add(new Keyword(reader.getText()));
                    }
                    if (elementName.equals("PublisherName")) {
                        this.refIdWithPublishers.put("PublisherList", reader.getText());
                        publishers.add(reader.getText());
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
        }

        return bibEntries;
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

    private void initMapper() throws Exception {
        // Lazy init because this is expensive
        if (mapper == null) {
            mapper = XmlMapper.xmlBuilder().addModule(new JaxbAnnotationModule()).build();
        }
    }

    private Object mapperRoot(BufferedReader reader) throws XMLStreamException {
        Objects.requireNonNull(reader);

        try {
            initMapper();

            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

            // Go to the root element
            while (!xmlStreamReader.isStartElement()) {
                xmlStreamReader.next();
            }

            return mapper.readValue(xmlStreamReader, Object.class);
        } catch (Exception e) {
            LOGGER.debug("could not read document", e);
            return ParserResult.fromError(e);
        }
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
        return Collections.emptyList();
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
                        new BOMInputStream(
                                Files.newInputStream(newFile, StandardOpenOption.READ),
                                false,
                                ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE)));
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

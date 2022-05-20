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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
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

/**
 * Importer for the citavi XML format.
 */

public class CitaviXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitaviXmlImporter.class);
    private final ImportFormatPreferences preferences;
    private Unmarshaller unmarshaller;
    private CitaviExchangeData.ReferenceAuthors authors;
    private CitaviExchangeData.Persons persons;

    public CitaviXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
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
        BufferedReader reader = getReaderFromZip(filePath);

        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase(Locale.ENGLISH).contains("citaviexchangedata")) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        BufferedReader reader = getReaderFromZip(filePath);

        Objects.requireNonNull(reader);

        try {
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
        BibEntry entry = new BibEntry();
        authors = data.getReferenceAuthors();
        persons = data.getPersons();
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
        Optional.ofNullable(getPages(data))
                .ifPresent(value -> entry.setField(StandardField.PAGES, clean(value)));
        Optional.ofNullable(data.getVolume())
                .ifPresent(value -> entry.setField(StandardField.VOLUME, clean(value)));
        Optional.ofNullable(getAuthorName(data))
                .ifPresent(value -> entry.setField(StandardField.AUTHOR, value));

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
        if (data.getPageCount() != null && data.getPageRange() == null) {
            tmpStr = data.getPageCount();
        } else if (data.getPageCount() == null && data.getPageRange() != null) {
            tmpStr = data.getPageRange();
        } else if (data.getPageCount() == null && data.getPageRange() == null) {
            return null;
        }
        int count = 0;
        String pages = "";
        for (int i = tmpStr.length() - 1; i >= 0; i--) {
            if (count == 2) {
                pages = tmpStr.substring(i + 2, tmpStr.length() - 1 - 5 + 1);
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
        int count = 0;
        int ref = 0;
        int start = 0;
        StringBuilder names = new StringBuilder();
        outerloop:
        for (int i = 0; i < authors.getOnetoN().size(); i++) {
            for (int j = 0; j < authors.getOnetoN().get(i).length(); j++) {
                if (authors.getOnetoN().get(i).charAt(j) == ';') {
                    count++;
                    if (count == 1) {
                        if (authors.getOnetoN().get(i).substring(0, j).equals(data.getId())) {
                            ref = i;
                            break outerloop;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < authors.getOnetoN().get(ref).length(); i++) {
            if (authors.getOnetoN().get(ref).charAt(i) == ';') {
                count++;
                if (count == 1) {
                    start = i + 1;
                } else if (count > 1) {
                    for (int j = 0; j < persons.getPerson().size(); j++) {
                        if (authors.getOnetoN().get(ref).substring(start, i).equals(persons.getPerson().get(j).getId())) {
                            if (i == authors.getOnetoN().get(ref).length() - 1) {
                                names.append(persons.getPerson().get(j).getFirstName()).append(" ").append(persons.getPerson().get(j).getLastName());
                            } else {
                                names.append(persons.getPerson().get(j).getFirstName()).append(" ").append(persons.getPerson().get(j).getLastName()).append(",");
                            }
                        }
                    }
                    start = i + 1;
                }
            }
        }
        return names.toString();
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

        return new BufferedReader(new InputStreamReader(newStream, StandardCharsets.UTF_8));
    }

    private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
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

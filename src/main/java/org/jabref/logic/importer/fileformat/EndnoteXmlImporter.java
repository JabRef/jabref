package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.endnote.Keywords;
import org.jabref.logic.importer.fileformat.endnote.PdfUrls;
import org.jabref.logic.importer.fileformat.endnote.Record;
import org.jabref.logic.importer.fileformat.endnote.Xml;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the Endnote XML format.
 *
 * Based on dtd scheme downloaded from Article #122577 in http://kbportal.thomson.com.
 */
public class EndnoteXmlImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndnoteXmlImporter.class);
    private final ImportFormatPreferences preferences;
    private Unmarshaller unmarshaller;

    public EndnoteXmlImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "EndNote XML";
    }

    @Override
    public FileType getFileType() {
        return FileType.ENDNOTE_XML;
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
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        try {
            Object unmarshalledObject = unmarshallRoot(reader);

            if (unmarshalledObject instanceof Xml) {
                // Check whether we have an article set, an article, a book article or a book article set
                Xml root = (Xml) unmarshalledObject;
                List<BibEntry> bibEntries = root.getRecords()
                        .getRecord()
                        .stream()
                        .map(this::parseRecord)
                        .collect(Collectors.toList());

                return new ParserResult(bibEntries);
            } else {
                return ParserResult.fromErrorMessage("File does not start with xml tag.");
            }
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.debug("could not parse document", e);
            return ParserResult.fromError(e);
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

    private void initUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            // Lazy init because this is expensive
            JAXBContext context = JAXBContext.newInstance("org.jabref.logic.importer.fileformat.endnote");
            unmarshaller = context.createUnmarshaller();
        }
    }

    private BibEntry parseRecord(Record record) {
        BibEntry entry = new BibEntry();

        entry.setType(BiblatexEntryTypes.ARTICLE);
        Optional.ofNullable(getAuthors(record))
                .ifPresent(value -> entry.setField(FieldName.AUTHOR, value));
        Optional.ofNullable(clean(record.getTitles().getTitle().getStyle().getvalue()))
                .ifPresent(value -> entry.setField(FieldName.TITLE, value));
        Optional.ofNullable(record.getPages())
                .map(value -> value.getStyle().getvalue())
                .ifPresent(value -> entry.setField(FieldName.PAGES, value));
        Optional.ofNullable(record.getNumber())
                .map(value -> value.getStyle().getvalue())
                .ifPresent(value -> entry.setField(FieldName.NUMBER, value));

        Optional.ofNullable(record.getVolume())
                .flatMap(value -> Optional.ofNullable(value.getStyle())
                        .map(values -> value.getStyle().getvalue()))
                .ifPresent(value -> entry.setField(FieldName.VOLUME, value));

        entry.setField(FieldName.YEAR, record.getDates().getYear().getStyle().getvalue());
        entry.putKeywords(getKeywords(record), preferences.getKeywordSeparator());
        Optional.ofNullable(record.getAbstract())
                .map(value -> value.getStyle().getvalue().trim())
                .ifPresent(value -> entry.setField(FieldName.ABSTRACT, value));
        entry.setFiles(getLinkedFiles(record));
        Optional.ofNullable(record.getIsbn())
                .map(value -> clean(value.getStyle().getvalue()))
                .ifPresent(value -> entry.setField(FieldName.ISBN, value));
        Optional.ofNullable(record.getElectronicResourceNum())
                .map(doi -> doi.getStyle().getvalue().trim())
                .ifPresent(doi -> entry.setField(FieldName.DOI, doi));

        return entry;
    }

    private List<LinkedFile> getLinkedFiles(Record record) {
        PdfUrls pdfUrls = record.getUrls()
                .getPdfUrls();
        if (pdfUrls != null) {
            return pdfUrls
                    .getUrl()
                    .stream()
                    .map(url -> new LinkedFile("", clean(url.getStyle().getvalue()), "PDF"))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getKeywords(Record record) {
        Keywords keywords = record.getKeywords();
        if (keywords != null) {
            return keywords.getKeyword()
                    .stream()
                    .map(keyword -> keyword.getStyle().getvalue())
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private String getAuthors(Record record) {
        return record.getContributors()
                .getAuthors()
                .getAuthor()
                .stream()
                .map(author -> author.getStyle().getvalue())
                .collect(Collectors.joining(" and "));
    }

    private String clean(String input) {
        return StringUtil.unifyLineBreaks(input, " ")
                .trim()
                .replaceAll(" +", " ");
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

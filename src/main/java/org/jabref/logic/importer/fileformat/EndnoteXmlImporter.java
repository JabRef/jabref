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
import org.jabref.logic.importer.fileformat.endnote.Abstract;
import org.jabref.logic.importer.fileformat.endnote.Authors;
import org.jabref.logic.importer.fileformat.endnote.Contributors;
import org.jabref.logic.importer.fileformat.endnote.Dates;
import org.jabref.logic.importer.fileformat.endnote.ElectronicResourceNum;
import org.jabref.logic.importer.fileformat.endnote.Isbn;
import org.jabref.logic.importer.fileformat.endnote.Keywords;
import org.jabref.logic.importer.fileformat.endnote.Notes;
import org.jabref.logic.importer.fileformat.endnote.Number;
import org.jabref.logic.importer.fileformat.endnote.Pages;
import org.jabref.logic.importer.fileformat.endnote.PdfUrls;
import org.jabref.logic.importer.fileformat.endnote.Record;
import org.jabref.logic.importer.fileformat.endnote.RefType;
import org.jabref.logic.importer.fileformat.endnote.RelatedUrls;
import org.jabref.logic.importer.fileformat.endnote.SecondaryTitle;
import org.jabref.logic.importer.fileformat.endnote.Style;
import org.jabref.logic.importer.fileformat.endnote.Title;
import org.jabref.logic.importer.fileformat.endnote.Titles;
import org.jabref.logic.importer.fileformat.endnote.Url;
import org.jabref.logic.importer.fileformat.endnote.Urls;
import org.jabref.logic.importer.fileformat.endnote.Volume;
import org.jabref.logic.importer.fileformat.endnote.Xml;
import org.jabref.logic.importer.fileformat.endnote.Year;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryType;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

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

    private static BiblatexEntryType convertRefNameToType(String refName) {
        switch (refName.toLowerCase().trim()) {
            case "artwork":
                return BiblatexEntryTypes.MISC;
            case "generic":
                return BiblatexEntryTypes.MISC;
            case "electronic rticle":
                return BiblatexEntryTypes.ELECTRONIC;
            case "book section":
                return BiblatexEntryTypes.INBOOK;
            case "book":
                return BiblatexEntryTypes.BOOK;
            case "journal article":
                return BiblatexEntryTypes.ARTICLE;

            default:
                return BiblatexEntryTypes.ARTICLE;
        }
    }

    private BibEntry parseRecord(Record record) {
        BibEntry entry = new BibEntry();

        entry.setType(getType(record));
        Optional.ofNullable(getAuthors(record))
                .ifPresent(value -> entry.setField(FieldName.AUTHOR, value));
        Optional.ofNullable(record.getTitles())
                .map(Titles::getTitle)
                .map(Title::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.TITLE, clean(value)));
        Optional.ofNullable(record.getTitles())
                .map(Titles::getSecondaryTitle)
                .map(SecondaryTitle::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.JOURNAL, clean(value)));
        Optional.ofNullable(record.getPages())
                .map(Pages::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.PAGES, value));
        Optional.ofNullable(record.getNumber())
                .map(Number::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.NUMBER, value));
        Optional.ofNullable(record.getVolume())
                .map(Volume::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.VOLUME, value));
        Optional.ofNullable(record.getDates())
                .map(Dates::getYear)
                .map(Year::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.YEAR, value));
        Optional.ofNullable(record.getNotes())
                .map(Notes::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.NOTE, value.trim()));
        getUrl(record)
                .ifPresent(value -> entry.setField(FieldName.URL, value));
        entry.putKeywords(getKeywords(record), preferences.getKeywordSeparator());
        Optional.ofNullable(record.getAbstract())
                .map(Abstract::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.ABSTRACT, value.trim()));
        entry.setFiles(getLinkedFiles(record));
        Optional.ofNullable(record.getIsbn())
                .map(Isbn::getStyle)
                .map(Style::getvalue)
                .ifPresent(value -> entry.setField(FieldName.ISBN, clean(value)));
        Optional.ofNullable(record.getElectronicResourceNum())
                .map(ElectronicResourceNum::getStyle)
                .map(Style::getvalue)
                .ifPresent(doi -> entry.setField(FieldName.DOI, doi.trim()));

        return entry;
    }

    private BiblatexEntryType getType(Record record) {
        return Optional.ofNullable(record.getRefType())
                       .map(RefType::getName)
                       .map(EndnoteXmlImporter::convertRefNameToType)
                       .orElse(BiblatexEntryTypes.ARTICLE);
    }

    private List<LinkedFile> getLinkedFiles(Record record) {
        Optional<PdfUrls> urls = Optional.ofNullable(record.getUrls())
                                         .map(Urls::getPdfUrls);
        return OptionalUtil.toStream(urls)
                           .flatMap(pdfUrls -> pdfUrls.getUrl().stream())
                           .flatMap(url -> OptionalUtil.toStream(getUrlValue(url)))
                           .map(url -> new LinkedFile("", url, "PDF"))
                           .collect(Collectors.toList());
    }

    private Optional<String> getUrl(Record record) {
        Optional<RelatedUrls> urls = Optional.ofNullable(record.getUrls())
                                             .map(Urls::getRelatedUrls);
        return OptionalUtil.toStream(urls)
                           .flatMap(url -> url.getUrl().stream())
                           .flatMap(url -> OptionalUtil.toStream(getUrlValue(url)))
                           .findFirst();
    }

    private Optional<String> getUrlValue(Url url) {
        return Optional.ofNullable(url)
                       .map(Url::getStyle)
                       .map(Style::getvalue)
                       .map(this::clean);
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
        Optional<Authors> authors = Optional.ofNullable(record.getContributors())
                                            .map(Contributors::getAuthors);
        return OptionalUtil.toStream(authors)
                           .flatMap(value -> value.getAuthor().stream())
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

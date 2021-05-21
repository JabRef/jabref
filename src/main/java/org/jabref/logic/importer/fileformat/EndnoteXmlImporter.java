package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.endnote.Abstract;
import org.jabref.logic.importer.fileformat.endnote.Authors;
import org.jabref.logic.importer.fileformat.endnote.Contributors;
import org.jabref.logic.importer.fileformat.endnote.Dates;
import org.jabref.logic.importer.fileformat.endnote.ElectronicResourceNum;
import org.jabref.logic.importer.fileformat.endnote.Isbn;
import org.jabref.logic.importer.fileformat.endnote.Keywords;
import org.jabref.logic.importer.fileformat.endnote.Label;
import org.jabref.logic.importer.fileformat.endnote.Notes;
import org.jabref.logic.importer.fileformat.endnote.Number;
import org.jabref.logic.importer.fileformat.endnote.Pages;
import org.jabref.logic.importer.fileformat.endnote.PdfUrls;
import org.jabref.logic.importer.fileformat.endnote.Publisher;
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
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

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
    private Unmarshaller unmarshaller;

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
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        try {
            Object unmarshalledObject = unmarshallRoot(reader);

            if (unmarshalledObject instanceof Xml) {
                // Check whether we have an article set, an article, a book article or a book article set
                Xml root = (Xml) unmarshalledObject;
                List<BibEntry> bibEntries = root
                        .getRecords().getRecord()
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

    private BibEntry parseRecord(Record record) {
        BibEntry entry = new BibEntry();

        entry.setType(getType(record));
        Optional.ofNullable(getAuthors(record))
                .ifPresent(value -> entry.setField(StandardField.AUTHOR, value));
        Optional.ofNullable(record.getTitles())
                .map(Titles::getTitle)
                .map(Title::getStyle)
                .map(this::mergeStyleContents)
                .ifPresent(value -> entry.setField(StandardField.TITLE, clean(value)));
        Optional.ofNullable(record.getTitles())
                .map(Titles::getSecondaryTitle)
                .map(SecondaryTitle::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.JOURNAL, clean(value)));
        Optional.ofNullable(record.getPages())
                .map(Pages::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.PAGES, value));
        Optional.ofNullable(record.getNumber())
                .map(Number::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.NUMBER, value));
        Optional.ofNullable(record.getVolume())
                .map(Volume::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.VOLUME, value));
        Optional.ofNullable(record.getDates())
                .map(Dates::getYear)
                .map(Year::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.YEAR, value));
        Optional.ofNullable(record.getNotes())
                .map(Notes::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.NOTE, value.trim()));
        getUrl(record)
                .ifPresent(value -> entry.setField(StandardField.URL, value));
        entry.putKeywords(getKeywords(record), preferences.getKeywordSeparator());
        Optional.ofNullable(record.getAbstract())
                .map(Abstract::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.ABSTRACT, value.trim()));
        entry.setFiles(getLinkedFiles(record));
        Optional.ofNullable(record.getIsbn())
                .map(Isbn::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.ISBN, clean(value)));
        Optional.ofNullable(record.getElectronicResourceNum())
                .map(ElectronicResourceNum::getStyle)
                .map(Style::getContent)
                .ifPresent(doi -> entry.setField(StandardField.DOI, doi.trim()));
        Optional.ofNullable(record.getPublisher())
                .map(Publisher::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(StandardField.PUBLISHER, value));
        Optional.ofNullable(record.getLabel())
                .map(Label::getStyle)
                .map(Style::getContent)
                .ifPresent(value -> entry.setField(new UnknownField("endnote-label"), value));

        return entry;
    }

    private EntryType getType(Record record) {
        return Optional.ofNullable(record.getRefType())
                       .map(RefType::getName)
                       .map(EndnoteXmlImporter::convertRefNameToType)
                       .orElse(StandardEntryType.Article);
    }

    private List<LinkedFile> getLinkedFiles(Record record) {
        Optional<PdfUrls> urls = Optional.ofNullable(record.getUrls())
                                         .map(Urls::getPdfUrls);
        return OptionalUtil.toStream(urls)
                           .flatMap(pdfUrls -> pdfUrls.getUrl().stream())
                           .flatMap(url -> OptionalUtil.toStream(getUrlValue(url)))
                           .map(url -> {
                               try {
                                   return new LinkedFile(new URL(url), "PDF");
                               } catch (MalformedURLException e) {
                                   LOGGER.info("Unable to parse {}", url);
                                   return null;
                               }
                           })
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

    private String mergeStyleContents(List<Style> styles) {
        return styles.stream().map(Style::getContent).collect(Collectors.joining());
    }

    private Optional<String> getUrlValue(Url url) {
        Optional<List<Object>> urlContent = Optional.ofNullable(url).map(Url::getContent);
        List<Object> list = urlContent.orElse(Collections.emptyList());
        Optional<String> ret;
        if (list.size() == 0) {
            return Optional.empty();
        } else {
            boolean isStyleExist = false;
            int style_index = -1;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Style) {
                    isStyleExist = true;
                    style_index = i;
                }
            }
            if (!isStyleExist) {
                ret = Optional.ofNullable((String) list.get(0))
                        .map(this::clean);
            } else {
                ret = Optional.ofNullable((Style) list.get(style_index))
                        .map(Style::getContent)
                        .map(this::clean);
            }
        }
        return ret;
    }

    private List<String> getKeywords(Record record) {
        Keywords keywords = record.getKeywords();
        if (keywords != null) {

            return keywords.getKeyword()
                           .stream()
                           .map(keyword -> keyword.getStyle())
                           .filter(Objects::nonNull)
                           .map(style->style.getContent())
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
                           .map(author -> author.getStyle().getContent())
                           .collect(Collectors.joining(" and "));
    }

    private String clean(String input) {
        return StringUtil.unifyLineBreaks(input, " ")
                         .trim()
                         .replaceAll(" +", " ");
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

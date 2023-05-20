import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.KnowledgeItems;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.KnowledgeItems.KnowledgeItem;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.Persons.Person;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.Publishers.Publisher;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.ReferenceAuthors;
import org.jabref.logic.importer.fileformat.citavi.CitaviExchangeData.References.Reference;
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

    private Map<String, String> refIdWithAuthors = new HashMap<>();
    private Map<String, String> refIdWithEditors = new HashMap<>();
    private Map<String, String> refIdWithKeywords = new HashMap<>();
    private Map<String, String> refIdWithPublishers = new HashMap<>();

    private CitaviExchangeData.Persons persons;
    private CitaviExchangeData.Keywords keywords;
    private CitaviExchangeData.Publishers publishers;

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        throw new UnsupportedOperationException("Importing from BufferedReader is not supported. Please provide a file.");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(filePath))) {
            zipInputStream.getNextEntry(); // We only have one entry, so no need to iterate over the entries

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(zipInputStream);

            return parseXML(xmlReader);
        } catch (XMLStreamException e) {
            throw new IOException("Error parsing Citavi XML file", e);
        }
    }

    private ParserResult parseXML(XMLStreamReader xmlReader) throws XMLStreamException {
        List<BibEntry> entries = new ArrayList<>();
        BibEntry entry = null;
        String tagName = null;
        String knowledgeItemKey = null;
        boolean insideKnowledgeItem = false;

        while (xmlReader.hasNext()) {
            int event = xmlReader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    tagName = xmlReader.getLocalName();
                    if (tagName.equals("KnowledgeItem")) {
                        knowledgeItemKey = xmlReader.getAttributeValue(null, "KnowledgeItemKey");
                        insideKnowledgeItem = true;
                        entry = new BibEntry();
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    String text = xmlReader.getText().trim();
                    if (StringUtil.isBlank(text)) {
                        break;
                    }

                    if (insideKnowledgeItem) {
                        handleKnowledgeItemElement(entry, tagName, text);
                    } else {
                        handleRootElement(tagName, text);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    tagName = xmlReader.getLocalName();
                    if (tagName.equals("KnowledgeItem")) {
                        insideKnowledgeItem = false;
                        if (entry != null) {
                            entries.add(entry);
                        }
                        entry = null;
                    }
                    break;
                default:
                    break;
            }
        }

        return new ParserResult(entries);
    }

    private void handleRootElement(String tagName, String text) {
        switch (tagName) {
            case "Persons":
                persons = CitaviExchangeData.Persons.fromValue(text);
                break;
            case "Keywords":
                keywords = CitaviExchangeData.Keywords.fromValue(text);
                break;
            case "Publishers":
                publishers = CitaviExchangeData.Publishers.fromValue(text);
                break;
            default:
                break;
        }
    }

    private void handleKnowledgeItemElement(BibEntry entry, String tagName, String text) {
        switch (tagName) {
            case "ReferenceType":
                EntryType entryType = mapEntryType(text);
                entry.setType(entryType);
                break;
            case "Title":
                entry.setField(StandardField.TITLE, text);
                break;
            case "SubTitle":
                entry.setField(StandardField.SUBTITLE, text);
                break;
            case "ShortTitle":
                entry.setField(StandardField.SHORTTITLE, text);
                break;
            case "AuthorString":
                refIdWithAuthors.put(entry.getId(), text);
                break;
            case "EditorString":
                refIdWithEditors.put(entry.getId(), text);
                break;
            case "PublicationYear":
                entry.setField(StandardField.YEAR, text);
                break;
            case "PublicationYearEnd":
                entry.setField(StandardField.ENDDATE, text);
                break;
            case "PlaceOfPublication":
                entry.setField(StandardField.ADDRESS, text);
                break;
            case "Publisher":
                refIdWithPublishers.put(entry.getId(), text);
                break;
            case "Volume":
                entry.setField(StandardField.VOLUME, text);
                break;
            case "Edition":
                entry.setField(StandardField.EDITION, text);
                break;
            case "PublicationLanguage":
                entry.setField(StandardField.LANGUAGE, text.toLowerCase(Locale.ENGLISH));
                break;
            case "PageRange":
                entry.setField(StandardField.PAGES, pagesFormatter.format(text));
                break;
            case "PublicationType":
                handlePublicationType(entry, text);
                break;
            case "ReferenceKeywords":
                refIdWithKeywords.put(entry.getId(), text);
                break;
            case "ReferenceAbstract":
                entry.setField(StandardField.ABSTRACT, text);
                break;
            case "ReferenceNotes":
                entry.setField(StandardField.NOTE, text);
                break;
            default:
                break;
        }
    }

    private void handlePublicationType(BibEntry entry, String publicationType) {
        switch (publicationType) {
            case "ConferenceProceedings":
                entry.setType(StandardEntryType.InProceedings);
                break;
            case "JournalArticle":
                entry.setType(StandardEntryType.Article);
                break;
            case "Book":
                entry.setType(StandardEntryType.Book);
                break;
            case "BookSection":
                entry.setType(StandardEntryType.InBook);
                break;
            case "Report":
                entry.setType(StandardEntryType.TechReport);
                break;
            case "Thesis":
                entry.setType(StandardEntryType.PhdThesis);
                break;
            case "Webpage":
                entry.setType(StandardEntryType.Webpage);
                break;
            default:
                break;
        }
    }

    private EntryType mapEntryType(String referenceType) {
        for (QuotationTypeMapping mapping : QUOTATION_TYPES) {
            if (mapping.matches(referenceType)) {
                return mapping.getEntryType();
            }
        }
        return StandardEntryType.Misc;
    }
}

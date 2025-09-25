package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndnoteXmlExporter extends Exporter {

    private static final Map<EntryType, EndNoteType> ENTRY_TYPE_MAPPING = new HashMap<>();

    static {
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Article, new EndNoteType("Journal Article", 1));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Book, new EndNoteType("Book", 2));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InBook, new EndNoteType("Book Section", 3));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InCollection, new EndNoteType("Book Section", 4));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Proceedings, new EndNoteType("Conference Proceedings", 5));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.MastersThesis, new EndNoteType("Thesis", 6));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.PhdThesis, new EndNoteType("Thesis", 7));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.TechReport, new EndNoteType("Report", 8));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Unpublished, new EndNoteType("Manuscript", 9));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.InProceedings, new EndNoteType("Conference Paper", 10));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Conference, new EndNoteType("Conference", 11));
        ENTRY_TYPE_MAPPING.put(IEEETranEntryType.Patent, new EndNoteType("Patent", 12));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Online, new EndNoteType("Web Page", 13));
        ENTRY_TYPE_MAPPING.put(IEEETranEntryType.Electronic, new EndNoteType("Electronic Article", 14));
        ENTRY_TYPE_MAPPING.put(StandardEntryType.Misc, new EndNoteType("Generic", 15));
    }

    /**
     * Contains the mapping of all fields not explicitly handled by mapX methods.
     * We need a fixed order here, so we use a SequencedMap
     */
    private static final SequencedMap<Field, String> STANDARD_FIELD_MAPPING = new LinkedHashMap<>();

    static {
        STANDARD_FIELD_MAPPING.put(StandardField.PAGES, "pages");
        STANDARD_FIELD_MAPPING.put(StandardField.VOLUME, "volume");
        STANDARD_FIELD_MAPPING.put(StandardField.PUBLISHER, "publisher");
        STANDARD_FIELD_MAPPING.put(StandardField.ISBN, "isbn");
        STANDARD_FIELD_MAPPING.put(StandardField.DOI, "electronic-resource-num");
        STANDARD_FIELD_MAPPING.put(StandardField.ABSTRACT, "abstract");
        STANDARD_FIELD_MAPPING.put(StandardField.BOOKTITLE, "secondary-title");
        STANDARD_FIELD_MAPPING.put(StandardField.EDITION, "edition");
        STANDARD_FIELD_MAPPING.put(StandardField.SERIES, "tertiary-title");
        STANDARD_FIELD_MAPPING.put(StandardField.NUMBER, "number");
        STANDARD_FIELD_MAPPING.put(StandardField.ISSUE, "issue");
        STANDARD_FIELD_MAPPING.put(StandardField.LOCATION, "pub-location");
        STANDARD_FIELD_MAPPING.put(StandardField.CHAPTER, "section");
        STANDARD_FIELD_MAPPING.put(StandardField.HOWPUBLISHED, "work-type");
        STANDARD_FIELD_MAPPING.put(StandardField.ISSN, "issn");
        STANDARD_FIELD_MAPPING.put(StandardField.ADDRESS, "auth-address");
        STANDARD_FIELD_MAPPING.put(StandardField.PAGETOTAL, "page-total");
        STANDARD_FIELD_MAPPING.put(StandardField.NOTE, "notes");
        STANDARD_FIELD_MAPPING.put(StandardField.LABEL, "label");
        STANDARD_FIELD_MAPPING.put(StandardField.LANGUAGE, "language");
        STANDARD_FIELD_MAPPING.put(StandardField.KEY, "foreign-keys");
        STANDARD_FIELD_MAPPING.put(new UnknownField("accession-num"), "accession-num");
    }

    private static final EndNoteType DEFAULT_TYPE = new EndNoteType("Generic", 15);

    private final DocumentBuilderFactory documentBuilderFactory;

    private record EndNoteType(String name, int number) {
    }

    private final BibEntryPreferences bibEntryPreferences;

    public EndnoteXmlExporter(BibEntryPreferences bibEntryPreferences) {
        super("endnote", "EndNote XML", StandardFileType.XML);
        this.bibEntryPreferences = bibEntryPreferences;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public void export(@NonNull BibDatabaseContext databaseContext,
                       @NonNull Path file,
                       @NonNull List<BibEntry> entries) throws ParserConfigurationException, TransformerException {
        if (entries.isEmpty()) {
            return;
        }

        DocumentBuilder dBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = dBuilder.newDocument();

        Element rootElement = document.createElement("xml");
        document.appendChild(rootElement);

        Element recordsElement = document.createElement("records");
        rootElement.appendChild(recordsElement);

        for (BibEntry entry : entries) {
            Element recordElement = document.createElement("record");
            recordsElement.appendChild(recordElement);

            mapEntryType(entry, document, recordElement);
            createMetaInformationElements(databaseContext, document, recordElement);
            mapAuthorAndEditor(entry, document, recordElement);
            mapTitle(entry, document, recordElement);
            mapJournalTitle(entry, document, recordElement);
            mapKeywords(databaseContext.getDatabase(), entry, document, recordElement);
            mapDates(entry, document, recordElement);
            mapUrls(entry, document, recordElement);

            for (Map.Entry<Field, String> fieldMapping : STANDARD_FIELD_MAPPING.entrySet()) {
                Field field = fieldMapping.getKey();
                String xmlElement = fieldMapping.getValue();

                entry.getField(field).ifPresent(value -> {
                    Element fieldElement = document.createElement(xmlElement);
                    fieldElement.setTextContent(value);
                    recordElement.appendChild(fieldElement);
                });
            }
        }

        Transformer transformer = createTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file.toFile());
        transformer.transform(source, result);
    }

    private static void mapTitle(BibEntry entry, Document document, Element recordElement) {
        entry.getFieldOrAlias(StandardField.TITLE).ifPresent(title -> {
            Element titlesElement = document.createElement("titles");

            Element titleElement = document.createElement("title");
            titleElement.setTextContent(title);
            titlesElement.appendChild(titleElement);

            entry.getField(new UnknownField("alt-title")).ifPresent(altTitle -> {
                Element altTitleElement = document.createElement("alt-title");
                altTitleElement.setTextContent(altTitle);
                titlesElement.appendChild(altTitleElement);
            });

            entry.getField(StandardField.BOOKTITLE).ifPresent(secondaryTitle -> {
                Element secondaryTitleElement = document.createElement("secondary-title");
                secondaryTitleElement.setTextContent(secondaryTitle);
                titlesElement.appendChild(secondaryTitleElement);
            });

            recordElement.appendChild(titlesElement);
        });
    }

    private static void mapJournalTitle(BibEntry entry, Document document, Element recordElement) {
        entry.getFieldOrAlias(StandardField.JOURNAL).ifPresent(journalTitle -> {
            Element periodicalElement = document.createElement("periodical");
            Element fullTitleElement = document.createElement("full-title");
            fullTitleElement.setTextContent(journalTitle);
            periodicalElement.appendChild(fullTitleElement);
            recordElement.appendChild(periodicalElement);
        });
    }

    private void mapKeywords(BibDatabase bibDatabase, BibEntry entry, Document document, Element recordElement) {
        entry.getFieldOrAlias(StandardField.KEYWORDS).ifPresent(_ -> {
            Element keywordsElement = document.createElement("keywords");
            entry.getResolvedKeywords(bibEntryPreferences.getKeywordSeparator(), bibDatabase).forEach(keyword -> {
                Element keywordElement = document.createElement("keyword");
                // Hierarchical keywords are separated by the '>' character. See {@link } for details.
                keywordElement.setTextContent(keyword.get());
                keywordsElement.appendChild(keywordElement);
            });
            recordElement.appendChild(keywordsElement);
        });
    }

    private static void mapUrls(BibEntry entry, Document document, Element recordElement) {
        Element urlsElement = document.createElement("urls");

        entry.getFieldOrAlias(StandardField.FILE).ifPresent(fileField -> {
            Element pdfUrlsElement = document.createElement("pdf-urls");
            Element urlElement = document.createElement("url");
            urlElement.setTextContent(fileField);
            pdfUrlsElement.appendChild(urlElement);
            urlsElement.appendChild(pdfUrlsElement);
        });

        entry.getFieldOrAlias(StandardField.URL).ifPresent(url -> {
            Element webUrlsElement = document.createElement("web-urls");
            Element urlElement = document.createElement("url");
            urlElement.setTextContent(url);
            webUrlsElement.appendChild(urlElement);
            urlsElement.appendChild(webUrlsElement);
        });

        if (urlsElement.hasChildNodes()) {
            recordElement.appendChild(urlsElement);
        }
    }

    private static void mapDates(BibEntry entry, Document document, Element recordElement) {
        Element datesElement = document.createElement("dates");
        entry.getFieldOrAlias(StandardField.YEAR).ifPresent(year -> {
            Element yearElement = document.createElement("year");
            yearElement.setTextContent(year);
            datesElement.appendChild(yearElement);
        });
        entry.getFieldOrAlias(StandardField.MONTH).ifPresent(month -> {
            Element yearElement = document.createElement("month");
            yearElement.setTextContent(month);
            datesElement.appendChild(yearElement);
        });
        entry.getFieldOrAlias(StandardField.DAY).ifPresent(day -> {
            Element yearElement = document.createElement("day");
            yearElement.setTextContent(day);
            datesElement.appendChild(yearElement);
        });
        // We need to use getField here - getFieldOrAlias for Date tries to convert year, month, and day to a date, which we do not want
        entry.getField(StandardField.DATE).ifPresent(date -> {
            Element pubDatesElement = document.createElement("pub-dates");
            Element dateElement = document.createElement("date");
            dateElement.setTextContent(date);
            pubDatesElement.appendChild(dateElement);
            datesElement.appendChild(pubDatesElement);
        });
        if (datesElement.hasChildNodes()) {
            recordElement.appendChild(datesElement);
        }
    }

    private static void mapEntryType(BibEntry entry, Document document, Element recordElement) {
        EntryType entryType = entry.getType();
        EndNoteType endNoteType = ENTRY_TYPE_MAPPING.getOrDefault(entryType, DEFAULT_TYPE);
        Element refTypeElement = document.createElement("ref-type");
        refTypeElement.setAttribute("name", endNoteType.name());
        refTypeElement.setTextContent(String.valueOf(endNoteType.number()));
        recordElement.appendChild(refTypeElement);
    }

    private static void createMetaInformationElements(BibDatabaseContext databaseContext, Document document, Element recordElement) {
        Element databaseElement = document.createElement("database");
        databaseElement.setAttribute("name", "MyLibrary");
        String name = databaseContext.getDatabasePath().map(Path::getFileName).map(Path::toString).orElse("MyLibrary");
        databaseElement.setTextContent(name);
        recordElement.appendChild(databaseElement);

        Element sourceAppElement = document.createElement("source-app");
        sourceAppElement.setAttribute("name", "JabRef");
        sourceAppElement.setTextContent("JabRef");
        recordElement.appendChild(sourceAppElement);
    }

    private static void mapAuthorAndEditor(BibEntry entry, Document document, Element recordElement) {
        Element contributorsElement = document.createElement("contributors");
        entry.getField(StandardField.AUTHOR).ifPresent(authors -> addPersons(authors, document, contributorsElement, "authors"));
        entry.getField(StandardField.EDITOR).ifPresent(editors -> addPersons(editors, document, contributorsElement, "secondary-authors"));
        if (contributorsElement.hasChildNodes()) {
            recordElement.appendChild(contributorsElement);
        }
    }

    private static void addPersons(String authors, Document document, Element contributorsElement, String wrapTagName) {
        Element container = document.createElement(wrapTagName);
        AuthorList parsedPersons = AuthorList.parse(authors).latexFree();
        for (Author person : parsedPersons) {
            Element authorElement = document.createElement("author");
            authorElement.setTextContent(person.getFamilyGiven(false));
            container.appendChild(authorElement);
        }
        contributorsElement.appendChild(container);
    }

    private static Transformer createTransformer() throws TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return transformer;
    }
}

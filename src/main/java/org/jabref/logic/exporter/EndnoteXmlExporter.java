package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndnoteXmlExporter extends Exporter {

    private record EndNoteType(String name, Integer number) {
    }

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

    private static final Map<Field, String> FIELD_MAPPING = new HashMap<>();

    static {
        FIELD_MAPPING.put(StandardField.TITLE, "title");
        FIELD_MAPPING.put(StandardField.AUTHOR, "authors");
        FIELD_MAPPING.put(StandardField.EDITOR, "secondary-authors");
        FIELD_MAPPING.put(StandardField.PAGES, "pages");
        FIELD_MAPPING.put(StandardField.VOLUME, "volume");
        FIELD_MAPPING.put(StandardField.KEYWORDS, "keywords");
        FIELD_MAPPING.put(StandardField.PUBLISHER, "publisher");
        FIELD_MAPPING.put(StandardField.ISBN, "isbn");
        FIELD_MAPPING.put(StandardField.DOI, "electronic-resource-num");
        FIELD_MAPPING.put(StandardField.ABSTRACT, "abstract");
        FIELD_MAPPING.put(StandardField.URL, "web-urls");
        FIELD_MAPPING.put(StandardField.FILE, "pdf-urls");
        FIELD_MAPPING.put(StandardField.JOURNALTITLE, "full-title");
        FIELD_MAPPING.put(StandardField.BOOKTITLE, "secondary-title");
        FIELD_MAPPING.put(StandardField.EDITION, "edition");
        FIELD_MAPPING.put(StandardField.SERIES, "tertiary-title");
        FIELD_MAPPING.put(StandardField.NUMBER, "number");
        FIELD_MAPPING.put(StandardField.ISSUE, "issue");
        FIELD_MAPPING.put(StandardField.LOCATION, "pub-location");
        FIELD_MAPPING.put(StandardField.CHAPTER, "section");
        FIELD_MAPPING.put(StandardField.HOWPUBLISHED, "work-type");
        FIELD_MAPPING.put(StandardField.ISSN, "issn");
        FIELD_MAPPING.put(StandardField.ADDRESS, "auth-address");
        FIELD_MAPPING.put(StandardField.PAGETOTAL, "page-total");
        FIELD_MAPPING.put(StandardField.NOTE, "notes");
        FIELD_MAPPING.put(StandardField.LABEL, "label");
        FIELD_MAPPING.put(StandardField.LANGUAGE, "language");
        FIELD_MAPPING.put(StandardField.KEY, "foreign-keys");
        FIELD_MAPPING.put(new UnknownField("accession-num"), "accession-num");
    }

    private static final EndNoteType DEFAULT_TYPE = new EndNoteType("Generic", 15);

    public EndnoteXmlExporter() {
        super("endnote", "EndNote XML", StandardFileType.XML);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.newDocument();

        Element rootElement = document.createElement("xml");
        document.appendChild(rootElement);

        Element recordsElement = document.createElement("records");
        rootElement.appendChild(recordsElement);

        for (BibEntry entry : entries) {
            Element recordElement = document.createElement("record");
            recordsElement.appendChild(recordElement);

            // Map entry type
            EntryType entryType = entry.getType();
            EndNoteType endNoteType = ENTRY_TYPE_MAPPING.getOrDefault(entryType, DEFAULT_TYPE);
            Element refTypeElement = document.createElement("ref-type");
            refTypeElement.setAttribute("name", endNoteType.name());
            refTypeElement.setTextContent(endNoteType.number().toString());
            recordElement.appendChild(refTypeElement);

            // Map database and source-app
            Element databaseElement = document.createElement("database");
            databaseElement.setAttribute("name", "MyLibrary");
            databaseElement.setTextContent("MyLibrary");
            recordElement.appendChild(databaseElement);

            Element sourceAppElement = document.createElement("source-app");
            sourceAppElement.setAttribute("name", "JabRef");
            sourceAppElement.setTextContent("JabRef");
            recordElement.appendChild(sourceAppElement);

            // Map contributors (authors and editors)
            Element contributorsElement = document.createElement("contributors");

            entry.getField(StandardField.AUTHOR).ifPresent(authors -> {
                Element authorsElement = document.createElement("authors");
                String[] authorArray = authors.split("\\s+and\\s+");
                for (String author : authorArray) {
                    Element authorElement = document.createElement("author");
                    authorElement.setTextContent(author);
                    authorsElement.appendChild(authorElement);
                }
                contributorsElement.appendChild(authorsElement);
            });

            entry.getField(StandardField.EDITOR).ifPresent(editors -> {
                Element secondaryAuthorsElement = document.createElement("secondary-authors");
                String[] editorArray = editors.split("\\s+and\\s+");
                for (String editor : editorArray) {
                    Element editorElement = document.createElement("author");
                    editorElement.setTextContent(editor);
                    secondaryAuthorsElement.appendChild(editorElement);
                }
                contributorsElement.appendChild(secondaryAuthorsElement);
            });

            if (contributorsElement.hasChildNodes()) {
                recordElement.appendChild(contributorsElement);
            }

            // Map titles
            Element titlesElement = document.createElement("titles");
            entry.getField(StandardField.TITLE).ifPresent(title -> {
                Element titleElement = document.createElement("title");
                titleElement.setTextContent(title);
                titlesElement.appendChild(titleElement);
            });

            entry.getField(StandardField.JOURNAL).ifPresent(journal -> {
                Element secondaryTitleElement = document.createElement("secondary-title");
                secondaryTitleElement.setTextContent(journal);
                titlesElement.appendChild(secondaryTitleElement);
            });

            if (titlesElement.hasChildNodes()) {
                recordElement.appendChild(titlesElement);
            }

            // Map periodical and full-title
            entry.getField(StandardField.JOURNALTITLE).ifPresent(journalTitle -> {
                Element periodicalElement = document.createElement("periodical");
                Element fullTitleElement = document.createElement("full-title");
                fullTitleElement.setTextContent(journalTitle);
                periodicalElement.appendChild(fullTitleElement);
                recordElement.appendChild(periodicalElement);
            });

            // Map keywords
            entry.getField(StandardField.KEYWORDS).ifPresent(keywords -> {
                Element keywordsElement = document.createElement("keywords");
                String[] keywordArray = keywords.split(",\\s*");
                for (String keyword : keywordArray) {
                    Element keywordElement = document.createElement("keyword");
                    keywordElement.setTextContent(keyword);
                    keywordsElement.appendChild(keywordElement);
                }
                recordElement.appendChild(keywordsElement);
            });

            // Map dates
            Element datesElement = document.createElement("dates");
            entry.getField(StandardField.YEAR).ifPresent(year -> {
                Element yearElement = document.createElement("year");
                yearElement.setTextContent(year);
                datesElement.appendChild(yearElement);
            });
            entry.getField(StandardField.MONTH).ifPresent(month -> {
                Element yearElement = document.createElement("month");
                yearElement.setTextContent(month);
                datesElement.appendChild(yearElement);
            });
            entry.getField(StandardField.DAY).ifPresent(day -> {
                Element yearElement = document.createElement("day");
                yearElement.setTextContent(day);
                datesElement.appendChild(yearElement);
            });
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

            // Map URLs
            Element urlsElement = document.createElement("urls");
            entry.getField(StandardField.FILE).ifPresent(fileField -> {
                Element pdfUrlsElement = document.createElement("pdf-urls");
                Element urlElement = document.createElement("url");
                urlElement.setTextContent(fileField);
                pdfUrlsElement.appendChild(urlElement);
                urlsElement.appendChild(pdfUrlsElement);
            });

            entry.getField(StandardField.URL).ifPresent(url -> {
                Element webUrlsElement = document.createElement("web-urls");
                Element urlElement = document.createElement("url");
                urlElement.setTextContent(url);
                webUrlsElement.appendChild(urlElement);
                urlsElement.appendChild(webUrlsElement);
            });

            if (urlsElement.hasChildNodes()) {
                recordElement.appendChild(urlsElement);
            }

            // Map other fields
            for (Map.Entry<Field, String> fieldMapping : FIELD_MAPPING.entrySet()) {
                Field field = fieldMapping.getKey();
                String xmlElement = fieldMapping.getValue();

                if (field != StandardField.AUTHOR && field != StandardField.EDITOR &&
                        field != StandardField.TITLE && field != StandardField.JOURNAL &&
                        field != StandardField.JOURNALTITLE && field != StandardField.KEYWORDS && field != StandardField.YEAR && field != StandardField.MONTH && field != StandardField.DAY && field != StandardField.DATE && field != StandardField.FILE && field != StandardField.URL) {
                    entry.getField(field).ifPresent(value -> {
                        Element fieldElement = document.createElement(xmlElement);
                        fieldElement.setTextContent(value);
                        recordElement.appendChild(fieldElement);
                    });
                }
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file.toFile());
        transformer.transform(source, result);
    }
}

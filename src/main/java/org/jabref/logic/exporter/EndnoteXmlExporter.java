package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndnoteXmlExporter extends Exporter {
    private static final List<Map.Entry<EntryType, String>> ENTRY_TYPE_MAPPING_LIST = List.of(
            Map.entry(StandardEntryType.Article, "Journal Article"),
            Map.entry(StandardEntryType.Book, "Book"),
            Map.entry(StandardEntryType.InBook, "Book Section"),
            Map.entry(StandardEntryType.InCollection, "Book Section"),
            Map.entry(StandardEntryType.Proceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.MastersThesis, "Thesis"),
            Map.entry(StandardEntryType.PhdThesis, "Thesis"),
            Map.entry(StandardEntryType.TechReport, "Report"),
            Map.entry(StandardEntryType.Unpublished, "Manuscript"),
            Map.entry(StandardEntryType.InProceedings, "Conference Paper"),
            Map.entry(StandardEntryType.Conference, "Conference"),
            Map.entry(IEEETranEntryType.Patent, "Patent"),
            Map.entry(StandardEntryType.Online, "Web Page"),
            Map.entry(IEEETranEntryType.Electronic, "Electronic Article"),
            Map.entry(StandardEntryType.Misc, "Generic")
    );

    private static final List<Map.Entry<EntryType, String>> EXPORT_REF_NUMBER_LIST = ENTRY_TYPE_MAPPING_LIST.stream()
                                                                                                            .map(entry -> Map.entry(entry.getKey(), Integer.toString(ENTRY_TYPE_MAPPING_LIST.indexOf(entry) + 1)))
                                                                                                            .toList();

    private static final List<Map.Entry<Field, String>> FIELD_MAPPING_LIST = List.of(
            Map.entry(StandardField.TITLE, "title"),
            Map.entry(StandardField.AUTHOR, "authors"),
            Map.entry(StandardField.EDITOR, "secondary-authors"),
            Map.entry(StandardField.PAGES, "pages"),
            Map.entry(StandardField.VOLUME, "volume"),
            Map.entry(StandardField.KEYWORDS, "keywords"),
            Map.entry(StandardField.PUBLISHER, "publisher"),
            Map.entry(StandardField.ISBN, "isbn"),
            Map.entry(StandardField.DOI, "electronic-resource-num"),
            Map.entry(StandardField.ABSTRACT, "abstract"),
            Map.entry(StandardField.URL, "web-urls"),
            Map.entry(StandardField.FILE, "pdf-urls"),
            Map.entry(StandardField.JOURNALTITLE, "full-title"),
            Map.entry(StandardField.BOOKTITLE, "secondary-title"),
            Map.entry(StandardField.EDITION, "edition"),
            Map.entry(StandardField.SERIES, "tertiary-title"),
            Map.entry(StandardField.NUMBER, "number"),
            Map.entry(StandardField.ISSUE, "issue"),
            Map.entry(StandardField.LOCATION, "pub-location"),
            Map.entry(StandardField.CHAPTER, "section"),
            Map.entry(StandardField.HOWPUBLISHED, "work-type"),
            Map.entry(StandardField.PUBLISHER, "publisher"),
            Map.entry(StandardField.ISSN, "issn"),
            Map.entry(StandardField.ADDRESS, "auth-address"),
            Map.entry(StandardField.PAGETOTAL, "page-total")
    );

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
            String refType = ENTRY_TYPE_MAPPING_LIST.stream()
                                                    .filter(mapping -> mapping.getKey().equals(entryType))
                                                    .map(Map.Entry::getValue)
                                                    .findFirst()
                                                    .orElse("Generic");

            String refNumber = EXPORT_REF_NUMBER_LIST.stream()
                                                     .filter(mapping -> mapping.getKey().equals(entryType))
                                                     .map(Map.Entry::getValue)
                                                     .findFirst()
                                                     .orElse("15");

            Element refTypeElement = document.createElement("ref-type");
            refTypeElement.setAttribute("name", refType);
            refTypeElement.setTextContent(refNumber);
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
            for (Map.Entry<Field, String> fieldMapping : FIELD_MAPPING_LIST) {
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

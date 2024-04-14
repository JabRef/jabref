package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private static final List<EntryType> ORDERED_ENTRY_TYPES = List.of(
            StandardEntryType.Article, // Journal Article -1
            StandardEntryType.Book, // Book -2
            StandardEntryType.InBook, // Book Section -3
            StandardEntryType.InCollection, // Book Section -4
            StandardEntryType.Proceedings, // Conference Proceedings -5
            StandardEntryType.MastersThesis, // Thesis -6
            StandardEntryType.PhdThesis, // Thesis -7
            StandardEntryType.TechReport, // Report -8
            StandardEntryType.Unpublished, // Manuscript -9
            StandardEntryType.InProceedings, // Conference Paper -10
            StandardEntryType.Conference, // Conference -11
            IEEETranEntryType.Patent, // Patent -12
            StandardEntryType.Online, // Web Page -13
            IEEETranEntryType.Electronic, // Electronic Article -14
            StandardEntryType.Misc // Generic -15
    );

    private static final Map<EntryType, String> ENTRY_TYPE_MAPPING = ORDERED_ENTRY_TYPES.stream()
                                                                                        .collect(Collectors.toMap(
                                                                                                entryType -> entryType,
                                                                                                EndnoteXmlExporter::getEndnoteEntryType
                                                                                        ));

    private static String getEndnoteEntryType(EntryType entryType) {
        return switch (entryType) {
            case StandardEntryType.Article ->
                    "Journal Article";
            case StandardEntryType.Book ->
                    "Book";
            case StandardEntryType.InBook,
                 StandardEntryType.InCollection ->
                    "Book Section";
            case StandardEntryType.Proceedings ->
                    "Conference Proceedings";
            case StandardEntryType.MastersThesis,
                 StandardEntryType.PhdThesis ->
                    "Thesis";
            case StandardEntryType.TechReport ->
                    "Report";
            case StandardEntryType.Unpublished ->
                    "Manuscript";
            case StandardEntryType.InProceedings ->
                    "Conference Paper";
            case StandardEntryType.Conference ->
                    "Conference";
            case IEEETranEntryType.Patent ->
                    "Patent";
            case StandardEntryType.Online ->
                    "Web Page";
            case IEEETranEntryType.Electronic ->
                    "Electronic Article";
            case StandardEntryType.Misc ->
                    "Generic";
            default ->
                    throw new IllegalArgumentException("Unsupported entry type: " + entryType);
        };
    }

    private static final Map<EntryType, String> EXPORT_REF_NUMBER = ORDERED_ENTRY_TYPES.stream()
                                                                                       .collect(Collectors.toMap(
                                                                                               entryType -> entryType,
                                                                                               entryType -> Integer.toString(ORDERED_ENTRY_TYPES.indexOf(entryType) + 1)
                                                                                       ));

    private static final Map<Field, String> FIELD_MAPPING = Map.ofEntries(
            Map.entry(StandardField.TITLE, "title"),
            Map.entry(StandardField.AUTHOR, "authors"),
            Map.entry(StandardField.EDITOR, "secondary-authors"),
            Map.entry(StandardField.YEAR, "year"),
            Map.entry(StandardField.MONTH, "pub-dates"),
            Map.entry(StandardField.JOURNAL, "full-title"),
            Map.entry(StandardField.JOURNALTITLE, "full-title"),
            Map.entry(StandardField.BOOKTITLE, "secondary-title"),
            Map.entry(StandardField.EDITION, "edition"),
            Map.entry(StandardField.SERIES, "tertiary-title"),
            Map.entry(StandardField.VOLUME, "volume"),
            Map.entry(StandardField.NUMBER, "number"),
            Map.entry(StandardField.ISSUE, "issue"),
            Map.entry(StandardField.PAGES, "pages"),
            Map.entry(StandardField.PUBLISHER, "publisher"),
            Map.entry(StandardField.ADDRESS, "pub-location"),
            Map.entry(StandardField.CHAPTER, "section"),
            Map.entry(StandardField.HOWPUBLISHED, "work-type"),
            Map.entry(StandardField.INSTITUTION, "publisher"),
            Map.entry(StandardField.ORGANIZATION, "publisher"),
            Map.entry(StandardField.SCHOOL, "publisher"),
            Map.entry(StandardField.ISBN, "isbn"),
            Map.entry(StandardField.ISSN, "isbn"),
            Map.entry(StandardField.DOI, "electronic-resource-num"),
            Map.entry(StandardField.URL, "web-urls"),
            Map.entry(StandardField.FILE, "pdf-urls"),
            Map.entry(StandardField.ABSTRACT, "abstract"),
            Map.entry(StandardField.KEYWORDS, "keywords")
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
            String refType = ENTRY_TYPE_MAPPING.getOrDefault(entryType, "Generic");
            Element refTypeElement = document.createElement("ref-type");
            refTypeElement.setAttribute("name", refType);
            refTypeElement.setTextContent(EXPORT_REF_NUMBER.getOrDefault(entryType, "Generic"));
            recordElement.appendChild(refTypeElement);

            // Map authors and editors
            entry.getField(StandardField.AUTHOR).ifPresent(authors -> {
                Element authorsElement = document.createElement("contributors");
                Element authorsNameElement = document.createElement("authors");
                authorsNameElement.setTextContent(authors);
                authorsElement.appendChild(authorsNameElement);
                recordElement.appendChild(authorsElement);
            });

            entry.getField(StandardField.EDITOR).ifPresent(editors -> {
                Element contributorsElement = document.createElement("contributors");
                Element editorsElement = document.createElement("secondary-authors");
                editorsElement.setTextContent(editors);
                contributorsElement.appendChild(editorsElement);
                recordElement.appendChild(contributorsElement);
            });

            // Map fields
            for (Map.Entry<Field, String> fieldMapping : FIELD_MAPPING.entrySet()) {
                Field field = fieldMapping.getKey();
                String xmlElement = fieldMapping.getValue();

                if (field != StandardField.AUTHOR && field != StandardField.EDITOR) {
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

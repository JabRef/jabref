package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndnoteXmlExporter extends Exporter {

    private static final String ENDNOTE_XML_VERSION = "20.1";
    private static final Map<EntryType, String> EXPORT_ITEM_TYPE = Map.ofEntries(
            Map.entry(StandardEntryType.Article, "Journal Article"),
            Map.entry(StandardEntryType.Book, "Book"),
            Map.entry(StandardEntryType.InBook, "Book Section"),
            Map.entry(StandardEntryType.InCollection, "Book Section"),
            Map.entry(StandardEntryType.InProceedings, "Conference Paper"),
            Map.entry(StandardEntryType.Conference, "Conference"),
            Map.entry(StandardEntryType.MastersThesis, "Thesis"),
            Map.entry(StandardEntryType.PhdThesis, "Thesis"),
            Map.entry(StandardEntryType.Proceedings, "Conference Proceedings"),
            Map.entry(StandardEntryType.TechReport, "Report"),
            Map.entry(StandardEntryType.Unpublished, "Manuscript"),
            Map.entry(IEEETranEntryType.Patent, "Patent"),
            Map.entry(StandardEntryType.Online, "Web Page"),
            Map.entry(IEEETranEntryType.Electronic, "Electronic Article"),
            Map.entry(StandardEntryType.Misc, "Generic")
    );

    private static final List<String> EXPORT_ITEM_TYPE_ORDER = List.of(
            "Journal Article",
            "Book",
            "Book Section",
            "Conference Paper",
            "Conference",
            "Thesis",
            "Conference Proceedings",
            "Report",
            "Manuscript",
            "Patent",
            "Web Page",
            "Electronic Article",
            "Generic"
    );

    private static final Map<EntryType, String> EXPORT_REF_NUMBER = EXPORT_ITEM_TYPE.entrySet().stream()
                                                                                    .collect(Collectors.toMap(
                                                                                            Map.Entry::getKey,
                                                                                            entry -> Integer.toString(EXPORT_ITEM_TYPE_ORDER.indexOf(entry.getValue()) + 1)));

    private final AuthorListParser authorListParser;
    private final DocumentBuilderFactory dbFactory;

    public EndnoteXmlExporter() {
        super("endnote", "EndNote XML", StandardFileType.XML);
        this.authorListParser = new AuthorListParser();
        this.dbFactory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.newDocument();

            Element rootElement = document.createElement("xml");
            document.appendChild(rootElement);

            Element recordsElement = document.createElement("records");
            rootElement.appendChild(recordsElement);

            for (BibEntry entry : entries) {
                Element recordElement = writeEntry(entry, document);
                recordsElement.appendChild(recordElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(file.toFile());
            transformer.transform(source, result);
        } catch (
                ParserConfigurationException |
                TransformerException e) {
            throw new SaveException(e);
        }
    }

    private Element writeEntry(BibEntry entry, Document document) {
        Element recordElement = document.createElement("record");

        // Write the necessary fields and elements
        writeField(document, recordElement, "database", "endnote.enl", Map.of("name", "My EndNote Library.enl", "path", "/path/to/My EndNote Library.enl"));
        writeField(document, recordElement, "source-app", "JabRef", Map.of("name", "JabRef", "version", ENDNOTE_XML_VERSION));
        writeField(document, recordElement, "rec-number", entry.getId(), null);

        Element foreignKeysElement = document.createElement("foreign-keys");
        Element keyElement = document.createElement("key");
        keyElement.setAttribute("app", "EN");
        keyElement.appendChild(document.createTextNode(entry.getId()));
        foreignKeysElement.appendChild(keyElement);
        recordElement.appendChild(foreignKeysElement);

        writeField(document, recordElement, "ref-type", EXPORT_REF_NUMBER.getOrDefault(entry.getType(), "Generic"), Map.of("name", EXPORT_ITEM_TYPE.getOrDefault(entry.getType(), "Generic")));

        writeContributors(entry, document, recordElement);
        writeField(document, recordElement, "titles", Map.of(), entry.getField(StandardField.TITLE).orElse(""), "title");
        writeField(document, recordElement, "periodical", Map.of(), entry.getField(StandardField.JOURNAL).orElse(""), "full-title");
        writeField(document, recordElement, "tertiary-title", entry.getField(StandardField.BOOKTITLE).orElse(""), null);
        writeField(document, recordElement, "pages", entry.getField(StandardField.PAGES).orElse(""), null);
        writeField(document, recordElement, "volume", entry.getField(StandardField.VOLUME).orElse(""), null);
        writeField(document, recordElement, "number", entry.getField(StandardField.NUMBER).orElse(""), null);
        writeField(document, recordElement, "dates", Map.of(), entry.getField(StandardField.YEAR).orElse(""), "year");
        writePublisher(entry, document, recordElement);
        writeField(document, recordElement, "isbn", entry.getField(StandardField.ISBN).orElse(""), null);
        writeField(document, recordElement, "abstract", entry.getField(StandardField.ABSTRACT).orElse(""), null);
        writeField(document, recordElement, "notes", entry.getField(StandardField.NOTE).orElse(""), null);
        writeField(document, recordElement, "urls", Map.of(), entry.getField(StandardField.URL).orElse(""), "web-urls");
        writeField(document, recordElement, "electronic-resource-num", entry.getField(StandardField.DOI).orElse(""), null);

        return recordElement;
    }

    private void writeField(Document document, Element parentElement, String name, String value, Map<String, String> attributes) {
        if (value != null && !value.isEmpty()) {
            Element fieldElement = document.createElement(name);
            if (attributes != null) {
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    fieldElement.setAttribute(attribute.getKey(), attribute.getValue());
                }
            }
            createStyleElement(document, fieldElement, value);
            parentElement.appendChild(fieldElement);
        }
    }

    private void writeField(Document document, Element parentElement, String name, Map<String, String> attributes, String childValue, String childElementName) {
        Element fieldElement = document.createElement(name);
        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                fieldElement.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }
        if (childValue != null && !childValue.isEmpty()) {
            Element childElement = document.createElement(childElementName);
            createStyleElement(document, childElement, childValue);
            fieldElement.appendChild(childElement);
        }
        parentElement.appendChild(fieldElement);
    }

    private void createStyleElement(Document document, Element parentElement, String value) {
        Element styleElement = document.createElement("style");
        styleElement.setAttribute("face", "normal");
        styleElement.setAttribute("font", "default");
        styleElement.setAttribute("size", "100%");
        styleElement.appendChild(document.createTextNode(value));
        parentElement.appendChild(styleElement);
    }

    private void writeContributors(BibEntry entry, Document document, Element parentElement) {
        entry.getField(StandardField.AUTHOR).ifPresent(authors -> {
            Element contributorsElement = document.createElement("contributors");
            Element authorsElement = document.createElement("authors");

            // Parse the authors string and get the list of Author objects
            List<Author> authorList = authorListParser.parse(authors).getAuthors();

            // Iterate over each Author object and create the corresponding XML elements
            for (Author author : authorList) {
                Element authorElement = document.createElement("author");
                Element styleElement = document.createElement("style");
                styleElement.setAttribute("face", "normal");
                styleElement.setAttribute("font", "default");
                styleElement.setAttribute("size", "100%");
                styleElement.appendChild(document.createTextNode(author.getFamilyGiven(false)));
                authorElement.appendChild(styleElement);
                authorsElement.appendChild(authorElement);
            }

            contributorsElement.appendChild(authorsElement);
            parentElement.appendChild(contributorsElement);
        });
    }

    private void writePublisher(BibEntry entry, Document document, Element parentElement) {
        String publisher = entry.getField(StandardField.PUBLISHER).orElse("");
        String address = entry.getField(StandardField.ADDRESS).orElse("");
        if (!publisher.isEmpty() || !address.isEmpty()) {
            Element publisherElement = document.createElement("publisher");
            if (!publisher.isEmpty()) {
                writeField(document, publisherElement, "publisher", publisher, null);
            }
            if (!address.isEmpty()) {
                Element placeElement = document.createElement("place");
                Element styleElement = document.createElement("style");
                styleElement.setAttribute("face", "normal");
                styleElement.setAttribute("font", "default");
                styleElement.setAttribute("size", "100%");
                styleElement.appendChild(document.createTextNode(address));
                placeElement.appendChild(styleElement);
                publisherElement.appendChild(placeElement);
            }
            parentElement.appendChild(publisherElement);
        }
    }
}

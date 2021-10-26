package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.layout.format.GetOpenOfficeType;
import org.jabref.logic.layout.format.RemoveBrackets;
import org.jabref.logic.layout.format.RemoveWhitespace;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

class OOCalcDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OOCalcDatabase.class);
    private static final Field REPORT_TYPE_FIELD = new UnknownField("reporttype");

    private final List<BibEntry> entries = new ArrayList<>();
    private final List<Field> toExportFields = Stream.concat(FieldFactory.getStandardFieldsWithCitationKey().stream(), Stream.of(REPORT_TYPE_FIELD))
                                                     .collect(Collectors.toList());

    public OOCalcDatabase(BibDatabase bibtex, List<BibEntry> entries) {
        this.entries.addAll(entries != null ? entries : bibtex.getEntries());

        List<FieldComparator> comparators = new ArrayList<>();
        comparators.add(new FieldComparator(StandardField.AUTHOR));
        comparators.add(new FieldComparator(StandardField.YEAR));
        comparators.add(new FieldComparator(InternalField.KEY_FIELD));

        this.entries.sort(new FieldComparatorStack<>(comparators));
    }

    private static String getField(BibEntry e, Field field) {
        return e.getField(field).orElse("");
    }

    public Document getDOMrepresentation() {
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = createRootElement(document);
            Element body = document.createElement("office:body");
            Element table = createTableElement(document);

            body.appendChild(table);
            root.appendChild(body);
            document.appendChild(root);

            addTableHeader(table, document);

            for (BibEntry entry : entries) {
                addEntryRow(entry, table, document);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception caught...", e);
        }
        return document;
    }

    private void addEntryRow(BibEntry entry, Element table, Document document) {
        final Element row = document.createElement("table:table-row");

        addTableCell(document, row, new GetOpenOfficeType().format(entry.getType().getName()));
        toExportFields.forEach(field -> {
            if (field.equals(StandardField.TITLE)) {
                addTableCell(document, row, new RemoveWhitespace().format(new RemoveBrackets().format(getField(entry, StandardField.TITLE))));
            } else {
                addTableCell(document, row, getField(entry, field));
            }
        });

        table.appendChild(row);
    }

    private Element createTableElement(Document document) {
        Element table = document.createElement("table:table");
        table.setAttribute("table:name", "biblio");
        table.setAttribute("table.style-name", "ta1");
        return table;
    }

    private Element createRootElement(Document document) {
        Element root = document.createElement("office:document-content");
        root.setAttribute("xmlns:office", "http://openoffice.org/2000/office");
        root.setAttribute("xmlns:style", "http://openoffice.org/2000/style");
        root.setAttribute("xmlns:text", "http://openoffice.org/2000/text");
        root.setAttribute("xmlns:table", "http://openoffice.org/2000/table");
        root.setAttribute("xmlns:office:class", "spreadsheet");
        root.setAttribute("xmlns:office:version", "1.0");
        root.setAttribute("xmlns:fo", "http://www.w3.org/1999/XSL/Format");
        Element el = document.createElement("office:script");
        root.appendChild(el);

        el = document.createElement("office:automatic-styles");
        Element el2 = document.createElement("style:style");
        el2.setAttribute("style:name", "ro1");
        el2.setAttribute("style:family", "table-row");
        Element el3 = document.createElement("style.properties");
        el3.setAttribute("style:row-height", "0.1681inch");
        el3.setAttribute("fo:break-before", "auto");
        el3.setAttribute("style:use-optimal-row-height", "true");
        el2.appendChild(el3);
        el.appendChild(el2);
        el2 = document.createElement("style:style");
        el2.setAttribute("style:name", "ta1");
        el2.setAttribute("style:family", "table");
        el2.setAttribute("style:master-page-name", "Default");
        el3 = document.createElement("style:properties");
        el3.setAttribute("table:display", "true");
        el2.appendChild(el3);
        el.appendChild(el2);
        root.appendChild(el);

        return root;
    }

    private static void addTableCell(Document doc, Element parent, String content) {
        Element cell = doc.createElement("table:table-cell");
        Element text = doc.createElement("text:p");
        Text textNode = doc.createTextNode(content);
        text.appendChild(textNode);
        cell.appendChild(text);
        parent.appendChild(cell);
    }

    private void addTableHeader(Element table, Document document) {
        Element firstRow = document.createElement("table:table-row");
        firstRow.setAttribute("table.style-name", "ro1");
        addTableCell(document, firstRow, "Type");
        for (Field field : toExportFields) {
            addTableCell(document, firstRow, field.getDisplayName());
        }

        table.appendChild(firstRow);
    }
}

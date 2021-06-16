package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.layout.format.GetOpenOfficeType;
import org.jabref.logic.layout.format.RemoveBrackets;
import org.jabref.logic.layout.format.RemoveWhitespace;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

class OpenDocumentRepresentation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDocumentRepresentation.class);
    private final List<BibEntry> entries;

    private final BibDatabase database;

    public OpenDocumentRepresentation(BibDatabase database, List<BibEntry> entries) {
        this.database = database;
        // Make a list of comparators for sorting the entries:
        List<FieldComparator> comparators = new ArrayList<>();
        comparators.add(new FieldComparator(StandardField.AUTHOR));
        comparators.add(new FieldComparator(StandardField.YEAR));
        comparators.add(new FieldComparator(InternalField.KEY_FIELD));
        // Use glazed lists to get a sorted view of the entries:
        List<BibEntry> entryList = new ArrayList<>();

        // Set up a list of all entries, if entries==null, or the entries in the given list
        if (entries == null) {
            entryList.addAll(database.getEntries());
        } else {
            entryList.addAll(entries);
        }

        Collections.sort(entryList, new FieldComparatorStack<>(comparators));
        this.entries = entryList;
    }

    public Document getDOMrepresentation() {
        Document result = null;
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            result = dbuild.newDocument();
            Element collection = result.createElement("office:document-content");
            // collection.setAttribute("xmlns", "http://openoffice.org/2000/office");
            collection.setAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0");
            collection.setAttribute("xmlns:style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0");
            collection.setAttribute("xmlns:text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0");
            collection.setAttribute("xmlns:table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0");
            collection.setAttribute("xmlns:meta", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0");
            collection.setAttribute("office:version", "1.0");
            collection.setAttribute("xmlns:fo", "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0");
            collection.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
            Element el = result.createElement("office:scripts");
            collection.appendChild(el);

            el = result.createElement("office:automatic-styles");
            Element el2 = result.createElement("style:style");
            el2.setAttribute("style:name", "ro1");
            el2.setAttribute("style:family", "table-row");
            Element el3 = result.createElement("style.table-row-properties");
            el3.setAttribute("style:row-height", "0.1681inch");
            el3.setAttribute("fo:break-before", "auto");
            el3.setAttribute("style:use-optimal-row-height", "true");
            el2.appendChild(el3);
            el.appendChild(el2);
            el2 = result.createElement("style:style");
            el2.setAttribute("style:name", "ta1");
            el2.setAttribute("style:family", "table");
            el2.setAttribute("style:master-page-name", "Default");
            el3 = result.createElement("style:properties");
            el3.setAttribute("table:display", "true");
            el2.appendChild(el3);
            el.appendChild(el2);
            collection.appendChild(el);

            Element body = result.createElement("office:body");
            Element spreadsheet = result.createElement("office:spreadsheet");
            Element table = result.createElement("table:table");
            table.setAttribute("table:name", "biblio");
            table.setAttribute("table.style-name", "ta1");

            Element row = result.createElement("table:table-row");
            row.setAttribute("table.style-name", "ro1");
            addTableCell(result, row, "Identifier");
            addTableCell(result, row, "Type");
            addTableCell(result, row, "Address");
            addTableCell(result, row, "Assignee");
            addTableCell(result, row, "Annote");
            addTableCell(result, row, "Author");
            addTableCell(result, row, "Booktitle");
            addTableCell(result, row, "Chapter");
            addTableCell(result, row, "Day");
            addTableCell(result, row, "Dayfiled");
            addTableCell(result, row, "Edition");
            addTableCell(result, row, "Editor");
            addTableCell(result, row, "Howpublish");
            addTableCell(result, row, "Institution");
            addTableCell(result, row, "Journal");
            addTableCell(result, row, "Language");
            addTableCell(result, row, "Month");
            addTableCell(result, row, "Monthfiled");
            addTableCell(result, row, "Nationality");
            addTableCell(result, row, "Note");
            addTableCell(result, row, "Number");
            addTableCell(result, row, "Organization");
            addTableCell(result, row, "Pages");
            addTableCell(result, row, "Publisher");
            addTableCell(result, row, "Revision");
            addTableCell(result, row, "School");
            addTableCell(result, row, "Series");
            addTableCell(result, row, "Title");
            addTableCell(result, row, "RepType");
            addTableCell(result, row, "Volume");
            addTableCell(result, row, "Year");
            addTableCell(result, row, "Yearfiled");
            addTableCell(result, row, "URL");
            addTableCell(result, row, "Custom1");
            addTableCell(result, row, "Custom2");
            addTableCell(result, row, "Custom3");
            addTableCell(result, row, "Custom4");
            addTableCell(result, row, "Custom5");
            addTableCell(result, row, "ISBN");
            table.appendChild(row);

            for (BibEntry e : entries) {
                row = result.createElement("table:table-row");
                addTableCell(result, row, getField(e, InternalField.KEY_FIELD));
                addTableCell(result, row, new GetOpenOfficeType().format(e.getType().getName()));
                addTableCell(result, row, getField(e, StandardField.ADDRESS));
                addTableCell(result, row, getField(e, StandardField.ASSIGNEE));
                addTableCell(result, row, getField(e, StandardField.ANNOTE));
                addTableCell(result, row, getField(e, StandardField.AUTHOR)); // new AuthorLastFirst().format(getField(e, StandardField.AUTHOR_FIELD)));
                addTableCell(result, row, getField(e, StandardField.BOOKTITLE));
                addTableCell(result, row, getField(e, StandardField.CHAPTER));
                addTableCell(result, row, getField(e, StandardField.DAY));
                addTableCell(result, row, getField(e, StandardField.DAYFILED));
                addTableCell(result, row, getField(e, StandardField.EDITION));
                addTableCell(result, row, getField(e, StandardField.EDITOR)); // new AuthorLastFirst().format(getField(e, StandardField.EDITOR_FIELD)));
                addTableCell(result, row, getField(e, StandardField.HOWPUBLISHED));
                addTableCell(result, row, getField(e, StandardField.INSTITUTION));
                addTableCell(result, row, getField(e, StandardField.JOURNAL));
                addTableCell(result, row, getField(e, StandardField.LANGUAGE));
                addTableCell(result, row, getField(e, StandardField.MONTH));
                addTableCell(result, row, getField(e, StandardField.MONTHFILED));
                addTableCell(result, row, getField(e, StandardField.NATIONALITY));
                addTableCell(result, row, getField(e, StandardField.NOTE));
                addTableCell(result, row, getField(e, StandardField.NUMBER));
                addTableCell(result, row, getField(e, StandardField.ORGANIZATION));
                addTableCell(result, row, getField(e, StandardField.PAGES));
                addTableCell(result, row, getField(e, StandardField.PUBLISHER));
                addTableCell(result, row, getField(e, StandardField.REVISION));
                addTableCell(result, row, getField(e, StandardField.SCHOOL));
                addTableCell(result, row, getField(e, StandardField.SERIES));
                addTableCell(result, row, new RemoveWhitespace().format(new RemoveBrackets().format(getField(e, StandardField.TITLE))));
                addTableCell(result, row, getField(e, new UnknownField("reporttype")));
                addTableCell(result, row, getField(e, StandardField.VOLUME));
                addTableCell(result, row, getField(e, StandardField.YEAR));
                addTableCell(result, row, getField(e, StandardField.YEARFILED));
                addTableCell(result, row, getField(e, StandardField.URL));
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, getField(e, StandardField.ISBN));
                table.appendChild(row);
            }

            spreadsheet.appendChild(table);
            body.appendChild(spreadsheet);
            collection.appendChild(body);

            result.appendChild(collection);
        } catch (Exception e) {
            LOGGER.warn("Exception caught...", e);
        }
        return result;
    }

    private String getField(BibEntry e, Field field) {
        return e.getResolvedFieldOrAlias(field, database).orElse("");
    }

    private void addTableCell(Document doc, Element parent, String content) {
        Element cell = doc.createElement("table:table-cell");
        Element text = doc.createElement("text:p");
        Text textNode = doc.createTextNode(content);
        text.appendChild(textNode);
        // text.setTextContent(content);
        cell.appendChild(text);
        parent.appendChild(cell);
    }
}

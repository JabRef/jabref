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
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * @author Morten O. Alver.
 * Based on net.sf.jabref.MODSDatabase by Michael Wrighton
 *
 */
class OOCalcDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OOCalcDatabase.class);

    private final List<BibEntry> entries;


    public OOCalcDatabase(BibDatabase bibtex, List<BibEntry> entries) {
        // Make a list of comparators for sorting the entries:
        List<FieldComparator> comparators = new ArrayList<>();
        comparators.add(new FieldComparator(FieldName.AUTHOR));
        comparators.add(new FieldComparator(FieldName.YEAR));
        comparators.add(new FieldComparator(BibEntry.KEY_FIELD));
        // Use glazed lists to get a sorted view of the entries:
        List<BibEntry> entryList = new ArrayList<>();
        // Set up a list of all entries, if keySet==null, or the entries whose
        // ids are in keySet, otherwise:
        if (entries == null) {
            entryList.addAll(bibtex.getEntries());
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
            //collection.setAttribute("xmlns", "http://openoffice.org/2000/office");
            collection.setAttribute("xmlns:office", "http://openoffice.org/2000/office");
            collection.setAttribute("xmlns:style", "http://openoffice.org/2000/style");
            collection.setAttribute("xmlns:text", "http://openoffice.org/2000/text");
            collection.setAttribute("xmlns:table", "http://openoffice.org/2000/table");
            collection.setAttribute("xmlns:office:class", "spreadsheet");
            collection.setAttribute("xmlns:office:version", "1.0");
            collection.setAttribute("xmlns:fo", "http://www.w3.org/1999/XSL/Format");
            Element el = result.createElement("office:script");
            collection.appendChild(el);

            el = result.createElement("office:automatic-styles");
            Element el2 = result.createElement("style:style");
            el2.setAttribute("style:name", "ro1");
            el2.setAttribute("style:family", "table-row");
            Element el3 = result.createElement("style.properties");
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
            Element table = result.createElement("table:table");
            table.setAttribute("table:name", "biblio");
            table.setAttribute("table.style-name", "ta1");

            Element row = result.createElement("table:table-row");
            row.setAttribute("table.style-name", "ro1");
            addTableCell(result, row, "Type");
            addTableCell(result, row, "ISBN");
            addTableCell(result, row, "Identifier");
            addTableCell(result, row, "Author");
            addTableCell(result, row, "Title");
            addTableCell(result, row, "Journal");
            addTableCell(result, row, "Volume");
            addTableCell(result, row, "Number");
            addTableCell(result, row, "Month");
            addTableCell(result, row, "Pages");
            addTableCell(result, row, "Year");
            addTableCell(result, row, "Address");
            addTableCell(result, row, "Note");
            addTableCell(result, row, "URL");
            addTableCell(result, row, "Booktitle");
            addTableCell(result, row, "Chapter");
            addTableCell(result, row, "Edition");
            addTableCell(result, row, "Series");
            addTableCell(result, row, "Editor");
            addTableCell(result, row, "Publisher");
            addTableCell(result, row, "ReportType");
            addTableCell(result, row, "Howpublished");
            addTableCell(result, row, "Institution");
            addTableCell(result, row, "Organization");
            addTableCell(result, row, "School");
            addTableCell(result, row, "Annote");
            addTableCell(result, row, "Assignee");
            addTableCell(result, row, "Day");
            addTableCell(result, row, "Dayfiled");
            addTableCell(result, row, "Monthfiled");
            addTableCell(result, row, "Yearfiled");
            addTableCell(result, row, "Language");
            addTableCell(result, row, "Nationality");
            addTableCell(result, row, "Revision");
            addTableCell(result, row, "Custom1");
            addTableCell(result, row, "Custom2");
            addTableCell(result, row, "Custom3");
            addTableCell(result, row, "Custom4");
            addTableCell(result, row, "Custom5");
            table.appendChild(row);

            for (BibEntry e : entries) {
                row = result.createElement("table:table-row");
                addTableCell(result, row, new GetOpenOfficeType().format(e.getType()));
                addTableCell(result, row, getField(e, FieldName.ISBN));
                addTableCell(result, row, getField(e, BibEntry.KEY_FIELD));
                addTableCell(result, row, getField(e, FieldName.AUTHOR));//new AuthorLastFirst().format(getField(e, FieldName.AUTHOR_FIELD)));
                addTableCell(result, row, new RemoveWhitespace().format(new RemoveBrackets().format(getField(e, FieldName.TITLE))));
                addTableCell(result, row, getField(e, FieldName.JOURNAL));
                addTableCell(result, row, getField(e, FieldName.VOLUME));
                addTableCell(result, row, getField(e, FieldName.NUMBER));
                addTableCell(result, row, getField(e, FieldName.MONTH));
                addTableCell(result, row, getField(e, FieldName.PAGES));
                addTableCell(result, row, getField(e, FieldName.YEAR));
                addTableCell(result, row, getField(e, FieldName.ADDRESS));
                addTableCell(result, row, getField(e, FieldName.NOTE));
                addTableCell(result, row, getField(e, FieldName.URL));
                addTableCell(result, row, getField(e, FieldName.BOOKTITLE));
                addTableCell(result, row, getField(e, FieldName.CHAPTER));
                addTableCell(result, row, getField(e, FieldName.EDITION));
                addTableCell(result, row, getField(e, FieldName.SERIES));
                addTableCell(result, row, getField(e, FieldName.EDITOR));//new AuthorLastFirst().format(getField(e, FieldName.EDITOR_FIELD)));
                addTableCell(result, row, getField(e, FieldName.PUBLISHER));
                addTableCell(result, row, getField(e, "reporttype"));
                addTableCell(result, row, getField(e, FieldName.HOWPUBLISHED));
                addTableCell(result, row, getField(e, FieldName.INSTITUTION));
                addTableCell(result, row, getField(e, FieldName.ORGANIZATION));
                addTableCell(result, row, getField(e, FieldName.SCHOOL));
                addTableCell(result, row, getField(e, FieldName.ANNOTE));
                addTableCell(result, row, getField(e, FieldName.ASSIGNEE));
                addTableCell(result, row, getField(e, FieldName.DAY));
                addTableCell(result, row, getField(e, FieldName.DAYFILED));
                addTableCell(result, row, getField(e, FieldName.MONTHFILED));
                addTableCell(result, row, getField(e, FieldName.YEARFILED));
                addTableCell(result, row, getField(e, FieldName.LANGUAGE));
                addTableCell(result, row, getField(e, FieldName.NATIONALITY));
                addTableCell(result, row, getField(e, FieldName.REVISION));
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                addTableCell(result, row, "");
                table.appendChild(row);
            }

            body.appendChild(table);
            collection.appendChild(body);

            result.appendChild(collection);
        } catch (Exception e) {
            LOGGER.warn("Exception caught...", e);
        }
        return result;
    }

    private static String getField(BibEntry e, String field) {
        return e.getField(field).orElse("");
    }

    private static void addTableCell(Document doc, Element parent, String content) {
        Element cell = doc.createElement("table:table-cell");
        Element text = doc.createElement("text:p");
        Text textNode = doc.createTextNode(content);
        text.appendChild(textNode);
        //text.setTextContent(content);
        cell.appendChild(text);
        parent.appendChild(cell);
    }
}

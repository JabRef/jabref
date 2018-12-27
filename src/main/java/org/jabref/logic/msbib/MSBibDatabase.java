package org.jabref.logic.msbib;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Microsoft Word bibliography.
 * The class is uesed both for import and export
 * See http://www.ecma-international.org/publications/standards/Ecma-376.htm
 */
public class MSBibDatabase {

    public static final String NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/bibliography";
    public static final String PREFIX = "b:";

    private static final Logger LOGGER = LoggerFactory.getLogger(MSBibDatabase.class);

    private Set<MSBibEntry> entries;


    /**
     * Creates a {@link MSBibDatabase} for <b>import</b>
     */
    public MSBibDatabase() {
        entries = new HashSet<>();
    }

    // TODO: why an additonal entry list? entries are included inside database!
    /**
     * Creates a new {@link MSBibDatabase} for <b>export</b>
     * @param database The bib database
     * @param entries List of {@link BibEntry}
     */
    public MSBibDatabase(BibDatabase database, List<BibEntry> entries) {
        if (entries == null) {
            addEntriesForExport(database.getEntries());
        } else {
            addEntriesForExport(entries);
        }
    }

    /**
     * Imports entries from an office xml file
     * @param reader
     * @return List of {@link BibEntry}
     */
    public List<BibEntry> importEntriesFromXml(BufferedReader reader) {
        entries = new HashSet<>();
        Document inputDocument;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            inputDocument = documentBuilder.parse(new InputSource(reader));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.warn("Could not parse document", e);
            return Collections.emptyList();
        }
        NodeList rootList = inputDocument.getElementsByTagNameNS("*", "Sources");
        if (rootList.getLength() == 0) {
            rootList = inputDocument.getElementsByTagNameNS("*", "Sources");
        }
        List<BibEntry> bibitems = new ArrayList<>();
        if (rootList.getLength() == 0) {
            return bibitems;
        }

        NodeList sourceList = ((Element) rootList.item(0)).getElementsByTagNameNS("*", "Source");
        for (int i = 0; i < sourceList.getLength(); i++) {
            MSBibEntry entry = new MSBibEntry((Element) sourceList.item(i));
            entries.add(entry);
            bibitems.add(BibTeXConverter.convert(entry));
        }

        return bibitems;
    }

    private void addEntriesForExport(List<BibEntry> entriesToAdd) {
        entries = new HashSet<>();
        for (BibEntry entry : entriesToAdd) {
            MSBibEntry newMods = MSBibConverter.convert(entry);
            entries.add(newMods);
        }
    }

    /**
     * Gets the assembled dom for export
     * @return XML Document
     */
    public Document getDomForExport() {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.newDocument();

            Element rootNode = document.createElementNS(NAMESPACE, PREFIX + "Sources");
            rootNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NAMESPACE);
            rootNode.setAttributeNS("http://www.w3.org/2000/xmlns/",
                    "xmlns:" + PREFIX.substring(0, PREFIX.length() - 1), NAMESPACE);
            rootNode.setAttribute("SelectedStyle", "");

            for (MSBibEntry entry : entries) {
                Node node = entry.getEntryDom(document);
                rootNode.appendChild(node);
            }
            document.appendChild(rootNode);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not build XML document", e);
        }
        return document;
    }
}

/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.msbib;

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

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Microsoft Word bibliography.
 *
 * See http://www.ecma-international.org/publications/standards/Ecma-376.htm
 */
public class MSBibDatabase {
    private static final Log LOGGER = LogFactory.getLog(MSBibDatabase.class);

    public static final String NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/bibliography";
    public static final String PREFIX = "b:";

    private Set<MSBibEntry> entries;

    public MSBibDatabase() {
        entries = new HashSet<>();
    }

    // TODO: why an additonal entry list? entries are included inside database!
    public MSBibDatabase(BibDatabase database, List<BibEntry> entries) {
        if (entries == null) {
            addEntries(database.getEntries());
        } else {
            addEntries(entries);
        }
    }

    public List<BibEntry> importEntries(BufferedReader reader) {
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

    private void addEntries(List<BibEntry> entriesToAdd) {
        entries = new HashSet<>();
        for (BibEntry entry : entriesToAdd) {
            MSBibEntry newMods = MSBibConverter.convert(entry);
            entries.add(newMods);
        }
    }

    public Document getDOM() {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.newDocument();

            Element rootNode = document.createElementNS(NAMESPACE, PREFIX + "Sources");
            rootNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NAMESPACE);
            rootNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + PREFIX.substring(0, PREFIX.length() - 1), NAMESPACE);
            rootNode.setAttribute("SelectedStyle", "");

            for (MSBibEntry entry : entries) {
                Node node = entry.getDOM(document);
                rootNode.appendChild(node);
            }

            document.appendChild(rootNode);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not build XML document", e);
        }

        return document;
    }
}

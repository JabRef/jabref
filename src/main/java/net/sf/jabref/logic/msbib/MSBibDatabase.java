/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Microsoft Word bibliography.
 *
 * See http://www.ecma-international.org/publications/standards/Ecma-376.htm
 */
public class MSBibDatabase {
    private static final Log LOGGER = LogFactory.getLog(MSBibDatabase.class);

    private Set<MSBibEntry> entries;

    public MSBibDatabase() {
        // maybe make this sorted later...
        entries = new HashSet<>();
    }

    public MSBibDatabase(InputStream stream) {
        importEntries(stream);
    }

    public MSBibDatabase(BibtexDatabase bibtex) {
        Set<String> keySet = bibtex.getKeySet();
        addEntries(bibtex, keySet);
    }

    public MSBibDatabase(BibtexDatabase bibtex, Set<String> keySet) {
        if (keySet == null) {
            keySet = bibtex.getKeySet();
        }
        addEntries(bibtex, keySet);
    }

    public List<BibtexEntry> importEntries(InputStream stream) {
        entries = new HashSet<>();
        ArrayList<BibtexEntry> bibitems = new ArrayList<>();
        Document inputDocument = null;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.
                    newInstance().
                    newDocumentBuilder();
            inputDocument = documentBuilder.parse(stream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.warn("Could not parse document", e);
        }
        String bcol = "b:";
        NodeList rootList = inputDocument.getElementsByTagName("b:Sources");
        if (rootList.getLength() == 0) {
            rootList = inputDocument.getElementsByTagName("Sources");
            bcol = "";
        }
        if (rootList.getLength() == 0) {
            return bibitems;
        }

        NodeList sourceList = ((Element) rootList.item(0)).getElementsByTagName(bcol + "Source");
        for (int i = 0; i < sourceList.getLength(); i++) {
            MSBibEntry entry = new MSBibEntry((Element) sourceList.item(i), bcol);
            entries.add(entry);
            bibitems.add(entry.getBibtexRepresentation());
        }

        return bibitems;
    }

    private void addEntries(BibtexDatabase database, Set<String> keySet) {
        entries = new HashSet<>();
        for (String key : keySet) {
            BibtexEntry entry = database.getEntryById(key);
            MSBibEntry newMods = new MSBibEntry(entry);
            entries.add(newMods);
        }
    }

    public Document getDOMrepresentation() {
        Document result = null;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.
                    newInstance().
                    newDocumentBuilder();
            result = documentBuilder.newDocument();
            Element msbibCollection = result.createElement("b:Sources");
            msbibCollection.setAttribute("SelectedStyle", "");
            msbibCollection.setAttribute("xmlns", "http://schemas.openxmlformats.org/officeDocument/2006/bibliography");
            msbibCollection.setAttribute("xmlns:b", "http://schemas.openxmlformats.org/officeDocument/2006/bibliography");

            for (MSBibEntry entry : entries) {
                Node node = entry.getDOMrepresentation(result);
                msbibCollection.appendChild(node);
            }

            result.appendChild(msbibCollection);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not build document", e);
        }
        return result;
    }
}

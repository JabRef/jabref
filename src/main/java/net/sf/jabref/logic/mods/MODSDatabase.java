package net.sf.jabref.logic.mods;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Michael Wrighton
 *
 */
public class MODSDatabase {

    private Set<MODSEntry> entries;

    private static final Log LOGGER = LogFactory.getLog(MODSDatabase.class);

    public MODSDatabase() {
        // maybe make this sorted later...
        entries = new HashSet<>();
    }

    public MODSDatabase(BibDatabase database, List<BibEntry> entries) {
        if (entries == null) {
            addEntries(database.getEntries());
        } else {
            addEntries(entries);
        }
    }

    private void addEntries(List<BibEntry> entriesToAdd) {
        entries = new HashSet<>();
        for (BibEntry entry : entriesToAdd) {
            MODSEntry newMods = new MODSEntry(entry);
            entries.add(newMods);
        }
    }

    public Document getDOMrepresentation() {
        Document result = null;
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.
                    newInstance().
                    newDocumentBuilder();
            result = dbuild.newDocument();
            Element modsCollection = result.createElement("modsCollection");
            modsCollection.setAttribute("xmlns", "http://www.loc.gov/mods/v3");
            modsCollection.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            modsCollection.setAttribute("xsi:schemaLocation", "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-0.xsd");

            for (MODSEntry entry : entries) {
                Node node = entry.getDOMrepresentation(result);
                modsCollection.appendChild(node);
            }

            result.appendChild(modsCollection);
        } catch (Exception e)         {
           LOGGER.info("Could not get DOM representation", e);
        }
        return result;
    }
}

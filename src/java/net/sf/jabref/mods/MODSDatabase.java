/*
 * Created on Oct 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref.mods;
import net.sf.jabref.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
/**
 * @author Michael Wrighton
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MODSDatabase {
	protected Set entries;
	
	public MODSDatabase() {
		// maybe make this sorted later...
		entries = new HashSet();
	}
	
	public MODSDatabase(BibtexDatabase bibtex) {
		this();
		Collection c = bibtex.getEntries();
		for(Iterator iter = c.iterator(); iter.hasNext(); ) {
			BibtexEntry entry = (BibtexEntry) iter.next();
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
	   		
	   		for(Iterator iter = entries.iterator(); iter.hasNext(); ) {
	   			MODSEntry entry = (MODSEntry) iter.next();
	   			Node node = entry.getDOMrepresentation(result);
	   			modsCollection.appendChild(node);
	   		}
	   		
	   		result.appendChild(modsCollection);	   		
	   	}
	   	catch (Exception e)
		{
	   		System.out.println("Exception caught..." + e);
	   		e.printStackTrace();
		}
	   	return result;
	   }
}

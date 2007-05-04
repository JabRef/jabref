/*
 * Created on April 01, 2007
 * Updated on May 03, 2007
 * */

package net.sf.jabref.msbib;
import net.sf.jabref.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
/**
 * @author S M Mahbub Murshed
 * @email udvranto@yahoo.com
 *
 * @version 1.0.0
 * @see http://mahbub.wordpress.com/2007/03/24/details-of-microsoft-office-2007-bibliographic-format-compared-to-bibtex/
 * @see http://mahbub.wordpress.com/2007/03/22/deciphering-microsoft-office-2007-bibliography-format/
 * 
 * Date: May 03, 2007
 * 
 */
public class MSBibDatabase {
	protected Set entries;
	
	public MSBibDatabase() {
		// maybe make this sorted later...
		entries = new HashSet();
	}
	
	public MSBibDatabase(BibtexDatabase bibtex) {
		Set keySet = bibtex.getKeySet();
        addEntries(bibtex, keySet);
    }

    public MSBibDatabase(BibtexDatabase bibtex, Set keySet) {
        if (keySet == null)
            keySet = bibtex.getKeySet();
        addEntries(bibtex, keySet);
    }


    private void addEntries(BibtexDatabase database, Set keySet) {
        entries = new HashSet();
        for(Iterator iter = keySet.iterator(); iter.hasNext(); ) {
			BibtexEntry entry = database.getEntryById((String)iter.next());
			MSBibEntry newMods = new MSBibEntry(entry);
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
	   		Element msbibCollection = result.createElement("b:Sources");
	   		msbibCollection.setAttribute("SelectedStyle","");
	   		msbibCollection.setAttribute("xmlns", "http://schemas.openxmlformats.org/officeDocument/2006/bibliography");
	   		msbibCollection.setAttribute("xmlns:b", "http://schemas.openxmlformats.org/officeDocument/2006/bibliography");	   			   		 
	   		
	   		for(Iterator iter = entries.iterator(); iter.hasNext(); ) {
	   			MSBibEntry entry = (MSBibEntry) iter.next();
	   			Node node = entry.getDOMrepresentation(result);
	   			msbibCollection.appendChild(node);
	   		}
	   		
	   		result.appendChild(msbibCollection);	   		
	   	}
	   	catch (Exception e)
		{
	   		System.out.println("Exception caught..." + e);
	   		e.printStackTrace();
		}
	   	return result;
	   }
}

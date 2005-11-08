package net.sf.jabref.mods;
import net.sf.jabref.*;
import net.sf.jabref.export.layout.format.*;
import net.sf.jabref.export.layout.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;
/**
 * @author Michael Wrighton
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MODSEntry {
	protected String entryType = "mods"; // could also be relatedItem
	protected String id;
	protected List authors = null;
	protected List editors = null;
	// should really be handled with an enum
	protected String issuance = "monographic";
	protected PageNumbers pages = null;
	
	protected String publisher = null;
	protected String date = null;
	protected String place = null;
	
	protected String title = null;
	// should really be handled with an enum
	protected String type = "text";
	
	protected String number;
	protected String volume;
	protected String genre = null;
	protected Set handledExtensions;
	
	protected MODSEntry host;
	Map extensionFields;
	
	public static String BIBTEX = "bibtex_";
	
	public MODSEntry() {
		extensionFields = new HashMap();
		handledExtensions = new HashSet();
	
	}
	
	public MODSEntry(BibtexEntry bibtex) {
		this();
		handledExtensions.add(BIBTEX + "publisher");
		handledExtensions.add(BIBTEX + "title");
		handledExtensions.add(BIBTEX + "bibtexkey");
		handledExtensions.add(BIBTEX + "author");
		populateFromBibtex(bibtex);
	}
	
	protected void populateFromBibtex(BibtexEntry bibtex) {
		LayoutFormatter chars = new XMLChars();
		if (bibtex.getField("title") != null)
			title = chars.format(bibtex.getField("title").toString());
		
		if (bibtex.getField("publisher") != null)
			publisher = chars.format(bibtex.getField("publisher").toString());
		if (bibtex.getField("bibtexkey") != null)
			id = bibtex.getField("bibtexkey").toString();
		if (bibtex.getField("place") != null)
			place = chars.format(bibtex.getField("place").toString());
		date = getDate(bibtex);	
		genre = getMODSgenre(bibtex);
		if (bibtex.getField("author") != null)
			authors = getAuthors(bibtex.getField("author").toString());
		if (bibtex.getType() == BibtexEntryType.ARTICLE || 
			bibtex.getType() == BibtexEntryType.INPROCEEDINGS)
		{
			host = new MODSEntry();
			host.entryType = "relatedItem";
			host.title = (String) bibtex.getField("booktitle");
			host.publisher = (String) bibtex.getField("publisher");
			host.number = (String) bibtex.getField("number");
			if (bibtex.getField("pages") != null)
				host.volume = (String) bibtex.getField("volume");
			host.issuance = "continuing";
			if (bibtex.getField("pages") != null)
				host.pages = new PageNumbers((String) bibtex.getField("pages"));
		}
		
		populateExtensionFields(bibtex);
		
	}
	
	protected void populateExtensionFields(BibtexEntry e) {
		Object fields [] = e.getAllFields();
		for(int i = 0; i < fields.length; i++) {
			String field = (String) fields[i];
			String value = (String) e.getField(field);
			field = BIBTEX + field;
			extensionFields.put(field, value);
		}
	}
	
	protected List getAuthors(String authors) {
		List result = new LinkedList();
		LayoutFormatter chars = new XMLChars();
		
		if (authors.indexOf(" and ") == -1)
          result.add(new PersonName(chars.format(authors)));
        else
        {
            String[] names = authors.split(" and ");
            for (int i=0; i<names.length; i++)
              result.add(new PersonName(chars.format(names[i])));
        }
		return result;
	}
	
	/* construct a MODS date object */
	protected String getDate(BibtexEntry bibtex) {
		String result = "";
		if (bibtex.getField("year") != null)
			result += (bibtex.getField("year").toString());
		if (bibtex.getField("month") != null)
			result += "-" + bibtex.getField("month").toString();
		
		return result;
	}
	// must be from http://www.loc.gov/marc/sourcecode/genre/genrelist.html
	protected String getMODSgenre(BibtexEntry bibtex) {
		String bibtexType = bibtex.getType().getName();
		String result;
		if (bibtexType.equals("Mastersthesis"))
			result = "theses";
		else
			result = "conference publication";
		// etc...
		return bibtexType;		
	}
	
	public Document getDOMrepresentation() {
		Document result = null;
		try {
			DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
		//	result = getDOMrepresentation(d);
		}
		catch (Exception e) 
		{
			throw new Error(e);
		}
		return result;
	}
		
	
	public Element getDOMrepresentation(Document d) {
		Node result = null;
	   	try {
	   		Element mods = d.createElement(entryType);
	   		mods.setAttribute("version", "3.0");
	   		// mods.setAttribute("xmlns:xlink:", "http://www.w3.org/1999/xlink");
	   		// title
	   		if(title != null) {
	   			Element titleInfo = d.createElement("titleInfo");
	   			Element mainTitle = d.createElement("title");
	   			mainTitle.appendChild(d.createTextNode(title));
	   			titleInfo.appendChild(mainTitle);
		   		mods.appendChild(titleInfo);
	   		}
	   		if (authors != null) {
	   			for(Iterator iter = authors.iterator(); iter.hasNext();) {
	   				PersonName name = (PersonName) iter.next();
	   				Element modsName = d.createElement("name");
	   				modsName.setAttribute("type", "personal");
	   				if (name.getSurname() != null) {
	   					Element namePart = d.createElement("namePart");
	   					namePart.setAttribute("type", "family");
	   					namePart.appendChild(d.createTextNode(name.getSurname()));
	   					modsName.appendChild(namePart);
	   				}
	   				if (name.getGivenNames() != null) {
	   					Element namePart = d.createElement("namePart");
	   					namePart.setAttribute("type", "given");
	   					namePart.appendChild(d.createTextNode(name.getGivenNames()));
	   					modsName.appendChild(namePart);
	   				}
	   				Element role = d.createElement("role");
	   				Element roleTerm = d.createElement("roleTerm");
	   				roleTerm.setAttribute("type", "text");
	   				roleTerm.appendChild(d.createTextNode("author"));
	   				role.appendChild(roleTerm);
	   				modsName.appendChild(role);
	   				mods.appendChild(modsName);
	   			}
	   		}
	   		//publisher
	   		Element originInfo = d.createElement("originInfo");
	   		mods.appendChild(originInfo);
	   		if (this.publisher != null) {
	   			Element publisher = d.createElement("publisher");
				publisher.appendChild(d.createTextNode(this.publisher));
	   			originInfo.appendChild(publisher);
	   		}
	   		if (date != null) {
	   			Element dateIssued = d.createElement("dateIssued");
	   			dateIssued.appendChild(d.createTextNode(date));
	   			originInfo.appendChild(dateIssued);
	   		}
	   		Element issuance = d.createElement("issuance");
	   		issuance.appendChild(d.createTextNode(this.issuance));
	   		originInfo.appendChild(issuance);
	   		
	   		if (id != null) {
	   			Element idref = d.createElement("identifier");
	   			idref.appendChild(d.createTextNode(id));
	   			mods.appendChild(idref);
	   			mods.setAttribute("ID", id);
		   		
	   		}
	   		Element typeOfResource = d.createElement("typeOfResource");
	   		typeOfResource.appendChild(d.createTextNode(type));
	   		mods.appendChild(typeOfResource);
	   		
	   		if (genre != null) {
	   			Element genreElement = d.createElement("genre");
	   			genreElement.setAttribute("authority", "marc");
	   			genreElement.appendChild(d.createTextNode(genre));
	   			mods.appendChild(genreElement);
	   		}
	   		
	   		if (host != null) {
	   			Element relatedItem = host.getDOMrepresentation(d);	   			
	   			relatedItem.setAttribute("type","host");	   			
	   			mods.appendChild(relatedItem);
	   		}
	   		if (pages != null) {
	   			mods.appendChild(pages.getDOMrepresentation(d));
	   		}
	   		
	   		/* now generate extension fields for unhandled data */
	   		for(Iterator iter = extensionFields.entrySet().iterator(); iter.hasNext(); ) {
	   			Element extension = d.createElement("extension");
	   			Map.Entry theEntry = (Map.Entry) iter.next();
	   			String field = (String) theEntry.getKey();
	   			String value = (String) theEntry.getValue();
	   			if (handledExtensions.contains(field))
	   				continue;
	   			Element theData = d.createElement(field);
	   			theData.appendChild(d.createTextNode(value));
	   			extension.appendChild(theData);
	   			mods.appendChild(extension);
	   		}
	   		return mods;
	   	}
	   	catch (Exception e)
		{
	   		System.out.println("Exception caught..." + e);
	   		e.printStackTrace();
	   		throw new Error(e);
		}
	   	// return result;
	   }
	
	/*
	 * render as XML
	 */
	public String toString() {
		StringWriter sresult = new StringWriter();
	   	try {
	      	 DOMSource source = new DOMSource(getDOMrepresentation());
	      	 StreamResult result = new StreamResult(sresult);
	      	 Transformer trans = TransformerFactory.newInstance().newTransformer();
	      	 trans.setOutputProperty(OutputKeys.INDENT, "yes");
	      	 trans.transform(source, result);
	      	}
	      	catch (Exception e) {
	      		throw new Error(e);
	      	}
	      return sresult.toString();
	}

}

package net.sf.jabref.mods;
import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.XMLChars;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
/**
 * @author Michael Wrighton
 *
 */
public class MODSEntry {
	protected String entryType = "mods"; // could also be relatedItem
	protected String id;
	protected List<PersonName> authors = null;

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
	protected Set<String> handledExtensions;
	
	protected MODSEntry host;
	Map<String, String> extensionFields;
	
	public static String BIBTEX = "bibtex_";
	
	private final boolean CHARFORMAT = false;
	
	public MODSEntry() {
		extensionFields = new HashMap<String, String>();
		handledExtensions = new HashSet<String>();
	
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
		if (bibtex.getField("title") != null) {
			if(CHARFORMAT)
				title = chars.format(bibtex.getField("title").toString());
			else
				title = bibtex.getField("title").toString();
		}
		
		if (bibtex.getField("publisher") != null) {
			if(CHARFORMAT)
				publisher = chars.format(bibtex.getField("publisher").toString());
			else
				publisher = bibtex.getField("publisher").toString();
		}
			
		if (bibtex.getField("bibtexkey") != null)
			id = bibtex.getField("bibtexkey").toString();
		if (bibtex.getField("place") != null) {
			if(CHARFORMAT)
				place = chars.format(bibtex.getField("place").toString());
			else
				place = bibtex.getField("place").toString();
		}
			
		date = getDate(bibtex);	
		genre = getMODSgenre(bibtex);
		if (bibtex.getField("author") != null)
			authors = getAuthors(bibtex.getField("author").toString());
		if (bibtex.getType() == BibtexEntryType.ARTICLE || 
			bibtex.getType() == BibtexEntryType.INPROCEEDINGS)
		{
			host = new MODSEntry();
			host.entryType = "relatedItem";
			host.title = bibtex.getField("booktitle");
			host.publisher = bibtex.getField("publisher");
			host.number = bibtex.getField("number");
			if (bibtex.getField("pages") != null)
				host.volume = bibtex.getField("volume");
			host.issuance = "continuing";
			if (bibtex.getField("pages") != null)
				host.pages = new PageNumbers(bibtex.getField("pages"));
		}
		
		populateExtensionFields(bibtex);
		
	}
	
	protected void populateExtensionFields(BibtexEntry e) {
		
		for (String field : e.getAllFields()){
			String value = e.getField(field);
			field = BIBTEX + field;
			extensionFields.put(field, value);
		}
	}
	
	protected List<PersonName> getAuthors(String authors) {
		List<PersonName> result = new LinkedList<PersonName>();
		LayoutFormatter chars = new XMLChars();
		
		if (authors.indexOf(" and ") == -1) {
			if(CHARFORMAT)
				result.add(new PersonName(chars.format(authors)));
			else
				result.add(new PersonName(authors));
		}
        else
        {
            String[] names = authors.split(" and ");
            for (int i=0; i<names.length; i++) {
            	if(CHARFORMAT)
            		result.add(new PersonName(chars.format(names[i])));
            	else
            		result.add(new PersonName(names[i]));
            }
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
		/**
		 * <pre> String result; if (bibtexType.equals("Mastersthesis")) result =
		 * "theses"; else result = "conference publication"; // etc... </pre>
		 */
		return bibtexType;		
	}
	
	public Node getDOMrepresentation() {
		Node result = null;
		try {
			DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			result = getDOMrepresentation(d.newDocument());
		}
		catch (Exception e) 
		{
			throw new Error(e);
		}
		return result;
	}
		
	
	public Element getDOMrepresentation(Document d) {
	   	try {
	   		Element mods = d.createElement(entryType);
	   		mods.setAttribute("version", "3.0");
	   		// mods.setAttribute("xmlns:xlink:", "http://www.w3.org/1999/xlink");
	   		// title
	   		if(title != null) {
	   			Element titleInfo = d.createElement("titleInfo");
	   			Element mainTitle = d.createElement("title");
	   			mainTitle.appendChild(d.createTextNode(stripNonValidXMLCharacters(title)));
	   			titleInfo.appendChild(mainTitle);
		   		mods.appendChild(titleInfo);
	   		}
	   		if (authors != null) {
	   			for(Iterator<PersonName> iter = authors.iterator(); iter.hasNext();) {
	   				PersonName name = iter.next();
	   				Element modsName = d.createElement("name");
	   				modsName.setAttribute("type", "personal");
	   				if (name.getSurname() != null) {
	   					Element namePart = d.createElement("namePart");
	   					namePart.setAttribute("type", "family");
	   					namePart.appendChild(d.createTextNode(stripNonValidXMLCharacters(name.getSurname())));
	   					modsName.appendChild(namePart);
	   				}
	   				if (name.getGivenNames() != null) {
	   					Element namePart = d.createElement("namePart");
	   					namePart.setAttribute("type", "given");
	   					namePart.appendChild(d.createTextNode(stripNonValidXMLCharacters(name.getGivenNames())));
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
				publisher.appendChild(d.createTextNode(stripNonValidXMLCharacters(this.publisher)));
	   			originInfo.appendChild(publisher);
	   		}
	   		if (date != null) {
	   			Element dateIssued = d.createElement("dateIssued");
	   			dateIssued.appendChild(d.createTextNode(stripNonValidXMLCharacters(date)));
	   			originInfo.appendChild(dateIssued);
	   		}
	   		Element issuance = d.createElement("issuance");
	   		issuance.appendChild(d.createTextNode(stripNonValidXMLCharacters(this.issuance)));
	   		originInfo.appendChild(issuance);
	   		
	   		if (id != null) {
	   			Element idref = d.createElement("identifier");
	   			idref.appendChild(d.createTextNode(stripNonValidXMLCharacters(id)));
	   			mods.appendChild(idref);
	   			mods.setAttribute("ID", id);
		   		
	   		}
	   		Element typeOfResource = d.createElement("typeOfResource");
	   		typeOfResource.appendChild(d.createTextNode(stripNonValidXMLCharacters(type)));
	   		mods.appendChild(typeOfResource);
	   		
	   		if (genre != null) {
	   			Element genreElement = d.createElement("genre");
	   			genreElement.setAttribute("authority", "marc");
	   			genreElement.appendChild(d.createTextNode(stripNonValidXMLCharacters(genre)));
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
	   		for(Map.Entry<String, String> theEntry : extensionFields.entrySet()){
	   			Element extension = d.createElement("extension");
	   			String field = theEntry.getKey();
	   			String value = theEntry.getValue();
	   			if (handledExtensions.contains(field))
	   				continue;
	   			Element theData = d.createElement(field);
	   			theData.appendChild(d.createTextNode(stripNonValidXMLCharacters(value)));
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
	
	/**
	 * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     * 
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

	/*
	 * render as XML
	 * 
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

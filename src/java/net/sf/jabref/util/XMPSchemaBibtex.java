package net.sf.jabref.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerException;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;

import org.jempbox.xmp.XMPMetadata;
import org.jempbox.xmp.XMPSchema;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMPSchemaBibtex extends XMPSchema {

	/**
	 * The namespace of this schema.
	 */
	public static final String NAMESPACE = "http://jabref.sourceforge.net/bibteXMP/";

	public static final String KEY = "bibtex";

	public XMPSchemaBibtex(XMPMetadata parent) {
		super(parent, KEY, NAMESPACE);
	}

	/**
	 * Create schema from an existing XML element.
	 * 
	 * @param element
	 *            The existing XML element.
	 */
	public XMPSchemaBibtex(Element e) {
		super(e);
	}

	protected String makeProperty(String propertyName) {
		return KEY + ":" + propertyName;
	}

	/**
	 * 
	 * @param field
	 * @return
	 * @derived Uses XMPSchema methods
	 */
	public List getPersonList(String field) {
		return getSequenceList(field);
	}

	/**
	 * 
	 * @param field
	 * @param value
	 * @derived Uses XMPSchema methods
	 */
	public void setPersonList(String field, String value) {
		AuthorList list = AuthorList.getAuthorList(value);

		int n = list.size();
		for (int i = 0; i < n; i++) {
			addSequenceValue(field, list.getAuthor(i).getFirstLast(false));
		}
	}

	public String getTextProperty(String field) {
		return super.getTextProperty(makeProperty(field));
	}

	public void setTextProperty(String field, String value) {
		super.setTextProperty(makeProperty(field), value);
	}

	public List getBagList(String bagName) {
		return super.getBagList(makeProperty(bagName));
	}

	public void removeBagValue(String bagName, String value) {
		super.removeBagValue(makeProperty(bagName), value);
	}

	public void addBagValue(String bagName, String value) {
		super.addBagValue(makeProperty(bagName), value);
	}

	public List getSequenceList(String seqName) {
		return super.getSequenceList(makeProperty(seqName));
	}

	public void removeSequenceValue(String seqName, String value) {
		super.removeSequenceValue(makeProperty(seqName), value);
	}

	public void addSequenceValue(String seqName, String value) {
		super.addSequenceValue(makeProperty(seqName), value);
	}

	public List getSequenceDateList(String seqName) throws IOException {
		return super.getSequenceDateList(makeProperty(seqName));
	}

	public void removeSequenceDateValue(String seqName, Calendar date) {
		super.removeSequenceDateValue(makeProperty(seqName), date);
	}

	public void addSequenceDateValue(String field, Calendar date) {
		super.addSequenceDateValue(makeProperty(field), date);
	}

	/**
	 * Returns a map of all properties and their values. LIs in seqs are
	 * concatenated using " and ".
	 * 
	 * @return Map from name of textproperty (String) to value (String). For
	 *         instance: "year" => "2005". Empty map if none found.
	 * @throws TransformerException
	 */
	public Map getAllProperties() {
		NodeList nodes = getElement().getChildNodes();

		Map result = new HashMap();

		if (nodes == null) {
			return result;
		}

		// Check child-nodes first
		int n = nodes.getLength();

		for (int i = 0; i < n; i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() != Node.ATTRIBUTE_NODE
				&& node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String nodeName = node.getNodeName();

			String[] split = nodeName.split(":");
			if (split.length == 2 && split[0].equals("bibtex")) {

				NodeList seqList = ((Element) node).getElementsByTagName("rdf:Seq");
				if (seqList.getLength() > 0) {
					Element seqNode = (Element) seqList.item(0);
					StringBuffer seq = null;

					NodeList items = seqNode.getElementsByTagName("rdf:li");
					for (int j = 0; j < items.getLength(); j++) {
						Element li = (Element) items.item(j);
						if (seq == null) {
							seq = new StringBuffer();
						} else {
							seq.append(" and ");
						}
						seq.append(getTextContent(li));
					}
					if (seq != null) {
						result.put(split[1], seq.toString());
					}
				} else {
					result.put(split[1], getTextContent(node));
				}
			}
		}

		// Then check Attributes
		NamedNodeMap attrs = getElement().getAttributes();
		int m = attrs.getLength();
		for (int j = 0; j < m; j++) {
			Node attr = attrs.item(j);

			String nodeName = attr.getNodeName();
			String[] split = nodeName.split(":");
			if (split.length == 2 && split[0].equals("bibtex")) {
				result.put(split[1], attr.getNodeValue());
			}
		}

		/*
		 * Collapse Whitespace
		 * 
		 * Quoting from
		 * http://www.gerg.ca/software/btOOL/doc/bt_postprocess.html: <cite>
		 * "The exact rules for collapsing whitespace are simple: non-space
		 * whitespace characters (tabs and newlines mainly) are converted to
		 * space, any strings of more than one space within are collapsed to a
		 * single space, and any leading or trailing spaces are deleted."
		 * </cite>
		 */
		Set entries = result.entrySet();
		Iterator it = entries.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			entry.setValue(((String) entry.getValue()).replaceAll("\\s+", " ").trim());
		}

		return result;
	}

	public void setBibtexEntry(BibtexEntry entry) {
		// Set all the values including key and entryType
		Object[] fields = entry.getAllFields();

		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equals("author") || fields[i].equals("editor")) {
				setPersonList(fields[i].toString(), entry.getField(fields[i].toString()).toString());
			} else {
				setTextProperty(fields[i].toString(), entry.getField(fields[i].toString())
					.toString());
			}
		}
		setTextProperty("entrytype", entry.getType().getName());
	}

	public BibtexEntry getBibtexEntry() {

		String type = getTextProperty("entrytype");
		BibtexEntryType t;
		if (type != null)
			t = BibtexEntryType.getStandardType(type);
		else
			t = BibtexEntryType.OTHER;

		BibtexEntry e = new BibtexEntry(Util.createNeutralId(), t);

		// Get Text Properties
		Map text = getAllProperties();
		e.setField(text);
		return e;

	}

	/**
	 * Taken from DOM2Utils.java:
	 * 
	 * JBoss, the OpenSource EJB server
	 * 
	 * Distributable under LGPL license. See terms of license at gnu.org.
	 */
	public static String getTextContent(Node node) {
		boolean hasTextContent = false;
		StringBuffer buffer = new StringBuffer();
		NodeList nlist = node.getChildNodes();
		for (int i = 0; i < nlist.getLength(); i++) {
			Node child = nlist.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				buffer.append(child.getNodeValue());
				hasTextContent = true;
			}
		}
		return (hasTextContent ? buffer.toString() : "");
	}

}

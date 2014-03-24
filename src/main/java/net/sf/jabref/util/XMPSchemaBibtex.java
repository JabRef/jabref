/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.util;

import java.io.IOException;
import java.util.*;

import javax.xml.transform.TransformerException;

import net.sf.jabref.*;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchema;
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

	/**
	 * Create a new empty XMPSchemaBibtex as a child in the given XMPMetadata.
	 * 
	 * @param parent
	 */
	public XMPSchemaBibtex(XMPMetadata parent) {
		super(parent, KEY, NAMESPACE);
	}

	/**
	 * Create schema from an existing XML element.
	 * 
	 * @param e
	 *            The existing XML element.
	 */
	public XMPSchemaBibtex(Element e, String namespace) {
		super(e, KEY);
	}

	protected String makeProperty(String propertyName) {
		return KEY + ":" + propertyName;
	}

	/**
     * Uses XMPSchema methods
	 * 
	 * @param field
	 * @return
	 */
	public List<String> getPersonList(String field) {
		return getSequenceList(field);
	}

	/**
	 * Uses XMPSchema methods
     *
	 * @param field
	 * @param value
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

	@SuppressWarnings("unchecked")
	public List<String> getBagList(String bagName) {
		return super.getBagList(makeProperty(bagName));
	}

	public void removeBagValue(String bagName, String value) {
		super.removeBagValue(makeProperty(bagName), value);
	}

	public void addBagValue(String bagName, String value) {
		super.addBagValue(makeProperty(bagName), value);
	}

	@SuppressWarnings("unchecked")
	public List<String> getSequenceList(String seqName) {
		return super.getSequenceList(makeProperty(seqName));
	}

	public void removeSequenceValue(String seqName, String value) {
		super.removeSequenceValue(makeProperty(seqName), value);
	}

	public void addSequenceValue(String seqName, String value) {
		super.addSequenceValue(makeProperty(seqName), value);
	}

	public List<Calendar> getSequenceDateList(String seqName) throws IOException {
		return super.getSequenceDateList(makeProperty(seqName));
	}

	public void removeSequenceDateValue(String seqName, Calendar date) {
		super.removeSequenceDateValue(makeProperty(seqName), date);
	}

	public void addSequenceDateValue(String field, Calendar date) {
		super.addSequenceDateValue(makeProperty(field), date);
	}

	public static String getContents(NodeList seqList) {

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
			return seq.toString();
		}
		return null;
	}

	/**
	 * Returns a map of all properties and their values. LIs and bags in seqs
	 * are concatenated using " and ".
	 * 
	 * @return Map from name of textproperty (String) to value (String). For
	 *         instance: "year" => "2005". Empty map if none found.
	 * @throws TransformerException
	 */
	public static Map<String, String> getAllProperties(XMPSchema schema, String namespaceName) {
		NodeList nodes = schema.getElement().getChildNodes();

		Map<String, String> result = new HashMap<String, String>();

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

			if (split.length == 2 && split[0].equals(namespaceName)) {
				NodeList seqList = ((Element) node).getElementsByTagName("rdf:Seq");
				if (seqList.getLength() > 0) {

					String seq = getContents(seqList);

					if (seq != null) {
						result.put(split[1], seq);
					}
				} else {
					NodeList bagList = ((Element) node).getElementsByTagName("rdf:Bag");
					if (bagList.getLength() > 0) {

						String seq = getContents(bagList);

						if (seq != null) {
							result.put(split[1], seq);
						}
					} else {
						result.put(split[1], getTextContent(node));
					}
				}
			}
		}

		// Then check Attributes
		NamedNodeMap attrs = schema.getElement().getAttributes();
		int m = attrs.getLength();
		for (int j = 0; j < m; j++) {
			Node attr = attrs.item(j);

			String nodeName = attr.getNodeName();
			String[] split = nodeName.split(":");
			if (split.length == 2 && split[0].equals(namespaceName)) {
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
		
		for (Map.Entry<String, String> entry : result.entrySet()){
			String key = entry.getKey();
			if (preserveWhiteSpace.contains(key))
				continue;
			entry.setValue(entry.getValue().replaceAll("\\s+", " ").trim());
		}

		return result;
	}

	public static HashSet<String> preserveWhiteSpace = new HashSet<String>();
	static {
		preserveWhiteSpace.add("abstract");
		preserveWhiteSpace.add("note");
		preserveWhiteSpace.add("review");
	}

	public void setBibtexEntry(BibtexEntry entry) {
		setBibtexEntry(entry, null);
	}
	
	/**
	 * 
	 * @param entry
	 * @param database maybenull
	 */
	public void setBibtexEntry(BibtexEntry entry, BibtexDatabase database) {
		// Set all the values including key and entryType
		Set<String> fields = entry.getAllFields();
		
		JabRefPreferences prefs = JabRefPreferences.getInstance();
		if (prefs.getBoolean("useXmpPrivacyFilter")) {
			TreeSet<String> filters = new TreeSet<String>(Arrays.asList(prefs.getStringArray(JabRefPreferences.XMP_PRIVACY_FILTERS)));
			fields.removeAll(filters);
		}
		
		for (String field : fields){
			String value = BibtexDatabase.getResolvedField(field, entry, database);
			if (field.equals("author") || field.equals("editor")) {
				setPersonList(field, value);
			} else {
				setTextProperty(field, value);
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
		Map<String, String> text = getAllProperties(this, "bibtex");
		text.remove("entrytype");
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
		StringBuilder buffer = new StringBuilder();
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

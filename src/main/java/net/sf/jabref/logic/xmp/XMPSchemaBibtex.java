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
package net.sf.jabref.logic.xmp;

import java.util.*;

import javax.xml.transform.TransformerException;

import net.sf.jabref.*;

import net.sf.jabref.model.entry.*;
import net.sf.jabref.model.database.BibDatabase;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.XMPSchema;
import org.apache.xmpbox.type.AbstractField;
import org.apache.xmpbox.type.Attribute;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMPSchemaBibtex extends XMPSchema {

    /**
     * The namespace of this schema.
     */
    public static final String NAMESPACE = "http://jabref.sourceforge.net/bibteXMP/";

    private static final String KEY = "bibtex";

    private static final Set<String> PRESERVE_WHITE_SPACE = new HashSet<>();


    static {
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add("abstract");
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add("note");
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add("review");
    }

    /**
     * Create a new empty XMPSchemaBibtex as a child in the given XMPMetadata.
     *
     * @param parent
     */
    public XMPSchemaBibtex(XMPMetadata parent) {
        super(parent, XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.KEY);
    }

    public static XMPSchemaBibtex parseFromXml(Element e) throws XmpParsingException {
        DomXmpParser parser = new DomXmpParser();
        XMPMetadata meta = parser.parse(e.toString().getBytes());
        XMPSchemaBibtex schema = (XMPSchemaBibtex)meta.getSchema(XMPSchemaBibtex.class);
        return schema;
    }

    /**
     * Uses XMPSchema methods
     *
     * @param field
     * @return
     */
    public List<String> getPersonList(String field) {
        return getUnqualifiedSequenceValueList(field);
    }

    /**
     * Uses XMPSchema methods
     *
     * @param field
     * @param value
     */
    public void setPersonList(String field, String value) {
        AuthorList list = AuthorList.parse(value);

        for (Author author : list.getAuthors()) {
            addUnqualifiedSequenceValue(field, author.getFirstLast(false));
        }
    }

    private static String getContents(NodeList seqList) {

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
            seq.append(XMPSchemaBibtex.getTextContent(li));
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
     * instance: "year" => "2005". Empty map if none found.
     * @throws TransformerException
     */
    public static Map<String, String> getAllProperties(XMPSchema schema, String namespaceName) {
        List<AbstractField> nodes = schema.getAllProperties();

        Map<String, String> result = new HashMap<>();

        if (nodes == null) {
            return result;
        }

        // Check child-nodes first
        int n = nodes.size();

        for (int i = 0; i < n; i++) {
            AbstractField node = nodes.get(i);

            String nodeName = node.getPropertyName();

            String[] split = nodeName.split(":");

            if ((split.length == 2) && split[0].equals(namespaceName)) {
                NodeList seqList = ((Element) node).getElementsByTagName("rdf:Seq");
                if (seqList.getLength() > 0) {

                    String seq = XMPSchemaBibtex.getContents(seqList);

                    if (seq != null) {
                        result.put(split[1], seq);
                    }
                } else {
                    NodeList bagList = ((Element) node).getElementsByTagName("rdf:Bag");
                    if (bagList.getLength() > 0) {

                        String seq = XMPSchemaBibtex.getContents(bagList);

                        if (seq != null) {
                            result.put(split[1], seq);
                        }
                    } else {
                        result.put(split[1], ((Element) node).getTextContent());
                    }
                }
            }
        }

        // Then check Attributes
        List<Attribute> attrs = schema.getAllAttributes();
        for (Attribute attribute: attrs) {

            String nodeName = attribute.getName();
            String[] split = nodeName.split(":");
            if ((split.length == 2) && split[0].equals(namespaceName)) {
                result.put(split[1], attribute.getValue());
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

        for (Map.Entry<String, String> entry : result.entrySet()) {
            String key = entry.getKey();
            if (XMPSchemaBibtex.PRESERVE_WHITE_SPACE.contains(key)) {
                continue;
            }
            entry.setValue(entry.getValue().replaceAll("\\s+", " ").trim());
        }

        return result;
    }


    public void setBibtexEntry(BibEntry entry) {
        setBibtexEntry(entry, null);
    }

    /**
     * @param entry
     * @param database maybenull
     */
    public void setBibtexEntry(BibEntry entry, BibDatabase database) {
        // Set all the values including key and entryType
        Set<String> fields = entry.getFieldNames();

        JabRefPreferences prefs = JabRefPreferences.getInstance();
        if (prefs.getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER)) {
            Set<String> filters = new TreeSet<>(prefs.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS));
            fields.removeAll(filters);
        }

        for (String field : fields) {
            String value = BibDatabase.getResolvedField(field, entry, database);
            if (value == null) {
                value = "";
            }
            if ("author".equals(field) || "editor".equals(field)) {
                setPersonList(field, value);
            } else {
               setTextPropertyValueAsSimple(field, value);
            }
        }
        setTextPropertyValueAsSimple("entrytype", entry.getType());
    }

    public BibEntry getBibtexEntry() {
        String type = getUnqualifiedTextPropertyValue("entrytype");
        BibEntry e = new BibEntry(IdGenerator.next(), type);

        // Get Text Properties
        Map<String, String> text = XMPSchemaBibtex.getAllProperties(this, "bibtex");
        text.remove("entrytype");
        e.setField(text);
        return e;
    }

    /**
     * Taken from DOM2Utils.java:
     * <p>
     * JBoss, the OpenSource EJB server
     * <p>
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
        return hasTextContent ? buffer.toString() : "";
    }

}

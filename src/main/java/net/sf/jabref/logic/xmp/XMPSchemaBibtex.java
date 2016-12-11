package net.sf.jabref.logic.xmp;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FieldProperty;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.InternalBibtexFields;

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

    private static final String KEY = "bibtex";

    private static final Set<String> PRESERVE_WHITE_SPACE = new HashSet<>();


    static {
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add(FieldName.ABSTRACT);
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add(FieldName.NOTE);
        XMPSchemaBibtex.PRESERVE_WHITE_SPACE.add(FieldName.REVIEW);
    }

    /**
     * Create a new empty XMPSchemaBibtex as a child in the given XMPMetadata.
     *
     * @param parent
     */
    public XMPSchemaBibtex(XMPMetadata parent) {
        super(parent, XMPSchemaBibtex.KEY, XMPSchemaBibtex.NAMESPACE);
    }

    /**
     * Create schema from an existing XML element.
     *
     * @param e
     *            The existing XML element.
     * @param namespace
     *            The name space considered. Must currently be there for compatibility reasons despite being unused.
     */
    public XMPSchemaBibtex(Element e, @SuppressWarnings("unused") String namespace) {
        super(e, XMPSchemaBibtex.KEY);
    }

    private static String makeProperty(String propertyName) {
        return XMPSchemaBibtex.KEY + ':' + propertyName;
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
        AuthorList list = AuthorList.parse(value);

        for (Author author : list.getAuthors()) {
            addSequenceValue(field, author.getFirstLast(false));
        }
    }

    @Override
    public String getTextProperty(String field) {
        return super.getTextProperty(makeProperty(field));
    }

    @Override
    public void setTextProperty(String field, String value) {
        super.setTextProperty(makeProperty(field), value);
    }

    @Override
    public List<String> getBagList(String bagName) {
        return super.getBagList(makeProperty(bagName));
    }

    @Override
    public void removeBagValue(String bagName, String value) {
        super.removeBagValue(makeProperty(bagName), value);
    }

    @Override
    public void addBagValue(String bagName, String value) {
        super.addBagValue(makeProperty(bagName), value);
    }

    @Override
    public List<String> getSequenceList(String seqName) {
        return super.getSequenceList(makeProperty(seqName));
    }

    @Override
    public void removeSequenceValue(String seqName, String value) {
        super.removeSequenceValue(makeProperty(seqName), value);
    }

    @Override
    public void addSequenceValue(String seqName, String value) {
        super.addSequenceValue(makeProperty(seqName), value);
    }

    @Override
    public List<Calendar> getSequenceDateList(String seqName) throws IOException {
        return super.getSequenceDateList(makeProperty(seqName));
    }

    @Override
    public void removeSequenceDateValue(String seqName, Calendar date) {
        super.removeSequenceDateValue(makeProperty(seqName), date);
    }

    @Override
    public void addSequenceDateValue(String field, Calendar date) {
        super.addSequenceDateValue(makeProperty(field), date);
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
     *         instance: "year" => "2005". Empty map if none found.
     * @throws TransformerException
     */
    public static Map<String, String> getAllProperties(XMPSchema schema, String namespaceName) {
        NodeList nodes = schema.getElement().getChildNodes();

        Map<String, String> result = new HashMap<>();

        if (nodes == null) {
            return result;
        }

        // Check child-nodes first
        int n = nodes.getLength();

        for (int i = 0; i < n; i++) {
            Node node = nodes.item(i);
            if ((node.getNodeType() != Node.ATTRIBUTE_NODE)
                    && (node.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            String nodeName = node.getNodeName();

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
                        result.put(split[1], XMPSchemaBibtex.getTextContent(node));
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
            if ((split.length == 2) && split[0].equals(namespaceName)) {
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

        for (Map.Entry<String, String> entry : result.entrySet()) {
            String key = entry.getKey();
            if (XMPSchemaBibtex.PRESERVE_WHITE_SPACE.contains(key)) {
                continue;
            }
            entry.setValue(entry.getValue().replaceAll("\\s+", " ").trim());
        }

        return result;
    }



    public void setBibtexEntry(BibEntry entry, XMPPreferences xmpPreferences) {
        setBibtexEntry(entry, null, xmpPreferences);
    }

    /**
     *
     * @param entry
     * @param database maybenull
     */
    public void setBibtexEntry(BibEntry entry, BibDatabase database, XMPPreferences xmpPreferences) {
        // Set all the values including key and entryType
        Set<String> fields = entry.getFieldNames();

        if ((xmpPreferences != null) && xmpPreferences.isUseXMPPrivacyFilter()) {
            Set<String> filters = new TreeSet<>(xmpPreferences.getXmpPrivacyFilter());
            fields.removeAll(filters);
        }

        for (String field : fields) {
            String value = entry.getResolvedFieldOrAlias(field, database).orElse("");
            if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
                setPersonList(field, value);
            } else {
                setTextProperty(field, value);
            }
        }
        setTextProperty(BibEntry.TYPE_HEADER, entry.getType());
    }

    public BibEntry getBibtexEntry() {
        String type = getTextProperty(BibEntry.TYPE_HEADER);
        BibEntry e = new BibEntry(IdGenerator.next(), type);

        // Get Text Properties
        Map<String, String> text = XMPSchemaBibtex.getAllProperties(this, "bibtex");
        text.remove(BibEntry.TYPE_HEADER);
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
        return hasTextContent ? buffer.toString() : "";
    }

}

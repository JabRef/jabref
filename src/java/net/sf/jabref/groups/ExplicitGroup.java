package net.sf.jabref.groups;

import java.util.*;

import net.sf.jabref.*;

/**
 * @author zieren
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExplicitGroup extends AbstractGroup implements SearchRule {
	public static final String ID = "ExplicitGroup:";
	private Set m_entries;
	private String m_name;
	
	public ExplicitGroup(String name) {
	    m_name = name;
	    m_entries = new HashSet();
	}
	
	public static AbstractGroup fromString(String s) throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: ExplicitGroup cannot be created from \""
                            + s + "\"");
	    QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
                .length()), SEPARATOR, QUOTE_CHAR);
	    ExplicitGroup newGroup = new ExplicitGroup(tok.nextToken());
	    while (tok.hasMoreTokens())
	        newGroup.m_entries.add(Util.unquote(tok.nextToken(),QUOTE_CHAR));
	    return newGroup;
	}
	
    public SearchRule getSearchRule() {
        return this;
    }

    public String getName() {
        return m_name;
    }

    public boolean supportsAdd() {
        return true;
    }

    public boolean supportsRemove() {
        return true;
    }

    public void addSelection(BasePanel basePanel) {
        BibtexEntry[] bes = basePanel.getSelectedEntries();
        Object bibtexkey;
        for (int i = 0; i < bes.length; ++i) {
            bibtexkey= bes[i].getField("bibtexkey");
            if (bibtexkey != null) // JZTODO : report if null
                m_entries.add(bibtexkey);
        }        
        // JZTODO undo information
    }

    public void removeSelection(BasePanel basePanel) {
        BibtexEntry[] bes = basePanel.getSelectedEntries();
        Object bibtexkey;
        for (int i = 0; i < bes.length; ++i) {
            bibtexkey= bes[i].getField("bibtexkey");
            if (bibtexkey != null) // JZTODO : report if null
                m_entries.remove(bibtexkey);
        }        
        // JZTODO undo information
    }

    public int contains(Map searchOptions, BibtexEntry entry) {
        return m_entries.contains(entry.getField("bibtexkey")) ? 1 : 0;
    }

    public int applyRule(Map searchStrings, BibtexEntry bibtexEntry) {
        return contains(searchStrings, bibtexEntry);
    }

    public AbstractGroup deepCopy() {
        ExplicitGroup copy = new ExplicitGroup(m_name);
        copy.m_entries.addAll(m_entries);
        return copy;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ExplicitGroup))
            return false;
        ExplicitGroup other = (ExplicitGroup)o;
        return other.m_name.equals(m_name) && other.m_entries.equals(m_entries);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
		sb.append(ID + Util.quote(m_name, SEPARATOR, QUOTE_CHAR) + SEPARATOR);
		for (Iterator it = m_entries.iterator(); it.hasNext();) {
            sb.append(Util.quote((String) it.next(), SEPARATOR, QUOTE_CHAR)
                    + SEPARATOR);
        }
		return sb.toString();
    }
}

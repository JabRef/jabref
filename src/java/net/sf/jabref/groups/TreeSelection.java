/*
 * Created on 10.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref.groups;

import java.util.*;
import java.util.HashSet;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.SearchRule;

/**
 * @author zieren
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TreeSelection implements SearchRule {
	private HashSet m_selection = new HashSet();
	public TreeSelection(BibtexEntry[] selection) {
		for (int i = 0; i < selection.length; ++i)
			m_selection.add(selection[i]);
	}

	public int applyRule(Map searchStrings, BibtexEntry bibtexEntry) {
		return m_selection.contains(bibtexEntry) ? 1 : 0;
	}

}

/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.SearchRule;

/**
 * A group of BibtexEntries.
 */
public abstract class AbstractGroup {

    /** The group's name (every type of group has one). */
	protected String m_name;

	/**
	 * The hierarchical context of the group (INDEPENDENT, REFINING, or
	 * INCLUDING). Defaults to INDEPENDENT, which will be used if and
	 * only if the context specified in the constructor is invalid.
	 */
	protected int m_context = INDEPENDENT;

    public abstract String getTypeId();

    public AbstractGroup(String name, int context) {
		m_name = name;
		setHierarchicalContext(context);
	}

	/** Group's contents are independent of its hierarchical position. */
	public static final int INDEPENDENT = 0;
	/**
	 * Group's content is the intersection of its own content with its
	 * supergroup's content.
	 */
	public static final int REFINING = 1;
	/**
	 * Group's content is the union of its own content with its subgroups'
	 * content.
	 */
	public static final int INCLUDING = 2;

	/** Character used for quoting in the string representation. */
	protected static final char QUOTE_CHAR = '\\';

	/**
	 * For separating units (e.g. name, which every group has) in the string
	 * representation
	 */
	protected static final String SEPARATOR = ";";

	/**
	 * @return A search rule that will identify this group's entries.
	 */
	public abstract SearchRule getSearchRule();

	/**
	 * Re-create a group instance from a textual representation.
	 * 
	 * @param s
	 *            The result from the group's toString() method.
	 * @return New instance of the encoded group.
	 * @throws Exception
	 *             If an error occured and a group could not be created, e.g.
	 *             due to a malformed regular expression.
	 */
	public static AbstractGroup fromString(String s, BibtexDatabase db,
			int version) throws Exception {
		if (s.startsWith(KeywordGroup.ID))
			return KeywordGroup.fromString(s, db, version);
		if (s.startsWith(AllEntriesGroup.ID))
			return AllEntriesGroup.fromString(s, db, version);
		if (s.startsWith(SearchGroup.ID))
			return SearchGroup.fromString(s, db, version);
		if (s.startsWith(ExplicitGroup.ID))
			return ExplicitGroup.fromString(s, db, version);
		return null; // unknown group
	}

	/** Returns this group's name, e.g. for display in a list/tree. */
	public final String getName() {
		return m_name;
	}

	/** Sets the group's name. */
	public final void setName(String name) {
		m_name = name;
	}

	/**
	 * @return true if this type of group supports the explicit adding of
	 *         entries.
	 */
	public abstract boolean supportsAdd();

	/**
	 * @return true if this type of group supports the explicit removal of
	 *         entries.
	 */
	public abstract boolean supportsRemove();

	/**
	 * Adds the specified entries to this group.
	 * 
	 * @return If this group or one or more entries was/were modified as a
	 *         result of this operation, an object is returned that allows to
	 *         undo this change. null is returned otherwise.
	 */
	public abstract AbstractUndoableEdit add(BibtexEntry[] entries);

	/**
	 * Removes the specified entries from this group.
	 * 
	 * @return If this group or one or more entries was/were modified as a
	 *         result of this operation, an object is returned that allows to
	 *         undo this change. null is returned otherwise.
	 */
	public abstract AbstractUndoableEdit remove(BibtexEntry[] entries);

	/**
	 * @param searchOptions
	 *            The search options to apply.
	 * @return true if this group contains the specified entry, false otherwise.
	 */
	public abstract boolean contains(Map<String, String> searchOptions, BibtexEntry entry);

	/**
	 * @return true if this group contains the specified entry, false otherwise.
	 */
	public abstract boolean contains(BibtexEntry entry);

	/**
	 * @return true if this group contains any of the specified entries, false
	 *         otherwise.
	 */
	public boolean containsAny(BibtexEntry[] entries) {
		for (int i = 0; i < entries.length; ++i)
			if (contains(entries[i]))
				return true;
		return false;
	}

	/**
	 * @return true if this group contains all of the specified entries, false
	 *         otherwise.
	 */
	public boolean containsAll(BibtexEntry[] entries) {
		for (int i = 0; i < entries.length; ++i)
			if (!contains(entries[i]))
				return false;
		return true;
	}

	/**
	 * Returns true if this group is dynamic, i.e. uses a search definition or
	 * equiv. that might match new entries, or false if this group contains a
	 * fixed set of entries and thus will never match a new entry that was not
	 * explicitly added to it.
	 */
	public abstract boolean isDynamic();

	/** Sets the groups's hierarchical context. If context is not a valid
	 * value, the call is ignored. */
	public void setHierarchicalContext(int context) {
		if (context != INDEPENDENT && context != REFINING
				&& context != INCLUDING)
			return;
		m_context = context;
	}
	
	/** Returns the group's hierarchical context. */
	public int getHierarchicalContext() {
		return m_context;
	}
	
	/** Returns a lengthy textual description of this instance (for 
     * the groups editor). The text is formatted in HTML. */
	public abstract String getDescription();

	/**
	 * @return A deep copy of this object.
	 */
	public abstract AbstractGroup deepCopy();

	/** Returns a short description of the group in HTML (for a tooltip). */
	public abstract String getShortDescription();

	// by general AbstractGroup contract, toString() must return
	// something from which this object can be reconstructed
	// using fromString(String).

	// by general AbstractGroup contract, equals() must be implemented
        
        /**
         * Update the group, if necessary, to handle the situation where the group
         * is applied to a different BibtexDatabase than it was created for. This
         * is for instance used when updating the group tree due to an external change.
         *
         * @param db The database to refresh for.
         */
        public void refreshForNewDatabase(BibtexDatabase db) {
            // Default is to do nothing. Group types that are affected by a change
            // of database must override this method.
        }
}

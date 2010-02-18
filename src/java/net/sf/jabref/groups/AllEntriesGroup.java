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
import net.sf.jabref.Globals;
import net.sf.jabref.SearchRule;

/**
 * This group contains all entries.
 */
public class AllEntriesGroup extends AbstractGroup implements SearchRule {
    public static final String ID = "AllEntriesGroup:";

    public AllEntriesGroup() {
        super(Globals.lang("All Entries"), AbstractGroup.INDEPENDENT);
    }
    
    public static AbstractGroup fromString(String s, BibtexDatabase db, int version) throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: AllEntriesGroup cannot be created from \""
                            + s + "\". "
                            + "Please report this on www.sf.net/projects/jabref");
        switch (version) {
        case 0:
        case 1:
        case 2:
        case 3:
            return new AllEntriesGroup();
        default:
            throw new UnsupportedVersionException("AllEntriesGroup", version); 
        }
    }

    public SearchRule getSearchRule() {
        return this;
    }

    public boolean supportsAdd() {
        return false;
    }

    public boolean supportsRemove() {
        return false;
    }

    public AbstractUndoableEdit add(BibtexEntry[] entries) {
        // not supported -> ignore
        return null;
    }

    public AbstractUndoableEdit remove(BibtexEntry[] entries) {
        // not supported -> ignore
        return null;
    }

    public boolean contains(Map<String, String> searchOptions, BibtexEntry entry) {
        return true; // contains everything
    }

    public AbstractGroup deepCopy() {
        return new AllEntriesGroup();
    }

    public int applyRule(Map<String, String> searchStrings, BibtexEntry bibtexEntry) {
        return 1; // contains everything
    }

    public boolean equals(Object o) {
        return o instanceof AllEntriesGroup;
    }

    public String toString() {
        return ID;
    }

    public boolean contains(BibtexEntry entry) {
        return true;
    }
    
    public boolean isDynamic() {
    	// this is actually a special case; I define it as non-dynamic
    	return false;
    }

	public String getDescription() {
		return "This group contains all entries. It cannot be edited or removed.";
		// JZTODO lyrics
	}
	
	public String getShortDescription() {
		return Globals.lang("<b>All Entries</b> (this group cannot be edited or removed)");
	}

    public String getTypeId() {
        return ID;
    }
}

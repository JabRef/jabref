/*
Copyright (C) 2003 Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.util.*;

public class EntrySorter {

    TreeSet set;
    String[] idArray;
    BibtexEntry[] entryArray;

    public EntrySorter(Map entries, Comparator comp) {
	set = new TreeSet(comp);
	Set keySet = entries.keySet();
	if (keySet != null) {
	    Iterator i = keySet.iterator();
	    while (i.hasNext()) {
		set.add(entries.get(i.next()));
	    }
	    index();
	}
    }

    public void index() {
	Object[] array = set.toArray();
        // Create aan array of IDs for quick access, since getIdAt() is called by
        // getValueAt() in EntryTableModel, which *has* to be efficient.
        idArray = new String[array.length];
        entryArray = new BibtexEntry[array.length];
        for (int i=0; i<idArray.length; i++) {
          idArray[i] = ( (BibtexEntry) (array[i])).getId();
          entryArray[i] = (BibtexEntry)array[i];
        }
    }

    public String getIdAt(int pos) {
      return idArray[pos];
	//return ((BibtexEntry)(entryArray[pos])).getId();
    }

    public BibtexEntry getEntryAt(int pos) {
      return entryArray[pos];
    }

    public int getEntryCount() {
	if (entryArray != null)
	    return entryArray.length;
	else
	    return 0;
    }
}

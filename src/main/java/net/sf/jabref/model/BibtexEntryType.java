/*
Copyright (C) 2003 David Weitzman, Morten O. Alver

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

Note:
Modified for use in JabRef.

*/
package net.sf.jabref.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.util.Util;

/**
 * Provides a list of known entry types
 * <p/>
 * The list of optional and required fields is derived from http://en.wikipedia.org/wiki/BibTeX#Entry_types
 */
public abstract class BibtexEntryType implements Comparable<BibtexEntryType> {

     public abstract String getName();

    @Override
    public int compareTo(BibtexEntryType o) {
        return getName().compareTo(o.getName());
    }

    public abstract String[] getOptionalFields();

    public abstract String[] getRequiredFields();

    public String[] getPrimaryOptionalFields() {
        return getOptionalFields();
    }
    
    public String[] getSecondaryOptionalFields() {
    	return Util.getRemainder(getOptionalFields(), getPrimaryOptionalFields());
    }

    public abstract String describeRequiredFields();

    public abstract boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database);

    public String[] getUtilityFields() {
        return new String[] {"search"};
    }

    public boolean isRequired(String field) {
        String[] req = getRequiredFields();
        if (req == null) {
            return false;
        }
        for (String aReq : req) {
            if (aReq.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOptional(String field) {
        String[] opt = getOptionalFields();
        if (opt == null) {
            return false;
        }
        for (String anOpt : opt) {
            if (anOpt.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisibleAtNewEntryDialog() {
        return true;
    }

    private static final TreeMap<String, BibtexEntryType> ALL_TYPES = new TreeMap<String, BibtexEntryType>();
    private static final TreeMap<String, BibtexEntryType> STANDARD_TYPES;

    static {
			// Put the standard entry types into the type map.
			if (!Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE)) {
				ALL_TYPES.put("article", BibtexEntryTypes.ARTICLE);
				ALL_TYPES.put("inbook", BibtexEntryTypes.INBOOK);
				ALL_TYPES.put("book", BibtexEntryTypes.BOOK);
				ALL_TYPES.put("booklet", BibtexEntryTypes.BOOKLET);
				ALL_TYPES.put("incollection", BibtexEntryTypes.INCOLLECTION);
				ALL_TYPES.put("conference", BibtexEntryTypes.CONFERENCE);
				ALL_TYPES.put("inproceedings", BibtexEntryTypes.INPROCEEDINGS);
				ALL_TYPES.put("proceedings", BibtexEntryTypes.PROCEEDINGS);
				ALL_TYPES.put("manual", BibtexEntryTypes.MANUAL);
				ALL_TYPES.put("mastersthesis", BibtexEntryTypes.MASTERSTHESIS);
				ALL_TYPES.put("phdthesis", BibtexEntryTypes.PHDTHESIS);
				ALL_TYPES.put("techreport", BibtexEntryTypes.TECHREPORT);
				ALL_TYPES.put("unpublished", BibtexEntryTypes.UNPUBLISHED);
				ALL_TYPES.put("patent", BibtexEntryTypes.PATENT);
				ALL_TYPES.put("standard", BibtexEntryTypes.STANDARD);
				ALL_TYPES.put("electronic", BibtexEntryTypes.ELECTRONIC);
				ALL_TYPES.put("periodical", BibtexEntryTypes.PERIODICAL);
				ALL_TYPES.put("misc", BibtexEntryTypes.MISC);
				ALL_TYPES.put("other", BibtexEntryTypes.OTHER);
				ALL_TYPES.put("ieeetranbstctl", BibtexEntryTypes.IEEETRANBSTCTL);
			} else {
				ALL_TYPES.put("article", BibLatexEntryTypes.ARTICLE);
				ALL_TYPES.put("book", BibLatexEntryTypes.BOOK);
				ALL_TYPES.put("inbook", BibLatexEntryTypes.INBOOK);
				ALL_TYPES.put("bookinbook", BibLatexEntryTypes.BOOKINBOOK);
				ALL_TYPES.put("suppbook", BibLatexEntryTypes.SUPPBOOK);
				ALL_TYPES.put("booklet", BibLatexEntryTypes.BOOKLET);
				ALL_TYPES.put("collection", BibLatexEntryTypes.COLLECTION);
				ALL_TYPES.put("incollection", BibLatexEntryTypes.INCOLLECTION);
				ALL_TYPES.put("suppcollection", BibLatexEntryTypes.SUPPCOLLECTION);
				ALL_TYPES.put("manual", BibLatexEntryTypes.MANUAL);
				ALL_TYPES.put("misc", BibLatexEntryTypes.MISC);
				ALL_TYPES.put("online", BibLatexEntryTypes.ONLINE);
				ALL_TYPES.put("patent", BibLatexEntryTypes.PATENT);
				ALL_TYPES.put("periodical", BibLatexEntryTypes.PERIODICAL);
				ALL_TYPES.put("suppperiodical",	BibLatexEntryTypes.SUPPPERIODICAL);
				ALL_TYPES.put("proceedings", BibLatexEntryTypes.PROCEEDINGS);
				ALL_TYPES.put("inproceedings", BibLatexEntryTypes.INPROCEEDINGS);
				ALL_TYPES.put("reference", BibLatexEntryTypes.REFERENCE);
				ALL_TYPES.put("inreference", BibLatexEntryTypes.INREFERENCE);
				ALL_TYPES.put("report", BibLatexEntryTypes.REPORT);
				ALL_TYPES.put("set", BibLatexEntryTypes.SET);
				ALL_TYPES.put("thesis", BibLatexEntryTypes.THESIS);
				ALL_TYPES.put("unpublished", BibLatexEntryTypes.UNPUBLISHED);
				ALL_TYPES.put("conference", BibLatexEntryTypes.CONFERENCE);
				ALL_TYPES.put("electronic", BibLatexEntryTypes.ELECTRONIC);
				ALL_TYPES.put("mastersthesis", BibLatexEntryTypes.MASTERSTHESIS);
				ALL_TYPES.put("phdthesis", BibLatexEntryTypes.PHDTHESIS);
				ALL_TYPES.put("techreport", BibLatexEntryTypes.TECHREPORT);
				ALL_TYPES.put("www", BibLatexEntryTypes.WWW);
				ALL_TYPES.put("ieeetranbstctl", BibLatexEntryTypes.IEEETRANBSTCTL);
			}
		// We need a record of the standard types, in case the user wants
		// to remove a customized version. Therefore we clone the map.
		STANDARD_TYPES = new TreeMap<String, BibtexEntryType>(ALL_TYPES);	
    }

    /**
     * This method returns the BibtexEntryType for the name of a type,
     * or null if it does not exist.
     */
    public static BibtexEntryType getType(String name) {
        //Util.pr("'"+name+"'");
        Object o = ALL_TYPES.get(name.toLowerCase(Locale.US));
        if (o == null) {
            return null;
        } else {
            return (BibtexEntryType) o;
        }
    }

    /**
     * This method returns the standard BibtexEntryType for the
     * name of a type, or null if it does not exist.
     */
    public static BibtexEntryType getStandardType(String name) {
        //Util.pr("'"+name+"'");
        Object o = STANDARD_TYPES.get(name.toLowerCase());
        if (o == null) {
            return null;
        } else {
            return (BibtexEntryType) o;
        }
    }
    
    public static void addOrModifyCustomEntryType(CustomEntryType type) {
    	ALL_TYPES.put(type.getName().toLowerCase(Locale.US), type);
    }
    
    public static Set<String> getAllTypes() {
    	return ALL_TYPES.keySet();
    }
    
    public static Collection<BibtexEntryType> getAllValues() {
    	return ALL_TYPES.values();
    }

    /**
     * Removes a customized entry type from the type map. If this type
     * overrode a standard type, we reinstate the standard one.
     *
     * @param name The customized entry type to remove.
     */
	public static void removeType(String name) {
		// BibtexEntryType type = getType(name);
		String nm = name.toLowerCase();
		// System.out.println(ALL_TYPES.size());
		ALL_TYPES.remove(nm);
		// System.out.println(ALL_TYPES.size());
		if (STANDARD_TYPES.get(nm) != null) {
			// In this case the user has removed a customized version
			// of a standard type. We reinstate the standard type.
			addOrModifyCustomEntryType((CustomEntryType) STANDARD_TYPES.get(nm));
		}
	}

    /**
     * Load all custom entry types from preferences. This method is
     * called from JabRef when the program starts.
     */
    public static void loadCustomEntryTypes(JabRefPreferences prefs) {
        int number = 0;
        CustomEntryType type;
        while ((type = prefs.getCustomEntryType(number)) != null) {
            addOrModifyCustomEntryType(type);
            number++;
        }
    }

    /**
     * Iterate through all entry types, and store those that are
     * custom defined to preferences. This method is called from
     * JabRefFrame when the program closes.
     */
    public static void saveCustomEntryTypes(JabRefPreferences prefs) {
        Iterator<String> i = ALL_TYPES.keySet().iterator();
        int number = 0;
			//Vector customTypes = new Vector(10, 10);
			while (i.hasNext()) {
            Object o = ALL_TYPES.get(i.next());
				if (o instanceof CustomEntryType) {
					// Store this entry type.
					prefs.storeCustomEntryType((CustomEntryType) o, number);
					number++;
				}
			}
		// Then, if there are more 'old' custom types defined, remove these
        // from preferences. This is necessary if the number of custom types
        // has decreased.
        prefs.purgeCustomEntryTypes(number);
    }

    /**
     * Get an array of the required fields in a form appropriate for the entry customization
     * dialog - that is, the either-or fields together and separated by slashes.
     *
     * @return Array of the required fields in a form appropriate for the entry customization dialog.
     */
    public String[] getRequiredFieldsForCustomization() {
        return getRequiredFields();
    }
}

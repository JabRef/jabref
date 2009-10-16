/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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
package net.sf.jabref.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.BibtexString;
import net.sf.jabref.BibtexStringComparator;
import net.sf.jabref.CrossRefEntryComparator;
import net.sf.jabref.CustomEntryType;
import net.sf.jabref.FieldComparator;
import net.sf.jabref.FieldComparatorStack;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.IdComparator;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.SortedList;

public class FileActions
{

    private static Pattern refPat = Pattern.compile("(#[A-Za-z]+#)"); // Used to detect string references in strings


    private static void writePreamble(Writer fw, String preamble) throws IOException {
    if (preamble != null) {
        fw.write("@PREAMBLE{");
        fw.write(preamble);
        fw.write("}"+Globals.NEWLINE +Globals.NEWLINE);
    }
    }

    /**
     * Write all strings in alphabetical order, modified to produce a safe (for BibTeX) order of the strings
     * if they reference each other.
     * @param fw The Writer to send the output to.
     * @param database The database whose strings we should write.
     * @throws IOException If anthing goes wrong in writing.
     */
    private static void writeStrings(Writer fw, BibtexDatabase database) throws IOException {
        List<BibtexString> strings = new ArrayList<BibtexString>();
        for (String s : database.getStringKeySet()) {
            strings.add(database.getString(s));
        }
        Collections.sort(strings, new BibtexStringComparator(false));
        // First, make a Map of all entries:
        HashMap<String, BibtexString> remaining = new HashMap<String, BibtexString>();
        for (Iterator<BibtexString> i=strings.iterator(); i.hasNext();) {
            BibtexString string = i.next();
            remaining.put(string.getName(), string);
        }
        for (Iterator<BibtexString> i = strings.iterator(); i.hasNext();) {
            BibtexString bs = i.next();
            if (remaining.containsKey(bs.getName()))
                writeString(fw, bs, remaining);
        }
    }

    private static void writeString(Writer fw, BibtexString bs, HashMap<String, BibtexString> remaining) throws IOException {
        // First remove this from the "remaining" list so it can't cause problem with circular refs:
        remaining.remove(bs.getName());

        // Then we go through the string looking for references to other strings. If we find references
        // to strings that we will write, but still haven't, we write those before proceeding. This ensures
        // that the string order will be acceptable for BibTeX.
        String content = bs.getContent();
        Matcher m;
        while ((m = refPat.matcher(content)).find()) {
            String foundLabel = m.group(1);
            int restIndex = content.indexOf(foundLabel)+foundLabel.length();
            content = content.substring(restIndex);
            Object referred = remaining.get(foundLabel.substring(1, foundLabel.length()-1));
            // If the label we found exists as a key in the "remaining" Map, we go on and write it now:
            if (referred != null)
                writeString(fw, (BibtexString)referred, remaining);
        }

        fw.write("@STRING{" + bs.getName() + " = ");
        if (!bs.getContent().equals("")) {
            try {
                String formatted = (new LatexFieldFormatter()).format(bs.getContent(), Globals.BIBTEX_STRING);
                fw.write(formatted);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        Globals.lang("The # character is not allowed in BibTeX strings unless escaped as in '\\#'.") + "\n" +
                        Globals.lang("Before saving, please edit any strings containing the # character."));
            }

        }
        else
            fw.write("{}");

        fw.write("}" + Globals.NEWLINE + Globals.NEWLINE);
    }

    /**
     * Writes the JabRef signature and the encoding.
     *
     * @param encoding String the name of the encoding, which is part of the header.
     */
    private static void writeBibFileHeader(Writer out, String encoding) throws IOException {
        out.write("% ");
      out.write(GUIGlobals.SIGNATURE);
      out.write(" "+GUIGlobals.version+"."+Globals.NEWLINE + "% " + 
              GUIGlobals.encPrefix+encoding+Globals.NEWLINE +Globals.NEWLINE);
    }

    /**
     * Saves the database to file. Two boolean values indicate whether
     * only entries with a nonzero Globals.SEARCH value and only
     * entries with a nonzero Globals.GROUPSEARCH value should be
     * saved. This can be used to let the user save only the results of
     * a search. False and false means all entries are saved.
     */
    public static SaveSession saveDatabase(BibtexDatabase database,
		MetaData metaData, File file, JabRefPreferences prefs,
		boolean checkSearch, boolean checkGroup, String encoding, boolean suppressBackup)
		throws SaveException {
    	
		TreeMap<String, BibtexEntryType> types = new TreeMap<String, BibtexEntryType>();
		
		boolean backup = prefs.getBoolean("backup");
        if (suppressBackup)
            backup = false;

		SaveSession session;
		BibtexEntry exceptionCause = null;
		try {
			session = new SaveSession(file, encoding, backup);
		} catch (Throwable e) {
			System.err.println("Error from encoding: '" + encoding + "' Len: "
				+ encoding.length());
			// we must catch all exceptions to be able notify users that
			// saving failed, no matter what the reason was
			// (and they won't just quit JabRef thinking
			// everyting worked and loosing data)
			e.printStackTrace();
			throw new SaveException(e.getMessage());
		}

		try {

			// Get our data stream. This stream writes only to a temporary file,
			// until committed.
			VerifyingWriter fw = session.getWriter();

			// Write signature.
			writeBibFileHeader(fw, encoding);

			// Write preamble if there is one.
			writePreamble(fw, database.getPreamble());

			// Write strings if there are any.
			writeStrings(fw, database);

			// Write database entries. Take care, using CrossRefEntry-
			// Comparator, that referred entries occur after referring
			// ones. Apart from crossref requirements, entries will be
			// sorted as they appear on the screen.
			List<BibtexEntry> sorter = getSortedEntries(database, null, true);

			FieldFormatter ff = new LatexFieldFormatter();

			for (BibtexEntry be : sorter) {
				exceptionCause = be;

				// Check if we must write the type definition for this
				// entry, as well. Our criterion is that all non-standard
				// types (*not* customized standard types) must be written.
				BibtexEntryType tp = be.getType();

				if (BibtexEntryType.getStandardType(tp.getName()) == null) {
					types.put(tp.getName(), tp);
				}

				// Check if the entry should be written.
				boolean write = true;

				if (checkSearch && !nonZeroField(be, BibtexFields.SEARCH)) {
					write = false;
				}

				if (checkGroup && !nonZeroField(be, BibtexFields.GROUPSEARCH)) {
					write = false;
				}

				if (write) {
					be.write(fw, ff, true);
					fw.write(Globals.NEWLINE);
				}
			}

			// Write meta data.
			if (metaData != null) {
				metaData.writeMetaData(fw);
			}

			// Write type definitions, if any:
			if (types.size() > 0) {
				for (Iterator<String> i = types.keySet().iterator(); i
					.hasNext();) {
                    BibtexEntryType type = types.get(i.next());
                    if (type instanceof CustomEntryType) {
                        CustomEntryType tp = (CustomEntryType)type;
                        tp.save(fw);
                        fw.write(Globals.NEWLINE);
                    }
				}

			}

			fw.close();
		} catch (Throwable ex) {
			ex.printStackTrace();
			try {
				session.cancel();
				// repairAfterError(file, backup, INIT_OK);
			} catch (IOException e) {
				// Argh, another error? Can we do anything?
				e.printStackTrace();
                throw new SaveException(ex.getMessage()+"\n"+
                        Globals.lang("Warning: could not complete file repair; your file may "
                        +"have been corrupted. Error message")+": "+e.getMessage());

			}
			throw new SaveException(ex.getMessage(), exceptionCause);
		}

		return session;


	}

    /**
	 * Saves the database to file, including only the entries included in the
	 * supplied input array bes.
	 * 
	 * @return A List containing warnings, if any.
	 */
    @SuppressWarnings("unchecked")
	public static SaveSession savePartOfDatabase(BibtexDatabase database, MetaData metaData,
                                                 File file, JabRefPreferences prefs, BibtexEntry[] bes, String encoding) throws SaveException
    {

    TreeMap<String, BibtexEntryType> types = new TreeMap<String, BibtexEntryType>(); // Map
																						// to
																						// collect
																						// entry
																						// type
																						// definitions
    // that we must save along with entries using them.

        BibtexEntry be = null;
        boolean backup = prefs.getBoolean("backup");

        SaveSession session;
        try {
            session = new SaveSession(file, encoding, backup);
        } catch (IOException e) {
            throw new SaveException(e.getMessage());
        }

        try
        {

            // Define our data stream.
            VerifyingWriter fw = session.getWriter();

            // Write signature.
            writeBibFileHeader(fw, encoding);

            // Write preamble if there is one.
            writePreamble(fw, database.getPreamble());

            // Write strings if there are any.
        writeStrings(fw, database);

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
        String pri, sec, ter;

        boolean priD, secD, terD;
        if (!prefs.getBoolean("saveInStandardOrder")) {
        // The setting is to save according to the current table order.
        pri = prefs.get("priSort");
        sec = prefs.get("secSort");
        // sorted as they appear on the screen.
        ter = prefs.get("terSort");
        priD = prefs.getBoolean("priDescending");
        secD = prefs.getBoolean("secDescending");
        terD = prefs.getBoolean("terDescending");
        } else {
        // The setting is to save in standard order: author, editor, year
        pri = "author";
        sec = "editor";
        ter = "year";
        priD = false;
        secD = false;
        terD = true;
        }

        List<Comparator<BibtexEntry>> comparators = new ArrayList<Comparator<BibtexEntry>>();
        comparators.add(new CrossRefEntryComparator());
        comparators.add(new FieldComparator(pri, priD));
        comparators.add(new FieldComparator(sec, secD));
        comparators.add(new FieldComparator(ter, terD));
        comparators.add(new FieldComparator(BibtexFields.KEY_FIELD));
        // Use glazed lists to get a sorted view of the entries:
        BasicEventList entryList = new BasicEventList();
        SortedList sorter = new SortedList(entryList, new FieldComparatorStack<BibtexEntry>(comparators));

        if ((bes != null) && (bes.length > 0))
        for (int i=0; i<bes.length; i++) {
            sorter.add(bes[i]);
        }

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator<BibtexEntry> i = sorter.iterator(); i.hasNext();)
            {
                be = (i.next());

        // Check if we must write the type definition for this
        // entry, as well. Our criterion is that all non-standard
        // types (*not* customized standard types) must be written.
        BibtexEntryType tp = be.getType();
        if (BibtexEntryType.getStandardType(tp.getName()) == null) {
            types.put(tp.getName(), tp);
        }

        be.write(fw, ff, true);
        fw.write(Globals.NEWLINE);
        }

            // Write meta data.
            if (metaData != null)
            {
                metaData.writeMetaData(fw);
            }

        // Write type definitions, if any:
        if (types.size() > 0) {
        for (Iterator<String> i=types.keySet().iterator(); i.hasNext();) {
            CustomEntryType tp = (CustomEntryType)types.get(i.next());
            tp.save(fw);
            fw.write(Globals.NEWLINE);
        }

        }

            fw.close();
        }
         catch (Throwable ex)
        {
            try {
                session.cancel();
                //repairAfterError(file, backup, status);
            } catch (IOException e) {
                // Argh, another error? Can we do anything?
                e.printStackTrace();
                throw new SaveException(ex.getMessage()+"\n"+
                        Globals.lang("Warning: could not complete file repair; your file may "
                        +"have been corrupted. Error message: ")+e.getMessage());
            }
            throw new SaveException(ex.getMessage(), be);
    }

        return session;

    }


    /**
     * This method attempts to get a Reader for the file path given, either by
     * loading it as a resource (from within jar), or as a normal file.
     * If unsuccessful (e.g. file not found), an IOException is thrown.
     */
    public static Reader getReader(String name) throws IOException {
      Reader reader = null;
      // Try loading as a resource first. This works for files inside the jar:
      URL reso = Globals.class.getResource(name);

      // If that didn't work, try loading as a normal file URL:
      if (reso != null) {
        try {
          reader = new InputStreamReader(reso.openStream());
        } catch (FileNotFoundException ex) {
          throw new IOException(Globals.lang("Could not find layout file")+": '"+name+"'.");
        }
      } else {
        File f = new File(name);
        try {
          reader = new FileReader(f);
        } catch (FileNotFoundException ex) {
          throw new IOException(Globals.lang("Could not find layout file")+": '"+name+"'.");
        }
      }

      return reader;
    }

    /*
    * We have begun to use getSortedEntries() for both database save operations
    * and non-database save operations.  In a non-database save operation
    * (such as the exportDatabase call), we do not wish to use the
    * global preference of saving in standard order.
    */
    @SuppressWarnings("unchecked")
	public static List<BibtexEntry> getSortedEntries(BibtexDatabase database, Set<String> keySet, boolean isSaveOperation) {
        FieldComparatorStack<BibtexEntry> comparatorStack = null;

        boolean inOriginalOrder = isSaveOperation ? Globals.prefs.getBoolean("saveInOriginalOrder") :
            Globals.prefs.getBoolean("exportInOriginalOrder");
        if (inOriginalOrder) {
            // Sort entries based on their creation order, utilizing the fact
            // that IDs used for entries are increasing, sortable numbers.
            List<Comparator<BibtexEntry>> comparators = new ArrayList<Comparator<BibtexEntry>>();
            comparators.add(new CrossRefEntryComparator());
            comparators.add(new IdComparator());
            comparatorStack = new FieldComparatorStack<BibtexEntry>(comparators);

        } else {
            String pri, sec, ter;
            boolean priD, secD, terD = false;

            boolean inStandardOrder = isSaveOperation ? Globals.prefs.getBoolean("saveInStandardOrder") :
                Globals.prefs.getBoolean("exportInStandardOrder");
            if (!inStandardOrder) {
                // The setting is to save according to the current table order.
                pri = Globals.prefs.get("priSort");
                sec = Globals.prefs.get("secSort");
                // sorted as they appear on the screen.
                ter = Globals.prefs.get("terSort");
                priD = Globals.prefs.getBoolean("priDescending");
                secD = Globals.prefs.getBoolean("secDescending");
                terD = Globals.prefs.getBoolean("terDescending");

            } else {
                // The setting is to save in standard order: author, editor, year
                pri = "author";
                sec = "editor";
                ter = "year";
                priD = false;
                secD = false;
                terD = true;
            }
            
            List<Comparator<BibtexEntry>> comparators = new ArrayList<Comparator<BibtexEntry>>();
            if (isSaveOperation)
                comparators.add(new CrossRefEntryComparator());
            comparators.add(new FieldComparator(pri, priD));
            comparators.add(new FieldComparator(sec, secD));
            comparators.add(new FieldComparator(ter, terD));
            comparators.add(new FieldComparator(BibtexFields.KEY_FIELD));

            comparatorStack = new FieldComparatorStack<BibtexEntry>(comparators);
        }
        // Use glazed lists to get a sorted view of the entries:
        BasicEventList entryList = new BasicEventList();
        SortedList sorter = new SortedList(entryList, comparatorStack);

        if (keySet == null)
            keySet = database.getKeySet();

        if (keySet != null) {
            Iterator<String> i = keySet.iterator();

            for (; i.hasNext();) {
                sorter.add(database.getEntryById((i.next())));
            }
        }
        return sorter;
    }

    /** Returns true iff the entry has a nonzero value in its field.
     */
    private static boolean nonZeroField(BibtexEntry be, String field)
    {
        String o = (be.getField(field));

        return ((o != null) && !o.equals("0"));
    }
}



///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

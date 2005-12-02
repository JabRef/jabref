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

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.*;
import net.sf.jabref.*;
import net.sf.jabref.mods.*;
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
        List strings = new ArrayList();
        for (Iterator i = database.getStringKeySet().iterator(); i.hasNext();) {
            strings.add(database.getString(i.next()));
        }
        Collections.sort(strings, new BibtexStringComparator(false));
        // First, make a Map of all entries:
        HashMap remaining = new HashMap();
        for (Iterator i=strings.iterator(); i.hasNext();) {
            BibtexString string = (BibtexString)i.next();
            remaining.put(string.getName(), string);
        }
        for (Iterator i = strings.iterator(); i.hasNext();) {
            BibtexString bs = (BibtexString) i.next();
            if (remaining.containsKey(bs.getName()))
                writeString(fw, bs, remaining);
        }
    }

    private static void writeString(Writer fw, BibtexString bs, HashMap remaining) throws IOException {
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
        if (!bs.getContent().equals(""))
            fw.write((new LatexFieldFormatter()).format(bs.getContent(), Globals.BIBTEX_STRING));
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
      out.write(GUIGlobals.SIGNATURE);
      out.write(" "+GUIGlobals.version+"."+Globals.NEWLINE +GUIGlobals.encPrefix+encoding+Globals.NEWLINE +Globals.NEWLINE);
    }

    /**
     * Saves the database to file. Two boolean values indicate whether
     * only entries with a nonzero Globals.SEARCH value and only
     * entries with a nonzero Globals.GROUPSEARCH value should be
     * saved. This can be used to let the user save only the results of
     * a search. False and false means all entries are saved.
     */
    public static SaveSession saveDatabase(BibtexDatabase database, MetaData metaData,
                                           File file, JabRefPreferences prefs, boolean checkSearch,
                                           boolean checkGroup, String encoding) throws SaveException
    {
        BibtexEntry be = null;
        TreeMap types = new TreeMap(); // Map to collect entry type definitions
        // that we must save along with entries using them.

        boolean backup = prefs.getBoolean("backup");

        SaveSession session;
        try {
            session = new SaveSession(file, encoding, backup);
        } catch (IOException e) {
            throw new SaveException(e.getMessage());
        }

        try
        {

            // Get our data stream. This stream writes only to a temporary file, until committed.
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
            List sorter = getSortedEntries(database, null, true);

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();) {
                be = (BibtexEntry) (i.next());

        // Check if we must write the type definition for this
        // entry, as well. Our criterion is that all non-standard
        // types (*not* customized standard types) must be written.
        BibtexEntryType tp = be.getType();
        if (BibtexEntryType.getStandardType(tp.getName()) == null) {
            types.put(tp.getName(), tp);
        }

                // Check if the entry should be written.
                boolean write = true;

                if (checkSearch && !nonZeroField(be, Globals.SEARCH))
                {
                    write = false;
                }

                if (checkGroup && !nonZeroField(be, Globals.GROUPSEARCH))
                {
                    write = false;
                }

                if (write)
                {
                    be.write(fw, ff, true);
                    fw.write(Globals.NEWLINE);
                }
            }

            // Write meta data.
            if (metaData != null)
            {
                metaData.writeMetaData(fw);
            }

        // Write type definitions, if any:
        if (types.size() > 0) {
        for (Iterator i=types.keySet().iterator(); i.hasNext();) {
            CustomEntryType tp = (CustomEntryType)types.get(i.next());
            tp.save(fw);
            fw.write(Globals.NEWLINE);
        }

        }


            fw.close();
        }
         catch (Throwable ex)
        {
            ex.printStackTrace();
            try {
                session.cancel();
                //repairAfterError(file, backup, INIT_OK);
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
     * Saves the database to file, including only the entries included
     * in the supplied input array bes.
     *
     * @return A List containing warnings, if any.
     */
    public static SaveSession savePartOfDatabase(BibtexDatabase database, MetaData metaData,
                                                 File file, JabRefPreferences prefs, BibtexEntry[] bes, String encoding) throws SaveException
    {

    TreeMap types = new TreeMap(); // Map to collect entry type definitions
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

        boolean priD, secD, terD, priBinary=false;
        if (!prefs.getBoolean("saveInStandardOrder")) {
        // The setting is to save according to the current table order.
            priBinary = prefs.getBoolean("priBinary");
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

        List comparators = new ArrayList();
        comparators.add(new CrossRefEntryComparator());
        comparators.add(new FieldComparator(pri, priD));
        comparators.add(new FieldComparator(sec, secD));
        comparators.add(new FieldComparator(ter, terD));
        comparators.add(new FieldComparator(Globals.KEY_FIELD));
        // Use glazed lists to get a sorted view of the entries:
        BasicEventList entryList = new BasicEventList();
        SortedList sorter = new SortedList(entryList, new FieldComparatorStack(comparators));

        if ((bes != null) && (bes.length > 0))
        for (int i=0; i<bes.length; i++) {
            sorter.add(bes[i]);
        }

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();)
            {
                be = (BibtexEntry) (i.next());

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
        for (Iterator i=types.keySet().iterator(); i.hasNext();) {
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


    public static void exportCustomDatabase(BibtexDatabase database, String directory, String lfName,
                                            File outFile, String encoding)
        throws Exception {

      exportDatabase(database, directory, lfName, outFile, encoding);
    }



    public static void exportDatabase(BibtexDatabase database, String lfName,
                                      File outFile, String encoding)
        throws Exception {

      exportDatabase(database, Globals.LAYOUT_PREFIX, lfName, outFile, encoding);

    }

    public static void exportDatabase(BibtexDatabase database, String prefix, String lfName,
                                      File outFile, String encoding)
            throws Exception {

        if (lfName.equals("oocalc")) {
            OpenOfficeDocumentCreator.exportOpenOfficeCalc(outFile, database);
            return;
        } else if (lfName.equals("ods")) {
            OpenDocumentSpreadsheetCreator.exportOpenDocumentSpreadsheet(outFile, database);
            return;
        }

        SaveSession ss = new SaveSession(outFile, encoding, false);
        VerifyingWriter ps = ss.getWriter();
        //ps = new OutputStreamWriter(new FileOutputStream(outFile), encoding);
        exportDatabase(database, null, prefix, lfName, ps);
        if (!ps.couldEncodeAll()) {
            System.out.println("Could not encode...");
        }
        try {
            ss.commit();
        } catch (SaveException e) {
            e.printStackTrace();
        }
    }

    public static void exportDatabase(BibtexDatabase database, Set entries, String prefix, String lfName,
                                      Writer ps)
    throws Exception {

    Object[] keys = database.getKeySet().toArray();
    String key, type;
    Reader reader;
    int c;


    if (lfName.equals("mods")) {
        MODSDatabase md = new MODSDatabase(database);
        try {
        DOMSource source = new DOMSource(md.getDOMrepresentation());
        StreamResult result = new StreamResult(ps);
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.transform(source, result);
        }
        catch (Exception e) {
        throw new Error(e);
        }
        ps.close();
        return;
    }

        // Print header
        // changed section - begin (arudert)
        Layout beginLayout = null;
        try {
            reader = getReader(prefix + lfName + ".begin.layout");
            LayoutHelper layoutHelper = new LayoutHelper(reader);
            beginLayout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
            reader.close();
/*
	    while ((c = reader.read()) != -1) {
                ps.write((char)c);
	    }
	    reader.close();
*/
        } catch (IOException ex) {
            //  // If an exception was cast, export filter doesn't have a begin file.
        }
        // Write the header
        if (beginLayout != null) {
            ps.write(beginLayout.doLayout(database));
        }
        // changed section - end (arudert)

    // Write database entries; entries will be sorted as they
    // appear on the screen, or sorted by author, depending on
    // Preferences.
    // We also supply the Set entries - if we are to export only certain entries,
    // it will be non-null, and be used to choose entries. Otherwise, it will be
    // null, and be ignored.
    List sorted = getSortedEntries(database, entries, false);


    // Load default layout
    reader = getReader(prefix+lfName+".layout");
    //Util.pr(prefix+lfName+".layout");

    LayoutHelper layoutHelper = new LayoutHelper(reader);
    Layout defLayout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
    reader.close();
    HashMap layouts = new HashMap();
    Layout layout;
    Iterator i = sorted.iterator();
    for (; i.hasNext();) {
        // Get the entry
        BibtexEntry entry = (BibtexEntry) (i.next());

        //System.out.println(entry.getType().getName());

        // Get the layout
        type = entry.getType().getName().toLowerCase();
        if (layouts.containsKey(type))
                layout = (Layout)layouts.get(type);
        else {
                try {
            // We try to get a type-specific layout for this entry.
            reader = getReader(prefix+lfName+"."+type+".layout");
            layoutHelper = new LayoutHelper(reader);
            layout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
            layouts.put(type, layout);
            reader.close();
                } catch (IOException ex) {
            // The exception indicates that no type-specific layout exists, so we
            // go with the default one.
            layout = defLayout;
                }
            }
            //Layout layout = layoutHelper.getLayoutFromText();

            // Write the entry
            ps.write(layout.doLayout(entry, database));
          }

        // Print footer

        // changed section - begin (arudert)
        Layout endLayout = null;
        try {
            reader = getReader(prefix + lfName + ".end.layout");
            layoutHelper = new LayoutHelper(reader);
            endLayout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
            reader.close();
            /*
                  while ((c = reader.read()) != -1) {
                            ps.write((char)c);
                  }
                  reader.close();
            */
        } catch (IOException ex) {
            //  // If an exception was cast, export filter doesn't have an end file.
        }
        // Write the header
        if (endLayout != null) {
            ps.write(endLayout.doLayout(database));
        }
        // changed section - end (arudert)

        ps.flush();
        ps.close();
    }


    public static void exportEntries(BibtexDatabase database, BibtexEntry[] bes,
                                     String lfName, boolean custom, String directory, Writer sw)
    throws Exception {

    HashSet keys = new HashSet();
    for (int i=0; i<bes.length; i++)
        keys.add(bes[i].getId());
    exportDatabase(database, keys, (custom ? directory : Globals.LAYOUT_PREFIX), lfName, sw);


    }

    public static void exportToCSV(BibtexDatabase database,
                                   File outFile, JabRefPreferences prefs)
        throws Exception {

    HashMap fieldFormatters = new HashMap();
    fieldFormatters.put("default", new RemoveLatexCommands());
    fieldFormatters.put("author", new Object[] {new AuthorLastFirst(),
                            new RemoveLatexCommands()});
    fieldFormatters.put("pdf", new ResolvePDF());

    String SEPARATOR = "\t";
    List sorted = getSortedEntries(database, null, true);
    Set fields = new TreeSet();
    for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++)
        fields.add(GUIGlobals.ALL_FIELDS[i]);

    //	try {
    Object[] o = fields.toArray();
    FileWriter out = new FileWriter(outFile);
    out.write((String)o[0]);
    for (int i=1; i<o.length; i++) {
        out.write(SEPARATOR+(String)o[i]);
    }
    out.write(Globals.NEWLINE);

    for (Iterator i=sorted.iterator(); i.hasNext();) {
        BibtexEntry entry = (BibtexEntry)i.next();
        writeField(database, entry, (String)o[0], fieldFormatters, out);
        for (int j=1; j<o.length; j++) {
        out.write(SEPARATOR);
        writeField(database, entry, (String)o[j], fieldFormatters, out);
        }
        out.write(Globals.NEWLINE);
    }


    out.close();
    //	} catch (Throwable ex) {}


    }



    private static void writeField(BibtexDatabase database, BibtexEntry entry, String field,
                                   HashMap fieldFormatters, Writer out)
    throws IOException {

    String s = (String)entry.getField(field);
    if (s == null) {
        return;
    }
        s = database.resolveForStrings(s);
    Object form = fieldFormatters.get(field);
    if (form == null)
        form = fieldFormatters.get("default");

    if (form instanceof LayoutFormatter) {
        s = ((LayoutFormatter)form).format(s);
    } else if (form instanceof Object[]) {
        Object[] forms = (Object[])form;
        for (int i=0; i<forms.length; i++) {
        s = ((LayoutFormatter)(forms[i])).format(s);
        }
    }

    out.write(s);
    }

    /**
     * This method attempts to get a Reader for the file path given, either by
     * loading it as a resource (from within jar), or as a normal file.
     * If unsuccessful (e.g. file not found), an IOException is thrown.
     */
    private static Reader getReader(String name) throws IOException {
      Reader reader = null;
      // Try loading as a resource first. This works for files inside the jar:
      URL reso = JabRefFrame.class.getResource(name);

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
    protected static List getSortedEntries(BibtexDatabase database, Set keySet, boolean isSaveOperation) {
        String pri, sec, ter;
        boolean priD, secD, terD, priBinary = false;
        if (!isSaveOperation || !Globals.prefs.getBoolean("saveInStandardOrder")) {
            // The setting is to save according to the current table order.
            priBinary = Globals.prefs.getBoolean("priBinary");
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

        List comparators = new ArrayList();
        comparators.add(new CrossRefEntryComparator());
        comparators.add(new FieldComparator(pri, priD));
        comparators.add(new FieldComparator(sec, secD));
        comparators.add(new FieldComparator(ter, terD));
        comparators.add(new FieldComparator(Globals.KEY_FIELD));
        // Use glazed lists to get a sorted view of the entries:
        BasicEventList entryList = new BasicEventList();
        SortedList sorter = new SortedList(entryList, new FieldComparatorStack(comparators));

        if (keySet == null)
            keySet = database.getKeySet();

        if (keySet != null) {
            Iterator i = keySet.iterator();

            for (; i.hasNext();) {
                sorter.add(database.getEntryById((String) (i.next())));
            }
        }
        return sorter;
    }

    /** Returns true iff the entry has a nonzero value in its field.
     */
    private static boolean nonZeroField(BibtexEntry be, String field)
    {
        String o = (String) (be.getField(field));

        return ((o != null) && !o.equals("0"));
    }
}



///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

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

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;

import net.sf.jabref.*;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class FileActions
{

    //~ Methods ////////////////////////////////////////////////////////////////

    private static void initFile(File file, boolean backup) throws IOException {
	String name = file.getName();
	String path = file.getParent();
	File temp = new File(path, name + GUIGlobals.tempExt);

	if (backup)
        {
	    File back = new File(path, name + GUIGlobals.backupExt);

	    if (back.exists()) {
		back.renameTo(temp);
	    }

	    if (file.exists())
            {
		file.renameTo(back);
	    }

	    if (temp.exists())
            {
		temp.delete();
	    }
	}
	else {
	    if (file.exists()) {
		file.renameTo(temp);
	    }
	}
    }

    private static void writePreamble(Writer fw, String preamble) throws IOException {
	if (preamble != null) {
	    fw.write("@PREAMBLE{");
	    fw.write(preamble);
	    fw.write("}\n\n");
	}
    }

    private static void writeStrings(Writer fw, BibtexDatabase database) throws IOException {
	for (int i = 0; i < database.getStringCount(); i++) {
	    BibtexString bs = database.getString(i);

	    //fw.write("@STRING{"+bs.getName()+" = \""+bs.getContent()+"\"}\n\n");
	    fw.write("@STRING{" + bs.getName() + " = ");
	    if (!bs.getContent().equals(""))
		fw.write((new LatexFieldFormatter()).format(bs.getContent(),
							    true));
	    else fw.write("{}");

	    //Util.writeField(bs.getName(), bs.getContent(), fw) ;
	    fw.write("}\n\n");
	}
    }

    public static void repairAfterError(File file) {
	// Repair the file with our temp file since saving failed.
	String name = file.getName();

	// Repair the file with our temp file since saving failed.
	String path = file.getParent();
	File temp = new File(path, name + GUIGlobals.tempExt);
	File back = new File(path, name + GUIGlobals.backupExt);

	if (file.exists()) {
	    file.delete();
	}

	if (temp.exists()) {
	    temp.renameTo(file);
	}
	else {
	    back.renameTo(file);
	}
    }

    /**
     * Saves the database to file. Two boolean values indicate whether
     * only entries with a nonzero Globals.SEARCH value and only
     * entries with a nonzero Globals.GROUPSEARCH value should be
     * saved. This can be used to let the user save only the results of
     * a search. False and false means all entries are saved.
     */
    public static void saveDatabase(BibtexDatabase database, MetaData metaData,
        File file, JabRefPreferences prefs, boolean checkSearch,
        boolean checkGroup) throws SaveException
    {
        BibtexEntry be = null;
        try
        {
	    initFile(file, prefs.getBoolean("backup"));

            // Define our data stream.
            //Writer fw = getWriter(file, "UTF-8");
            FileWriter fw = new FileWriter(file);

            // Write signature.
            fw.write(GUIGlobals.SIGNATURE);

            // Write preamble if there is one.
            writePreamble(fw, database.getPreamble());

            // Write strings if there are any.
	    writeStrings(fw, database);

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            String pri = prefs.get("priSort");
            String sec = prefs.get("secSort");
            // sorted as they appear on the screen.
            String ter = prefs.get("terSort");
            TreeSet sorter = new TreeSet(new CrossRefEntryComparator(
                        new EntryComparator(prefs.getBoolean("priDescending"),
                            prefs.getBoolean("secDescending"),
                            prefs.getBoolean("terDescending"), pri, sec, ter)));
            Set keySet = database.getKeySet();

            if (keySet != null)
            {
                Iterator i = keySet.iterator();

                for (; i.hasNext();)
                {
                    sorter.add(database.getEntryById((String) (i.next())));
                }
            }

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();)
            {
                be = (BibtexEntry) (i.next());

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
                    fw.write("\n");
                }
            }

            // Write meta data.
            if (metaData != null)
            {
                metaData.writeMetaData(fw);
            }

            fw.close();
        }
         catch (Throwable ex)
        {
	    repairAfterError(file);
            throw new SaveException(ex.getMessage(), be);
	}

    }

    /**
     * Saves the database to file, including only the entries included
     * in the supplied input array bes.
     */
    public static void savePartOfDatabase(BibtexDatabase database, MetaData metaData,
        File file, JabRefPreferences prefs, BibtexEntry[] bes) throws SaveException
    {

        BibtexEntry be = null;

        try
        {
	    initFile(file, prefs.getBoolean("backup"));

            // Define our data stream.
            FileWriter fw = new FileWriter(file);


            // Write signature.
            fw.write(GUIGlobals.SIGNATURE);

            // Write preamble if there is one.
            writePreamble(fw, database.getPreamble());

            // Write strings if there are any.
	    writeStrings(fw, database);

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            String pri = prefs.get("priSort");
            String sec = prefs.get("secSort");
            // sorted as they appear on the screen.
            String ter = prefs.get("terSort");
            TreeSet sorter = new TreeSet(new CrossRefEntryComparator(
                        new EntryComparator(prefs.getBoolean("priDescending"),
                            prefs.getBoolean("secDescending"),
                            prefs.getBoolean("terDescending"), pri, sec, ter)));

	    if ((bes != null) && (bes.length > 0))
		for (int i=0; i<bes.length; i++) {
		    sorter.add(bes[i]);
		}

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();)
            {
                be = (BibtexEntry) (i.next());
		be.write(fw, ff, true);
		fw.write("\n");
	    }

            // Write meta data.
            if (metaData != null)
            {
                metaData.writeMetaData(fw);
            }

            fw.close();
        }
         catch (Throwable ex)
        {
	    repairAfterError(file);
            throw new SaveException(ex.getMessage(), be);
	}

    }


  public static OutputStreamWriter getWriter(File f, String encoding)
      throws IOException {
    OutputStreamWriter ow;
    //try {
    ow = new OutputStreamWriter(new FileOutputStream(f), encoding);
    //} catch (UnsupportedEncodingException ex) {
    //  ow = new OutputStreamWriter(new FileOutputStream(f));
    //}

    return ow;
  }

    public static void exportCustomDatabase(BibtexDatabase database, String directory, String lfName,
                                            File outFile, JabRefPreferences prefs)
        throws Exception {

      exportDatabase(database, directory, lfName, outFile, prefs);
    }



    public static void exportDatabase(BibtexDatabase database, String lfName,
                                      File outFile, JabRefPreferences prefs)
        throws Exception {

      exportDatabase(database, Globals.LAYOUT_PREFIX, lfName, outFile, prefs);
    }

    public static void exportDatabase(BibtexDatabase database, String prefix, String lfName,
                                      File outFile, JabRefPreferences prefs)
    throws Exception {

	//PrintStream ps=null;
        OutputStreamWriter ps=null;

	//try {
	    Object[] keys = database.getKeySet().toArray();
	    String key, type;
	    Reader reader;
            int c;

            // Trying to change the encoding:
            //ps=new PrintStream(new FileOutputStream(outFile));
            ps=new OutputStreamWriter(new FileOutputStream(outFile), "iso-8859-1");

	    // Print header
            try {
              reader = getReader(prefix+lfName+".begin.layout");
              while ((c = reader.read()) != -1) {
                ps.write((char)c);
              }
              reader.close();
            } catch (IOException ex) {}
            // If an exception was cast, export filter doesn't have a begin file.

            // Write database entrie; entries will be sorted as they
            // appear on the screen.
            String pri = prefs.get("priSort"),
		sec = prefs.get("secSort"),
		ter = prefs.get("terSort");
            EntrySorter sorter = database.getSorter
		(new EntryComparator(prefs.getBoolean("priDescending"),
				     prefs.getBoolean("secDescending"),
				     prefs.getBoolean("terDescending"),
				     pri, sec, ter));

	    // Load default layout
            reader = getReader(prefix+lfName+".layout");
            //Util.pr(prefix+lfName+".layout");

	    LayoutHelper layoutHelper = new LayoutHelper(reader);
	    Layout defLayout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
            reader.close();
	    HashMap layouts = new HashMap();
	    Layout layout;
            for (int i=0; i<sorter.getEntryCount(); i++) {
              // Get the entry

              key = (String)sorter.getIdAt(i);
              BibtexEntry entry = database.getEntryById(key);

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
            ps.write(layout.doLayout(entry));
          }

          // Print footer
          try {
            reader = getReader(prefix+lfName+".end.layout");
            while ((c = reader.read()) != -1) {
              ps.write((char)c);
            }
            reader.close();
          } catch (IOException ex) {}
          // If an exception was cast, export filter doesn't have a end file.

          ps.flush();
          ps.close();
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

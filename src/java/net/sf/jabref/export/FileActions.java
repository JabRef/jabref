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

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.*;
import net.sf.jabref.*;
import net.sf.jabref.mods.*;

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
	TreeSet strings = new TreeSet(new BibtexStringComparator(true));
	for (Iterator i=database.getStringKeySet().iterator(); i.hasNext();) {
	    strings.add(database.getString(i.next()));
	}

	for (Iterator i=strings.iterator(); i.hasNext();) {
	    BibtexString bs = (BibtexString)i.next();

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
     * Writes the JabRef signature and the encoding.
     *
     * @param encoding String the name of the encoding, which is part of the header.
     */
    private static void writeBibFileHeader(Writer out, String encoding) throws IOException {
      out.write(GUIGlobals.SIGNATURE);
      out.write(" "+GUIGlobals.version+".\n"+GUIGlobals.encPrefix+encoding+"\n\n");
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
        boolean checkGroup, String encoding) throws SaveException
    {
        BibtexEntry be = null;
        try
        {
	    initFile(file, prefs.getBoolean("backup"));

            // Define our data stream.
            Writer fw = getWriter(file, encoding);

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
	    TreeSet sorter = getSortedEntries(database);

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();) {
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
        File file, JabRefPreferences prefs, BibtexEntry[] bes, String encoding) throws SaveException
    {

        BibtexEntry be = null;

        try
        {
	    initFile(file, prefs.getBoolean("backup"));

            // Define our data stream.
            Writer fw = getWriter(file, encoding);

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
            TreeSet sorter = new TreeSet(new CrossRefEntryComparator
					 (new EntryComparator(priD, secD, terD, pri, sec, ter)));
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

    ow = new OutputStreamWriter(new FileOutputStream(f), encoding);

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

	String encoding = prefs.get("defaultEncoding");//"iso-8859-1";
	//System.out.println(encoding);
	//PrintStream ps=null;
        OutputStreamWriter ps=null;

	//try {
	    Object[] keys = database.getKeySet().toArray();
	    String key, type;
	    Reader reader;
            int c;

            // Trying to change the encoding:
            //ps=new PrintStream(new FileOutputStream(outFile));
            ps=new OutputStreamWriter(new FileOutputStream(outFile), encoding);
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
            try {
              reader = getReader(prefix+lfName+".begin.layout");
              while ((c = reader.read()) != -1) {
                ps.write((char)c);
              }
              reader.close();
            } catch (IOException ex) {}
            // If an exception was cast, export filter doesn't have a begin file.

            // Write database entrie; entries will be sorted as they
            // appear on the screen, or sorted by author, depending on
	    // Preferences.
	    TreeSet sorted = getSortedEntries(database);
            /*String pri = prefs.get("priSort"),
		sec = prefs.get("secSort"),
		ter = prefs.get("terSort");
            EntrySorter sorter = database.getSorter
		(new EntryComparator(prefs.getBoolean("priDescending"),
				     prefs.getBoolean("secDescending"),
				     prefs.getBoolean("terDescending"),
				     pri, sec, ter));
	    */
	    // Load default layout
            reader = getReader(prefix+lfName+".layout");
            //Util.pr(prefix+lfName+".layout");

	    LayoutHelper layoutHelper = new LayoutHelper(reader);
	    Layout defLayout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);
            reader.close();
	    HashMap layouts = new HashMap();
	    Layout layout;
            for (Iterator i = sorted.iterator(); i.hasNext();) {
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

    public static void exportToCSV(BibtexDatabase database, 
                                      File outFile, JabRefPreferences prefs)
        throws Exception {

	HashMap fieldFormatters = new HashMap();
	fieldFormatters.put("default", new RemoveLatexCommands());
	fieldFormatters.put("author", new Object[] {new AuthorLastFirst(),
						    new RemoveLatexCommands()});
	fieldFormatters.put("pdf", new ResolvePDF());
	    
	String SEPARATOR = "\t";
	TreeSet sorted = getSortedEntries(database);
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
	out.write("\n");

	for (Iterator i=sorted.iterator(); i.hasNext();) {
	    BibtexEntry entry = (BibtexEntry)i.next();
	    writeField(entry, (String)o[0], fieldFormatters, out);
	    for (int j=1; j<o.length; j++) {
		out.write(SEPARATOR);
		writeField(entry, (String)o[j], fieldFormatters, out);
	    }
	    out.write("\n");
	}


	out.close();
	//	} catch (Throwable ex) {}
	    
	
    }

    private static void writeField(BibtexEntry entry, String field, 
				   HashMap fieldFormatters, Writer out) 
	throws IOException {

	String s = (String)entry.getField(field);
	if (s == null) {
	    return;
	}
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
    
    protected static TreeSet getSortedEntries(BibtexDatabase database) {
	String pri, sec, ter;
	boolean priD, secD, terD;
	if (!Globals.prefs.getBoolean("saveInStandardOrder")) {
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
	TreeSet sorter = new TreeSet(new CrossRefEntryComparator
				     (new EntryComparator(priD, secD, terD, pri, sec, ter)));
	Set keySet = database.getKeySet();
	
	if (keySet != null)
            {
                Iterator i = keySet.iterator();
		
                for (; i.hasNext();)
		    {
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

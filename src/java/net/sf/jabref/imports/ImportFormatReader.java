/*
 * Copyright (C) 2003 Morten O. Alver and Nizar N. Batada
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref.imports;

import net.sf.jabref.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

import java.util.regex.*;


/*
 * // int jabrefframe BibtexDatabase database=new BibtexDatabase(); String
 * filename=Globals.getNewFile(); ArrayList bibitems=readISI(filename); // is
 * there a getFileName(); Iterator it = bibitems.iterator();
 * while(it.hasNext()){ BibtexEntry entry = (BibtexEntry)it.next();
 * entry.setId(Util.createId(entry.getType(), database); try {
 * database.insertEntry(entry); } catch (KeyCollisionException ex) {
 *  } }
 */
public class ImportFormatReader {

    public static String BIBTEX_FORMAT = "BibTeX";
    private final static String SPACE_MARKER = "__SPC__";
    private final static Pattern bracketsPattern = Pattern.compile("\\{.*\\}"),
	spaceMarkerPattern = Pattern.compile(SPACE_MARKER);

  /** all import formats, in the defalt order of import formats */
  private SortedSet formats = new TreeSet();

  public ImportFormatReader() {
    super();
  }

  public void resetImportFormats() {
    formats.clear();
    
    // Add all our importers to the TreeMap. The map is used to build the import
    // menus, and .
    formats.add(new CsaImporter());   
    formats.add(new IsiImporter());
    formats.add(new EndnoteImporter());
    formats.add(new MedlineImporter());
    formats.add(new BibteXMLImporter());
    formats.add(new BiblioscapeImporter());
    formats.add(new SixpackImporter());
    formats.add(new InspecImporter());
    formats.add(new ScifinderImporter());
    formats.add(new OvidImporter());
    formats.add(new RisImporter());
    formats.add(new JstorImporter());
    formats.add(new SilverPlatterImporter());
    formats.add(new BiomailImporter());
    formats.add(new RepecNepImporter());    
    
    // add all custom importers
    for (Iterator i = Globals.prefs.customImports.iterator(); i.hasNext(); ) {
      CustomImportList.Importer importer = (CustomImportList.Importer)i.next();

      try {
        ImportFormat imFo = importer.getInstance();
        formats.add(imFo);
      } catch(Exception e) {
        System.err.println("Could not instantiate " + importer.getName() + " importer, will ignore it. Please check if the class is still available.");
        e.printStackTrace();
      }      
    }
  }
  
  /**
   * Format for a given CLI-ID.
   * 
   * <p>Will return the first format according to the default-order of
   * format that matches the given ID.</p>
   * 
   * @param cliId  CLI-Id
   * @return  Import Format or <code>null</code> if none matches
   */
  public ImportFormat getByCliId(String cliId) {
    ImportFormat result = null;
    for (Iterator i = formats.iterator(); i.hasNext() && result == null; ) {
      ImportFormat format = (ImportFormat)i.next();
      if (format.getCLIId().equals(cliId)) {
        result = format;
      }
    }
    return result;
  }
  
  public List importFromStream(String format, InputStream in)
    throws IOException {
    ImportFormat importer = getByCliId(format);

    if (importer == null)
      throw new IllegalArgumentException("Unknown import format: " + format);

    List res = importer.importEntries(in);

    // Remove all empty entries
    if (res != null)
      purgeEmptyEntries(res);

    return res;
  }

  public List importFromFile(String format, String filename)
    throws IOException {
    ImportFormat importer = getByCliId(format);

    if (importer == null)
      throw new IllegalArgumentException("Unknown import format: " + format);

    return importFromFile(importer, filename);
  }

  public List importFromFile(ImportFormat importer, String filename)
    throws IOException {
    File file = new File(filename);
    InputStream stream = new FileInputStream(file);

    if (!importer.isRecognizedFormat(stream))
      throw new IOException(Globals.lang("Wrong file format"));

    stream = new FileInputStream(file);

    return importer.importEntries(stream);
  }

  public static BibtexDatabase createDatabase(List bibentries) {
    purgeEmptyEntries(bibentries);

    BibtexDatabase database = new BibtexDatabase();

    for (Iterator i = bibentries.iterator(); i.hasNext();) {
      BibtexEntry entry = (BibtexEntry) i.next();

      try {
        entry.setId(Util.createNeutralId());
        database.insertEntry(entry);
      } catch (KeyCollisionException ex) {
        //ignore
        System.err.println("KeyCollisionException [ addBibEntries(...) ]");
      }
    }

    return database;
  }

  /**
   * All custom importers.
   * 
   * <p>Elements are in default order.</p>
   * 
   * @return all custom importers, elements are of type {@link InputFormat}
   */
  public SortedSet getCustomImportFormats() {
    SortedSet result = new TreeSet();
    for (Iterator i = this.formats.iterator(); i.hasNext(); ) {
      ImportFormat format = (ImportFormat)i.next();
      if (format.getIsCustomImporter()) {
        result.add(format);  
      }
    }
    return result;
  }
  
  /**
   * All built-in importers.
   * 
   * <p>Elements are in default order.</p>
   * 
   * @return all custom importers, elements are of type {@link InputFormat}
   */
  public SortedSet getBuiltInInputFormats() {
    SortedSet result = new TreeSet();
    for (Iterator i = this.formats.iterator(); i.hasNext(); ) {
      ImportFormat format = (ImportFormat)i.next();
      if (!format.getIsCustomImporter()) {
        result.add(format);  
      }
    }
    return result;    
  }
  
  /**
   * All importers.
   * 
   * <p>Elements are in default order.</p>
   * 
   * @return all custom importers, elements are of type {@link InputFormat}
   */
  public SortedSet getImportFormats() {
    return this.formats;
  }

  /**
   * Human readable list of all known import formats (name and CLI Id).
   * 
   * <p>List is in default-order.</p>
   * 
   * @return  human readable list of all known import formats
   */
  public String getImportFormatList() {
    StringBuffer sb = new StringBuffer();

    for (Iterator i = this.formats.iterator(); i.hasNext();) {
      ImportFormat imFo = (ImportFormat)i.next();
      int pad = Math.max(0, 14 - imFo.getFormatName().length());
      sb.append("  ");
      sb.append(imFo.getFormatName());

      for (int j = 0; j < pad; j++)
        sb.append(" ");

      sb.append(" : ");
      sb.append(imFo.getCLIId());
      sb.append("\n");
    }

    String res = sb.toString();

    return res; //.substring(0, res.length()-1);
  }


    /**
     * Expand initials, e.g. EH Wissler -> E. H. Wissler or Wissler, EH -> Wissler, E. H.
     * @param name
     * @return The name after expanding initials.
     */
    public static String expandAuthorInitials(String name) {
      String[] authors = name.split(" and ");
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<authors.length; i++) {
          if (authors[i].indexOf(", ") >= 0) {
              String[] names = authors[i].split(", ");
              if (names.length > 0) {
                  sb.append(names[0]);
                  if (names.length > 1)
                    sb.append(", ");
              }
              for (int j=1; j<names.length; j++) {
                  sb.append(expandAll(names[j]));
              }

          } else {
              String[] names = authors[i].split(" ");
              if (names.length > 0) {
                  sb.append(expandAll(names[0]));
              }
              for (int j=1; j<names.length; j++) {
                  sb.append(" ");
                  sb.append(names[j]);
              }
          }
          if (i < authors.length-1)
              sb.append(" and ");
      }

      return sb.toString().trim();
  }
  
//------------------------------------------------------------------------------


    public static String expandAll(String s) {
        //System.out.println("'"+s+"'");
        // Avoid arrayindexoutof.... :
        if (s.length() == 0)
          return s;
        // If only one character (uppercase letter), add a dot and return immediately:
        if ((s.length() == 1) && (Character.isLetter(s.charAt(0)) &&
                Character.isUpperCase(s.charAt(0))))
          return s+".";
        StringBuffer sb = new StringBuffer();
        char c = s.charAt(0), d = 0;
        for (int i=1; i<s.length(); i++) {
            d = s.charAt(i);
            if (Character.isLetter(c) && Character.isUpperCase(c) &&
                    Character.isLetter(d) && Character.isUpperCase(d)) {
                sb.append(c);
                sb.append(". ");
            }
            else {
                sb.append(c);
            }
            c = d;
        }
        if (Character.isLetter(c) && Character.isUpperCase(c) &&
              Character.isLetter(d) && Character.isUpperCase(d)) {
            sb.append(c);
            sb.append(". ");
        }
        else {
            sb.append(c);
        }
        return sb.toString().trim();
    }


  static File checkAndCreateFile(String filename) {
    File f = new File(filename);

    if (!f.exists() && !f.canRead() && !f.isFile()) {
      System.err.println("Error " + filename
        + " is not a valid file and|or is not readable.");
      Globals.logger("Error " + filename + " is not a valid file and|or is not readable.");

      return null;
    } else

      return f;
  }

  //==================================================
  // Set a field, unless the string to set is empty.
  //==================================================
  public static void setIfNecessary(BibtexEntry be, String field, String content) {
    if (!content.equals(""))
      be.setField(field, content);
  }



    public static Reader getReader(File f, String encoding)
      throws IOException {
      InputStreamReader reader;
      reader = new InputStreamReader(new FileInputStream(f), encoding);

      return reader;
    }

  public static Reader getReaderDefaultEncoding(InputStream in)
    throws IOException {
    InputStreamReader reader;
    reader = new InputStreamReader(in, Globals.prefs.get("defaultEncoding"));

    return reader;
  }

  public static BibtexDatabase import_File(String format, String filename)
    throws IOException {
    BibtexDatabase database = null;
    List bibentries = null;
    File f = new File(filename);

    if (!f.exists())
      throw new IOException(Globals.lang("File not found") + ": " + filename);

    try {
      bibentries = Globals.importFormatReader.importFromFile(format, filename);
    } catch (IllegalArgumentException ex) {
      throw new IOException(Globals.lang("Could not resolve import format") + " '"
        + format + "'");
    }

    if (bibentries == null)
      throw new IOException(Globals.lang("Import failed"));

    // Remove all empty entries:
    purgeEmptyEntries(bibentries);

    // Add entries to database.
    database = new BibtexDatabase();

    Iterator it = bibentries.iterator();

    while (it.hasNext()) {
      BibtexEntry entry = (BibtexEntry) it.next();

      try {
        entry.setId(Util.createNeutralId());
        database.insertEntry(entry);
      } catch (KeyCollisionException ex) {
        //ignore
        System.err.println("KeyCollisionException [ addBibEntries(...) ]");
      }
    }

    return database;
  }

  /**
   * Receives an ArrayList of BibtexEntry instances, iterates through them, and
   * removes all entries that have no fields set. This is useful for rooting out
   * an unsucessful import (wrong format) that returns a number of empty entries.
   */
  public static void purgeEmptyEntries(List entries) {
    for (Iterator i = entries.iterator(); i.hasNext();) {
      BibtexEntry entry = (BibtexEntry) i.next();

      // Get all fields of the entry:
      Object[] o = entry.getAllFields();

      // If there are no fields, remove the entry:
      if (o.length == 0)
        i.remove();
    }
  }

  /**
   * Tries to import a file by iterating through the available import filters,
   * and keeping the import that seems most promising. Returns an Object array
   * with two elements, 0: the name of the format used, 1: a List of entries.
   */
  public Object[] importUnknownFormat(String filename) {
    Object entryList = null;
    String usedFormat = null;
    int bestResult = 0;

    // Cycle through all importers:
    for (Iterator i = getImportFormats().iterator(); i.hasNext();) {
      ImportFormat imFo = (ImportFormat)i.next();

      try {
        //System.out.println("Trying format: "+imFo.getFormatName());
        List entries = importFromFile(imFo, filename);

        if (entries != null)
          purgeEmptyEntries(entries);

        int entryCount = ((entries != null) ? entries.size() : 0);

        //System.out.println("Entries: "+entryCount);
        //BibtexDatabase base = importFile(formats[i], filename);
        if (entryCount > bestResult) {
          bestResult = entryCount;
          usedFormat = imFo.getFormatName();
          entryList = entries;
        }
      } catch (IOException ex) {
	  //ex.printStackTrace();
        //System.out.println("Import failed");
      }
    }

    // Finally, if all else fails, see if it is a BibTeX file:	
    if (entryList == null) {
	try {
	    ParserResult pr = OpenDatabaseAction.loadDatabase(new File(filename), Globals.prefs.get("defaultEncoding"));
	    if ((pr.getDatabase().getEntryCount() > 0)
		|| (pr.getDatabase().getStringCount() > 0)) {
		entryList = pr;
        pr.setFile(new File(filename));
		usedFormat = BIBTEX_FORMAT;
	    }
	} catch (Throwable ex) {
	    //ex.printStackTrace();
	}
	
    }

    return new Object[] { usedFormat, entryList };
  }
}

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Vector;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.KeyCollisionException;
import net.sf.jabref.Util;

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

    private TreeMap formats = new TreeMap();

    public ImportFormatReader() {
	// Add all our importers to the TreeMap. The map is used to build the import
	// menus, and to resolve command-line import instructions.
	formats.put("isi", new IsiImporter());
	formats.put("refer", new EndnoteImporter());
	formats.put("medline", new MedlineImporter());
	formats.put("bibtexml", new BibteXMLImporter());
	formats.put("biblioscape", new BiblioscapeImporter());
	formats.put("sixpack", new SixpackImporter());
	formats.put("inspec", new InspecImporter());
	formats.put("scifinder", new ScifinderImporter());
	formats.put("ovid", new OvidImporter());
	formats.put("ris", new RisImporter());
	formats.put("jstor", new JstorImporter());
	formats.put("silverplatter", new SilverPlatterImporter());
    }

    public List importFromStream(String format, InputStream in) throws IOException {
	Object o = formats.get(format);
	if (o == null)
	    throw new IllegalArgumentException("Unknown import format: "+format);
	ImportFormat importer = (ImportFormat)o;
	
	return importer.importEntries(in);
    }

    public List importFromFile(String format, String filename) throws IOException {
	Object o = formats.get(format);
	if (o == null)
	    throw new IllegalArgumentException("Unknown import format: "+format);
	ImportFormat importer = (ImportFormat)o;
	return importFromFile(importer, filename);
    }

    public List importFromFile(ImportFormat importer, String filename) throws IOException {
	File file = new File(filename);
	InputStream stream = new FileInputStream(file);
	if (!importer.isRecognizedFormat(stream))
	    return null;
	stream = new FileInputStream(file);
	return importer.importEntries(stream);
    }

    public Set getImportFormats() {
	return formats.entrySet();
    }

  /**
   * Describe <code>fixAuthor</code> method here.
   * 
   * @param in
   *          a <code>String</code> value
   * @return a <code>String</code> value // input format string: LN FN [and
   *         LN, FN]* // output format string: FN LN [and FN LN]*
   */
  public static String fixAuthor_nocomma(String in) {

    return fixAuthor(in);
    /*
     * // Check if we have cached this particular name string before: Object old =
     * Globals.nameCache.get(in); if (old != null) return (String)old;
     * 
     * StringBuffer sb=new StringBuffer(); String[] authors = in.split(" and ");
     * for(int i=0; i <authors.length; i++){ //System.out.println(authors[i]);
     * authors[i]=authors[i].trim(); String[] t = authors[i].split(" "); if
     * (t.length > 1) { sb.append(t[t.length-1].trim()); for (int cnt=0; cnt
     * <=t.length-2; cnt++) sb.append(" " + t[cnt].trim()); } else
     * sb.append(t[0].trim()); if(i==authors.length-1) sb.append("."); else
     * sb.append(" and ");
     *  }
     * 
     * String fixed = sb.toString();
     *  // Add the fixed name string to the cache. Globals.nameCache.put(in,
     * fixed);
     * 
     * return fixed;
     */
  }

  //========================================================
  // rearranges the author names
  // input format string: LN, FN [and LN, FN]*
  // output format string: FN LN [, FN LN]+ [and FN LN]
  //========================================================
  public static String fixAuthor_commas(String in) {
    return (fixAuthor(in, false));
  }

  //========================================================
  // rearranges the author names
  // input format string: LN, FN [and LN, FN]*

  // output format string: FN LN [and FN LN]*
  //========================================================

  public static String fixAuthor(String in) {
    return (fixAuthor(in, true));
  }

  public static String fixAuthor(String in, boolean includeAnds) {

    // Check if we have cached this particular name string before:
    if (includeAnds){
      Object old = Globals.nameCache.get(in);
      if (old != null) return (String) old;
    }else{
      Object old = Globals.nameCache_commas.get(in);
      if (old != null) return (String) old;
    }

    //Util.pr("firstnamefirst");
    StringBuffer sb = new StringBuffer();
    //System.out.println("FIX AUTHOR: in= " + in);

    String[] authors = in.split(" and ");
    for (int i = 0; i < authors.length; i++){
      authors[i] = authors[i].trim();
      String[] t = authors[i].split(",");
      if (t.length < 2)
      // there is no comma, assume we have FN LN order
      sb.append(authors[i].trim());
      else sb.append(t[1].trim() + " " + t[0].trim());
      if (includeAnds){
        if (i != authors.length - 1) // put back the " and "
        sb.append(" and ");
      }else{
        if (i == authors.length - 2) sb.append(" and ");
        else if (i != (authors.length - 1)) sb.append(", ");
      }
    }

    String fixed = sb.toString();

    // Add the fixed name string to the cache.
    Globals.nameCache.put(in, fixed);

    return fixed;
  }

  //========================================================
  // rearranges the author names
  // input format string: LN, FN [and LN, FN]*
  // output format string: LN, FN [and LN, FN]*
  //========================================================
  public static String fixAuthor_lastnameFirst(String in) {

    // Check if we have cached this particular name string before:
    Object old = Globals.nameCache_lastFirst.get(in);
    if (old != null) return (String) old;

    //Util.pr("lastnamefirst: in");
    StringBuffer sb = new StringBuffer();

    String[] authors = in.split(" and ");
    for (int i = 0; i < authors.length; i++){
      authors[i] = authors[i].trim();
      int comma = authors[i].indexOf(',');
      test: if (comma >= 0){
        // There is a comma, so we assume it's ok.
        sb.append(authors[i]);
      }else{
        // The name is without a comma, so it must be rearranged.
        int pos = authors[i].lastIndexOf(' ');
        if (pos == -1){
          // No spaces. Give up and just add the name.
          sb.append(authors[i]);
          break test;
        }
        String surname = authors[i].substring(pos + 1);
        if (surname.equalsIgnoreCase("jr.")){
          pos = authors[i].lastIndexOf(' ', pos - 1);
          if (pos == -1){
            // Only last name and jr?
            sb.append(authors[i]);
            break test;
          }else surname = authors[i].substring(pos + 1);
        }
        // Ok, we've isolated the last name. Put together the rearranged name:
        sb.append(surname + ", ");
        sb.append(authors[i].substring(0, pos));

      }
      if (i != authors.length - 1) sb.append(" and ");
    }
    /*
     * String[] t = authors[i].split(","); if(t.length < 2) { // The name is
     * without a comma, so it must be rearranged. t = authors[i].split(" "); if
     * (t.length > 1) { sb.append(t[t.length - 1]+ ","); // Last name for (int
     * j=0; j <t.length-1; j++) sb.append(" "+t[j]); } else if (t.length > 0)
     * sb.append(t[0]); } else { // The name is written with last name first, so
     * it's ok. sb.append(authors[i]); }
     * 
     * if(i !=authors.length-1) sb.append(" and ");
     *  }
     */
    //Util.pr(in+" -> "+sb.toString());
    String fixed = sb.toString();

    // Add the fixed name string to the cache.
    Globals.nameCache_lastFirst.put(in, fixed);

    return fixed;
  }



  static File checkAndCreateFile(String filename) {
    File f = new File(filename);
    if (!f.exists() && !f.canRead() && !f.isFile()){
      System.err.println("Error " + filename
          + " is not a valid file and|or is not readable.");
      Globals.logger("Error " + filename
          + " is not a valid file and|or is not readable.");
      return null;
    }else return f;

  }

  //==================================================
  // Set a field, unless the string to set is empty.
  //==================================================
  public static void setIfNecessary(BibtexEntry be, String field,
      String content) {
    if (!content.equals("")) be.setField(field, content);
  }



  public static ParserResult loadDatabase(File fileToOpen, String encoding)
      throws IOException {
    // Temporary (old method):
    //FileLoader fl = new FileLoader();
    //BibtexDatabase db = fl.load(fileToOpen.getPath());

    Reader reader = getReader(fileToOpen, encoding);
    String suppliedEncoding = null;
    try{
      boolean keepon = true;
      int piv = 0, c;
      while (keepon){
        c = reader.read();
        if ((piv == 0 && Character.isWhitespace((char) c))
            || c == GUIGlobals.SIGNATURE.charAt(piv)) piv++;
        else keepon = false;
        found: if (piv == GUIGlobals.SIGNATURE.length()){
          keepon = false;
          // Found the signature. The rest of the line is unknown, so we skip
          // it:
          while (reader.read() != '\n')
            ;
          // Then we must skip the "Encoding: "
          for (int i = 0; i < GUIGlobals.encPrefix.length(); i++){
            if (reader.read() != GUIGlobals.encPrefix.charAt(i)) break found; // No,
                                                                              // it
                                                                              // doesn't
                                                                              // seem
                                                                              // to
                                                                              // match.
          }
          // If ok, then read the rest of the line, which should contain the
          // name
          // of the encoding:
          StringBuffer sb = new StringBuffer();
          while ((c = reader.read()) != '\n')
            sb.append((char) c);
          suppliedEncoding = sb.toString();
        }

      }
    }catch (IOException ex){
    }

    if ((suppliedEncoding != null)
        && (!suppliedEncoding.equalsIgnoreCase(encoding))){
      Reader oldReader = reader;
      try{
        // Ok, the supplied encoding is different from our default, so we must
        // make a new
        // reader. Then close the old one.
        reader = getReader(fileToOpen, suppliedEncoding);
        oldReader.close();
        //System.out.println("Using encoding: "+suppliedEncoding);
      }catch (IOException ex){
        reader = oldReader; // The supplied encoding didn't work out, so we keep
                            // our
        // existing reader.

        //System.out.println("Error, using default encoding.");
      }
    }else{
      // We couldn't find a supplied encoding. Since we don't know far into the
      // file we read,
      // we start a new reader.
      reader.close();
      reader = getReader(fileToOpen, encoding);
      //System.out.println("No encoding supplied, or supplied encoding equals
      // default. Using default encoding.");
    }

    //return null;

    BibtexParser bp = new BibtexParser(reader);

    ParserResult pr = bp.parse();
    pr.setEncoding(encoding);

    return pr;
  }

  public static Reader getReader(File f, String encoding) throws IOException {
    InputStreamReader reader;
    reader = new InputStreamReader(new FileInputStream(f), encoding);
    return reader;
  }

  public static Reader getReaderDefaultEncoding(InputStream in) throws IOException {
      InputStreamReader reader;
      reader = new InputStreamReader(in, Globals.prefs.get("defaultEncoding"));
      return reader;
  }

  public static BibtexDatabase importFile(String format, String filename)
      throws IOException {
    BibtexDatabase database = null;
    List bibentries = null;
    File f = new File(filename);
    if (!f.exists()) throw new IOException(Globals.lang("File not found")
        + ": " + filename);
    
    try {
	bibentries = Globals.importFormatReader.importFromFile(format, filename);
    } catch (IllegalArgumentException ex) {
	throw new IOException(Globals.lang("Could not resolve import format")
			      + " '" + format + "'");
    }


    if (bibentries == null) throw new IOException(Globals.lang("Import failed"));

    // Remove all empty entries:
    purgeEmptyEntries(bibentries);

    // Add entries to database.
    database = new BibtexDatabase();
    Iterator it = bibentries.iterator();
    while (it.hasNext()){
      BibtexEntry entry = (BibtexEntry) it.next();
      try{
        entry.setId(Util.createNeutralId());
        database.insertEntry(entry);
      }catch (KeyCollisionException ex){
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
	for (Iterator i=entries.iterator(); i.hasNext();) {
	    BibtexEntry entry = (BibtexEntry)i.next();
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
    public Object[] importUnknownFormat(String filename) throws IOException {
	List entryList = null;
	String usedFormat = null;
	int bestResult = 0;

	// Cycle through all importers:
	for (Iterator i=getImportFormats().iterator(); i.hasNext();) {
	    ImportFormat imFo = (ImportFormat)((Map.Entry)i.next()).getValue();
	    // TODO: Here we should use the feature to check if the importer recognizes the
	    //       format, but none of the importers have implemented this yet.
	    try {
		//System.out.println("Trying format: "+imFo.getFormatName());
		List entries = importFromFile(imFo, filename);
		int entryCount = (entries != null ? entries.size() : 0);
		//System.out.println("Entries: "+entryCount);

		//BibtexDatabase base = importFile(formats[i], filename);
		if (entryCount > bestResult) {	    
		    bestResult = entryCount;
		    usedFormat = imFo.getFormatName();
		    entryList = entries;
		}
	    } catch (IOException ex) {
		//System.out.println("Import failed");
	    }
	    
	}
	return new Object[] {usedFormat, entryList};
    }


    

}

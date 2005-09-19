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

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.KeyCollisionException;
import net.sf.jabref.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

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

    /* Use a WeakHashMAp for storing cached names, so the cached mapping will not prevent
     * an obsoleted name string from being garbage collected.
     */

    private final static Map nameCacheLastFirst = new WeakHashMap();
    private final static Map nameCacheFirstFirst = new WeakHashMap();

  private TreeMap formats = new TreeMap();

  public ImportFormatReader() {
    // Add all our importers to the TreeMap. The map is used to build the import
    // menus, and to resolve command-line import instructions.
    formats.put("csa", new CsaImporter());   
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
    formats.put("biomail", new BiomailImporter());
      
  }

    public static void clearNameCache() {
	nameCacheLastFirst.clear();
	nameCacheFirstFirst.clear();
    }


  public List importFromStream(String format, InputStream in)
    throws IOException {
    Object o = formats.get(format);

    if (o == null)
      throw new IllegalArgumentException("Unknown import format: " + format);

    ImportFormat importer = (ImportFormat) o;
    List res = importer.importEntries(in);

    // Remove all empty entries
    if (res != null)
      purgeEmptyEntries(res);

    return res;
  }

  public List importFromFile(String format, String filename)
    throws IOException {
    Object o = formats.get(format);

    if (o == null)
      throw new IllegalArgumentException("Unknown import format: " + format);

    ImportFormat importer = (ImportFormat) o;

    //System.out.println(importer.getFormatName());
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

  public Set getImportFormats() {
    return formats.entrySet();
  }

  public String getImportFormatList() {
    StringBuffer sb = new StringBuffer();

    for (Iterator i = formats.keySet().iterator(); i.hasNext();) {
      String format = (String) i.next();
      ImportFormat imFo = (ImportFormat) formats.get(format);
      int pad = Math.max(0, 14 - imFo.getFormatName().length());
      sb.append("  ");
      sb.append(imFo.getFormatName());

      for (int j = 0; j < pad; j++)
        sb.append(" ");

      sb.append(" : ");
      sb.append(format);
      sb.append("\n");
    }

    String res = sb.toString();

    return res; //.substring(0, res.length()-1);
  }

  /**
   * Describe <code>fixAuthor</code> method here.
   *
   * @param in
   *          a <code>String</code> value
   * @return a <code>String</code> value // input format string: LN FN [and
   *         LN, FN]* // output format string: FN LN [and FN LN]*
   */
    public static String fixAuthor_firstNameFirst(final String inOrig) {

	String in = inOrig;

	// Check if we have cached this particular name string before: 
	Object old = nameCacheFirstFirst.get(in); if (old != null) return (String)old;

	StringBuffer sb = new StringBuffer();

	//System.out.println("FIX AUTHOR: in= " + in);
	String[] authors = in.split(" and ");
	
	for (int i = 0; i < authors.length; i++) {
	    authors[i] = authors[i].trim();
	    
	    String[] t = authors[i].split(",");
	    
	    if (t.length < 2)
		// there is no comma, assume we have FN LN order
		sb.append(authors[i].trim());
	    else
		sb.append(t[1].trim() + " " + t[0].trim());
	    
	    if (i != (authors.length - 1)) // put back the " and "
		sb.append(" and ");

	    //	    if (i == (authors.length - 2))
	    //		sb.append(" and ");
	    //	    else if (i != (authors.length - 1))
	    //		sb.append(", ");
	}
	
	String fixed = sb.toString();
	
	// Cache this transformation so we don't have to repeat it unnecessarily:
	nameCacheFirstFirst.put(inOrig, fixed);

	return fixed;
    }

  //========================================================
  // rearranges the author names
  // input format string: LN, FN [and LN, FN]*
  // output format string: LN, FN [and LN, FN]*
  //========================================================
  public static String fixAuthor_lastnameFirst(final String inOrig) {

      String in = inOrig;

      // Check if we have cached this particular name string before:
      Object old = nameCacheLastFirst.get(in); if (old != null) return (String)old;

      if (in.indexOf("{") >= 0) {
	  StringBuffer tmp = new StringBuffer();
	  int start = -1, end = 0;
	  while ((start = in.indexOf("{", end)) > -1) {
              tmp.append(in.substring(end, start));
	      end = in.indexOf("}", start);
	      if (end > 0) {
		  tmp.append(in.substring(start, end).replaceAll(" ", SPACE_MARKER));
	      } else if (end < 0) {
                  // The braces are mismatched, so give up this.
                  tmp.append(in.substring(start));
                  break;
              }
	  }
	  if ((end > 0) && (end < in.length()))
	      tmp.append(in.substring(end));

	  in = tmp.toString();

      }

    StringBuffer sb = new StringBuffer();

    String[] authors = in.split(" and ");

    for (int i = 0; i < authors.length; i++) {
        authors[i] = authors[i].trim();
        sb.append(fixSingleAuthor_lastNameFirst(authors[i]));
        if (i != (authors.length - 1))
          sb.append(" and ");
    }

    String fixed = sb.toString();
    if (spaceMarkerPattern.matcher(fixed).find())
    fixed = fixed.replaceAll(SPACE_MARKER, " ");

    // Cache this transformation so we don't have to repeat it unnecessarily:
    nameCacheLastFirst.put(inOrig, fixed);

    return fixed;
}

    public static String fixAuthor_lastnameOnly(final String inOrig) {

          String in = inOrig;

          // // Check if we have cached this particular name string before:
          //Object old = nameCacheLastFirst.get(in); if (old != null) return (String)old;

          if (in.indexOf("{") >= 0) {
          StringBuffer tmp = new StringBuffer();
          int start = -1, end = 0;
          while ((start = in.indexOf("{", end)) > -1) {
                  tmp.append(in.substring(end, start));
              end = in.indexOf("}", start);
              if (end > 0) {
              tmp.append(in.substring(start, end).replaceAll(" ", SPACE_MARKER));
              } else if (end < 0) {
                      // The braces are mismatched, so give up this.
                      tmp.append(in.substring(start));
                      break;
                  }
          }
          if ((end > 0) && (end < in.length()))
              tmp.append(in.substring(end));

          in = tmp.toString();

          }

        StringBuffer sb = new StringBuffer();

        String[] authors = in.split(" and ");

        for (int i = 0; i < authors.length; i++) {
            authors[i] = authors[i].trim();
            sb.append(fixSingleAuthor_lastNameOnly(authors[i]));
            if (i < (authors.length - 2))
                sb.append(", ");
            else if (i == authors.length-2)
                sb.append(" and ");
        }

        String fixed = sb.toString();
        if (spaceMarkerPattern.matcher(fixed).find())
        fixed = fixed.replaceAll(SPACE_MARKER, " ");

        // // Cache this transformation so we don't have to repeat it unnecessarily:
        // nameCacheLastFirst.put(inOrig, fixed);

        return fixed;
    }



public static String fixAuthorForAlphabetization(final String inOrig) {

    String in = inOrig;

    if (in.indexOf("{") >= 0) {
    StringBuffer tmp = new StringBuffer();
    int start = -1, end = 0;
    while ((start = in.indexOf("{", end)) > -1) {
            tmp.append(in.substring(end, start));
        end = in.indexOf("}", start);
        if (end > 0) {
        tmp.append(in.substring(start, end).replaceAll(" ", SPACE_MARKER));
        } else if (end < 0) {
                // The braces are mismatched, so give up this.
                tmp.append(in.substring(start));
                break;
            }
    }
    if ((end > 0) && (end < in.length()))
        tmp.append(in.substring(end));

    in = tmp.toString();

    }

  StringBuffer sb = new StringBuffer();

  String[] authors = in.split(" and ");

  for (int i = 0; i < authors.length; i++) {
      authors[i] = authors[i].trim();
      sb.append(getSortableNameForm(authors[i]));
      if (i != (authors.length - 1))
        sb.append(" and ");
  }

  String fixed = sb.toString();
  if (spaceMarkerPattern.matcher(fixed).find())
  fixed = fixed.replaceAll(SPACE_MARKER, " ");

  return fixed;

}
    /*
int comma = authors[i].indexOf(",");
test: 
if (comma >= 0) {
// There is a comma, so we assume it's ok. Fix it so there is no white
// space in front of the comma, and one space after:
String[] parts = authors[i].split(",");
sb.append(parts[0].trim());
for (int part=1; part<parts.length; part++) {
sb.append(", ");
sb.append(parts[part].trim());
}
//sb.append(authors[i]);
}
else {
// The name is without a comma, so it must be rearranged.
int pos = authors[i].lastIndexOf(' ');

if (pos == -1) {
// No spaces. Give up and just add the name.
sb.append(authors[i]);

break test;
}

String surname = authors[i].substring(pos + 1).trim();

if (surname.equalsIgnoreCase("jr.")) {
pos = authors[i].lastIndexOf(' ', pos - 1);

if (pos == -1) {
// Only last name and jr?
sb.append(authors[i]);

break test;
} else
surname = authors[i].substring(pos + 1);
}

// Ok, we've isolated the last name. Put together the rearranged name:
sb.append(surname + ", ");
sb.append(authors[i].substring(0, pos).trim());
}                    */


    public static boolean isVonParticle(String name) {
        return Globals.NAME_PARTICLES.contains(name);
    }

    /**
     * Rearranges a single name to "Lastname, Firstname" format. Particles like "von" and
     * "de la" are considered part of the last name, and placed in front.
     * @param name The name to rearrange.
     * @return The rearranged name.
     */
    private static String fixSingleAuthor_lastNameFirst(String name) {
        int commaPos = name.indexOf(',');
        if (commaPos == -1) {
            // No comma: name in "Firstname Lastname" form.
            String[] parts = name.split(" ");
            int piv = parts.length - 1;

            if (piv < 0)
                return name; // Empty name...

            StringBuffer sb = new StringBuffer();

            // Add "jr" particle(s) if any:
            while (Globals.JUNIOR_PARTICLES.contains(parts[piv])) {
                if (sb.length() > 0)
                    sb.insert(0, ' ');
                sb.insert(0, parts[piv]);
                piv--;
            }

            // Add the last name:
            if (sb.length() > 0)
                sb.insert(0, ' ');
            sb.insert(0, parts[piv]);
            piv--;

            // Then add the ones before, as long as they are von particles:
            while ((piv > 0) && isVonParticle(parts[piv])) {
                sb.insert(0, ' ');
                sb.insert(0, parts[piv]);
                piv--;
            }
            // Add a comma, a space and the first name(s):
            if (piv >= 0)
                sb.append(",");
            for (int i=0; i<=piv; i++) {
                sb.append(' ');
                sb.append(parts[i]);
            }
            return sb.toString();
        } else {
            int splitPos = Math.min(name.length()-1, commaPos+1);
            StringBuffer sb = new StringBuffer(name.substring(0, splitPos));
            //System.out.println("'"+sb.toString()+"'");
            String[] restParts = name.substring(splitPos).trim().split(" ");
            int piv = restParts.length - 1;
            while ((piv > 0) && isVonParticle(restParts[piv])) {
                sb.insert(0, ' ');
                sb.insert(0, restParts[piv]);
                piv--;
            }
            //System.out.println("'"+sb.toString()+"'");
            for (int i=0; i<=piv; i++) {
                sb.append(' ');
                sb.append(restParts[i]);
            }
            return sb.toString();
        }

    }


    /**
      * Rearranges a single name to "Lastname" format. Particles like "von" and
      * "de la" are considered part of the last name, and placed in front.
      * @param name The name to rearrange.
      * @return The rearranged name.
      */
     private static String fixSingleAuthor_lastNameOnly(String name) {
         int commaPos = name.indexOf(',');
         if (commaPos == -1) {
             // No comma: name in "Firstname Lastname" form.
             String[] parts = name.split(" ");
             int piv = parts.length - 1;

             if (piv < 0)
                 return name; // Empty name...

             StringBuffer sb = new StringBuffer();

             // Add "jr" particle(s) if any:
             while (Globals.JUNIOR_PARTICLES.contains(parts[piv])) {
                 if (sb.length() > 0)
                     sb.insert(0, ' ');
                 sb.insert(0, parts[piv]);
                 piv--;
             }

             // Add the last name:
             if (sb.length() > 0)
                 sb.insert(0, ' ');
             sb.insert(0, parts[piv]);
             piv--;

             // Then add the ones before, as long as they are von particles:
             while ((piv > 0) && isVonParticle(parts[piv])) {
                 sb.insert(0, ' ');
                 sb.insert(0, parts[piv]);
                 piv--;
             }

             return sb.toString();

         } else {
             StringBuffer sb = new StringBuffer(name.substring(0, commaPos));
             String[] restParts = name.substring(commaPos).trim().split(" ");
             int piv = restParts.length - 1;
             while ((piv > 0) && isVonParticle(restParts[piv])) {
                 sb.insert(0, ' ');
                 sb.insert(0, restParts[piv]);
                 piv--;
             }

             return sb.toString();
         }

     }

    /**
     * Rearranges a single name to sortable "Lastname, Firstname" format.
     * Particles like "von" and "de la" are placed behind the first name, as they are
     * not to disturb sorting of names.
     * @param name The name to rearrange.
     * @return The rearranged name.
     */
    private static String getSortableNameForm(String name) {
        int commaPos = name.indexOf(',');
        if (commaPos == -1) {
            // No comma: name in "Firstname Lastname" form.
            String[] parts = name.split(" ");
            int piv = parts.length - 1;
            if (piv < 0)
                return name;

            // Count down past "jr" particle(s), if any:
            while (Globals.JUNIOR_PARTICLES.contains(parts[piv])) {
                piv--;
            }

            // Add the last name, including any "jr" particle(s):
            StringBuffer sb = new StringBuffer(parts[piv]);
            for (int i=piv+1; i<parts.length; i++) {
                sb.append(' ');
                sb.append(parts[i]);
            }

            piv--;

            // Add a comma, a space and the first name(s):
            if (piv >= 0)
                sb.append(",");
            for (int i=0; i<=piv; i++) {
                sb.append(' ');
                sb.append(parts[i]);
            }
            return sb.toString();
        } else {
            String[] lnParts = name.substring(0, commaPos).split(" ");
            // Count past any von particles in the last name:
            int piv = 0;
            while ((piv < lnParts.length-1) && isVonParticle(lnParts[piv]))
                piv++;

            // Start building the name, with the last name:
            StringBuffer sb = new StringBuffer(lnParts[piv]);
            // Add more lastnames if there are any:
            for (int i=piv+1; i<lnParts.length; i++) {
                sb.append(' ');
                sb.append(lnParts[i]);
            }
            // Add a comma:
            sb.append(',');

            // Add the first name(s):
            int splitPos = Math.min(name.length()-1, commaPos+1);
            String[] fnParts = name.substring(splitPos).trim().split(" ");
            for (int i=0; i<fnParts.length; i++) {
                sb.append(' ');
                sb.append(fnParts[i]);
            }
            // If we counted past any von particles earlier, add them now:
            if (piv > 0) for (int i=0; i<piv; i++) {
                sb.append(' ');
                sb.append(lnParts[i]);
            }
            // Done.
            return sb.toString();
        }

    }

    /**
     * Expand initials, e.g. EH Wissler -> E. H. Wissler or Wissler, EH -> Wissler, E. H.
     * @param name
     * @return
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

  public static ParserResult loadDatabase(File fileToOpen, String encoding)
    throws IOException {
    // Temporary (old method):
    //FileLoader fl = new FileLoader();
    //BibtexDatabase db = fl.load(fileToOpen.getPath());

    // First we make a quick check to see if this looks like a BibTeX file:
    Reader reader = getReader(fileToOpen, encoding);
    if (!BibtexParser.isRecognizedFormat(reader))
        return null;

    // The file looks promising. Reinitialize the reader and go on:
    reader = getReader(fileToOpen, encoding);

    String suppliedEncoding = null;
    StringBuffer headerText = new StringBuffer();
    try {
      boolean keepon = true;
      int piv = 0;
      int c;

      while (keepon) {
        c = reader.read();
        headerText.append((char)c);
        if (((piv == 0) && Character.isWhitespace((char) c))
            || (c == GUIGlobals.SIGNATURE.charAt(piv)))
          piv++;
        else //if (((char)c) == '@')
          keepon = false;
      //System.out.println(headerText.toString());
found: 
        if (piv == GUIGlobals.SIGNATURE.length()) {
          keepon = false;

          //if (headerText.length() > GUIGlobals.SIGNATURE.length())
          //    System.out.println("'"+headerText.toString().substring(0, headerText.length()-GUIGlobals.SIGNATURE.length())+"'");
          // Found the signature. The rest of the line is unknown, so we skip
          // it:
          while (reader.read() != '\n')
            ;

          // Then we must skip the "Encoding: "
          for (int i = 0; i < GUIGlobals.encPrefix.length(); i++) {
            if (reader.read() != GUIGlobals.encPrefix.charAt(i))
              break found; // No,
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
    } catch (IOException ex) {
    }

    if ((suppliedEncoding != null) && (!suppliedEncoding.equalsIgnoreCase(encoding))) {
      Reader oldReader = reader;

      try {
        // Ok, the supplied encoding is different from our default, so we must
        // make a new
        // reader. Then close the old one.
        reader = getReader(fileToOpen, suppliedEncoding);
        oldReader.close();

        //System.out.println("Using encoding: "+suppliedEncoding);
      } catch (IOException ex) {
        reader = oldReader; // The supplied encoding didn't work out, so we keep
                            // our

        // existing reader.
        //System.out.println("Error, using default encoding.");
      }
    } else {
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
      ImportFormat imFo = (ImportFormat) ((Map.Entry) i.next()).getValue();

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
	    ParserResult pr =
		loadDatabase(new File(filename), Globals.prefs.get("defaultEncoding"));
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

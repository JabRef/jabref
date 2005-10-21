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

//MK:vvv Methods fixAuthor_firstNameFirst(String) and fixAuthor_lastnameFirst(String) are re-written
//  /**
//   * Describe <code>fixAuthor</code> method here.
//   *
//   * @param in
//   *          a <code>String</code> value
//   * @return a <code>String</code> value // input format string: LN FN [and
//   *         LN, FN]* // output format string: FN LN [and FN LN]*
//   */
//    public static String fixAuthor_firstNameFirst(final String inOrig) {
//
//	String in = inOrig;
//
//	// Check if we have cached this particular name string before: 
//	Object old = nameCacheFirstFirst.get(in); if (old != null) return (String)old;
//
//	StringBuffer sb = new StringBuffer();
//
//	//System.out.println("FIX AUTHOR: in= " + in);
//	String[] authors = in.split(" and ");
//	
//	for (int i = 0; i < authors.length; i++) {
//	    authors[i] = authors[i].trim();
//	    
//	    String[] t = authors[i].split(",");
//	    
//	    if (t.length < 2)
//		// there is no comma, assume we have FN LN order
//		sb.append(authors[i].trim());
//	    else
//		sb.append(t[1].trim() + " " + t[0].trim());
//	    
//	    if (i != (authors.length - 1)) // put back the " and "
//		sb.append(" and ");
//
//	    //	    if (i == (authors.length - 2))
//	    //		sb.append(" and ");
//	    //	    else if (i != (authors.length - 1))
//	    //		sb.append(", ");
//	}
//	
//	String fixed = sb.toString();
//	
//	// Cache this transformation so we don't have to repeat it unnecessarily:
//	nameCacheFirstFirst.put(inOrig, fixed);
//
//	return fixed;
//    }
//
//  //========================================================
//  // rearranges the author names
//  // input format string: LN, FN [and LN, FN]*
//  // output format string: LN, FN [and LN, FN]*
//  //========================================================
//  public static String fixAuthor_lastnameFirst(final String inOrig) {
//
//      String in = inOrig;
//
//      // Check if we have cached this particular name string before: 
//      Object old = nameCacheLastFirst.get(in); if (old != null) return (String)old;
//      
//      if (in.indexOf("{") >= 0) {
//	  StringBuffer tmp = new StringBuffer();
//	  int start = -1, end = 0;
//	  while ((start = in.indexOf("{", end)) > -1) {
//              tmp.append(in.substring(end, start));
//	      end = in.indexOf("}", start);
//	      if (end > 0) {
//		  tmp.append(in.substring(start, end).replaceAll(" ", SPACE_MARKER));
//	      } else if (end < 0) {
//                  // The braces are mismatched, so give up this.
//                  tmp.append(in.substring(start));
//                  break;
//              }
//	  }
//	  if ((end > 0) && (end < in.length()))
//	      tmp.append(in.substring(end));
//
//	  in = tmp.toString();
//
//      }
//
//    StringBuffer sb = new StringBuffer();
//
//    String[] authors = in.split(" and ");
//
//    for (int i = 0; i < authors.length; i++) {
//      authors[i] = authors[i].trim();
//
//      int comma = authors[i].indexOf(",");
//test: 
//      if (comma >= 0) {
//          // There is a comma, so we assume it's ok. Fix it so there is no white
//          // space in front of the comma, and one space after:
//          String[] parts = authors[i].split(",");
//          sb.append(parts[0].trim());
//          for (int part=1; part<parts.length; part++) {
//              sb.append(", ");
//              sb.append(parts[part].trim());
//          }
//          //sb.append(authors[i]);
//      }
//      else {
//        // The name is without a comma, so it must be rearranged.
//        int pos = authors[i].lastIndexOf(' ');
//
//        if (pos == -1) {
//          // No spaces. Give up and just add the name.
//          sb.append(authors[i]);
//
//          break test;
//        }
//
//        String surname = authors[i].substring(pos + 1).trim();
//
//        if (surname.equalsIgnoreCase("jr.")) {
//          pos = authors[i].lastIndexOf(' ', pos - 1);
//
//          if (pos == -1) {
//            // Only last name and jr?
//            sb.append(authors[i]);
//
//            break test;
//          } else
//            surname = authors[i].substring(pos + 1);
//        }
//
//        // Ok, we've isolated the last name. Put together the rearranged name:
//        sb.append(surname + ", ");
//        sb.append(authors[i].substring(0, pos).trim());
//      }
//
//      if (i != (authors.length - 1))
//        sb.append(" and ");
//    }
//
//    /*
//     * String[] t = authors[i].split(","); if(t.length < 2) { // The name is
//     * without a comma, so it must be rearranged. t = authors[i].split(" "); if
//     * (t.length > 1) { sb.append(t[t.length - 1]+ ","); // Last name for (int
//     * j=0; j <t.length-1; j++) sb.append(" "+t[j]); } else if (t.length > 0)
//     * sb.append(t[0]); } else { // The name is written with last name first, so
//     * it's ok. sb.append(authors[i]); }
//     *
//     * if(i !=authors.length-1) sb.append(" and ");
//     *  }
//     */
//
//    //Util.pr(in+" -> "+sb.toString());
//    String fixed = sb.toString();
//
//    if (spaceMarkerPattern.matcher(fixed).find())
//	fixed = fixed.replaceAll(SPACE_MARKER, " ");
//
//    // Cache this transformation so we don't have to repeat it unnecessarily:
//    nameCacheLastFirst.put(inOrig, fixed);
//
//    return fixed;
//  }
  
  public static String fixAuthor_Natbib(final String inOrig) {
      AuthorList authors = new AuthorList(inOrig);
      return authors.getAuthorsNatbib();
  }

  public static String fixAuthor_firstNameFirstCommas(final String inOrig, final boolean abbr) {
      AuthorList authors = new AuthorList(inOrig);
      return authors.getAuthorsFirstLast(abbr);
  }
  public static String fixAuthor_firstNameFirst(final String inOrig) {
      AuthorList authors = new AuthorList(inOrig);
      return authors.getAuthorsFirstLastAnds();
  }

  public static String fixAuthor_lastnameFirstCommas(final String inOrig, final boolean abbr) {
      AuthorList authors = new AuthorList(inOrig);
      return authors.getAuthorsLastFirst(abbr);
  }
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
  
//------------------------------------------------------------------------------


/**
 * This is an immutable class representing information of either
 * <CODE>author</CODE> or <CODE>editor</CODE> field in bibtex record.
 * <p>
 * Constructor performs parsing of raw field text and stores preformatted data.
 * Various accessor methods return author/editor field in different formats.
 * <p>
 * Parsing algorithm is designed to satisfy two requirements:
 * (a) when author's name is typed correctly, the result should
 *     coincide with the one of BiBTeX;
 * (b) for erroneous names, output should be reasonable (but may differ
 *     from BiBTeX output).
 * The following rules are used:
 * <ol>
 * <li> 'author field' is a sequence of tokens;
 *    <ul>
 *    <li> tokens are separated by sequences of whitespaces 
 *         (<CODE>Character.isWhitespace(c)==true</CODE>), commas (,),
 *         dashes (-), and tildas (~);
 *    <li> every comma separates tokens, while sequences of other separators
 *         are equivalent to a single separator; for example:
 *         "a - b" consists of 2 tokens ("a" and "b"), while
 *         "a,-,b" consists of 3 tokens ("a", "", and "b")
 *    <li> anything enclosed in braces belonges to a single token; for example:
 *         "abc x{a,b,-~ c}x" consists of 2 tokens, while
 *         "abc xa,b,-~ cx" consists of 4 tokens ("abc", "xa","b", and "cx");
 *    <li> a token followed immediately by a dash is "dash-terminated" token,
 *         and all other tokens are "space-terminated" tokens; for example:
 *         in "a-b- c - d" tokens "a" and "b" are dash-terminated and
 *         "c" and "d" are space-terminated;
 *    <li> for the purposes of splitting of 'author name' into parts and
 *         construction of abbreviation of first name,
 *         one needs definitions of first latter of a token, case of a token, 
 *         and abbreviation of a token:
 *         <ul>
 *         <li> 'first letter' of a token is the first letter character
 *              (<CODE>Character.isLetter(c)==true</CODE>) that does not
 *              belong to a sequence of letters that immediately follows
 *              "\" character, with one exception:
 *              if "\" is followed by "aa", "AA", "ae", "AE", "l", "L", "o", "O",
 *              "oe", "OE", "i", or "j" followed by non-letter, the 'first letter'
 *              of a token is a letter that follows "\"; for example:
 *              in "a{x}b" 'first letter' is "a",
 *              in "{\"{U}}bel" 'first letter' is "U",
 *              in "{\noopsort{\"o}}xyz" 'first letter' is "o",
 *              in "{\AE}x" 'first letter' is "A",
 *              in "\aex\ijk\Oe\j" 'first letter' is "j";
 *              if there is no letter satisfying the above rule, 'first letter'
 *              is undefined;
 *         <li> token is "lower-case" token, if its first letter id defined and
 *              is lower-case (<CODE>Character.isLowerCase(c)==true</CODE>),
 *              and token is "upper-case" token otherwise;
 *         <li> 'abbreviation' of a token is the shortest prefix of the token
 *              that (a) contains 'first letter' and (b) is braces-balanced;
 *              if 'first letter' is undefined, 'abbreviation' is the token
 *              itself; in the above examples, 'abbreviation's are
 *              "a", "{\"{U}}", "{\noopsort{\"o}}", "{\AE}", "\aex\ijk\Oe\j";
 *         </ul>
 *    <li> the behavior based on the above definitions will be erroneous only
 *         in one case: if the first-name-token is "{\noopsort{A}}john",
 *         we abbreviate it as "{\noopsort{A}}.", while BiBTeX produces "j.";
 *         fixing this problem, however, requires processing of the preabmle;
 *    </ul>
 * <li> 'author name's in 'author field' are subsequences of tokens separated
 *      by token "and" ("and" is case-insensitive); if 'author name' is an
 *      empty sequence of tokens, it is ignored; for examle, both
 *      "John Smith and Peter Black" and "and and John Smith and and Peter Black"
 *      consists of 2 'author name's "Johm Smith" and "Peter Black"
 *      (in erroneous situations, this is a bit different from BiBTeX behavior);
 * <li> 'author name' consists of 'first-part', 'von-part', 'last-part', and
 *      'junior-part', each of which is a sequence of tokens;
 *      how a sequence of tokens has to be splitted into these parts, depends
 *      the number of commas:
 *     <ul>
 *     <li> no commas, all tokens are upper-case:
 *          'junior-part' and 'von-part' are empty, 'last-part' consist
 *          of the last token, 'first-part' consists of all other tokens
 *          ('first-part' is empty, if 'author name' consists of a single token);
 *          for example, in "John James Smith", 'last-part'="Smith" and
 *          'first-part'="John James";
 *     <li> no commas, there exists lower-case token:
 *          'junior-part' is empty, 'first-part' consists of all upper-case
 *          tokens before the first lower-case token, 'von-part' consists of
 *          lower-case tokens starting the first lower-case token and ending
 *          the lower-case token that is followed by upper-case token,
 *          'last-part' consists of the rest of tokens;
 *          note that both 'first-part' and 'latst-part' may be empty and
 *          'last-part' may contain lower-case tokens; for example:
 *          in "von der", 'first-part'='last-part'="", 'von-part'="von der";
 *          in "Charles Louis Xavier Joseph de la Vall{\'e}e la Poussin",
 *          'first-part'="Charles Louis Xavier Joseph", 'von-part'="de la",
 *          'last-part'="Vall{\'e}e la Poussin";
 *     <li> one comma:
 *          'junior-part' is empty, 'first-part' consists of all tokens after comma,
 *          'von-part' consists of the longest sequence of lower-case tokens
 *          in the very beginning, 'last-part' consists of all tokens after
 *          'von-part' and before comma; note that any part can be empty;
 *          for example: in "de la Vall{\'e}e la Poussin, Charles Louis Xavier Joseph",
 *          'first-part'="Charles Louis Xavier Joseph", 'von-part'="de la",
 *          'last-part'="Vall{\'e}e la Poussin";
 *          in "Joseph de la Vall{\'e}e la Poussin, Charles Louis Xavier",
 *          'first-part'="Charles Louis Xavier", 'von-part'="",
 *          'last-part'="Joseph de la Vall{\'e}e la Poussin";
 *     <li> two or more commas (any comma after the second one is ignored;
 *          it merely separates tokens):
 *          'junior-part' consists of all tokens between first and second commas,
 *          'first-part' consists of all tokens after the second comma,
 *          tokens before the first comma are splitted into 'von-part' and
 *          'last-part' similarly to the case of one comma; for example:
 *          in "de la Vall{\'e}e Poussin, Jr., Charles Louis Xavier Joseph",
 *          'first-part'="Charles Louis Xavier Joseph", 'von-part'="de la",
 *          'last-part'="Vall{\'e}e la Poussin", and 'junior-part'="Jr.";
 *     </ul>
 * <li> when 'first-part', 'last-part', 'von-part', or 'junior-part' is reconstructed
 *      from tokens, tokens in a part are separated either by space or by dash,
 *      depending on whether the token before the separator was space-terminated
 *      or dash-terminated; for the last token in a part it does not matter
 *      whether it was dash- or space-terminated;
 * <li> when 'first-part' is abbreviated, each token is replaced by its abbreviation
 *      followed by a period; separators are the same as in the case of non-abbreviated
 *      name; for example: in "Heinrich-{\"{U}}bel Kurt von Minich",
 *      'first-part'="Heinrich-{\"{U}}bel Kurt", and its abbreviation is
 *      "H.-{\"{U}}. K."
 * </ol>
 */
private static class AuthorList {
    // This is the only meaningful field after construction of the object
    private Vector authors;     // of Author 
    
    // The following variables are used only during parsing
    
    private String orig;        // the raw bibtex author/editor field
    // the following variables are updated by getToken procedure
    private int token_start;    // index in orig
    private int token_end;      // to point 'abc' in '  abc xyz', start=2 and end=5
    // the following variables are valid only if getToken returns TOKEN_WORD
    private int token_abbr;     // end of token abbreviation (always: token_start < token_abbr <= token_end)
    private char token_term;    // either space or dash
    private boolean token_case; // true if upper-case token, false if lower-case token
    
    // Tokens of one author name.
    // Each token occupies TGL consecutive entries in this vector (as described below)
    private Vector tokens;
    private static final int TOKEN_GROUP_LENGTH = 4;   // number of entries for a token
    // the following are offsets of an entry in a group of entries for one token
    private static final int OFFSET_TOKEN = 0;         // String -- token itself;
    private static final int OFFSET_TOKEN_ABBR = 1;    // String -- token abbreviation;
    private static final int OFFSET_TOKEN_TERM = 2;    // Character -- token terminator (either " " or "-")
    private static final int OFFSET_TOKEN_CASE = 3;    // Boolean -- true=uppercase, false=lowercase
    // the following are indices in 'tokens' vector created during parsing of author name
    // and later used to properly split author name into parts
    int von_start,      // first lower-case token (-1 if all tokens upper-case)
        last_start,     // first upper-case token after first lower-case token (-1 if does not exist)
        comma_first,    // token after first comma (-1 if no commas)
        comma_second;   // token after second comma (-1 if no commas or only one comma)
    
    // Token types (returned by getToken procedure)
    private static final int TOKEN_EOF = 0;    
    private static final int TOKEN_AND = 1;
    private static final int TOKEN_COMMA = 2;
    private static final int TOKEN_WORD = 3;
    
    // Constant Hashtable containing names of TeX special characters
    private static final java.util.Hashtable tex_names = new java.util.Hashtable();
    // and static constructor to initialize it
    static {
        tex_names.put("aa","aa");   // only keys are important in this table
        tex_names.put("ae","ae");
        tex_names.put("l","l");
        tex_names.put("o","o");
        tex_names.put("oe","oe");
        tex_names.put("i","i");
        tex_names.put("AA","AA");
        tex_names.put("AE","AE");
        tex_names.put("L","L");
        tex_names.put("O","O");
        tex_names.put("OE","OE");
        tex_names.put("j","j");
    }
    
    /**
     * Parses the parameter strings and stores preformatted author information.
     * @param bibtex_authors contents of either <CODE>author</CODE> or
     * <CODE>editor</CODE> bibtex field.
     */
    public AuthorList (String bibtex_authors) {
        authors = new Vector(5);        // 5 seems to be reasonable initial size
        orig = bibtex_authors;              // initialization 
        token_start = 0; token_end = 0;     // of parser
        while (token_start < orig.length()) {
            Author author = getAuthor();
            if (author != null) authors.add(author);
        };
        // clean-up
        orig = null; tokens = null;
    }
    
    /**
     * Parses one author name and returns preformatted information.
     * @return Preformatted author name; <CODE>null</CODE> if author name is empty.
     */
    private Author getAuthor() {
        tokens = new Vector();      // initialization
        von_start = -1;  last_start = -1;  comma_first = -1;  comma_second = -1;
        
        // First step: collect tokens in 'tokens' Vector and calculate indices
    token_loop:
        while (true) {
            int token = getToken();
    cases:    switch (token) {
                case TOKEN_EOF : case TOKEN_AND : break token_loop;
                case TOKEN_COMMA :
                    if (comma_first < 0) comma_first = tokens.size();
                    else if (comma_second < 0) comma_second = tokens.size();
                    break cases;
                case TOKEN_WORD :
                    tokens.add(orig.substring(token_start, token_end));
                    tokens.add(orig.substring(token_start, token_abbr));
                    tokens.add(new Character(token_term));
                    tokens.add(new Boolean(token_case));
                    if (comma_first >= 0) break cases;
                    if (last_start >= 0) break cases;
                    if (von_start < 0) {
                        if (!token_case) {
                            von_start = tokens.size()-TOKEN_GROUP_LENGTH; break cases;
                        };
                    } else if (last_start < 0 && token_case) {
                        last_start = tokens.size()-TOKEN_GROUP_LENGTH; break cases;
                    };
            };
        }; // end token_loop
        
        // Second step: split name into parts (here: calculate indices
        // of parts in 'tokens' Vector)
        if (tokens.size()==0) return null;  // no author information
        
        // the following negatives indicate absence of the corresponding part
        int first_part_start=-1, von_part_start=-1, last_part_start=-1, jr_part_start=-1;
        int first_part_end=0, von_part_end=0, last_part_end=0, jr_part_end=0;
        if (comma_first<0) {            // no commas
            if (von_start<0) {              // no 'von part'
                last_part_end = tokens.size();
                last_part_start = tokens.size() - TOKEN_GROUP_LENGTH;
                first_part_end = last_part_start;
                if (first_part_end>0) first_part_start = 0;
            } else {                        // 'von part' is present
                if (last_start>=0) {
                    last_part_end = tokens.size();
                    last_part_start = last_start;
                    von_part_end = last_part_start;
                } else {
                    von_part_end = tokens.size();
                };
                von_part_start = von_start;
                first_part_end = von_part_start;
                if (first_part_end>0) first_part_start = 0;
            };
        } else {    // commas are present: it affects only 'first part' and 'junior part'
            first_part_end = tokens.size();
            if (comma_second<0) {    // one comma
                if (comma_first < first_part_end) first_part_start = comma_first;
            } else {                 // two or more commas
                if (comma_second < first_part_end) first_part_start = comma_second;
                jr_part_end = comma_second;
                if (comma_first < jr_part_end) jr_part_start = comma_first;
            };
            if (von_start!=0) {     // no 'von part'
                last_part_end = comma_first;
                if (last_part_end>0) last_part_start = 0;
            } else {                // 'von part' is present
                if (last_start<0) {
                    von_part_end = comma_first;
                } else {
                    last_part_end = comma_first;
                    last_part_start = last_start;
                    von_part_end = last_part_start;
                };
                von_part_start = 0;
            };
        };
        
        // Third step: do actual splitting, construct Author object
        return new Author(
          (first_part_start<0 ? null : concatTokens(first_part_start,first_part_end,OFFSET_TOKEN,false)),
          (first_part_start<0 ? null : concatTokens(first_part_start,first_part_end,OFFSET_TOKEN_ABBR,true)),
          (  von_part_start<0 ? null : concatTokens(  von_part_start,  von_part_end,OFFSET_TOKEN,false)),
          ( last_part_start<0 ? null : concatTokens( last_part_start, last_part_end,OFFSET_TOKEN,false)),
          (   jr_part_start<0 ? null : concatTokens(   jr_part_start,   jr_part_end,OFFSET_TOKEN,false))
        );
    }
    
    /**
     * Concatenates list of tokens from 'tokens' Vector.
     * Tokens are separated by spaces or dashes, dependeing on stored in 'tokens'.
     * Callers always ensure that start < end; thus, there exists at least
     * one token to be concatenated.
     * @param start index of the first token to be concatenated in 'tokens' Vector
     * (always divisible by TOKEN_GROUP_LENGTH).
     * @param end index of the first token not to be concatenated in 'tokens' Vector
     * (always divisible by TOKEN_GROUP_LENGTH).
     * @param offset offset within token group (used to request concatenation of
     * either full tokens or abbreviation).
     * @param dot_after <CODE>true</CODE> -- add period after each token, 
     * <CODE>false</CODE> -- do not add.
     * @return the result of concatenation.
     */
    private String concatTokens(int start, int end, int offset, boolean dot_after) {
        StringBuilder res = new StringBuilder();
        // Here we always have start < end
        res.append((String) tokens.get(start+offset));
        if (dot_after) res.append('.');
        start += TOKEN_GROUP_LENGTH;
        while (start < end) { 
            res.append((Character) tokens.get(start-TOKEN_GROUP_LENGTH+OFFSET_TOKEN_TERM)); 
            res.append((String) tokens.get(start+offset)); 
            if (dot_after) res.append('.');
            start += TOKEN_GROUP_LENGTH; 
        };
        return res.toString();
    }
    
    /**
     * Parses the next token.
     * <p>
     * The string being parsed is stored in global variable <CODE>orig</CODE>,
     * and position which parsing has to start from is stored in global
     * variable <CODE>token_end</CODE>; thus, <CODE>token_end</CODE> has to be
     * set to 0 before the first invocation. Procedure updates <CODE>token_end</CODE>;
     * thus, subsequent invocations do not require any additional variable settings.
     * <p>
     * The type of the token is returned; if it is <CODE>TOKEN_WORD</CODE>, additional
     * information is given  in global variables <CODE>token_start</CODE>, <CODE>token_end</CODE>,
     * <CODE>token_abbr</CODE>, <CODE>token_term</CODE>, and <CODE>token_case</CODE>;
     * namely: <CODE>orig.substring(token_start,token_end)</CODE> is the thext of the token,
     * <CODE>orig.substring(token_start,token_abbr)</CODE> is the token abbreviation,
     * <CODE>token_term</CODE> contains token terminator (space or dash),
     * and <CODE>token_case</CODE> is <CODE>true</CODE>, if token is upper-case
     * and <CODE>false</CODE> if token is lower-case.
     * @return <CODE>TOKEN_EOF</CODE> -- no more tokens,
     * <CODE>TOKEN_COMMA</CODE> -- token is comma,
     * <CODE>TOKEN_AND</CODE> -- token is the word "and" (or "And", or "aND", etc.),
     * <CODE>TOKEN_WORD</CODE> -- token is a word; additional information is given
     * in global variables <CODE>token_start</CODE>, <CODE>token_end</CODE>,
     * <CODE>token_abbr</CODE>, <CODE>token_term</CODE>, and <CODE>token_case</CODE>.
     */
    private int getToken() {
        token_start = token_end;
        while (token_start < orig.length()) {
            char c = orig.charAt(token_start);
            if ( !(c=='~' || c=='-' || Character.isWhitespace(c)) ) break;
            token_start++;
        };
        token_end = token_start;
        if (token_start >= orig.length()) return TOKEN_EOF;
        if (orig.charAt(token_start)==',') { token_end++; return TOKEN_COMMA; };
        token_abbr = -1;
        token_term = ' ';
        token_case = true;
        int braces_level = 0;
        int current_backslash = -1;
        boolean first_letter_is_found = false;
        while (token_end < orig.length()) {
            char c = orig.charAt(token_end);
            if (c=='{') { braces_level++; };
            if (braces_level > 0) if (c=='}') braces_level--;
            if (first_letter_is_found && token_abbr<0 && braces_level==0) token_abbr = token_end;
            if (!first_letter_is_found && current_backslash<0 && Character.isLetter(c)) {
                token_case = Character.isUpperCase(c); first_letter_is_found = true;
            };
            if (current_backslash>=0 && !Character.isLetter(c)) {
                if (!first_letter_is_found) {
                    String tex_cmd_name = orig.substring(current_backslash+1, token_end);
                    if (tex_names.get(tex_cmd_name)!=null) {
                        token_case = Character.isUpperCase(tex_cmd_name.charAt(0));
                        first_letter_is_found = true;
                    }
                };
                current_backslash = -1;
            };
            if (c=='\\') current_backslash = token_end;
            if (braces_level==0) 
                if (c==',' || c=='~' || c=='-' || Character.isWhitespace(c)) break;
            token_end++;
        };
        if (token_abbr<0) token_abbr = token_end;
        if (token_end<orig.length() && orig.charAt(token_end)=='-') token_term='-';
        if (orig.substring(token_start,token_end).equalsIgnoreCase("and"))
            return TOKEN_AND; else return TOKEN_WORD;
    }
    
    /**
     * Returns the number of author names in this object.
     * @return the number of author names in this object.
     */
    public int size() { return authors.size(); }
    /**
     * Returns the <CODE>Author</CODE> object for ith author.
     * @param i index of the author (from 0 to <CODE>size()-1</CODE>).
     * @return the <CODE>Author</CODE> object.
     */
    public Author getAuthor(int i) { return (Author) authors.get(i); }
    /**
     * Returns the list of authors in "natbib" format.
     * <p>
     * "John Smith" ==> "Smith";
     * "John Smith and Black Brown, Peter" ==> "Smith and Black Brown";
     * "John von Neumann and John Smith and Black Brown, Peter" ==> "von Neumann et al.".
     * @return formatted list of authors.
     */
    public String getAuthorsNatbib() {
        StringBuilder res = new StringBuilder();
        if (size()>0) {
            res.append(getAuthor(0).getLastOnly());
            if (size()==2) {
                res.append(" and ");
                res.append(getAuthor(1).getLastOnly());
            } else if (size()>2) {
                res.append(" et al.");
            };
        }
        return res.toString();
    }
    /**
     * Returns the list of authors separated by commas with first names after last name;
     * first names are abbreviated or not depending on parameter.
     * If the list consists of three or more authors, "and" is inserted before
     * the last author's name.
     * <p>
     * "John Smith" ==> "Smith, John" or "Smith, J.";
     * "John Smith and Black Brown, Peter" ==>
     * "Smith, John and Black Brown, Peter" or "Smith, J. and Black Brown, P.";
     * "John von Neumann and John Smith and Black Brown, Peter" ==>
     * "von Neumann, John, Smith, John and Black Brown, Peter" or
     * "von Neumann, J., Smith, J. and Black Brown, P.".
     * @param abbr <CODE>true</CODE> -- abbreviate first names,
     * <CODE>false</CODE> -- do not abbreviate.
     * @return formatted list of authors.
     */
    public String getAuthorsLastFirst(boolean abbr) {
        StringBuilder res = new StringBuilder();
        if (size()>0) { 
            res.append(getAuthor(0).getLastFirst(abbr));
            int i = 1;
            while (i < size()-1) {
                res.append(", ");
                res.append(getAuthor(i).getLastFirst(abbr));
                i++;
            };
            if (size() > 2) res.append(",");
            if (size() > 1) {
                res.append(" and ");
                res.append(getAuthor(i).getLastFirst(abbr));
            }
        };
        return res.toString();
    }
    /**
     * Returns the list of authors separated by "and"s with first names after last name;
     * first names are not abbreviated.
     * <p>
     * "John Smith" ==> "Smith, John";
     * "John Smith and Black Brown, Peter" ==> "Smith, John and Black Brown, Peter";
     * "John von Neumann and John Smith and Black Brown, Peter" ==>
     * "von Neumann, John and Smith, John and Black Brown, Peter".
     * @return formatted list of authors.
     */
    public String getAuthorsLastFirstAnds() {
        StringBuilder res = new StringBuilder();
        if (size()>0) {
            res.append(getAuthor(0).getLastFirst(false));
            for (int i=1; i<size(); i++) {
                res.append(" and ");
                res.append(getAuthor(i).getLastFirst(false));
            };
        };
        return res.toString();
    }
    /**
     * Returns the list of authors separated by commas with first names before last name;
     * first names are abbreviated or not depending on parameter.
     * If the list consists of three or more authors, "and" is inserted before
     * the last author's name.
     * <p>
     * "John Smith" ==> "John Smith" or "J. Smith";
     * "John Smith and Black Brown, Peter" ==>
     * "John Smith and Peter Black Brown" or "J. Smith and P. Black Brown";
     * "John von Neumann and John Smith and Black Brown, Peter" ==>
     * "John von Neumann, John Smith and Peter Black Brown" or
     * "J. von Neumann, J. Smith and P. Black Brown".
     * @param abbr <CODE>true</CODE> -- abbreviate first names,
     * <CODE>false</CODE> -- do not abbreviate.
     * @return formatted list of authors.
     */
    public String getAuthorsFirstLast(boolean abbr) {
        StringBuilder res = new StringBuilder();
        if (size()>0) { 
            res.append(getAuthor(0).getFirstLast(abbr));
            int i = 1;
            while (i < size()-1) {
                res.append(", ");
                res.append(getAuthor(i).getFirstLast(abbr));
                i++;
            };
            if (size() > 2) res.append(",");
            if (size() > 1) { 
                res.append(" and "); 
                res.append(getAuthor(i).getFirstLast(abbr)); 
            };
        };
        return res.toString();
    }
    /**
     * Returns the list of authors separated by "and"s with first names before last name;
     * first names are not abbreviated.
     * <p>
     * "John Smith" ==> "John Smith";
     * "John Smith and Black Brown, Peter" ==> "John Smith and Peter Black Brown";
     * "John von Neumann and John Smith and Black Brown, Peter" ==>
     * "John von Neumann and John Smith and Peter Black Brown".
     * @return formatted list of authors.
     */
    public String getAuthorsFirstLastAnds() {
        StringBuilder res = new StringBuilder();
        if (size()>0) { 
            res.append(getAuthor(0).getFirstLast(false));
            for (int i=1; i<size(); i++) {
                res.append(" and ");
                res.append(getAuthor(i).getFirstLast(false));
            };
        };
        return res.toString();
    }
/**
 *  This is an immutable class that keeps information regarding single author.
 *  It is just a container for the information, with very simple methods
 *  to access it.
 *  <p>
 *  Current usage: only methods <code>getLastOnly</code>,
 *  <code>getFirstLast</code>, and <code>getLastFirst</code> are used;
 *  all other methods are provided for completeness.
 */
private static class Author {
    private final String first_part;
    private final String first_abbr;
    private final String von_part;
    private final String last_part;
    private final String jr_part;
    /**
     * Creates the Author object.
     * If any part of the name is absent, <CODE>null</CODE> must be passes;
     * otherwise other methods may return erroneous results.
     * @param first the first name of the author (may consist of several tokens,
     * like "Charles Louis Xavier Joseph" in 
     * "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param firstabbr the abbreviated first name of the author (may consist
     * of several tokens, like "C. L. X. J." in 
     * "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin").
     * It is a responsibility of the caller to create a reasonable
     * abbreviation of the first name.
     * @param von the von part of the author's name (may consist of
     * several tokens, like "de la" in 
     * "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param last the lats name of the author (may consist of several tokens,
     * like "Vall{\'e}e Poussin" in 
     * "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param jr the junior part of the author's name (may consist of
     * several tokens, like "Jr. III" in 
     * "Smith, Jr. III, John")
     */
    public Author(String first, String firstabbr, String von, String last, String jr) {
        first_part = first;
        first_abbr = firstabbr;
        von_part = von;
        last_part = last;
        jr_part = jr;
    }
    /**
     * Retunrns the first name of the author stored in this object.
     * @return first name of the author (may consist of several tokens)
     */
    public String getFirst() { return first_part; }
    /**
     * Retunrns the abbreviated first name of the author stored in this object.
     * @return abbreviated first name of the author (may consist of several tokens)
     */
    public String getFirstAbbr() { return first_abbr; }
    /**
     * Retunrns the von part of the author's name stored in this object.
     * @return von part of the author's name (may consist of several tokens)
     */
    public String getVon() { return von_part; }
    /**
     * Retunrns the last name of the author stored in this object.
     * @return last name of the author (may consist of several tokens)
     */
    public String getLast() { return last_part; }
    /**
     * Retunrns the junior part of the author's name stored in this object.
     * @return junior part of the author's name (may consist of several tokens)
     */
    public String getJr() { return jr_part; }
    /**
     * Returns von part followed by last name.
     * If both fields were specified as <CODE>null</CODE>,
     * the empty string <CODE>""</CODE> is returned.
     * @return 'von Last'
     */
    public String getLastOnly() {
        if (von_part==null) {
            return (last_part==null ? "" : last_part);
        } else {
            return (last_part==null ? von_part : von_part + " " + last_part);
        }
    }
    /**
     * Returns the author's name in form 'von Last, Jr., First' with the first name
     * full or abbreviated depending on parameter.
     * @param abbr <CODE>true</CODE> - abbreviate first name,
     * <CODE>false</CODE> - do not abbreviate
     * @return 'von Last, Jr., First' (if <CODE>abbr==false</CODE>) or
     * 'von Last, Jr., F.' (if <CODE>abbr==true</CODE>)
     */
    public String getLastFirst(boolean abbr) {
        String res = getLastOnly();
        if (jr_part != null) res += ", " + jr_part;
        if (abbr) {
            if (first_abbr != null) res += ", " + first_abbr;
        } else {
            if (first_part != null) res += ", " + first_part;
        }
        return res;
    }
    /**
     * Returns the author's name in form 'First von Last, Jr.' with the first name
     * full or abbreviated depending on parameter.
     * @param abbr <CODE>true</CODE> - abbreviate first name,
     * <CODE>false</CODE> - do not abbreviate
     * @return 'First von Last, Jr.' (if <CODE>abbr==false</CODE>) or
     * 'F. von Last, Jr.' (if <CODE>abbr==true</CODE>)
     */
    public String getFirstLast(boolean abbr) {
        String res = getLastOnly();
        if (abbr) {
            res = (first_abbr==null ? "" : first_abbr + " ") + res;
        } else {
            res = (first_part==null ? "" : first_part + " ") + res;
        };
        if (jr_part != null) res += ", " + jr_part;
        return res;
    }
}//end Author
}//end AuthorList
  

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

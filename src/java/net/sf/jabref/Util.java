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

import java.io.*;
import java.util.*;

import net.sf.jabref.imports.CiteSeerFetcher;
import net.sf.jabref.imports.ImportFormatReader;
import java.awt.*;
import java.util.regex.Pattern;
import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.groups.GroupSelector;

/**
 * Describe class <code>Util</code> here.
 *
 * @author <a href="mailto:"></a>
 * @version 1.0
 */
public class Util {

    // Colors are defined here.
    public static Color fieldsCol = new Color(180,180,200);

  // Integer values for indicating result of duplicate check (for entries):
    final static int TYPE_MISMATCH = -1,
        NOT_EQUAL = 0,
        EQUAL = 1,
        EMPTY_IN_ONE = 2,
        EMPTY_IN_TWO = 3;

    public static void bool(boolean b) {
        if (b) System.out.println("true");
        else System.out.println("false");
    }

    public static void pr(String s) {
        System.out.println(s);
    }

    public static void pr_(String s) {
        System.out.print(s);
    }

    public static String nCase(String s) {
        // Make first character of String uppercase, and the
        // rest lowercase.
        if (s.length() > 1)
          return s.substring(0,1).toUpperCase()
              + s.substring(1,s.length()).toLowerCase();
        else
          return s.toUpperCase();

    }

    public static String checkName(String s) {
        // Append '.bib' to the string unless it ends with that.
        String extension = s.substring(s.length()-4);
        if (!extension.equalsIgnoreCase(".bib"))
            return s+".bib";
        else
            return s;
    }

    public static String createId(BibtexEntryType type, BibtexDatabase database) {
        String s;
        do {
            s = type.getName()+(new Integer((int)(Math.random()*10000))).toString();
        } while (database.getEntryById(s) != null);
        return s;
    }

    private static int idCounter=0;
    public static String createNeutralId() {
        return ""+(idCounter++);
    }


    /**
     * This method sets the location of a Dialog such that it is centered with
     * regard to another window, but not outside the screen on the left and the top.
     */
    public static void placeDialog(javax.swing.JDialog diag,
                                   java.awt.Container win) {
        Dimension ds = diag.getSize(), df = win.getSize();
        Point pf = win.getLocation();
        diag.setLocation(new Point(Math.max(0, pf.x+(df.width-ds.width)/2),
                                   Math.max(0,pf.y+(df.height-ds.height)/2)));

    }

    /**
     * This method translates a field or string from Bibtex notation, with
     * possibly text contained in " " or { }, and string references, concatenated
     * by '#' characters, into Bibkeeper notation, where string references are
     * enclosed in a pair of '#' characters.
     */
    public static String parseField(String content) {
        if (content.length() == 0)
            return "";
        String toSet = "";
        boolean string;
        // Keeps track of whether the next item is
        // a reference to a string, or normal content. First we must
        // check which we begin with. We simply check if we can find
        // a '#' before either '"' or '{'.
        int hash = content.indexOf('#'),
            wr1 = content.indexOf('"'),
            wr2 = content.indexOf('{'),
            end = content.length();
        if (hash == -1) hash = end;
        if (wr1 == -1) wr1 = end;
        if (wr2 == -1) wr2 = end;
        if (((wr1 == end) && (wr2 == end)) || (hash < Math.min(wr1, wr2)))
            string = true;
        else string = false;

        //System.out.println("FileLoader: "+content+" "+string+" "+hash+" "+wr1+" "+wr2);
        StringTokenizer tok = new StringTokenizer(content, "#", true);
        // 'tok' splits at the '#' sign, and keeps delimiters

        while (tok.hasMoreTokens()) {
            String str = tok.nextToken();
            if (str.equals("#"))
                string = !string;
            else {
                if (string) {
                    // This part should normally be a string, but if it's
                    // a pure number, it is not.
                    String s = shaveString(str);
                    try {
                        Integer.parseInt(s);
                        // If there's no exception, it's a number.
                        toSet = toSet+s;
                    } catch (NumberFormatException ex) {
                        toSet = toSet+"#"+shaveString(str)+"#";
                    }

                }
                else
                    toSet = toSet+shaveString(str);
            }
        }
        return toSet;
    }

    public static String shaveString(String s) {
        // returns the string, after shaving off whitespace at the beginning
        // and end, and removing (at most) one pair of braces or " surrounding it.
        if (s == null)
            return null;
        char ch = 0, ch2 = 0;
        int beg = 0, end = s.length();
        // We start out assuming nothing will be removed.
        boolean begok = false, endok = false, braok = false;
        while (!begok) {
            if (beg < s.length()) {
                ch = s.charAt(beg);
                if (Character.isWhitespace(ch))
                    beg++;
                else
                    begok = true;
            } else begok = true;

        }
        while (!endok) {
            if (end > beg+1) {
                ch = s.charAt(end-1);
                if (Character.isWhitespace(ch))
                    end--;
                else
                    endok = true;
            } else
                endok = true;
        }

        //	while (!braok) {
        if (end > beg+1) {
            ch = s.charAt(beg);
            ch2 = s.charAt(end-1);
            if (((ch == '{') && (ch2 == '}')) ||
                ((ch == '"') && (ch2 == '"'))) {
                beg++;
                end--;
            }
        } //else
        //braok = true;

        //  } else
        //braok = true;
        //}

        s = s.substring(beg, end);
        //Util.pr(s);
        return s;
    }


    /**
     * This method returns a String similar to the one passed in,
     * except all whitespace and '#' characters are removed. These
     * characters make a key unusable by bibtex.
     */
    public static String checkLegalKey(String key) {
        if (key == null) return null;
        StringBuffer newKey = new StringBuffer();
        for (int i=0; i<key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && (c != '#') && (c != '{')
                && (c != '}') && (c != '~') && (c != ',') && (c != '^'))
                newKey.append(c);
        }

	String newKeyS = replaceSpecialCharacters(newKey.toString());

        return newKeyS;
    }

    public static String replaceSpecialCharacters(String s) {
	for (Iterator i=Globals.UNICODE_CHARS.keySet().iterator(); i.hasNext();) {
	    String chr = (String)i.next(),
		replacer = (String)Globals.UNICODE_CHARS.get(chr);
	    pr(chr+" "+replacer);
	    s = s.replaceAll(chr, replacer);
	}
	return s;
    }
    static public String wrap2(String in, int wrapAmount){
        StringBuffer out = new StringBuffer(in.replaceAll("[ \\t\\n\\r]+"," "));
        int p = in.length() - wrapAmount;
        while( p > 0 ){
            p = out.lastIndexOf(" ", p);
            if(p <= 0 || p <= 20)
                break;
            else{
                out.insert(p, "\n\t");
            }
            p -= wrapAmount;
        }
        return out.toString();
     }

    public static HashSet findDeliminatedWordsInField(BibtexDatabase db, String field, String deliminator) {
        HashSet res = new HashSet();
            Iterator i = db.getKeySet().iterator();
            while (i.hasNext()) {
                BibtexEntry be = db.getEntryById(i.next().toString());
                Object o = be.getField(field);
                if (o != null) {
                    String fieldValue = o.toString().trim();
                    String[] resultSet = fieldValue.split(deliminator);
                    for(int index=0; index<resultSet.length; index++)
                        res.add(resultSet[index].trim());
                }
            }
            return res;
    }

    /**
     * Returns a HashMap containing all words used in the database in the
     * given field type. Characters in @param remove are not included.
     * @param db a <code>BibtexDatabase</code> value
     * @param field a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>HashSet</code> value
     */
    public static HashSet findAllWordsInField(BibtexDatabase db,
                                              String field, String remove) {
        HashSet res = new HashSet();
        StringTokenizer tok;
        Iterator i = db.getKeySet().iterator();
        while (i.hasNext()) {
            BibtexEntry be = db.getEntryById(i.next().toString());
            Object o = be.getField(field);
            if (o != null) {
                tok = new StringTokenizer(o.toString(), remove, false);
                while (tok.hasMoreTokens())
                    res.add(tok.nextToken());
            }
        }
        return res;
    }

    /**
     * Takes a String array and returns a string with the array's
     * elements delimited by a certain String.
     *
     * @param strs String array to convert.
     * @param delimiter String to use as delimiter.
     * @return Delimited String.
     */
    public static String stringArrayToDelimited(String[] strs,
                                                String delimiter) {
        if ((strs == null) || (strs.length == 0))
            return "";
        if (strs.length == 1)
            return strs[0];
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<strs.length-1; i++) {
            sb.append(strs[i]);
            sb.append(delimiter);
        }
        sb.append(strs[strs.length-1]);
        return sb.toString();
    }


    /**
     * Takes a delimited string, splits it and returns
     *
     * @param names a <code>String</code> value
     * @return a <code>String[]</code> value
     */
    public static String[] delimToStringArray(String names,
                                              String delimiter) {
        if (names == null)
            return null;
        return names.split(delimiter);
    }


    /**
     * Open a http/pdf/ps viewer for the given link string.
     *
     */
    public static void openExternalViewer(String link, String fieldName, JabRefPreferences prefs) throws IOException
    {
      File file;

      if(fieldName.equals("pdf")) {
        String dir = prefs.get("pdfDirectory");
        file = expandFilename(link, dir);
      } else
        file = new File(link);

      if (fieldName.equals("ps") || fieldName.equals("pdf")) {
        // Check that the file exists:

        if ((file == null) || !file.exists()) {
          throw new IOException(Globals.lang("File not found") + ": '" + link +
                                "'.");
        }

	link = file.getPath();

        // Use the correct viewer even if pdf and ps are mixed up:
        String[] split = file.getName().split("\\.");
        if ((split.length >= 2) && (split[split.length-1].equalsIgnoreCase("pdf")))
          fieldName = "pdf";
        else if ((split.length >= 2) && (split[split.length-1].equalsIgnoreCase("ps")))
          fieldName = "ps";
      }
        String cmdArray[] = new String[2];
        // check html first since browser can invoke viewers
        if (fieldName.equals("doi"))
            {
                //cmdArray[0] = prefs.get("htmlviewer");
                //cmdArray[1] = Globals.DOI_LOOKUP_PREFIX+link;
                //Process child = Runtime.getRuntime().exec(cmdArray[0]+" "+cmdArray[1]);
                fieldName = "url";
                link = Globals.DOI_LOOKUP_PREFIX+link;
                //String[] cmd = {"/usr/bin/open", "-a", prefs.get("htmlviewer"), 
                //                Globals.DOI_LOOKUP_PREFIX+link};
                //Process child = Runtime.getRuntime().exec(cmd);
            }
        
        if(fieldName.equals("url") || fieldName.equals("citeseerurl"))
            { // html
                try
                {
                             if (fieldName.equals("citeseerurl")) {
                                 String canonicalLink = CiteSeerFetcher.generateCanonicalURL(link);
                                 if (canonicalLink != null)
                                     link = canonicalLink;
                             }
                  // First check if the url is enclosed in \\url{}. If so, remove the wrapper.
                  if (link.startsWith("\\url{") && link.endsWith("}"))
                    link = link.substring(5, link.length()-1);

                  //System.err.println("Starting HTML browser: "
                  //                   + prefs.get("htmlviewer") + " " + link);
                  if(Globals.ON_MAC){
                    String[] cmd = {"/usr/bin/open", "-a", prefs.get("htmlviewer"), link};
                    Process child = Runtime.getRuntime().exec(cmd);
                  } else if (Globals.ON_WIN) {
                    cmdArray[0] = prefs.get("htmlviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(
                    	cmdArray[0] + " " + cmdArray[1]);
                  } else{
                    cmdArray[0] = prefs.get("htmlviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(cmdArray);
                  }

                }
                catch (IOException e)
                {
                  System.err.println("An error occured on the command: "
                                     + prefs.get("htmlviewer") + " " + link);
                }
              }
              else if(fieldName.equals("ps"))
            {
                try
                    {
                        //System.err.println("Starting external viewer: "
                        //		   + prefs.get("psviewer") + " " + link);
                if(Globals.ON_MAC){
                    String[] cmd = {"/usr/bin/open", "-a", prefs.get("psviewer"), link};
                    Process child = Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    cmdArray[0] = prefs.get("psviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(
                       	cmdArray[0] + " " + cmdArray[1]);
                } else {
                    cmdArray[0] = prefs.get("psviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(cmdArray);
                }
                    }
                catch (IOException e)
                    {
                        System.err.println("An error occured on the command: "
                                           + prefs.get("psviewer") + " " + link);
                    }
            }
        else if(fieldName.equals("pdf"))
            {
                try
                    {
                        System.err.println("Starting external viewer: "
                                           + prefs.get("pdfviewer") + " " + link);
            if(Globals.ON_MAC){
                String[] cmd = {"/usr/bin/open", "-a", prefs.get("pdfviewer"), link};
                Process child = Runtime.getRuntime().exec(cmd);
            }
	    else if (Globals.ON_WIN) {
                String[] spl = link.split("\\\\");
                StringBuffer sb = new StringBuffer();
                for (int i=0; i<spl.length; i++) {
                    if (i>0)
                        sb.append("\\");
                    if (spl[i].indexOf(" ") >= 0)
                        spl[i] = "\""+spl[i]+"\"";
                    sb.append(spl[i]);
                    
                                       
                }
                //pr(sb.toString());
                link = sb.toString();

		String cmd = "cmd.exe /c start "+link;
		
                Process child = Runtime.getRuntime().exec(cmd);
	    }
            else{
                cmdArray[0] = prefs.get("pdfviewer");
                cmdArray[1] = link;
                //Process child = Runtime.getRuntime().exec(cmdArray[0]+" "+cmdArray[1]);
		Process child = Runtime.getRuntime().exec(cmdArray);
            }
                    }
                catch (IOException e)
                    {
                        e.printStackTrace();
                        System.err.println("An error occured on the command: "
                                           + prefs.get("pdfviewer") + " #" + link);
            System.err.println(e.getMessage());
                    }
            }
        else{
            System.err.println("Message: currently only PDF, PS and HTML files can be opened by double clicking");
            //ignore
        }
    }

  /**
   * Searches the given directory and subdirectories for a pdf file with name as given + ".pdf"
   */
  public static String findPdf(String key, String pdfDir) {
      String filename = key+".pdf";
      if (!pdfDir.endsWith(System.getProperty("file.separator")))
        pdfDir += System.getProperty("file.separator");
      String found = findInDir(filename, pdfDir);
      if (found != null)
        return found.substring(pdfDir.length());
      else
        return null;
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns null if
     * the file does not exist.
     */
    public static File expandFilename(String name, String dir) {
      File file = new File(name);
      if (!file.exists() && (dir != null))
      {
        if (dir.endsWith(System.getProperty("file.separator")))
          name = dir + name;
        else
          name = dir + System.getProperty("file.separator") + name;
        file = new File(name);
        if (file.exists())
            return file;
        // Ok, try to fix / and \ problems:
        if (Globals.ON_WIN)
            name = name.replaceAll("/", "\\");
        else
            name = name.replaceAll("\\\\", "/");
        file = new File(name);
        return (file.exists() ? file : null);
      }
      else
        return file;
    }

    private static String findInDir(String file, String dir) {
      File f = new File(dir, file);
      if (f.exists())
        return f.getPath();
      f = new File(dir);
      File[] all = f.listFiles();
      if (all == null)
        return null; // An error occured. We may not have permission to list the files.
      String found = null;
      int i=0;
      while ((i < all.length) && (found == null)) {
        if (all[i].isDirectory())
          found = findInDir(file, all[i].getPath());
        i++;
      }
      return found;
    }

  /**
   * Checks if the two entries represent the same publication.
   *
   * @param one BibtexEntry
   * @param two BibtexEntry
   * @return boolean
   */
  public static boolean isDuplicate(BibtexEntry one, BibtexEntry two, float threshold) {

    // First check if they are of the same type - a necessary condition:
    if (one.getType() != two.getType()) return false;

    // The check if they have the same required fields:
    String[] fields = one.getType().getRequiredFields();

    if (fields == null) return false ;

    float req = compareFieldSet(fields, one, two);
    fields = one.getType().getOptionalFields();

    if (fields != null) {
        float opt = compareFieldSet(fields, one, two);
        return (2*req + opt)/3 >= threshold;
    } else {
        return (req >= threshold);
    }
  }

  private static float compareFieldSet(String[] fields,
                                       BibtexEntry one, BibtexEntry two)
  {
    int res = 0;
    for (int i=0; i<fields.length; i++)
    {
      //Util.pr(":"+compareSingleField(fields[i], one, two));
      if (compareSingleField(fields[i], one, two) == EQUAL) {
        res++;
        //Util.pr(fields[i]);
      }
    }
    return ((float)res) / ((float)fields.length);
  }

  private static int compareSingleField(String field, BibtexEntry one, BibtexEntry two) {
    String s1 = (String)one.getField(field),
        s2 = (String)two.getField(field);
    if (s1 == null) {
      if (s2 == null) return EQUAL;
      else return EMPTY_IN_ONE;
    }
    else if (s2 == null) return EMPTY_IN_TWO;
    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();
    //Util.pr(field+": '"+s1+"' vs '"+s2+"'");
    if (field.equals("author") || field.equals("editor")) {
      // Specific for name fields.
      // Harmonise case:
      String[] aus1 = ImportFormatReader.fixAuthor_lastnameFirst(s1).split(" and "),
          aus2 = ImportFormatReader.fixAuthor_lastnameFirst(s2).split(" and "),
          au1 = aus1[0].split(","),
          au2 = aus2[0].split(",");

      // Can check number of authors, all authors or only the first.
      if ((aus1.length > 0) && (aus1.length == aus2.length) && au1[0].trim().equals(au2[0].trim()))
        return EQUAL;
      else return NOT_EQUAL;
    }
    else {
      if (s1.trim().equals(s2.trim()))
        return EQUAL;
      else return NOT_EQUAL;
    }

  }


  public static double compareEntriesStrictly(BibtexEntry one, BibtexEntry two) {
    HashSet allFields = new HashSet();//one.getAllFields());
    Object[] o = one.getAllFields();
    for (int i=0; i<o.length; i++)
      allFields.add(o[i]);
    o = two.getAllFields();
    for (int i=0; i<o.length; i++)
      allFields.add(o[i]);
    int score = 0;
    for (Iterator fld = allFields.iterator(); fld.hasNext();) {
      String field = (String)fld.next();
      Object en = one.getField(field),
          to = two.getField(field);
      if ((en != null) && (to != null) && (en.equals(to)))
        score++;
      else if ((en == null) && (to == null))
        score++;
    }
    if (score == allFields.size())
      return 1.01; // Just to make sure we can use score>1 without trouble.
    else
      return ((double)score)/allFields.size();
  }



  /**
     * This methods assures all words in the given entry are recorded
     * in their respective Completers, if any.
     */
    /*    public static void updateCompletersForEntry(Hashtable autoCompleters,
                                         BibtexEntry be) {

        for (Iterator j=autoCompleters.keySet().iterator();
             j.hasNext();) {
            String field = (String)j.next();
            Completer comp = (Completer)autoCompleters.get(field);
            comp.addAll(be.getField(field));
        }
        }*/


        /**
         * Sets empty or non-existing owner fields of bibtex entries inside an array to
         * a specified default value.
         * @param bibs array of bibtex entries
         * @param defaultOwner default owner of bibtex entries
         */
        public static void setDefaultOwner( ArrayList bibs, String defaultOwner )
        {

                // Iterate through all entries
                for (int i = 0; i < bibs.size(); i++)
                {
                        // Get current entry
                        BibtexEntry curEntry = (BibtexEntry)bibs.get(i);
                        // No or empty owner field?
                        if (curEntry.getField(Globals.OWNER) == null ||
                                ((String)curEntry.getField(Globals.OWNER)).length() == 0)
                        {
                                // Set owner field to default value
                                curEntry.setField(Globals.OWNER, defaultOwner);
                        }
                }
        }


        public static void printEntry(BibtexEntry entry) {
          StringWriter sw = new StringWriter();
          try {
            entry.write(sw, new LatexFieldFormatter(), false);
          } catch (IOException ex) {}
          pr(sw.toString());
        }

        /**
         * Copies a file.
         * @param source File Source file
         * @param dest File Destination file
         * @param deleteIfExists boolean Determines whether the copy goes on even if the file exists.
         * @returns boolean Whether the copy succeeded, or was stopped due to the file already existing.
         * @throws IOException
         */
        public static boolean copyFile(File source, File dest, boolean deleteIfExists)
            throws IOException
        {

          // Check if the file already exists.
          if (dest.exists()) {
            if (!deleteIfExists)
              return false;
            else
              dest.delete();
          }

          BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
          BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
          int el;
          while ((el = in.read()) >= 0)
            out.write(el);

          in.close();
          out.close();
          return true;
        }

        /**
         * Look for a group name in a groups vector
         * @param nameToFind String The group name to search for.
         * @param v Vector The vector to search in.
         * @return int The group number where the name was found, or -1 if not found.
         */
        public static int findGroup(String nameToFind, Vector v) {
          for (int i=GroupSelector.OFFSET; (i+2)<v.size(); i+=GroupSelector.DIM) {
            String name = (String)v.elementAt(i+1);
            if (name.equals(nameToFind))
              return i;
          }
          return -1;
        }

    /**
     * This method is called at startup, and makes necessary adaptations
     * to preferences for users from an earlier version of Jabref.
     */
    public static void performCompatibilityUpdate() {

        // Make sure "abstract" is not in General fields, because
        // Jabref 1.55 moves the abstract to its own tab.
        String genFields = Globals.prefs.get("generalFields");
        //pr(genFields+"\t"+genFields.indexOf("abstract"));
        if (genFields.indexOf("abstract") >= 0) {
            //pr(genFields+"\t"+genFields.indexOf("abstract"));
            String newGen = null;
            if (genFields.equals("abstract"))
                newGen = "";
            else if (genFields.indexOf(";abstract;") >= 0) {
                newGen = genFields.replaceAll(";abstract;", ";");
            } else if (genFields.indexOf("abstract;") == 0) {
                newGen = genFields.replaceAll("abstract;", "");
            } else if (genFields.indexOf(";abstract") == genFields.length()-9) {
                newGen = genFields.replaceAll(";abstract", "");
            }
            else
                newGen = genFields;
            //pr(newGen);
            Globals.prefs.put("generalFields", newGen);
        }

    }

// -------------------------------------------------------------------------------

    /** extends the filename with a default Extension, if no Extension '.x' could be found */
    public static String getCorrectFileName(String orgName, String defaultExtension)
    {
      if (orgName == null)
        return "" ;

      String back = orgName ;
      int t = orgName.indexOf(".", 1) ;  // hidden files Linux/Unix (?)
      if (t < 1)
        back = back +"." +defaultExtension ;

      return back ;
    }

}

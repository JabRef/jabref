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

import java.awt.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashSet;

/**
 * Describe class <code>Util</code> here.
 *
 * @author <a href="mailto:"></a>
 * @version 1.0
 */
public class Util {

    // Colors are defined here.
    public static Color fieldsCol = new Color(180,180,200);

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
	String res = s.substring(0,1).toUpperCase()
	    + s.substring(1,s.length()).toLowerCase();
	return res;
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
	StringBuffer newKey = new StringBuffer(); 
	for (int i=0; i<key.length(); i++) { 
	    char c = key.charAt(i); 
	    if (!Character.isWhitespace(c) & (c != '#'))
		newKey.append(c); 
	} 
	return newKey.toString();
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
     * Takes a 
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
    

}

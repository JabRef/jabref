/*
 * Created on 13-Dec-2003
 */
package net.sf.jabref.labelPattern;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.imports.ImportFormatReader;
import net.sf.jabref.Util;

/**
 *
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
/**
 * This is the utility class of the LabelPattern package.
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
public class LabelPatternUtil {
  //this is SO crappy, but i have no idea of converting unicode into a String
  // the content of the AL is build with the buildLetters()
  private static ArrayList letters = builtLetters();
  public static ArrayList DEFAULT_LABELPATTERN = split("[auth][year]");

  private static BibtexDatabase _db;

  /**
   * This method takes a string of the form [field1]spacer[field2]spacer[field3]...,
   * where the fields are the (required) fields of a BibTex entry. The string is split
   * into firlds and spacers by recognizing the [ and ].
   *
   * @param keyPattern a <code>String</code>
   * @return an <code>ArrayList</code> The first item of the list
   * is a string representation of the key pattern (the parameter),
   * the second item is the spacer character (a <code>String</code>).
   */
  public static ArrayList split(String labelPattern) {
    // A holder for fields of the entry to be used for the key
    ArrayList _alist = new ArrayList();

    // Before we do anything, we add the parameter to the ArrayLIst
    _alist.add(labelPattern);

    //String[] ss = labelPattern.split("\\[|\\]");
    StringTokenizer tok = new StringTokenizer(labelPattern, "[]", true);
    while (tok.hasMoreTokens()) {
      _alist.add(tok.nextToken());

    }
    return _alist;

    /*
       // Regular expresion for identifying the fields
       Pattern pi = Pattern.compile("\\[\\w*\\]");
       // Regular expresion for identifying the spacer
       Pattern ps = Pattern.compile("\\].()*\\[");

       // The matcher for the field
       Matcher mi = pi.matcher(labelPattern);
       // The matcher for the spacer char
       Matcher ms = ps.matcher(labelPattern);

       // Before we do anything, we add the parameter to the ArrayLIst
       _alist.add(labelPattern);

       // If we can find the spacer character
       if(ms.find()){
     String t_spacer = ms.group();
      // Remove the `]' and `[' at the ends
      // We cant imagine a spacer of omre than one character.
      t_spacer = t_spacer.substring(1,2);
      _alist.add(t_spacer);
       }

       while(mi.find()){
     // Get the matched string
     String t_str = mi.group();
      int _sindex = 1;
      int _eindex = t_str.length() -1;
      // Remove the `[' and `]' at the ends
      t_str = t_str.substring(_sindex, _eindex);
     _alist.add(t_str);
       }

       return _alist;*/
  }

  /**
   * Generates a BibTeX label according to the pattern for a given entry type, and
   * returns the <code>Bibtexentry</code> with the unique label.
   * @param table a <code>LabelPattern</code>
   * @param database a <code>BibtexDatabase</code>
   * @param entryId a <code>String</code>
   * @return modified Bibtexentry
   */
  public static BibtexEntry makeLabel(LabelPattern table,
                                      BibtexDatabase database,
                                      BibtexEntry _entry) {
    _db = database;
    ArrayList _al;
    String _spacer, _label;
    StringBuffer _sb = new StringBuffer();
    boolean forceUpper = false, forceLower = false;

    try {
      // get the type of entry
      String _type = _entry.getType().getName().toLowerCase();
      // Get the arrayList corrosponding to the type
      _al = table.getValue(_type);
      int _alSize = _al.size();
      boolean field = false;
      for (int i = 1; i < _alSize; i++) {
        String val = _al.get(i).toString();
        if (val.equals("[")) {
          field = true;
        }
        else if (val.equals("]")) {
          field = false;
        }
        else if (field) {
	    /* Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 13-Apr-2004
	       Added new pseudo-fields "shortyear" and "veryshorttitle", and
	       and ":lower" modifier for all fields (in a way easily extended to other modifiers).
	       Helpfile help/LabelPatterns.html updated accordingly.
	    */
	    // check whether there is a modifier on the end such as ":lower"
	    String modifier = null;
	    int _mi = val.indexOf(":");
	    if(_mi != -1 && _mi != val.length()-1 && _mi != 0) { // ":" is in val and isn't first or last character
		modifier=val.substring(_mi+1);
		val=val.substring(0,_mi);
	    }
	    StringBuffer _sbvalue = new StringBuffer();

	    try {
		/*if (val.equals("uppercase")) {
		    forceUpper = true;
		}
		else if (val.equals("lowercase")) {
		    forceLower = true;
		    }*/
		if (val.equals("auth")) {
		    _sbvalue.append(firstAuthor(_entry.getField("author").toString()));
		}
		else if (val.equals("edtr")) {
		    _sbvalue.append(firstAuthor(_entry.getField("editor").toString()));
		}
		else if (val.equals("authors")) {
		    _sbvalue.append(allAuthors(_entry.getField("author").toString()));
		}
		else if (val.equals("editors")) {
		    _sbvalue.append(allAuthors(_entry.getField("editor").toString()));
		}
		else if (val.equals("authorIni")) {
		    _sbvalue.append(oneAuthorPlusIni(_entry.getField("author").toString()));
		}
		else if (val.equals("editorIni")) {
		    _sbvalue.append(oneAuthorPlusIni(_entry.getField("editor").toString()));
		}
		else if (val.equals("firstpage")) {
		    _sbvalue.append(firstPage(_entry.getField("pages").toString()));
		}
		else if (val.equals("lastpage")) {
		    _sbvalue.append(lastPage(_entry.getField("pages").toString()));
		}
		else if (val.equals("shorttitle")) {
		    String ss = _entry.getField("title").toString();
		    int piv = 0, wrd = 0;
		    while ( (piv < ss.length()) && (wrd < 3)) {
			if (Character.isWhitespace(ss.charAt(piv))) {
			    wrd++;
			}
			else {
			    _sbvalue.append(ss.charAt(piv));
			}
			piv++;
		    }
		}
		else if (val.equals("shortyear")) {
		    String ss = _entry.getField("year").toString();
		    if (ss.length() > 2) {
			_sbvalue.append(ss.substring(ss.length() - 2));
		    }
		    else {
			_sbvalue.append(ss);
		    }
		}

		else if(val.equals("veryshorttitle")) {
		    String ss = _entry.getField("title").toString();
		    int piv=0;
		    String[] skipWords = {"a", "an", "the"};
		    // sorry for being English-centric. I guess these
		    // words should really be an editable preference.

		    for(int _i=0; _i< skipWords.length; _i++) {
			if(ss.toLowerCase().startsWith(skipWords[_i]+" ")) {
			    piv=skipWords[_i].length()+1;
			}
		    }
		    // skip multiple whitespaces:
		    while ((piv<ss.length()) && Character.isWhitespace(ss.charAt(piv))) {
			piv++;
		    }
		    // copy next word:
		    while ((piv<ss.length()) && !Character.isWhitespace(ss.charAt(piv))) {
			_sbvalue.append(ss.charAt(piv));
			piv++;
		    }
		}

		// authN.  First N chars of the first author's last name.
		else if ( Pattern.matches("^auth\\d+$", val ) ) {
			Pattern p = Pattern.compile("^auth(\\d+)$");
			Matcher m = p.matcher( val );
			m.matches(); // necessary
			int num = Integer.parseInt(m.group(1));
			String fa = firstAuthor(_entry.getField("author").toString());
			if ( num > fa.length() )
				num = fa.length();
		    _sbvalue.append(fa.substring(0,num));
		}

		// edtrN.  First N chars of the first editor's last name.
		else if ( Pattern.matches("^edtr\\d+$", val ) ) {
			Pattern p = Pattern.compile("^edtr(\\d+)$");
			Matcher m = p.matcher( val );
			m.matches(); // necessary
			int num = Integer.parseInt(m.group(1));
			String fa = firstAuthor(_entry.getField("editor").toString());
			if ( num > fa.length() )
				num = fa.length();
		    _sbvalue.append(fa.substring(0,num));
		}

		// we havent seen any special demands
		else {
		    _sbvalue.append(_entry.getField(val).toString());
		}
	    }
	    catch (Exception ex) {
		Globals.logger("Key generator warning: field '" + val + "' empty.");
	    }
	    // apply modifier if present
	    if(modifier != null) {
		if(modifier.equals("lower")) {
		    _sb.append(_sbvalue.toString().toLowerCase());
		}
		else {
		    Globals.logger("Key generator warning: unknown modifier '"+modifier+"'.");
		}
	    } else {
		// no modifier
		_sb.append(_sbvalue);
	    }

        }
        else {
          _sb.append(val);
        }
      }
    }

    catch (Exception e) {
      System.err.println(e);
    }

    /**
     * Edited by Morten Alver 2004.02.04.
     *
     * We now have a system for easing key duplicate prevention, so
     * I am changing this method to conform to it.
     *

        // here we make sure the key is unique
       _label = makeLabelUnique(_sb.toString());
       _entry.setField(Globals.KEY_FIELD, _label);
       return _entry;
     */

    // Remove all illegal characters from the key.
    _label = Util.checkLegalKey(_sb.toString());

    if (forceUpper) {
      _label = _label.toUpperCase();
    }
    if (forceLower) {
      _label = _label.toLowerCase();

      // Try new keys until we get a unique one:
    }
    if (_db.setCiteKeyForEntry(_entry.getId(), _label)) {
      char c = 'b';
      String modKey = _label + "a";
      while (_db.setCiteKeyForEntry(_entry.getId(), modKey)) {
        modKey = _label + ( (char) (c++));
      }
    }
    return _entry;
    /** End of edit, Morten Alver 2004.02.04.  */

  }


  /**
   * This method returns a truely unique label (in the BibtexDatabase), by taking a
   * label and add the letters a-z until a unique key is found.
   * @param key a <code>String</code>
   * @return a unique label
   */
  public static String makeLabelUnique(String label) {
    // First I tried to make this recursive, but had to give up. I needed to
    // do too many chacks of different kinds.
    String _orgLabel = label;
    String _newLabel = label;
    int lettersSize = letters.size();

    for (int i = 0; i < lettersSize; i++) {
      if (isLabelUnique(_newLabel)) {
        // Hurray! the key is unique! lets get outta here
        break;
      }
      else {
        // though luck! lets add a new letter...
        _newLabel = _orgLabel + letters.get(i).toString();
      }
    }
    return _newLabel;

  }

  /**
   * Tests whether a given label is unique.
   * @param label a <code>String</code>
   * @return <code>true</code> if and only if the <code>label</code> is unique
   */
  public static boolean isLabelUnique(String label) {
    boolean _isUnique = true;
    BibtexEntry _entry;
    int _dbSize = _db.getEntryCount();
    // run through the whole DB and check the key field
    // if this could be made recursive I would be very happy
    // it kinda sux that we have to run through the whole db.
    // The idea here is that if we meet NO match, the _duplicate
    // field will be true

    for (int i = 0; i < _dbSize; i++) {
      _entry = _db.getEntryById(String.valueOf(i));

      // oh my! there is a match! we better set the uniqueness to false
      // and leave this for-loop all together
      if (_entry.getField(Globals.KEY_FIELD).equals(label)) {
        _isUnique = false;
        break;
      }
    }

    return _isUnique;

  }

  /**
   * Gets the last name of the first author/editor
   * @param authorField a <code>String</code>
   * @return the sur name of an author/editor
   */
  private static String firstAuthor(String authorField) {
    String author = "";
    // This code was part of 'ApplyRule' in 'ArticleLabelRule'
    String[] tokens = authorField.split("\\band\\b");
    if (tokens.length > 0) { // if author is empty
      if (tokens[0].indexOf(",") > 0) {
        tokens[0] = ImportFormatReader.fixAuthor(tokens[0]); // convert lastname, firstname to firstname lastname

      }
      String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").split(" ");
      // lastname, firstname

      author += firstAuthor[firstAuthor.length - 1];

    }
    return author;
  }

  /**
   * Gets the last name of all authors/editors
   * @param authorField a <code>String</code>
   * @return the sur name of all authors/editors
   */
  private static String allAuthors(String authorField) {
    String author = "";
    // This code was part of 'ApplyRule' in 'ArticleLabelRule'
    String[] tokens = authorField.split("\\band\\b");
    int i = 0;
    while (tokens.length > i) {
      // convert lastname, firstname to firstname lastname
      if (tokens[i].indexOf(",") > 0) {
        tokens[i] = ImportFormatReader.fixAuthor(tokens[i]);

      }
      String[] firstAuthor = tokens[i].replaceAll("\\s+", " ").split(" ");
      // lastname, firstname

      author += firstAuthor[firstAuthor.length - 1];
      i++;
    }
    return author;
  }

  /**
   * Gets the first part of the last name of the first
   * author/editor, and appends the last name initial of the
   * remaining authors/editors.
   * @param authorField a <code>String</code>
   * @return the sur name of all authors/editors
   */
  private static String oneAuthorPlusIni(String authorField) {
    final int CHARS_OF_FIRST = 5;
    authorField = ImportFormatReader.fixAuthor_lastnameFirst(authorField);
    String author = "";
    // This code was part of 'ApplyRule' in 'ArticleLabelRule'
    String[] tokens = authorField.split("\\band\\b");
    int i = 1;
    if (tokens.length == 0) {
      return author;
    }
    String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").split(" ");
    author = firstAuthor[0].substring(0,
                                      (int) Math.min(CHARS_OF_FIRST,
        firstAuthor[0].length()));
    while (tokens.length > i) {
      // convert lastname, firstname to firstname lastname
      author += tokens[i].trim().charAt(0);
      i++;
    }
    return author;

  }

  /**
   * Split the pages field into two and return the first one
   * @param pages a <code>String</code>
   * @return the first page number
   */
  private static String firstPage(String pages) {
    String[] _pages = pages.split("-");
    return _pages[0];
  }

  /**
   * Split the pages field into two and return the last one
   * @param pages a <code>String</code>
   * @return the last page number
   */
  private static String lastPage(String pages) {
    String[] _pages = pages.split("-");
    return _pages[1];
  }

  /**
   * I <b>HATE</b> this method!! I looked and looked but couldn't find a way to
   * turn 61 (or in real unicode 0061) into the letter 'a' - crap!
   * @return an <code>ArrayList</code> which shouldn't be!!
   */
  private static ArrayList builtLetters() {
    ArrayList _letters = new ArrayList();
    _letters.add("a");
    _letters.add("b");
    _letters.add("c");
    _letters.add("d");
    _letters.add("e");
    _letters.add("f");
    _letters.add("g");
    _letters.add("h");
    _letters.add("i");
    _letters.add("j");
    _letters.add("k");
    _letters.add("l");
    _letters.add("m");
    _letters.add("n");
    _letters.add("o");
    _letters.add("p");
    _letters.add("q");
    _letters.add("r");
    _letters.add("s");
    _letters.add("t");
    _letters.add("u");
    _letters.add("v");
    _letters.add("x");
    _letters.add("y");
    _letters.add("z");

    return _letters;
  }
}

/*
 * Created on 13-Dec-2003
 */
package net.sf.jabref.labelPattern;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.*;
import net.sf.jabref.export.layout.format.RemoveLatexCommands;

/**
 *
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
/**
 * This is the utility class of the LabelPattern package.
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
public class LabelPatternUtil {

    // All single characters that we can use for extending a key to make it unique:
    private static String CHARS = "abcdefghijklmnopqrstuvwxyz";

    public static ArrayList<String> DEFAULT_LABELPATTERN;
    static {
        updateDefaultPattern();
    }

    private static BibtexDatabase _db;

    public static void updateDefaultPattern() {
        DEFAULT_LABELPATTERN = split(JabRefPreferences.getInstance().get("defaultLabelPattern"));
    }

    /**
     * This method takes a string of the form [field1]spacer[field2]spacer[field3]...,
     * where the fields are the (required) fields of a BibTex entry. The string is split
     * into fields and spacers by recognizing the [ and ].
     *
     * @param labelPattern a <code>String</code>
     * @return an <code>ArrayList</code> The first item of the list
     * is a string representation of the key pattern (the parameter),
     * the second item is the spacer character (a <code>String</code>).
     */
    public static ArrayList<String> split(String labelPattern) {
        // A holder for fields of the entry to be used for the key
        ArrayList<String> _alist = new ArrayList<String>();

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
     * @param _entry a <code>BibtexEntry</code>
     * @return modified Bibtexentry
     */
    public static BibtexEntry makeLabel(LabelPattern table,
        BibtexDatabase database, BibtexEntry _entry) {
        _db = database;
        ArrayList<String> _al;
        String _label;
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
                } else if (val.equals("]")) {
                    field = false;
                } else if (field) {
                    /*
                     * Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 13-Apr-2004
                     * Added new pseudo-fields "shortyear" and "veryshorttitle",
                     * and and ":lower" modifier for all fields (in a way easily
                     * extended to other modifiers). Helpfile
                     * help/LabelPatterns.html updated accordingly.
                     */
                    // check whether there is a modifier on the end such as
                    // ":lower"
                    // String modifier = null;
                    String[] parts = parseFieldMarker(val);//val.split(":");

                    String label = makeLabel(_entry, parts[0]);
                    
                    // apply modifier if present
                    if (parts.length > 1)
                        label = applyModifiers(label, parts, 1);
                    
                    _sb.append(label);

                } else {
                    _sb.append(val);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }


        // Remove all illegal characters from the key.
        _label = Util.checkLegalKey(_sb.toString());

        // Patch by Toralf Senger:
        // Remove Regular Expressions while generating Keys
        String regex = Globals.prefs.get("KeyPatternRegex");
        if ((regex != null) && (regex.trim().length() > 0)) {
            String replacement = Globals.prefs.get("KeyPatternReplacement");
            _label = _label.replaceAll(regex, replacement);
        }

        if (forceUpper) {
            _label = _label.toUpperCase();
        }
        if (forceLower) {
            _label = _label.toLowerCase();
        }

        String oldKey = _entry.getCiteKey();
        int occurences = _db.getNumberOfKeyOccurences(_label);

        if ((oldKey != null) && oldKey.equals(_label))
            occurences--; // No change, so we can accept one dupe.

        if (occurences == 0) {
            // No dupes found, so we can just go ahead.
            if (!_label.equals(oldKey))
                _db.setCiteKeyForEntry(_entry.getId(), _label);

        } else {
            // The key is already in use, so we must modify it.
            int number = 0;

            String moddedKey = _label + getAddition(number);
            occurences = _db.getNumberOfKeyOccurences(moddedKey);

            if ((oldKey != null) && oldKey.equals(moddedKey))
                occurences--;

            while (occurences > 0) {
                number++;
                moddedKey = _label + getAddition(number);

                occurences = _db.getNumberOfKeyOccurences(moddedKey);
                if ((oldKey != null) && oldKey.equals(moddedKey))
                    occurences--;
            }

            if (!moddedKey.equals(oldKey)) {
                _db.setCiteKeyForEntry(_entry.getId(), moddedKey);
            }
        }

        return _entry;
        /** End of edit, Morten Alver 2004.02.04.  */
    }

    /**
     * Applies modifiers to a label generated based on a field marker.
     * @param label The generated label.
     * @param parts String array containing the modifiers.
     * @param offset The number of initial items in the modifiers array to skip.
     * @return The modified label.
     */
    public static String applyModifiers(String label, String[] parts, int offset) {
        if (parts.length > offset)
            for (int j = offset; j < parts.length; j++) {
                String modifier = parts[j];

                if (modifier.equals("lower")) {
                    label = label.toLowerCase();
                } else if (modifier.equals("upper")) {
                    label = label.toUpperCase();
                } else if (modifier.equals("abbr")) {
                    // Abbreviate - that is,
                    // System.out.println(_sbvalue.toString());
                    StringBuffer abbr = new StringBuffer();
                    String[] words = label.toString().replaceAll("[\\{\\}']","")
                            .split("[ \r\n\"]");
                    for (int word = 0; word < words.length; word++)
                        if (words[word].length() > 0)
                            abbr.append(words[word].charAt(0));
                    label = abbr.toString();

                } else if (modifier.startsWith("(") && modifier.endsWith(")")) {
                    // Alternate text modifier in parentheses. Should be inserted if
                    // the label is empty:
                    if (label.equals("") && (modifier.length() > 2))
                        return modifier.substring(1, modifier.length()-1);

                } else {
                    Globals
                        .logger("Key generator warning: unknown modifier '"
                            + modifier + "'.");
                }
            }
        return label;
    }

    public static String makeLabel(BibtexEntry _entry, String val) {

        try {
            if (val.startsWith("auth") || val.startsWith("pureauth")) {

                /*
                 * For label code "auth...": if there is no author, but there
                 * are editor(s) (e.g. for an Edited Book), use the editor(s)
                 * instead. (saw27@mrao.cam.ac.uk). This is what most people
                 * want, but in case somebody really needs a field which expands
                 * to nothing if there is no author (e.g. someone who uses both
                 * "auth" and "ed" in the same label), we provide an alternative
                 * form "pureauth..." which does not do this fallback
                 * substitution of editor.
                 */
                String authString = _entry.getField("author");

                if (val.startsWith("pure")) {
                    // remove the "pure" prefix so the remaining
                    // code in this section functions correctly
                    val = val.substring(4);
                } else {
                    if (authString == null || authString.equals("")) {
                        authString = _entry.getField("editor");
                    }
                }

                // Gather all author-related checks, so we don't
                // have to check all the time.
                if (val.equals("auth")) {
                    return firstAuthor(authString);
                } else if (val.equals("authors")) {
                    return allAuthors(authString);
                } else if (val.equals("authorsAlpha")) {
                	return authorsAlpha(authString);
                }
                // Last author's last name
                else if (val.equals("authorLast")) {
                    return lastAuthor(authString);
                } else if (val.equals("authorIni")) {
                    String s = oneAuthorPlusIni(authString);
                    return s == null ? "" : s;
                } else if (val.matches("authIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = authIniN(authString, num);
                    return s == null ? "" : s;
                } else if (val.equals("auth.auth.ea")) {
                    String s = authAuthEa(authString);
                    return s == null ? "" : s;
                } else if (val.equals("auth.etal")) {
                    String s = authEtal(authString);
                    return s == null ? "" : s;
                } else if (val.equals("authshort")) {
                    String s = authshort(authString);
                    return s == null ? "" : s;
                } else if (val.matches("auth[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = authN_M(authString, Integer.parseInt(nums[0]),
                        Integer.parseInt(nums[1]) - 1);
                    return s == null ? "" : s;
                } else if (val.matches("auth\\d+")) {
                    // authN. First N chars of the first author's last
                    // name.

                    int num = Integer.parseInt(val.substring(4));
                    String fa = firstAuthor(authString);
                    if (fa == null)
                        return "";
                    if (num > fa.length())
                        num = fa.length();
                    return fa.substring(0, num);
                } else if (val.matches("authors\\d+")) {
                    String s = NAuthors(authString, Integer.parseInt(val
                        .substring(7)));
                    return s == null ? "" : s;
                } else {
                    // This "auth" business was a dead end, so just
                    // use it literally:
                    return getField(_entry, val);
                }
            } else if (val.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                if (val.equals("edtr")) {
                    return firstAuthor(_entry.getField("editor").toString());
                } else if (val.equals("editors")) {
                    return allAuthors(_entry.getField("editor").toString());
                // Last author's last name
                } else if (val.equals("editorLast")) {
                    return lastAuthor(_entry.getField("editor").toString());
                } else if (val.equals("editorIni")) {
                    String s = oneAuthorPlusIni(_entry.getField("editor")
                        .toString());
                    return s == null ? "" : s;
                } else if (val.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = authIniN(_entry.getField("editor").toString(), num);
                    return s == null ? "" : s;
                } else if (val.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = authN_M(_entry.getField("editor").toString(),
                        Integer.parseInt(nums[0]),
                        Integer.parseInt(nums[1]) - 1);
                    return s == null ? "" : s;
                } else if (val.equals("edtr.edtr.ea")) {
                    String s = authAuthEa(_entry.getField("editor").toString());
                    return s == null ? "" : s;
                } else if (val.equals("edtrshort")) {
                    String s = authshort(_entry.getField("editor").toString());
                    return s == null ? "" : s;
                }
                // authN. First N chars of the first author's last
                // name.
                else if (val.matches("edtr\\d+")) {
                    int num = Integer.parseInt(val.substring(4));
                    String fa = firstAuthor(_entry.getField("editor")
                        .toString());
                    if (fa == null)
                        return "";
                    if (num > fa.length())
                        num = fa.length();
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return getField(_entry, val);
                }
            } else if (val.equals("firstpage")) {
                return firstPage(_entry.getField("pages"));
            } else if (val.equals("lastpage")) {
                return lastPage(_entry.getField("pages"));
            } else if (val.equals("shorttitle")) {
                return getTitleWords(3, _entry);
            } else if (val.equals("shortyear")) {
                String ss = _entry.getField("year");
                if (ss.startsWith("in") || ss.startsWith("sub")) {
                    return "IP";
                } else if (ss.length() > 2) {
                    return ss.substring(ss.length() - 2);
                } else {
                    return ss;
                }
            } else if (val.equals("veryshorttitle")) {
                return getTitleWords(1, _entry);
            } else if (val.matches("keyword\\d+")) {
                StringBuilder sb = new StringBuilder();
                int num = Integer.parseInt(val.substring(7));
                String kw = getField(_entry, "keywords");
                if (kw != null) {
                    String[] keywords = kw.split("[,;]\\s*");
                    if ((num > 0) && (num < keywords.length))
                        sb.append(keywords[num - 1].trim());
                }
                return sb.toString();
            } else {
                // we havent seen any special demands
                return getField(_entry, val);
            }
        } catch (NullPointerException ex) {
            return "";
        }

    }

    /**
     * Look up a field of a BibtexEntry, returning its String value, or an
     * empty string if it isn't set.
     * @param entry The entry.
     * @param field The field to look up.
     * @return The field value.
     */
    private static String getField(BibtexEntry entry, String field) {
        Object o = entry.getField(field);
        return o != null ? (String)o : "";
    }

    /**
     * Computes an appendix to a BibTeX key that could make it unique. We use
     * a-z for numbers 0-25, and then aa-az, ba-bz, etc.
     * 
     * @param number
     *            The appendix number.
     * @return The String to append.
     */
    private static String getAddition(int number) {
        if (number >= CHARS.length()) {
            int lastChar = number % CHARS.length();
            return getAddition(number/CHARS.length()-1) + CHARS.substring(lastChar, lastChar+1);
        } else
            return CHARS.substring(number, number+1);
    }


    static String getTitleWords(int number, BibtexEntry _entry) {
        String ss = (new RemoveLatexCommands()).format(_entry.getField("title").toString());
        StringBuffer _sbvalue = new StringBuffer(),
        current;
        int piv=0, words = 0;

        // sorry for being English-centric. I guess these
        // words should really be an editable preference.
        mainl: while ((piv < ss.length()) && (words < number)) {
            current = new StringBuffer();
            // Get the next word:
            while ((piv<ss.length()) && !Character.isWhitespace(ss.charAt(piv))) {
                current.append(ss.charAt(piv));
                piv++;
                //System.out.println(".. "+piv+" '"+current.toString()+"'");
            }
            piv++;
            // Check if it is ok:
            String word = current.toString().trim();
            if (word.length() == 0)
                continue mainl;
            for(int _i=0; _i< Globals.SKIP_WORDS.length; _i++) {
                if (word.equalsIgnoreCase(Globals.SKIP_WORDS[_i])) {
                    continue mainl;
                }
            }

            // If we get here, the word was accepted.
            if (_sbvalue.length() > 0)
                _sbvalue.append(" ");
            _sbvalue.append(word);
            words++;
        }

        return _sbvalue.toString();
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
            if (_entry.getField(BibtexFields.KEY_FIELD).equals(label)) {
                _isUnique = false;
                break;
            }
        }

        return _isUnique;

    }

    /**
     * Gets the last name of the first author/editor
     * 
     * @param authorField
     *            a <code>String</code>
     * @return the surname of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     * 
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthor(String authorField) {
        AuthorList al = AuthorList.getAuthorList(authorField);
        if (al.size() == 0)
            return "";
        String s = al.getAuthor(0).getLast();
        return s != null ? s : "";

    }

    /**
     * Gets the von part and the last name of the first author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the von part and surname of an author/editor or "" if no author was found.
     *  This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthorVonAndLast(String authorField) {
        AuthorList al = AuthorList.getAuthorList(authorField);
        if (al.size() == 0)
            return "";
        String s = al.getAuthor(0).getVon();
        StringBuilder sb = new StringBuilder();
        if (s != null) {
            sb.append(s);
            sb.append(' ');
        }
        s = al.getAuthor(0).getLast();
        if (s != null)
            sb.append(s);
        return sb.toString();
    }

    /**
     * Gets the last name of the last author/editor
     * @param authorField a <code>String</code>
     * @return the sur name of an author/editor
     */
    private static String lastAuthor(String authorField) {
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\band\\b");
        if (tokens.length > 0) { // if author is empty
            String[] lastAuthor = tokens[tokens.length-1].replaceAll("\\s+", " ").trim().split(" ");
            return lastAuthor[0];

        }
        else return "";
    }

    /**
     * Gets the last name of all authors/editors
     * @param authorField a <code>String</code>
     * @return the sur name of all authors/editors
     */
    private static String allAuthors(String authorField) {
        String author = "";
        // This code was part of 'ApplyRule' in 'ArticleLabelRule'
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\band\\b");
        int i = 0;
        while (tokens.length > i) {
            // convert lastname, firstname to firstname lastname
            String[] firstAuthor = tokens[i].replaceAll("\\s+", " ").trim().split(" ");
            // lastname, firstname
            author += firstAuthor[0];
            i++;
        }
        return author;
    }
    
    /**
     * Returns the authors according to the BibTeX-alpha-Style
     * @param authorField string containing the value of the author field
     * @return the initials of all authornames
     */
    private static String authorsAlpha(String authorField) {
    	String authors = "";
    	
    	String fixedAuthors = AuthorList.fixAuthor_lastNameOnlyCommas(authorField, false);
    	
    	// drop the "and" before the last author
    	// -> makes processing easier
    	fixedAuthors = fixedAuthors.replace(" and ", ", ");
    	
    	String[] tokens = fixedAuthors.split(",");
    	int max = (tokens.length > 4 ? 3 : tokens.length);
    	if (max==1) {
			String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").trim().split(" ");
			// take first letter of any "prefixes" (e.g. van der Aalst -> vd) 
			for (int j=0; j<firstAuthor.length-1; j++) {
				authors = authors.concat(firstAuthor[j].substring(0,1));
			}
			// append last part of last name completely
			authors = authors.concat(firstAuthor[firstAuthor.length-1].substring(0,
                    Math.min(3, firstAuthor[firstAuthor.length-1].length())));
    	} else {
    		for (int i = 0; i < max; i++) {
    			// replace all whitespaces by " "
    			// split the lastname at " "
    			String[] curAuthor = tokens[i].replaceAll("\\s+", " ").trim().split(" ");
    			for (int j=0; j<curAuthor.length; j++) {
    				// use first character of each part of lastname
    				authors = authors.concat(curAuthor[j].substring(0, 1));
    			}
    		}
    		if (tokens.length > 4) {
    			authors = authors.concat("+");
    		}
    	}
    	return authors;
    }

    /**
     * Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     * @param authorField a <code>String</code>
     * @param n the number of desired authors
     * @return Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     */
    private static String NAuthors(String authorField, int n) {
        String author = "";
        // This code was part of 'ApplyRule' in 'ArticleLabelRule'
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\band\\b");
        int i = 0;
        while (tokens.length > i && i < n) {
            // convert lastname, firstname to firstname lastname
            String[] firstAuthor = tokens[i].replaceAll("\\s+", " ").trim().split(" ");
            // lastname, firstname
            author += firstAuthor[0];
            i++;
        }
        if (tokens.length <= n) return author;
        return author += "EtAl";
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
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        String author = "";
        // This code was part of 'ApplyRule' in 'ArticleLabelRule'
        String[] tokens = authorField.split("\\band\\b");
        int i = 1;
        if (tokens.length == 0) {
            return author;
        }
        String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").split(" ");
        author = firstAuthor[0].substring(0,
            Math.min(CHARS_OF_FIRST,
                firstAuthor[0].length()));
        while (tokens.length > i) {
            // convert lastname, firstname to firstname lastname
            author += tokens[i].trim().charAt(0);
            i++;
        }
        return author;

    }

    /**
     * auth.auth.ea format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *  give:
     * Newton.Maxwell.ea
     * Newton.Maxwell
     */
    private static String authAuthEa(String authorField) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuffer author = new StringBuffer();

        String[] tokens = authorField.split("\\band\\b");
        if (tokens.length == 0) {
            return "";
        }
        author.append((tokens[0].split(","))[0]);
        if (tokens.length >= 2)
            author.append(".").append((tokens[1].split(","))[0]);
        if (tokens.length > 2)
            author.append(".ea");

        return author.toString();
    }

    /**
     * auth.etal format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *  give:
     * Newton.etal
     * Newton.Maxwell
     */
    private static String authEtal(String authorField) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuffer author = new StringBuffer();

        String[] tokens = authorField.split("\\band\\b");
        if (tokens.length == 0) {
            return "";
        }
        author.append((tokens[0].split(","))[0]);
        if (tokens.length == 2)
            author.append(".").append((tokens[1].split(","))[0]);
        else if (tokens.length > 2)
            author.append(".etal");

        return author.toString();
    }

    /**
     * The first N characters of the Mth author/editor.
     */
    private static String authN_M(String authorField, int n, int m) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = authorField.split("\\band\\b");
        if ((tokens.length <= m) || (n<0) || (m<0)) {
            return "";
        }
        String lastName = (tokens[m].split(","))[0].trim();
        if (lastName.length() <= n)
            return lastName;
        else
            return lastName.substring(0, n);
    }

    /**
     * authshort format:
     * added by Kolja Brix, kbx@users.sourceforge.net
     *
     * given author names
     * 
     *   Isaac Newton and James Maxwell and Albert Einstein and N. Bohr
     * 
     *   Isaac Newton and James Maxwell and Albert Einstein
     *   
     *   Isaac Newton and James Maxwell
     *   
     *   Isaac Newton
     * 
     * yield
     * 
     *   NME+
     *   
     *   NME
     *   
     *   NM
     *   
     *   Newton
     */
    private static String authshort(String authorField) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuffer author = new StringBuffer();
        String[] tokens = authorField.split("\\band\\b");
        int i = 0;

        if (tokens.length == 1) {

            author.append(authN_M(authorField,authorField.length(),0));

        } else if (tokens.length >= 2) {

            while (tokens.length > i && i<3) {
                author.append(authN_M(authorField,1,i));
                i++;
            }

            if (tokens.length > 3)
                author.append("+");

        }

        return author.toString();
    }

    /**
     * authIniN format:
     * 
     * Each author gets (N div #authors) chars, the remaining (N mod #authors)
     * chars are equally distributed to the authors first in the row.
     * 
     * If (N < #authors), only the first N authors get mentioned.
     * 
     * For example if
     * 
     * a) I. Newton and J. Maxwell and A. Einstein and N. Bohr (..)
     * 
     * b) I. Newton and J. Maxwell and A. Einstein
     * 
     * c) I. Newton and J. Maxwell
     * 
     * d) I. Newton
     * 
     * authIni4 gives: a) NMEB, b) NeME, c) NeMa, d) Newt
     * 
     * @param authorField
     *            The authors to format.
     * 
     * @param n
     *            The maximum number of characters this string will be long. A
     *            negative number or zero will lead to "" be returned.
     * 
     * @throws NullPointerException
     *             if authorField is null and n > 0
     */
    public static String authIniN(String authorField, int n) {
        
        if (n <= 0)
            return "";
        
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuffer author = new StringBuffer();
        String[] tokens = authorField.split("\\band\\b");
        int i = 0;
        int charsAll = n / tokens.length;

        if (tokens.length == 0) {
            return author.toString();
        }

        while (tokens.length > i) {
            if ( i < (n % tokens.length) ) {
                author.append(authN_M(authorField,charsAll+1,i));
            } else {
                author.append(authN_M(authorField,charsAll,i));
            }
            i++;
        }

        if (author.length() <= n)
            return author.toString();
        else
            return author.toString().substring(0, n);
    }


    /**
     * Split the pages field into separate numbers and return the lowest
     * 
     * @param pages
     *            (may not be null) a pages string such as 42--111 or
     *            7,41,73--97 or 43+
     * 
     * @return the first page number or "" if no number is found in the string
     * 
     * @throws NullPointerException
     *             if pages is null
     */
    public static String firstPage(String pages) {
        String[] _pages = pages.split("\\D+");
        int result = Integer.MAX_VALUE;
        for (String n : _pages){
            if (n.matches("\\d+"))
                result = Math.min(Integer.parseInt(n), result);
        }
        
        if (result == Integer.MAX_VALUE)
            return "";
        else 
            return String.valueOf(result);
    }

    /**
     * Split the pages field into separate numbers and return the highest
     * 
     * @param pages
     *            a pages string such as 42--111 or 7,41,73--97 or 43+
     * 
     * @return the first page number or "" if no number is found in the string
     * 
     * @throws NullPointerException
     *             if pages is null.
     */
    public static String lastPage(String pages) {
        String[] _pages = pages.split("\\D+");
        int result = Integer.MIN_VALUE;
        for (String n : _pages){
            if (n.matches("\\d+"))
                result = Math.max(Integer.parseInt(n), result);
        }
        
        if (result == Integer.MIN_VALUE)
            return "";
        else 
            return String.valueOf(result);
    }

    /**
         * Parse a field marker with modifiers, possibly containing a parenthesised modifier,
         * as well as escaped colons and parentheses.
         * @param arg The argument string.
         * @return An array of strings representing the parts of the marker
         */
        public static String[] parseFieldMarker(String arg) {
            List<String> parts = new ArrayList<String>();
            StringBuilder current = new StringBuilder();
            boolean escaped = false;
            int inParenthesis = 0;
            for (int i=0; i<arg.length(); i++) {
                if ((arg.charAt(i) == ':') && !escaped && (inParenthesis == 0)) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                } else if ((arg.charAt(i) == '(') && !escaped) {
                    inParenthesis++;
                    current.append(arg.charAt(i));
                } else if ((arg.charAt(i) == ')') && !escaped && (inParenthesis > 0)) {
                    inParenthesis--;
                    current.append(arg.charAt(i));
                } else if (arg.charAt(i) == '\\') {
                    if (escaped) {
                        escaped = false;
                        current.append(arg.charAt(i));
                    } else
                        escaped = true;
                } else if (escaped) {
                    current.append(arg.charAt(i));
                    escaped = false;
                } else
                    current.append(arg.charAt(i));
            }
            parts.add(current.toString());
            return parts.toArray(new String[parts.size()]);
        }

}

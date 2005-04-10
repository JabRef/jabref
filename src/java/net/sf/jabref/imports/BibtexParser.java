/*
Copyright (C) 2003 David Weitzman, Nizar N. Batada, Morten O. Alver

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

package net.sf.jabref.imports;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import net.sf.jabref.*;

public class BibtexParser
{
    private PushbackReader _in;
    private BibtexDatabase _db;
    private HashMap _meta, entryTypes;
    private boolean _eof = false;
    private int line = 1;

    public BibtexParser(Reader in)
    {
        if (in == null)
        {
            throw new NullPointerException();
        }

        _in = new PushbackReader(in);

    }

    private void skipWhitespace() throws IOException
    {
        int c;

        while (true)
        {
            c = read();
            if ((c == -1) || (c == 65535))
            {
                _eof = true;
                return;
            }

	    if (Character.isWhitespace((char) c))
            {
                continue;
            }
	    else
            // found non-whitespace char
	    //Util.pr("SkipWhitespace, stops: "+c);
            unread(c);
	    /*	    try {
		Thread.currentThread().sleep(500);
		} catch (InterruptedException ex) {}*/
            break;
        }
    }

    private String skipAndRecordWhitespace(int j) throws IOException
    {
        int c;
        StringBuffer sb = new StringBuffer();
        if (j != ' ')
            sb.append((char)j);
        while (true)
        {
            c = read();
            if ((c == -1) || (c == 65535))
            {
                _eof = true;
                return sb.toString();
            }

	    if (Character.isWhitespace((char) c))
            {
                if (c != ' ')
                    sb.append((char)c);
                continue;
            }
	    else
            // found non-whitespace char
	    //Util.pr("SkipWhitespace, stops: "+c);
        unread(c);
	    /*	    try {
		Thread.currentThread().sleep(500);
		} catch (InterruptedException ex) {}*/
            break;
        }
        return sb.toString();
    }


    public ParserResult parse() throws IOException {

        _db = new BibtexDatabase(); // Bibtex related contents.
	_meta = new HashMap();      // Metadata in comments for Bibkeeper.
	entryTypes = new HashMap(); // To store custem entry types parsed.
        ParserResult _pr = new ParserResult(_db, _meta, entryTypes);
        skipWhitespace();

        try
        {
            while (!_eof)
            {
		consumeUncritically('@');
		skipWhitespace();
		String entryType = parseTextToken();
		BibtexEntryType tp = BibtexEntryType.getType(entryType);
		boolean isEntry = (tp != null);
	       
		if (!isEntry) {
		    // The entry type name was not recognized. This can mean
		    // that it is a string, preamble, or comment. If so,
		    // parse and set accordingly. If not, assume it is an entry
		    // with an unknown type.
		    if (entryType.toLowerCase().equals("preamble")) {
			_db.setPreamble(parsePreamble());
		    }
		    else if (entryType.toLowerCase().equals("string")) {
			BibtexString bs = parseString();
			try {
			    _db.addString(bs);
			} catch (KeyCollisionException ex) {
			    _pr.addWarning(Globals.lang("Duplicate string name")+": "+bs.getName());
			    //ex.printStackTrace();
			}
		    }
		    else if (entryType.toLowerCase().equals("comment")) {
			StringBuffer commentBuf = parseBracketedTextExactly();
			/**
			 *
			 * Metadata are used to store Bibkeeper-specific
			 * information in .bib files.
			 *
			 * Metadata are stored in bibtex files in the format
			 * @comment{jabref-meta: type:data0;data1;data2;...}
			 *
			 * Each comment that starts with the META_FLAG is stored
			 * in the meta HashMap, with type as key.
			 * Unluckily, the old META_FLAG bibkeeper-meta: was used
			 * in JabRef 1.0 and 1.1, so we need to support it as
			 * well. At least for a while. We'll always save with the
			 * new one.
			 */
			String comment = commentBuf.toString().replaceAll("\\n","");
                        
			if (comment.substring(0, GUIGlobals.META_FLAG.length())
			    .equals(GUIGlobals.META_FLAG) ||
			    comment.substring(0, GUIGlobals.META_FLAG_OLD.length())
			    .equals(GUIGlobals.META_FLAG_OLD)) {
			    
			    String rest;
			    if (comment.substring(0, GUIGlobals.META_FLAG.length())
				.equals(GUIGlobals.META_FLAG))
				rest = comment.substring
				    (GUIGlobals.META_FLAG.length());
			    else
				rest = comment.substring
				    (GUIGlobals.META_FLAG_OLD.length());
			    
			    int pos = rest.indexOf(':');
			    
			    if (pos > 0)
				_meta.put
				    (rest.substring(0, pos), rest.substring(pos+1));
                                    // We remove all line breaks in the metadata - these will have been inserted
                                    // to prevent too long lines when the file was saved, and are not part of the data.
			}
			
			/**
			 * A custom entry type can also be stored in a @comment:
			 */
			if (comment.substring(0, GUIGlobals.ENTRYTYPE_FLAG.length())
			    .equals(GUIGlobals.ENTRYTYPE_FLAG)) {
			    
			    CustomEntryType typ = CustomEntryType.parseEntryType(comment.toString());
			    entryTypes.put(typ.getName().toLowerCase(), typ);

			}
		    }
		    else {
			// The entry type was not recognized. This may mean that
			// it is a custom entry type whose definition will appear
			// at the bottom of the file. So we use an UnknownEntryType
			// to remember the type name by.
			tp = new UnknownEntryType(entryType.toLowerCase());
			//System.out.println("unknown type: "+entryType); 
			isEntry = true;
		    }
		}

		if (isEntry) // True if not comment, preamble or string.
                {
		    //Util.pr("Found: "+tp.getName());
                    BibtexEntry be = parseEntry(tp);
                    boolean duplicateKey = _db.insertEntry(be);
                    if (duplicateKey)
                      _pr.addWarning(Globals.lang("Duplicate BibTeX key")+": "+be.getCiteKey());
		}

                skipWhitespace();
	}

	    // Before returning the database, update entries with unknown type
	    // based on parsed type definitions, if possible.
	    checkEntryTypes(_pr);

            return _pr;
        }
        catch (KeyCollisionException kce)
        {
          //kce.printStackTrace();
            throw new IOException("Duplicate ID in bibtex file: " +
                kce.toString());
        }
    }

    private int peek() throws IOException
    {
        int c = read();
        unread(c);

        return c;
    }

    private int read() throws IOException
    {
	int c = _in.read();
	if (c == '\n')
	    line++;
	return c;
    }

    private void unread(int c) throws IOException
    {
	if (c == '\n')
	    line--;
	_in.unread(c);
    }

    public BibtexString parseString() throws IOException
    {
	//Util.pr("Parsing string");
	skipWhitespace();
	consume('{','(');
	//while (read() != '}');
	skipWhitespace();
	//Util.pr("Parsing string name");
	String name = parseTextToken();
	//Util.pr("Parsed string name");
	skipWhitespace();
	//Util.pr("Now the contents");
        consume('=');
	String content = parseFieldContent(true);
	//Util.pr("Now I'm going to consume a }");
	consume('}',')');
	//Util.pr("Finished string parsing.");
	String id = Util.createNeutralId();
	return new BibtexString(id, name, content);
    }

    public String parsePreamble() throws IOException
    {
	return parseBracketedText().toString();
    }

    public BibtexEntry parseEntry(BibtexEntryType tp) throws IOException
    {
	String id = Util.createId(tp, _db);
	BibtexEntry result = new BibtexEntry(id, tp);

	skipWhitespace();
	consume('{','(');
	skipWhitespace();
	String key = null;
	boolean doAgain = true;
	while (doAgain) {
	    doAgain = false;
	    try {
		if (key != null)
		    key = key+parseKey();//parseTextToken(),
		else key = parseKey();                
	    } catch (NoLabelException ex) {
		// This exception will be thrown if the entry lacks a key
		// altogether, like in "@article{ author = { ...".
		// It will also be thrown if a key contains =.
	        char c = (char)peek();
		if (Character.isWhitespace(c) || (c == '{')
		    || (c == '\"')) {
                    String fieldName = ex.getMessage().trim().toLowerCase();
		    String cont = parseFieldContent(GUIGlobals.isStandardField(fieldName));
		    result.setField(fieldName, cont);
		} else {
		    if (key != null)
			key = key+ex.getMessage()+"=";
		    else key = ex.getMessage()+"=";
		    doAgain = true;
		}
	    }
	}
 
	if ((key != null) && key.equals(""))
	    key = null;
	if(result!=null)result.setField(GUIGlobals.KEY_FIELD, key);
//System.out.println(key+"");
	skipWhitespace();

	while (true)
	{
	    int c = peek();
	    if ((c == '}') || (c == ')'))
	    {
		break;
	    }

	    if (c == ',')
                consume(',');

	    skipWhitespace();

	    c = peek();
	    if ((c == '}') || (c == ')'))
	    {
		break;
	    }
	    parseField(result);
	}

	consume('}',')');
	return result;
    }

    private void parseField(BibtexEntry entry) throws IOException
    {
        String key = parseTextToken().toLowerCase();
	//Util.pr("_"+key+"_");
        skipWhitespace();
        consume('=');
	String content = parseFieldContent(GUIGlobals.isStandardField(key));
	if (content.length() > 0) {
          if (entry.getField(key) == null)
            entry.setField(key, content);
          else {
            // The following hack enables the parser to deal with multiple author or
            // editor lines, stringing them together instead of getting just one of them.
            // Multiple author or editor lines are not allowed by the bibtex format, but
            // at least one online database exports bibtex like that, making it inconvenient
            // for users if JabRef didn't accept it.
            if (key.equals("author") || key.equals("editor"))
              entry.setField(key, entry.getField(key)+" and "+content);
          }
        }
    }

    private String parseFieldContent(boolean isStandardBibtexField) throws IOException
    {
        skipWhitespace();
	StringBuffer value = new StringBuffer();
        int c,j='.';

	while (((c = peek()) != ',') && (c != '}') && (c != ')'))
        {

	    if (_eof) {
			throw new RuntimeException("Error in line "+line+
						   ": EOF in mid-string");
	    }
	    if (c == '"')
	    {
		// value is a string
		consume('"');

		while (!((peek() == '"') && (j != '\\')))
		{
		    j = read();
		    if (_eof || (j == -1) || (j == 65535))
                    {
			throw new RuntimeException("Error in line "+line+
						   ": EOF in mid-string");
                    }

		    value.append((char) j);
		}

		consume('"');
                
	    }
	    else if (c == '{') {
		// Value is a string enclosed in brackets. There can be pairs
		// of brackets inside of a field, so we need to count the brackets
		// to know when the string is finished.
               
                if (isStandardBibtexField || !Globals.prefs.getBoolean("preserveFieldFormatting"))
                    value.append(parseBracketedText());
                else
                    value.append(parseBracketedTextExactly());
	    }
	    else if (Character.isDigit((char) c))
	    {
		// value is a number
		String numString = parseTextToken();
		int numVal = Integer.parseInt(numString);
		value.append((new Integer(numVal)).toString());
		//entry.setField(key, new Integer(numVal));
	    }
	    else if (c == '#')
	    {
		//value.append(" # ");
		consume('#');
	    }
	    else
	    {
		String textToken = parseTextToken();
		if (textToken.length() == 0)
		    throw new IOException("Error in line "+line+" or above: "+
					  "Empty text token.\nThis could be caused "+
					  "by a missing comma between two fields.");
		value.append("#"+textToken+"#");
		//Util.pr(parseTextToken());
		//throw new RuntimeException("Unknown field type");
	    }
	    skipWhitespace();
	}
	//Util.pr("Returning field content: "+value.toString());
	return value.toString();

    }

    /**
     * This method is used to parse string labels, field names, entry
     * type and numbers outside brackets.
     */
    private String parseTextToken() throws IOException
    {
        StringBuffer token = new StringBuffer(20);

        while (true)
        {
            int c = read();
	    //Util.pr(".. "+c);
            if (c == -1)
            {
                _eof = true;

                return token.toString();
            }

            if (Character.isLetterOrDigit((char) c) ||
		(c == ':') || (c == '-')
		|| (c == '_') || (c == '*') || (c == '+') || (c == '.')
		|| (c == '/') || (c == '\''))
            {
                token.append((char) c);
            }
            else
            {
                unread(c);
		//Util.pr("Pasted text token: "+token.toString());
                return token.toString();
            }
        }
    }

    /**
     * This method is used to parse the bibtex key for an entry.
     */
    private String parseKey() throws IOException,NoLabelException
    {
       StringBuffer token = new StringBuffer(20);

        while (true)
        {
            int c = read();
	    //Util.pr(".. '"+(char)c+"'\t"+c);
            if (c == -1)
            {
                _eof = true;

                return token.toString();
            }

	    // Ikke: #{}\uFFFD~\uFFFD
	    //
	    // G\uFFFDr:  $_*+.-\/?"^
            if (!Character.isWhitespace((char)c) && (Character.isLetterOrDigit((char) c) ||
		((c != '#') && (c != '{') && (c != '}') && (c != '\uFFFD')
		 && (c != '~') && (c != '\uFFFD') && (c != ',') && (c != '=')
		 )))
	    {
                token.append((char) c);
            }
            else
            {
                
                if (Character.isWhitespace((char)c)) {
                    // We have encountered white space instead of the comma at the end of
                    // the key. Possibly the comma is missing, so we try to return what we
                    // have found, as the key.
                    return token.toString();
                }
                else if (c == ',') {
		    unread(c);        
		    return token.toString();
		    //} else if (Character.isWhitespace((char)c)) {
		    //throw new NoLabelException(token.toString());
		} else if (c == '=') {
		    // If we find a '=' sign, it is either an error, or
		    // the entry lacked a comma signifying the end of the key.

                    return token.toString();
		    //throw new NoLabelException(token.toString());
                    
		} else
		    throw new IOException("Error in line "+line+":"+
					  "Character '"+(char)c+"' is not "+
					  "allowed in bibtex keys.");

            }
        }


    }

    private class NoLabelException extends Exception {
	public NoLabelException(String hasRead) {
	    super(hasRead);
	}
    }

    private StringBuffer parseBracketedText() throws IOException
    {
	//Util.pr("Parse bracketed text");
	StringBuffer value = new StringBuffer();

	consume('{');

	int brackets = 0;

	while (!((peek() == '}') && (brackets == 0)))
        {

	    int j = read();
            if ((j == -1) || (j == 65535))
	    {
		throw new RuntimeException("Error in line "+line
					   +": EOF in mid-string");
	    }
	    else if (j == '{')
		brackets++;
	    else if (j == '}')
		brackets--;

	    // If we encounter whitespace of any kind, read it as a
	    // simple space, and ignore any others that follow immediately.
        /*if (j == '\n') {
            if (peek() == '\n')
                value.append('\n');
        }
	    else*/ if (Character.isWhitespace((char)j)) {
            String whs = skipAndRecordWhitespace(j);
            //System.out.println(":"+whs+":");
		    if (!whs.equals("") && !whs.equals("\n\t")) { // && !whs.equals("\n"))
                whs = whs.replaceAll("\t", ""); // Remove tabulators.
                //while (whs.endsWith("\t"))
                //    whs = whs.substring(0, whs.length()-1);
                value.append(whs);
            }
            else
                value.append(' ');


	    } else
		value.append((char) j);

	}

	consume('}');

	return value;
    }

    private StringBuffer parseBracketedTextExactly() throws IOException
    {

	StringBuffer value = new StringBuffer();

	consume('{');

	int brackets = 0;

	while (!((peek() == '}') && (brackets == 0)))
        {

	    int j = read();
            if ((j == -1) || (j == 65535))
	    {
		throw new RuntimeException("Error in line "+line
					   +": EOF in mid-string");
	    }
	    else if (j == '{')
		brackets++;
	    else if (j == '}')
		brackets--;
            
            value.append((char) j);

	}

	consume('}');

	return value;
    }
    
    private void consume(char expected) throws IOException
    {
        int c = read();

        if (c != expected)
        {
            throw new RuntimeException("Error in line "+line
		    +": Expected "
		    + expected + " but received " + (char) c);
        }

    }

    private void consumeUncritically(char expected) throws IOException
    {
	int c;
	while (((c = read()) != expected) && (c != -1) && (c != 65535));
	if ((c == -1) || (c == 65535))
	    _eof = true;
    }

    private void consume(char expected1, char expected2) throws IOException
    {
	// Consumes one of the two, doesn't care which appears.

        int c = read();

        if ((c != expected1) && (c != expected2))
        {
            throw new RuntimeException("Error in line "+line+": Expected " +
                expected1 + " or " + expected2 + " but received " + (int) c);

        }

    }

    public void checkEntryTypes(ParserResult _pr) {
	for (Iterator i=_db.getKeySet().iterator(); i.hasNext();) {
        Object key = i.next();
	    BibtexEntry be = (BibtexEntry)_db.getEntryById((String)key);
	    if (be.getType() instanceof UnknownEntryType) {
		// Look up the unknown type name in our map of parsed types:
           
		Object o = entryTypes.get(be.getType().getName().toLowerCase());
		if (o != null) {
		    BibtexEntryType type = (BibtexEntryType)o;
		    be.setType(type);
		} else {
            //System.out.println("Unknown entry type: "+be.getType().getName());
            _pr.addWarning(Globals.lang("unknown entry type")+": "+be.getType().getName()+". "+
                 Globals.lang("Type set to 'other'")+".");
            be.setType(BibtexEntryType.OTHER);
        }
	    }
	}
    }
}

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

package net.sf.jabref;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class BibtexParser
{
    private PushbackReader _in;
    private BibtexDatabase _db;
    private HashMap _meta;
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

    public ParserResult parse() throws IOException {

        _db = new BibtexDatabase(); // Bibtex related contents.
	_meta = new HashMap();      // Metadata in comments for Bibkeeper.

        skipWhitespace();

        try
        {
            while (!_eof)
            {

		consumeUncritically('@');
		skipWhitespace();
		String entryType = parseTextToken();
		BibtexEntryType tp = 
		    BibtexEntryType.getType(entryType); 
		if (tp != null) 
                {
		    //Util.pr("Found: "+tp.getName());
		    _db.insertEntry(parseEntry(tp));
		}
		else if (entryType.toLowerCase().equals("preamble")) {
		    _db.setPreamble(parsePreamble());
		}
		else if (entryType.toLowerCase().equals("string")) {
		    _db.addString(parseString(), _db.getStringCount());
		}
		else if (entryType.toLowerCase().equals("comment")) {
		    StringBuffer comment = parseBracketedText();
		    /**
		     *
		     * Metadata are used to store Bibkeeper-specific
		     * information in .bib files.
		     *
		     * Metadata are stored in bibtex files in the format
		     * @comment{bibkeeper-meta: type:data0;data1;data2;...}
		     *
		     * Each comment that starts with the META_FLAG is stored
		     * in the meta HashMap, with type as key.
		     */

		    if (comment.substring(0, GUIGlobals.META_FLAG.length())
			.equals(GUIGlobals.META_FLAG)) {

			String rest = comment.substring
			    (GUIGlobals.META_FLAG.length());
			int pos = rest.indexOf(':');

			if (pos > 0)
			    _meta.put
				(rest.substring(0, pos), rest.substring(pos+1));
		    }
		}
		//else
		//    throw new RuntimeException("Unknown entry type: "+entryType);
                skipWhitespace();
            }

            return new ParserResult(_db, _meta);
        }
        catch (KeyCollisionException kce)
        {
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
	String content = parseFieldContent();
	//Util.pr("Now I'm going to consume a }");
	consume('}',')');
	//Util.pr("Finished string parsing.");
	return new BibtexString(name, content);
    }

    public String parsePreamble() throws IOException
    {
	int brackets = 0;

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

		    String cont = parseFieldContent();
		    result.setField(ex.getMessage().trim().toLowerCase(), cont);
		} else {
		    key = ex.getMessage()+"=";
		    doAgain = true;		  
		}
	    }
	}

	result.setField(GUIGlobals.KEY_FIELD, key);

	skipWhitespace();
	
	while (true)
	{
	    int c = peek();
	    if ((c == '}') || (c == ')'))	    
	    {
		break;
	    }

	    //if (key != null)
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
	String content = parseFieldContent();
	if (content.length() > 0)
	    entry.setField(key, content);
    }

    private String parseFieldContent() throws IOException
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

		value.append(parseBracketedText());
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
    private String parseKey() throws IOException, NoLabelException
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

	    // Ikke: #{}¤~¨
	    //
	    // Går:  $_*+.-\/?"^
            if (Character.isLetterOrDigit((char) c) || 
		((c != '#') && (c != '{') && (c != '}') && (c != '¤')
		 && (c != '~') && (c != '¨') && (c != ',') && (c != '=')
		 ))
	    {
                token.append((char) c);
            }
            else
            {
		if (c == ',') {
		    unread(c);
		    return token.toString();
		} else if (Character.isWhitespace((char)c)) {
		    //throw new NoLabelException(token.toString());
		} else if (c == '=') {
		    // If we find a '=' sign, it is either an error, or
		    // the entry lacked a comma signifying the end of the key.
		    //unread(c);
		    throw new NoLabelException(token.toString());
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
	    if (Character.isWhitespace((char)j)) {
		value.append(' ');
		skipWhitespace();
	    } else
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
}

/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.export.layout;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Vector;

import net.sf.jabref.Globals;


/**
 * Helper class to get a Layout object.
 * 
 * <code>
 * LayoutHelper helper = new LayoutHelper(...a reader...);
 * Layout layout = helper.getLayoutFromText();
 * </code>
 *
 */
public class LayoutHelper {

    public static final int IS_LAYOUT_TEXT = 1;
    public static final int IS_SIMPLE_FIELD = 2;
    public static final int IS_FIELD_START = 3;
    public static final int IS_FIELD_END = 4;
    public static final int IS_OPTION_FIELD = 5;
    public static final int IS_GROUP_START = 6;
    public static final int IS_GROUP_END = 7;
    public static final int IS_ENCODING_NAME = 8;
    public static final int IS_FILENAME = 9;
    public static final int IS_FILEPATH = 10;
    
    private static String currentGroup = null;
    
    private PushbackReader _in;
    private Vector<StringInt> parsedEntries = new Vector<StringInt>();

    private boolean _eof = false;
    private int line = 1;

    public LayoutHelper(Reader in)
    {
        if (in == null)
        {
            throw new NullPointerException();
        }

        _in = new PushbackReader(in);
    }

    public Layout getLayoutFromText(String classPrefix) throws Exception
    {    	
    	parse();

        StringInt si;

        for (StringInt parsedEntry : parsedEntries) {
            si = parsedEntry;

            if ((si.i == IS_SIMPLE_FIELD) || (si.i == IS_FIELD_START) ||
                    (si.i == IS_FIELD_END) || (si.i == IS_GROUP_START) ||
                    (si.i == IS_GROUP_END)) {
                si.s = si.s.trim().toLowerCase();
            }
        }
        
        Layout layout = new Layout(parsedEntries, classPrefix);

        return layout;
    }

    
    public static String getCurrentGroup() {
        return currentGroup;
    }
    
    public static void setCurrentGroup(String newGroup) {
        currentGroup = newGroup;
    }
    
    private String getBracketedField(int _field) throws IOException
    {
        StringBuffer buffer = null;
        int c;
        boolean start = false;

        while (!_eof)
        {
            c = read();

            //System.out.println((char)c);
            if (c == -1)
            {
                _eof = true;

                if (buffer != null)
                {
                    //myStrings.add(buffer.toString());
                    parsedEntries.add(new StringInt(buffer.toString(), _field));

                    //System.out.println("\nbracketedEOF: " + buffer.toString());
                }

                //myStrings.add(buffer.toString());
                //System.out.println("aha: " + buffer.toString());
                return null;
            }

            if ((c == '{') || (c == '}'))
            {
                if (c == '}')
                {
                    if (buffer != null)
                    {
                        //myStrings.add(buffer.toString());
                        parsedEntries.add(new StringInt(buffer.toString(),
                                _field));

                        //System.out.println("\nbracketed: " + buffer.toString());
                        return null;
                    }
                }
                else
                {
                    start = true;
                }
            }
            else
            {
                if (buffer == null)
                {
                    buffer = new StringBuffer(100);
                }

                if (start)
                {
                    if (c == '}')
                    {
                    }
                    else
                    {
                        buffer.append((char) c);
                    }
                }
            }
        }

        return null;
    }

    /**
     *
     */
    private String getBracketedOptionField(int _field)
        throws IOException
    {
        StringBuffer buffer = null;
        int c;
        boolean start = false;
        boolean inQuotes = false;
        boolean doneWithOptions = false;
        String option = null;
        String tmp;

        while (!_eof)
        {
            c = read();

            //System.out.println((char)c);
            if (c == -1)
            {
                _eof = true;

                if (buffer != null)
                {
                    //myStrings.add(buffer.toString());
                    if (option != null)
                    {
                        tmp = buffer.toString() + "\n" + option;
                    }
                    else
                    {
                        tmp = buffer.toString();
                    }

                    parsedEntries.add(new StringInt(tmp, IS_OPTION_FIELD));

                    //System.out.println("\nbracketedOptionEOF: " + buffer.toString());
                }

                return null;
            }
            if (!inQuotes && ((c == ']') || (c == '[') || (doneWithOptions && ((c == '{') || (c == '}')))))
            //if ((c == '{') || (c == '}') || (c == ']') || (c == '['))
            {
                if ((c == ']') || (doneWithOptions && (c == '}')))
                {
                    // changed section start - arudert
                    // buffer may be null for parameters
                    //if (buffer != null)
                    //{
                        if (c == ']' && buffer != null)
                        {
                    // changed section end - arudert
                            option = buffer.toString();
                            buffer = null;
                            start = false;
                            doneWithOptions = true;
                        }

                        //myStrings.add(buffer.toString());
                        //System.out.println("\nbracketedOption: " + buffer.toString());
                        
                        // changed section begin - arudert
                        // bracketed option must be followed by an (optionally empty) parameter
                        // if empty, the parameter is set to " " (whitespace to avoid that the tokenizer that
                        // splits the string later on ignores the empty parameter)
                        //if (buffer != null)
                        else if (c == '}')
                        {
                           String parameter = buffer == null ? " " : buffer.toString();
                           if (option != null)
                            {
                                tmp = parameter + "\n" + option;
                            }
                            else
                            {
                                tmp = parameter;
                            }

                            //System.out.println("FORMAT: '"+tmp+"'");
                            parsedEntries.add(new StringInt(tmp, IS_OPTION_FIELD));

                            return null;
                        }
                        // changed section end - arudert
                     // changed section start - arudert
                     // }
                     // changed section end - arudert
                }
                else
                {
                    start = true;
                }
            }
            else if (c == '"') {
                inQuotes = !inQuotes;

                if (buffer == null)
                    buffer = new StringBuffer(100);
                buffer.append('"');
            }
            else
            {
                if (buffer == null)
                {
                    buffer = new StringBuffer(100);
                }

                if (start)
                {

                    // changed section begin - arudert
                    // keep the backslash so we know wether this is a fieldname or an ordinary parameter
                    //if (c != '\\')
                    //{
                    buffer.append((char) c);
                    //}
                    // changed section end - arudert

                }
            }
        }

        return null;
    }

    private Object parse() throws IOException, StringIndexOutOfBoundsException {
		skipWhitespace();

		int c;

		StringBuffer buffer = null;
		boolean escaped = false;

		while (!_eof) {
			c = read();

			if (c == -1) {
				_eof = true;

				/*
				 * CO 2006-11-11: Added check for null, otherwise a Layout that
				 * finishs with a curly brace throws a NPE
				 */
				if (buffer != null)
					parsedEntries.add(new StringInt(buffer.toString(), IS_LAYOUT_TEXT));

				return null;
			}

			if ((c == '\\') && (peek() != '\\') && !escaped) {
				if (buffer != null) {
					parsedEntries.add(new StringInt(buffer.toString(), IS_LAYOUT_TEXT));

					buffer = null;
				}

				parseField();

				// To make sure the next character, if it is a backslash,
				// doesn't get ignored, since "previous" now holds a backslash:
				escaped = false;
			} else {
				if (buffer == null) {
					buffer = new StringBuffer(100);
				}

				if ((c != '\\') || escaped)// (previous == '\\')))
				{
					buffer.append((char) c);
				}

				escaped = (c == '\\') && !escaped;
			}
		}

		return null;
	}

    /**
	 * 
	 */
    private void parseField() throws IOException, StringIndexOutOfBoundsException
    {
        int c;
        StringBuffer buffer = null;
        char firstLetter = ' ';
        String name;

        while (!_eof)
        {
            c = read();
            // System.out.print((char)c);
            if (c == -1)
            {
                _eof = true;
            }

            if (!Character.isLetter((char) c) && (c != '_') && (c != '-'))
            {
                unread(c);

                //System.out.println("\n#" + (char) c);
                name = buffer != null ? buffer.toString() : "";
                
                try {
                	firstLetter = name.charAt(0);
                } catch (StringIndexOutOfBoundsException ex) {
                	StringBuilder lastFive = new StringBuilder(10); 
                	for (StringInt entry : parsedEntries.subList(Math.max(0, parsedEntries.size()-6), parsedEntries.size()-1)) {
                		lastFive.append(entry.s);
                	}
                	throw new StringIndexOutOfBoundsException(Globals.lang("Backslash parsing error near") + " \'" + lastFive.toString().replace("\n", " ") +  "\'");
                }
                
                //System.out.println("NAME:" + name);
                buffer = null;
                
                if (firstLetter == 'b')
                {
                    if (name.equalsIgnoreCase("begin"))
                    {
                        // get field name
                        getBracketedField(IS_FIELD_START);

                        return;
                    }
                    else if (name.equalsIgnoreCase("begingroup"))
                    {
                        // get field name
                        getBracketedField(IS_GROUP_START);
                        return;                    
                    }
                }
                else if (firstLetter == 'f')
                {
                    if (name.equalsIgnoreCase("format"))
                    {
                        if (c == '[')
                        {
                            // get format parameter
                            // get field name
                            getBracketedOptionField(IS_OPTION_FIELD);

                            return;
                        }
                        else
                        {
                            // get field name
                            getBracketedField(IS_OPTION_FIELD);

                            return;
                        }
                    }
                    else if (name.equalsIgnoreCase("filename"))
                    {
                        // Print the name of the database bib file.
                        // This is only supported in begin/end layouts, not in
                        // entry layouts.
                        parsedEntries.add(new StringInt(name, IS_FILENAME));
                        return;
                    }
                    else if (name.equalsIgnoreCase("filepath"))
                    {
                        // Print the full path of the database bib file.
                        // This is only supported in begin/end layouts, not in
                        // entry layouts.
                        parsedEntries.add(new StringInt(name, IS_FILEPATH));
                        return;
                    }
                }
                else if (firstLetter == 'e')
                {
                    if (name.equalsIgnoreCase("end"))
                    {
                        // get field name
                        getBracketedField(IS_FIELD_END);
                        return;
                    }
                    else if (name.equalsIgnoreCase("endgroup"))
                    {
                        // get field name
                        getBracketedField(IS_GROUP_END);
                        return;
                    }
                    else if (name.equalsIgnoreCase("encoding"))
                    {
                        // Print the name of the current encoding used for export.
                        // This is only supported in begin/end layouts, not in
                        // entry layouts.
                        parsedEntries.add(new StringInt(name, IS_ENCODING_NAME));
                        return;
                    }
                }

                // for all other cases
                parsedEntries.add(new StringInt(name, IS_SIMPLE_FIELD));

                //System.out.println(name);
                return;
            }
            else
            {
                if (buffer == null)
                {
                    buffer = new StringBuffer(100);
                }

                buffer.append((char) c);
            }
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
        {
            line++;
        }

        //System.out.print((char) c);
        return c;
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
            {
                unread(c);
            }

            break;
        }
    }

    private void unread(int c) throws IOException
    {
        if (c == '\n')
        {
            line--;
        }

        _in.unread(c);
    }
}

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
package net.sf.jabref.export.layout;

import wsi.ra.types.StringInt;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class LayoutHelper
{
    //~ Static fields/initializers /////////////////////////////////////////////

    public static final int IS_LAYOUT_TEXT = 1;
    public static final int IS_SIMPLE_FIELD = 2;
    public static final int IS_FIELD_START = 3;
    public static final int IS_FIELD_END = 4;
    public static final int IS_OPTION_FIELD = 5;
    public static final int IS_GROUP_START = 6;
    public static final int IS_GROUP_END = 7;
    private static String currentGroup = null;
    
    //~ Instance fields ////////////////////////////////////////////////////////


    //public static final int IS_OPTION_FIELD_PARAM = 6;
    private PushbackReader _in;
    private Vector parsedEntries = new Vector();

    //private HashMap _meta;
    private boolean _eof = false;
    private int line = 1;

    //~ Constructors ///////////////////////////////////////////////////////////

    public LayoutHelper(Reader in)
    {

        if (in == null)
        {
            throw new NullPointerException();
        }

        _in = new PushbackReader(in);
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public Layout getLayoutFromText(String classPrefix) throws Exception
    {
        parse();

        StringInt si;

        for (int i = 0; i < parsedEntries.size(); i++)
        {
            si = (StringInt) parsedEntries.get(i);

            if ((si.i == IS_SIMPLE_FIELD) || (si.i == IS_FIELD_START) ||
                    (si.i == IS_FIELD_END) || (si.i == IS_GROUP_START) ||
                    (si.i == IS_GROUP_END))
            {
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
    
    /**
     *
     */
    private String getBracketedField(int _field) throws IOException
    {
        StringBuffer buffer = null;
        int previous = -1;
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

            previous = c;
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
        int previous = -1;
        int c;
        boolean start = false;
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

            if ((c == '{') || (c == '}') || (c == ']') || (c == '['))
            {
                if ((c == '}') || (c == ']'))
                {
                    if (buffer != null)
                    {
                        if (c == ']')
                        {
                            option = buffer.toString();
                            buffer = null;
                            start = false;
                        }

                        //myStrings.add(buffer.toString());
                        //System.out.println("\nbracketedOption: " + buffer.toString());
                        if (buffer != null)
                        {
                            if (option != null)
                            {
                                tmp = buffer.toString() + "\n" + option;
                            }
                            else
                            {
                                tmp = buffer.toString();
                            }

                            //System.out.println("FORMAT: '"+tmp+"'");
                            parsedEntries.add(new StringInt(tmp, IS_OPTION_FIELD));

                            return null;
                        }
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
                    if ((c == '}') || (c == ']'))
                    {
                    }
                    else
                    {
                        if (c != '\\')
                        {
                            buffer.append((char) c);
                        }
                    }
                }
            }

            previous = c;
        }

        return null;
    }

    private Object parse() throws IOException
    {
        //_meta = new HashMap(); // Metadata in comments for Bibkeeper.
        skipWhitespace();

        int c;

        StringBuffer buffer = null;
        int previous = -1;

        while (!_eof)
        {
            c = read();

            //System.out.println((char)c);
            if (c == -1)
            {
                _eof = true;
                parsedEntries.add(new StringInt(buffer.toString(),
                        IS_LAYOUT_TEXT));

                //System.out.println("aha: " + buffer.toString());
                return null;
            }

            if ((c == '\\') && (peek() != '\\') && (previous != '\\'))
            {
                if (buffer != null)
                {
                    parsedEntries.add(new StringInt(buffer.toString(),
                            IS_LAYOUT_TEXT));

                    //System.out.println("aha: " + buffer.toString());
                    buffer = null;
                }

                parseField();
            }
            else
            {
                if (buffer == null)
                {
                    buffer = new StringBuffer(100);
                }

                if (!((c == '\\') && (previous == '\\')))
                {
                    buffer.append((char) c);
                }
            }

            previous = c;
        }

        return null;
    }

    /**
     *
     */
    private void parseField() throws IOException
    {
        int c;
        StringBuffer buffer = null;
        String name;

        while (!_eof)
        {
            c = read();

            if (c == -1)
            {
                _eof = true;
            }

            if (!Character.isLetter((char) c))
            {
                unread(c);

                //System.out.println("\n#" + (char) c);
                name = buffer.toString();

                //System.out.println("NAME:" + name);
                buffer = null;

                if (name.charAt(0) == 'b')
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
                else if (name.charAt(0) == 'f')
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
                }
                else if (name.charAt(0) == 'e')
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

    //
    //	private String parseFieldContent() throws IOException
    //	{
    //		skipWhitespace();
    //		consume('=');
    //		skipWhitespace();
    //		StringBuffer value = new StringBuffer();
    //		int c, j = '.';
    //
    //		while (((c = peek()) != ',') && (c != '}') && (c != ')'))
    //		{
    //
    //			if (_eof)
    //			{
    //				throw new RuntimeException(
    //					"Error in line " + line + ": EOF in mid-string");
    //			}
    //			if (c == '"')
    //			{
    //				// value is a string
    //				consume('"');
    //
    //				while (!((peek() == '"') && (j != '\\')))
    //				{
    //					j = read();
    //					if (_eof || (j == -1) || (j == 65535))
    //					{
    //						throw new RuntimeException(
    //							"Error in line " + line + ": EOF in mid-string");
    //					}
    //
    //					value.append((char) j);
    //				}
    //
    //				consume('"');
    //
    //			}
    //			skipWhitespace();
    //		}
    //		//Util.pr("Returning field content: "+value.toString());
    //		return value.toString();
    //
    //	}
    //
    //	private StringBuffer parseBracketedText() throws IOException
    //	{
    //		//Util.pr("Parse bracketed text");
    //		StringBuffer value = new StringBuffer();
    //
    //		consume('{');
    //
    //		int brackets = 0;
    //
    //		while (!((peek() == '}') && (brackets == 0)))
    //		{
    //
    //			int j = read();
    //			if ((j == -1) || (j == 65535))
    //			{
    //				throw new RuntimeException(
    //					"Error in line " + line + ": EOF in mid-string");
    //			}
    //			else if (j == '{')
    //				brackets++;
    //			else if (j == '}')
    //				brackets--;
    //
    //			// If we encounter whitespace of any kind, read it as a
    //			// simple space, and ignore any others that follow immediately.
    //			if (Character.isWhitespace((char) j))
    //			{
    //				value.append(' ');
    //				skipWhitespace();
    //			}
    //			else
    //				value.append((char) j);
    //
    //		}
    //
    //		consume('}');
    //
    //		return value;
    //	}
    //	private void consume(char expected) throws IOException
    //	{
    //		int c = read();
    //
    //		if (c != expected)
    //		{
    //			throw new RuntimeException(
    //				"Error in line "
    //					+ line
    //					+ ": Expected "
    //					+ expected
    //					+ " but received "
    //					+ (char) c);
    //		}
    //
    //	}
    //
    //	private void consumeUncritically(char expected) throws IOException
    //	{
    //		int c;
    //		while (((c = read()) != expected) && (c != -1) && (c != 65535));
    //		if ((c == -1) || (c == 65535))
    //			_eof = true;
    //	}
    //
    //	private void consume(char expected1, char expected2) throws IOException
    //	{
    //		// Consumes one of the two, doesn't care which appears.
    //
    //		int c = read();
    //
    //		if ((c != expected1) && (c != expected2))
    //		{
    //			throw new RuntimeException(
    //				"Error in line "
    //					+ line
    //					+ ": Expected "
    //					+ expected1
    //					+ " or "
    //					+ expected2
    //					+ " but received "
    //					+ (int) c);
    //		}
    //
    //	}
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

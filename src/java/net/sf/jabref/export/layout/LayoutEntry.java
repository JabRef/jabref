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
package net.sf.jabref.export.layout;

import wsi.ra.tool.WSITools;

import wsi.ra.types.StringInt;

import java.util.Vector;

import net.sf.jabref.BibtexEntry;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class LayoutEntry
{
    //~ Instance fields ////////////////////////////////////////////////////////

    private LayoutFormatter option;
    private String text;
    private LayoutEntry[] layoutEntries;
    private int type;

    //~ Constructors ///////////////////////////////////////////////////////////

    public LayoutEntry(StringInt si)
    {
        type = si.i;

        if (si.i == LayoutHelper.IS_LAYOUT_TEXT)
        {
            text = si.s;
        }
        else if (si.i == LayoutHelper.IS_SIMPLE_FIELD)
        {
            text = si.s.trim();
        }
        else if (si.i == LayoutHelper.IS_FIELD_START)
        {
        }
        else if (si.i == LayoutHelper.IS_FIELD_END)
        {
        }
        else if (si.i == LayoutHelper.IS_OPTION_FIELD)
        {
            Vector v = new Vector();
            WSITools.tokenize(v, si.s, "\n");

            if (v.size() == 1)
            {
                text = (String) v.get(0);
            }
            else
            {
                text = ((String) v.get(0)).trim();

                try
                {
                    option = getOptionalLayout((String) v.get(1));
                }
                 catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        //		else if (si.i == LayoutHelper.IS_OPTION_FIELD_PARAM)
        //		{
        //		}
    }

    public LayoutEntry(Vector parsedEntries)
    {
        String blockStart = null;
        String blockEnd = null;
        StringInt si;
        Vector blockEntries = null;
        Vector tmpEntries = new Vector();
        LayoutEntry le;
        si = (StringInt) parsedEntries.get(0);
        blockStart = si.s;
        si = (StringInt) parsedEntries.get(parsedEntries.size() - 1);
        blockEnd = si.s;

        if (!blockStart.equals(blockEnd))
        {
            System.err.println("Field start and end entry must be equal.");
        }

        type = LayoutHelper.IS_FIELD_START;
        text = si.s;

        for (int i = 1; i < (parsedEntries.size() - 1); i++)
        {
            si = (StringInt) parsedEntries.get(i);

            //System.out.println("PARSED-ENTRY: "+si.s+"="+si.i);
            if (si.i == LayoutHelper.IS_LAYOUT_TEXT)
            {
            }
            else if (si.i == LayoutHelper.IS_SIMPLE_FIELD)
            {
            }
            else if (si.i == LayoutHelper.IS_FIELD_START)
            {
                blockEntries = new Vector();
                blockStart = si.s;
            }
            else if (si.i == LayoutHelper.IS_FIELD_END)
            {
                if (blockStart.equals(si.s))
                {
                    blockEntries.add(si);
                    le = new LayoutEntry(blockEntries);
                    tmpEntries.add(le);
                    blockEntries = null;
                }
                else
                {
                    System.err.println(
                        "Nested field entries are not implemented !!!");
                }
            }
            else if (si.i == LayoutHelper.IS_OPTION_FIELD)
            {
            }

            //			else if (si.i == LayoutHelper.IS_OPTION_FIELD_PARAM)
            //			{
            //			}
            if (blockEntries == null)
            {
                //System.out.println("BLOCK ADD: "+si.s+"="+si.i);
                tmpEntries.add(new LayoutEntry(si));
            }
            else
            {
                blockEntries.add(si);
            }
        }

        layoutEntries = new LayoutEntry[tmpEntries.size()];

        for (int i = 0; i < tmpEntries.size(); i++)
        {
            layoutEntries[i] = (LayoutEntry) tmpEntries.get(i);

            //System.out.println(layoutEntries[i].text);
        }
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public String doLayout(BibtexEntry bibtex)
    {
        if (type == LayoutHelper.IS_LAYOUT_TEXT)
        {
            return text;
        }
        else if (type == LayoutHelper.IS_SIMPLE_FIELD)
        {
            if (text.equals("bibtextype"))
            {
                return bibtex.getType().getName();
            }

            String field = (String) bibtex.getField(text);

            if (field == null)
            {
                return null;
            }
            else
            {
                return field;
            }
        }
        else if (type == LayoutHelper.IS_FIELD_START)
        {
            String field = (String) bibtex.getField(text);

            if (field == null)
            {
                return null;
            }
            else
            {
                StringBuffer sb = new StringBuffer(100);
                String fieldText;
                boolean previousSkipped = false;

                for (int i = 0; i < layoutEntries.length; i++)
                {
                    fieldText = layoutEntries[i].doLayout(bibtex);

                    //System.out.println("'" + fieldText + "'");
                    if (fieldText == null)
                    {
                        if ((i + 1) < layoutEntries.length)
                        {
                            if (layoutEntries[i + 1].doLayout(bibtex).trim()
                                                        .length() == 0)
                            {
                                //sb.append("MISSING");
                                i++;
                                previousSkipped = true;

                                continue;
                            }
                        }
                    }
                    else
                    {
                        // if previous was skipped --> remove leading line breaks
                        if (previousSkipped)
                        {
                            int eol = 0;

                            while ((eol < fieldText.length()) &&
                                    ((fieldText.charAt(eol) == '\n') ||
                                    (fieldText.charAt(eol) == '\r')))
                            {
                                eol++;
                            }

                            if (eol < fieldText.length())
                            {
                                sb.append(fieldText.substring(eol));
                            }
                        }
                        else
                        {
                            //System.out.println("ENTRY-BLOCK: " + layoutEntries[i].doLayout(bibtex));
                            sb.append(fieldText);
                        }
                    }

                    previousSkipped = false;
                }

                return sb.toString();
            }
        }
        else if (type == LayoutHelper.IS_FIELD_END)
        {
        }
        else if (type == LayoutHelper.IS_OPTION_FIELD)
        {
	    //System.out.println("doLayout IS_OPTION_FIELD '"+text+"'");
            String fieldEntry;

            if (text.equals("bibtextype"))
            {
                fieldEntry = bibtex.getType().getName();
            }
            else{
            String field = (String) bibtex.getField(text);

            if (field == null)
            {
                fieldEntry = "";
            }
            else
            {
                fieldEntry = field;
            }
            }

            //System.out.println("OPTION: "+option);
            if (option != null)
            {
                fieldEntry = option.format(fieldEntry);
            }

            return fieldEntry;
        }

        //		else if (type == LayoutHelper.IS_OPTION_FIELD_PARAM)
        //		{
        //		}
        return "";
    }

    /**
     * @param string
     * @return
     */
    private LayoutFormatter getOptionalLayout(String formatterName)
        throws Exception
    {
        LayoutFormatter formatter;

        try
        {
            formatter = (LayoutFormatter) Class.forName(formatterName)
                                               .newInstance();
        }
         catch (ClassNotFoundException ex)
        {
            throw new Exception(formatterName + " not found.");
        }
         catch (InstantiationException ex)
        {
            throw new Exception(formatterName + " can not be instantiated.");
        }
         catch (IllegalAccessException ex)
        {
            throw new Exception(formatterName + " can't be accessed.");
        }

        if (formatter == null)
        {
            throw new Exception(formatterName + " not found.");
        }

        return formatter;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

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

import wsi.ra.types.StringInt;

import java.util.Vector;

import net.sf.jabref.BibtexEntry;
import java.io.IOException;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class Layout
{
    //~ Instance fields ////////////////////////////////////////////////////////

    private LayoutEntry[] layoutEntries;

    //~ Constructors ///////////////////////////////////////////////////////////

    public Layout(Vector parsedEntries, String classPrefix)  throws Exception
    {
        StringInt si;
        Vector tmpEntries = new Vector(parsedEntries.size());

        //layoutEntries=new LayoutEntry[parsedEntries.size()];
        Vector blockEntries = null;
        LayoutEntry le;
        String blockStart = null;

        for (int i = 0; i < parsedEntries.size(); i++)
        {
            si = (StringInt) parsedEntries.get(i);

            //System.out.println("PARSED: "+si.s+"="+si.i);
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
                    le = new LayoutEntry(blockEntries, classPrefix);
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
                tmpEntries.add(new LayoutEntry(si, classPrefix));
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
        //System.out.println("LAYOUT: " + bibtex.getId());
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
                    if (layoutEntries[i + 1].doLayout(bibtex).trim().length() == 0)
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
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner
//  Version:  $Revision$
//            $Date$
//            $Author$
//
//  Copyright (c) Dept. Computer Architecture, University of Tuebingen, Germany
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
///////////////////////////////////////////////////////////////////////////////
package net.sf.jabref.export.layout.format;

import wsi.ra.tool.WSITools;

import java.util.Vector;

import net.sf.jabref.export.layout.LayoutFormatter;


/**
 * Create DocBook authors formatter.
 *
 * @author $author$
 * @version $Revision$
 */
public class CreateDocBookAuthors implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////

    public String format(String fieldText)
    {
        //		<author><firstname>L.</firstname><surname>Xue</surname></author>
        //     <author><firstname>F.</firstname><othername role="mi">L.</othername><surname>Stahura</surname></author>
        //     <author><firstname>J.</firstname><othername role="mi">W.</othername><surname>Godden</surname></author>
        //     <author><firstname>J.</firstname><surname>Bajorath</surname></author>
        int index = 0;
        int oldPos = 0;
        String author;
        StringBuffer sb = new StringBuffer(100);
        sb.append("<author>");

        if (fieldText.indexOf(" and ") == -1)
        {
            singleAuthor(sb, fieldText);
        }
        else
        {
            while ((index = fieldText.indexOf(" and ", index + 1)) != -1)
            {
                author = fieldText.substring(oldPos, index);

                //System.out.println(author);
                singleAuthor(sb, author);
                sb.append("\n");
                oldPos = index + 4;
            }
        }

        sb.append("</author>");

        fieldText = sb.toString();

        return fieldText;
    }

    /**
     * @param sb
     * @param fieldText
     */
    private void singleAuthor(StringBuffer sb, String author)
    {
        // TODO: replace special characters
        Vector v = new Vector();
        WSITools.tokenize(v, author, " \n\r");

        if (v.size() == 1)
        {
            sb.append("<surname>");
            sb.append(v.get(0));
            sb.append("</surname>");
        }
        else if (v.size() == 2)
        {
            sb.append("<firstname>");
            sb.append(v.get(0));
            sb.append("</firstname>");
            sb.append("<surname>");
            sb.append(v.get(1));
            sb.append("</surname>");
        }
        else
        {
            sb.append("<firstname>");
            sb.append(v.get(0));
            sb.append("</firstname>");
            sb.append("<othername role=\"mi\">");

            for (int i = 1; i < (v.size() - 1); i++)
            {
                sb.append(v.get(i));

                if (i < (v.size() - 2))
                {
                    sb.append(' ');
                }
            }

            sb.append("</othername>");
            sb.append("<surname>");
            sb.append(v.get(v.size() - 1));
            sb.append("</surname>");
        }
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

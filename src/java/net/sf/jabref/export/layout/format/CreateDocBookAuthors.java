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

import net.sf.jabref.export.layout.WSITools;

import java.util.Vector;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.AuthorList;


/**
 * Create DocBook authors formatter.
 *
 * @author $author$
 * @version $Revision$
 */
public class CreateDocBookAuthors implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////

    static XMLChars xc = new XMLChars();

    public String format(String fieldText)
    {

        StringBuilder sb = new StringBuilder(100);

        AuthorList al = AuthorList.getAuthorList(fieldText);

        addBody(sb, al, "author");
        return sb.toString();
        
        //		<author><firstname>L.</firstname><surname>Xue</surname></author>
        //     <author><firstname>F.</firstname><othername role="mi">L.</othername><surname>Stahura</surname></author>
        //     <author><firstname>J.</firstname><othername role="mi">W.</othername><surname>Godden</surname></author>
        //     <author><firstname>J.</firstname><surname>Bajorath</surname></author>


        /*
        if (fieldText.indexOf(" and ") == -1)
        {
          sb.append("<author>");
          singleAuthor(sb, fieldText);
          sb.append("</author>");
        }
        else
        {
            String[] names = fieldText.split(" and ");
            for (int i=0; i<names.length; i++)
            {
              sb.append("<author>");
              singleAuthor(sb, names[i]);
              sb.append("</author>");
              if (i < names.length -1)
                sb.append("\n       ");
            }
        }



        fieldText = sb.toString();

        return fieldText;*/
    }

    public void addBody(StringBuilder sb, AuthorList al, String tagName) {
        for (int i=0; i<al.size(); i++) {
            sb.append("<"+tagName+">");
            AuthorList.Author a = al.getAuthor(i);
            if ((a.getFirst() != null) && (a.getFirst().length() > 0)) {
                sb.append("<firstname>");
                sb.append(xc.format(a.getFirst()));
                sb.append("</firstname>");
            }
            if ((a.getVon() != null) && (a.getVon().length() > 0)) {
                sb.append("<othername>");
                sb.append(xc.format(a.getVon()));
                sb.append("</othername>");
            }
            if ((a.getLast() != null) && (a.getLast().length() > 0)) {
                sb.append("<surname>");
                sb.append(xc.format(a.getLast()));
                if ((a.getJr() != null) && (a.getJr().length() > 0)) {
                    sb.append(" "+xc.format(a.getJr()));
                }
                sb.append("</surname>");
            }

            if (i < al.size()-1)
                sb.append("</"+tagName+">\n       ");
            else
                sb.append("</"+tagName+">");
        }
    }

    /**
     * @param sb
     * @param author
     */
    protected void singleAuthor(StringBuffer sb, String author)
    {
        // TODO: replace special characters
        Vector<String> v = new Vector<String>();

        String authorMod = AuthorList.fixAuthor_firstNameFirst(author);

        WSITools.tokenize(v, authorMod, " \n\r");

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

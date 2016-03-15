/*  Copyright (C) 2003-2015 JabRef contributors.
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
//  Version:  $Revision: 2268 $
//            $Date: 2007-08-20 01:37:05 +0200 (Mon, 20 Aug 2007) $
//            $Author: coezbek $
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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * @author $author$
 * @version $Revision: 2268 $
 */
public class CreateBibORDFAuthors implements LayoutFormatter {

    //~ Methods ////////////////////////////////////////////////////////////////

    @Override
    public String format(String fieldText) {
        // Yeah, the format is quite verbose... sorry about that :)

        //      <bibo:contribution>
        //        <bibo:Contribution>
        //          <bibo:role rdf:resource="http://purl.org/ontology/bibo/roles/author" />
        //          <bibo:contributor><foaf:Person foaf:name="Ola Spjuth"/></bibo:contributor>
        //          <bibo:position>1</bibo:position>
        //        </bibo:Contribution>
        //      </bibo:contribution>

        StringBuilder sb = new StringBuilder(100);

        if (!fieldText.contains(" and ")) {
            singleAuthor(sb, fieldText, 1);
        } else {
            String[] names = fieldText.split(" and ");
            for (int i = 0; i < names.length; i++) {
                singleAuthor(sb, names[i], i + 1);
                if (i < (names.length - 1)) {
                    sb.append('\n');
                }
            }
        }

        return sb.toString();
    }

    /**
     * @param sb
     * @param author
     * @param position
     */
    private static void singleAuthor(StringBuilder sb, String author, int position) {
        sb.append("<bibo:contribution>\n");
        sb.append("  <bibo:Contribution>\n");
        sb.append("    <bibo:role rdf:resource=\"http://purl.org/ontology/bibo/roles/author\" />\n");
        sb.append("    <bibo:contributor><foaf:Person foaf:name=\"").append(author).append("\"/></bibo:contributor>\n");
        sb.append("    <bibo:position>").append(position).append("</bibo:position>\n");
        sb.append("  </bibo:Contribution>\n");
        sb.append("</bibo:contribution>\n");
    }
}

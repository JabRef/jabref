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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;

/**
 * Create DocBook authors formatter.
 */
public class CreateDocBookAuthors implements LayoutFormatter {

    private static final XMLChars XML_CHARS = new XMLChars();


    @Override
    public String format(String fieldText) {

        StringBuilder sb = new StringBuilder(100);

        AuthorList al = AuthorList.parse(fieldText);

        addBody(sb, al, "author");
        return sb.toString();

    }

    public void addBody(StringBuilder sb, AuthorList al, String tagName) {
        for (int i = 0; i < al.getNumberOfAuthors(); i++) {
            sb.append('<').append(tagName).append('>');
            Author a = al.getAuthor(i);
            if ((a.getFirst() != null) && !a.getFirst().isEmpty()) {
                sb.append("<firstname>").append(CreateDocBookAuthors.XML_CHARS.format(a.getFirst()))
                        .append("</firstname>");
            }
            if ((a.getVon() != null) && !a.getVon().isEmpty()) {
                sb.append("<othername>").append(CreateDocBookAuthors.XML_CHARS.format(a.getVon()))
                        .append("</othername>");
            }
            if ((a.getLast() != null) && !a.getLast().isEmpty()) {
                sb.append("<surname>").append(CreateDocBookAuthors.XML_CHARS.format(a.getLast()));
                if ((a.getJr() != null) && !a.getJr().isEmpty()) {
                    sb.append(' ').append(CreateDocBookAuthors.XML_CHARS.format(a.getJr()));
                }
                sb.append("</surname>");
            }

            if (i < (al.getNumberOfAuthors() - 1)) {
                sb.append("</").append(tagName).append(">\n       ");
            } else {
                sb.append("</").append(tagName).append('>');
            }
        }
    }

}

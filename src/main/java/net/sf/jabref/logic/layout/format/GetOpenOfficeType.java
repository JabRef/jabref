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

package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Change type of record to match the one used by OpenOffice formatter.
 *
 * Based on the RemoveBrackets.java class (Revision 1.2) by mortenalver
 * @author $author$
 * @version $Revision$
 */
public class GetOpenOfficeType implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        if ("Article".equalsIgnoreCase(fieldText)) {
            return "7";
        }
        if ("Book".equalsIgnoreCase(fieldText)) {
            return "1";
        }
        if ("Booklet".equalsIgnoreCase(fieldText)) {
            return "2";
        }
        if ("Inbook".equalsIgnoreCase(fieldText)) {
            return "5";
        }
        if ("Incollection".equalsIgnoreCase(fieldText)) {
            return "5";
        }
        if ("Inproceedings".equalsIgnoreCase(fieldText)) {
            return "6";
        }
        if ("Manual".equalsIgnoreCase(fieldText)) {
            return "8";
        }
        if ("Mastersthesis".equalsIgnoreCase(fieldText)) {
            return "9";
        }
        if ("Misc".equalsIgnoreCase(fieldText)) {
            return "10";
        }
        if ("Other".equalsIgnoreCase(fieldText)) {
            return "10";
        }
        if ("Phdthesis".equalsIgnoreCase(fieldText)) {
            return "9";
        }
        if ("Proceedings".equalsIgnoreCase(fieldText)) {
            return "3";
        }
        if ("Techreport".equalsIgnoreCase(fieldText)) {
            return "13";
        }
        if ("Unpublished".equalsIgnoreCase(fieldText)) {
            return "14";
        }
        // Default, Miscelaneous
        return "10";
    }
}

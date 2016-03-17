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
//  Authors:  Joerg K. Wegner, Morten O. Alver
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

import java.util.HashMap;
import java.util.Map;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.strings.XmlCharsMap;

/**
 * Changes {\^o} or {\^{o}} to ?
 */
public class XMLChars implements LayoutFormatter {

    private static final XmlCharsMap XML_CHARS = new XmlCharsMap();

    private static final Map<String, String> ASCII_TO_XML_CHARS = new HashMap<>();

    private boolean[] forceReplace;


    static {
        ASCII_TO_XML_CHARS.put("<", "&lt;");
        ASCII_TO_XML_CHARS.put("\"", "&quot;");
        ASCII_TO_XML_CHARS.put(">", "&gt;");
    }

    @Override
    public String format(String fieldText) {

        if (fieldText == null) {
            return fieldText;
        }

        String formattedFieldText = firstFormat(fieldText);

        for (Map.Entry<String, String> entry : XML_CHARS.entrySet()) {
            String s = entry.getKey();
            String repl = entry.getValue();
            if (repl != null) {
                formattedFieldText = formattedFieldText.replaceAll(s, repl);
            }
        }
        return restFormat(formattedFieldText);
    }

    private static String firstFormat(String s) {
        return s.replaceAll("&|\\\\&", "&#x0026;").replace("--", "&#x2013;");
    }



    private String restFormat(String toFormat) {

        String fieldText = toFormat.replace("}", "").replace("{", "");

        // now some copy-paste problems most often occuring in abstracts when
        // copied from PDF
        // AND: this is accepted in the abstract of bibtex files, so are forced
        // to catch those cases

        if (forceReplace == null) {
            forceReplace = new boolean[126];
            for (int i = 0; i < 40; i++) {
                forceReplace[i] = true;
            }
            forceReplace[32] = false;
            for (int i : new int[] {44, 45, 63, 64, 94, 95, 96, 124}) {
                forceReplace[i] = true;
            }
        }

        StringBuilder buffer = new StringBuilder(fieldText.length() * 2);

        for (int i = 0; i < fieldText.length(); i++) {
            int code = fieldText.charAt(i);

            // Checking the case when the character is already escaped
            // Just push "&#" to the buffer and keep going from the next char
            if ((code == 38) && (fieldText.charAt(i + 1) == 35)) {
                i += 2;
                buffer.append("&#");
                code = fieldText.charAt(i);
            }

            // TODO: Check whether > 125 is correct here or whether it should rather be >=
            if ((code > 125) || forceReplace[code]) {
                buffer.append("&#").append(code).append(';');
            } else {
                buffer.append((char) code);
            }
        }
        fieldText = buffer.toString();

        // use common abbreviations for <, > instead of code
        for (Map.Entry<String, String> entry : ASCII_TO_XML_CHARS.entrySet()) {
            fieldText = fieldText.replace(entry.getKey(), entry.getValue());
        }

        return fieldText;
    }
}

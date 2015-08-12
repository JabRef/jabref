/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.export.layout.format;

import java.util.HashMap;

class XmlCharsMap extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    public XmlCharsMap() {
        put("\\{\\\\\\\"\\{a\\}\\}", "&#x00E4;");
        put("\\{\\\\\\\"\\{A\\}\\}", "&#x00C4;");
        put("\\{\\\\\\\"\\{e\\}\\}", "&#x00EB;");
        put("\\{\\\\\\\"\\{E\\}\\}", "&#x00CB;");
        put("\\{\\\\\\\"\\{i\\}\\}", "&#x00EF;");
        put("\\{\\\\\\\"\\{I\\}\\}", "&#x00CF;");
        put("\\{\\\\\\\"\\{o\\}\\}", "&#x00F6;");
        put("\\{\\\\\\\"\\{O\\}\\}", "&#x00D6;");
        put("\\{\\\\\\\"\\{u\\}\\}", "&#x00FC;");
        put("\\{\\\\\\\"\\{U\\}\\}", "&#x00DC;");

        //next 2 rows were missing...
        put("\\{\\\\\\`\\{a\\}\\}", "&#x00E0;");
        put("\\{\\\\\\`\\{A\\}\\}", "&#x00C0;");

        put("\\{\\\\\\`\\{e\\}\\}", "&#x00E8;");
        put("\\{\\\\\\`\\{E\\}\\}", "&#x00C8;");
        put("\\{\\\\\\`\\{i\\}\\}", "&#x00EC;");
        put("\\{\\\\\\`\\{I\\}\\}", "&#x00CC;");
        put("\\{\\\\\\`\\{o\\}\\}", "&#x00F2;");
        put("\\{\\\\\\`\\{O\\}\\}", "&#x00D2;");
        put("\\{\\\\\\`\\{u\\}\\}", "&#x00F9;");
        put("\\{\\\\\\`\\{U\\}\\}", "&#x00D9;");

        //corrected these 10 lines below...
        put("\\{\\\\\\'\\{a\\}\\}", "&#x00E1;");
        put("\\{\\\\\\'\\{A\\}\\}", "&#x00C1;");
        put("\\{\\\\\\'\\{e\\}\\}", "&#x00E9;");
        put("\\{\\\\\\'\\{E\\}\\}", "&#x00C9;");
        put("\\{\\\\\\'\\{i\\}\\}", "&#x00ED;");
        put("\\{\\\\\\'\\{I\\}\\}", "&#x00CD;");
        put("\\{\\\\\\'\\{o\\}\\}", "&#x00F3;");
        put("\\{\\\\\\'\\{O\\}\\}", "&#x00D3;");
        put("\\{\\\\\\'\\{u\\}\\}", "&#x00FA;");
        put("\\{\\\\\\'\\{U\\}\\}", "&#x00DA;");
        //added next four chars...
        put("\\{\\\\\\'\\{c\\}\\}", "&#x0107;");
        put("\\{\\\\\\'\\{C\\}\\}", "&#x0106;");
        put("\\{\\\\c\\{c\\}\\}", "&#x00E7;");
        put("\\{\\\\c\\{C\\}\\}", "&#x00C7;");

        put("\\{\\\\\\\uFFFD\\{E\\}\\}", "&#x00C9;");
        put("\\{\\\\\\\uFFFD\\{i\\}\\}", "&#x00ED;");
        put("\\{\\\\\\\uFFFD\\{I\\}\\}", "&#x00CD;");
        put("\\{\\\\\\\uFFFD\\{o\\}\\}", "&#x00F3;");
        put("\\{\\\\\\\uFFFD\\{O\\}\\}", "&#x00D3;");
        put("\\{\\\\\\\uFFFD\\{u\\}\\}", "&#x00FA;");
        put("\\{\\\\\\\uFFFD\\{U\\}\\}", "&#x00DA;");
        put("\\{\\\\\\\uFFFD\\{a\\}\\}", "&#x00E1;");
        put("\\{\\\\\\\uFFFD\\{A\\}\\}", "&#x00C1;");

        //next 2 rows were missing...
        put("\\{\\\\\\^\\{a\\}\\}", "&#x00E2;");
        put("\\{\\\\\\^\\{A\\}\\}", "&#x00C2;");

        put("\\{\\\\\\^\\{o\\}\\}", "&#x00F4;");
        put("\\{\\\\\\^\\{O\\}\\}", "&#x00D4;");
        put("\\{\\\\\\^\\{u\\}\\}", "&#x00F9;");
        put("\\{\\\\\\^\\{U\\}\\}", "&#x00D9;");
        put("\\{\\\\\\^\\{e\\}\\}", "&#x00EA;");
        put("\\{\\\\\\^\\{E\\}\\}", "&#x00CA;");
        put("\\{\\\\\\^\\{i\\}\\}", "&#x00EE;");
        put("\\{\\\\\\^\\{I\\}\\}", "&#x00CE;");

        put("\\{\\\\\\~\\{o\\}\\}", "&#x00F5;");
        put("\\{\\\\\\~\\{O\\}\\}", "&#x00D5;");
        put("\\{\\\\\\~\\{n\\}\\}", "&#x00F1;");
        put("\\{\\\\\\~\\{N\\}\\}", "&#x00D1;");
        put("\\{\\\\\\~\\{a\\}\\}", "&#x00E3;");
        put("\\{\\\\\\~\\{A\\}\\}", "&#x00C3;");

        put("\\{\\\\\\\"a\\}", "&#x00E4;");
        put("\\{\\\\\\\"A\\}", "&#x00C4;");
        put("\\{\\\\\\\"e\\}", "&#x00EB;");
        put("\\{\\\\\\\"E\\}", "&#x00CB;");
        put("\\{\\\\\\\"i\\}", "&#x00EF;");
        put("\\{\\\\\\\"I\\}", "&#x00CF;");
        put("\\{\\\\\\\"o\\}", "&#x00F6;");
        put("\\{\\\\\\\"O\\}", "&#x00D6;");
        put("\\{\\\\\\\"u\\}", "&#x00FC;");
        put("\\{\\\\\\\"U\\}", "&#x00DC;");

        //next 2 rows were missing...
        put("\\{\\\\\\`a\\}", "&#x00E0;");
        put("\\{\\\\\\`A\\}", "&#x00C0;");

        put("\\{\\\\\\`e\\}", "&#x00E8;");
        put("\\{\\\\\\`E\\}", "&#x00C8;");
        put("\\{\\\\\\`i\\}", "&#x00EC;");
        put("\\{\\\\\\`I\\}", "&#x00CC;");
        put("\\{\\\\\\`o\\}", "&#x00F2;");
        put("\\{\\\\\\`O\\}", "&#x00D2;");
        put("\\{\\\\\\`u\\}", "&#x00F9;");
        put("\\{\\\\\\`U\\}", "&#x00D9;");
        put("\\{\\\\\\'e\\}", "&#x00E9;");
        put("\\{\\\\\\'E\\}", "&#x00C9;");
        put("\\{\\\\\\'i\\}", "&#x00ED;");
        put("\\{\\\\\\'I\\}", "&#x00CD;");
        put("\\{\\\\\\'o\\}", "&#x00F3;");
        put("\\{\\\\\\'O\\}", "&#x00D3;");
        put("\\{\\\\\\'u\\}", "&#x00FA;");
        put("\\{\\\\\\'U\\}", "&#x00DA;");
        put("\\{\\\\\\'a\\}", "&#x00E1;");
        put("\\{\\\\\\'A\\}", "&#x00C1;");
        //added next two chars...
        put("\\{\\\\\\'c\\}", "&#x0107;");
        put("\\{\\\\\\'C\\}", "&#x0106;");

        //next two lines were wrong...
        put("\\{\\\\\\^a\\}", "&#x00E2;");
        put("\\{\\\\\\^A\\}", "&#x00C2;");

        put("\\{\\\\\\^o\\}", "&#x00F4;");
        put("\\{\\\\\\^O\\}", "&#x00D4;");
        put("\\{\\\\\\^u\\}", "&#x00F9;");
        put("\\{\\\\\\^U\\}", "&#x00D9;");
        put("\\{\\\\\\^e\\}", "&#x00EA;");
        put("\\{\\\\\\^E\\}", "&#x00CA;");
        put("\\{\\\\\\^i\\}", "&#x00EE;");
        put("\\{\\\\\\^I\\}", "&#x00CE;");
        put("\\{\\\\\\~o\\}", "&#x00F5;");
        put("\\{\\\\\\~O\\}", "&#x00D5;");
        put("\\{\\\\\\~n\\}", "&#x00F1;");
        put("\\{\\\\\\~N\\}", "&#x00D1;");
        put("\\{\\\\\\~a\\}", "&#x00E3;");
        put("\\{\\\\\\~A\\}", "&#x00C3;");
    }

}

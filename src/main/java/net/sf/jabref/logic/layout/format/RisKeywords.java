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
package net.sf.jabref.logic.layout.format;

import java.util.Set;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;

public class RisKeywords implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Set<String> keywords = net.sf.jabref.model.entry.EntryUtil.getSeparatedKeywords(s);
        int i = 0;
        for (String keyword : keywords) {
            sb.append("KW  - ");
            sb.append(keyword);
            if (i < (keywords.size() - 1)) {
                sb.append(StringUtil.NEWLINE);
            }
            i++;
        }
        return sb.toString();
    }
}

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
package net.sf.jabref.logic.mods;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Michael Wrighton
 * @author S M Mahbub Murshed
 *
 */
public class PageNumbers {

    private String freeform;
    private int start;
    private int end;

    private static final Pattern PAGE_PATTERN = Pattern.compile("\\s*(\\d+)\\s*-{1,2}\\s*(\\d+)\\s*");

    public PageNumbers(String s) {
        parsePageNums(s);
    }

    private void parsePageNums(String numberString) {
        Matcher matcher = PAGE_PATTERN.matcher(numberString);
        if (matcher.matches()) {
            start = Integer.parseInt(matcher.group(1));
            end = Integer.parseInt(matcher.group(2));
        } else {
            freeform = numberString;
        }
    }

    public Element getDOMrepresentation(Document document) {
        Element result = document.createElement("extent");
        result.setAttribute("unit", "page");
        if (freeform == null) {
            Element tmpStart = document.createElement("start");
            Element tmpEnd = document.createElement("end");
            tmpStart.appendChild(document.createTextNode(String.valueOf(this.start)));
            tmpEnd.appendChild(document.createTextNode(String.valueOf(this.end)));
            result.appendChild(tmpStart);
            result.appendChild(tmpEnd);
        } else {
            Node textNode = document.createTextNode(freeform);
            result.appendChild(textNode);
        }
        return result;
    }

    public String toString(String separator) {
        if (freeform != null) {
            return freeform;
        }
        return start + separator + end;
    }

    @Override
    public String toString() {
        return toString("--");
    }

}

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
package net.sf.jabref.logic.mods;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.jabref.util.Util;

/**
 * @author Michael Wrighton
 * @author S M Mahbub Murshed
 *
 */
public class PageNumbers {

    private String freeform;
    private int start;
    private int end;


    public PageNumbers(String s) {
        parsePageNums(s);
    }

    private void parsePageNums(String numberString) {
        Pattern pattern = Pattern.compile("\\s*(\\d+)\\s*-{1,2}\\s*(\\d+)\\s*");
        Matcher matcher = pattern.matcher(numberString);
        if (matcher.matches()) {
            start = Util.intValueOf(matcher.group(1));
            end = Util.intValueOf(matcher.group(2));
        } else {
            freeform = numberString;
        }
    }

    public Element getDOMrepresentation(Document document) {
        Element result = document.createElement("extent");
        result.setAttribute("unit", "page");
        if (freeform != null) {
            Node textNode = document.createTextNode(freeform);
            result.appendChild(textNode);
        }
        else {
            Element start = document.createElement("start");
            Element end = document.createElement("end");
            start.appendChild(document.createTextNode("" + this.start));
            end.appendChild(document.createTextNode("" + this.end));
            result.appendChild(start);
            result.appendChild(end);
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

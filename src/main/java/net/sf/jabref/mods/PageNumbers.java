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
package net.sf.jabref.mods;
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
	String freeform = null;
	int start, end;
	
	public PageNumbers(String s) {
		parsePageNums(s);
	}
	
	protected void parsePageNums(String s) {
		Pattern p = Pattern.compile("\\s*(\\d+)\\s*-{1,2}\\s*(\\d+)\\s*");
		Matcher m = p.matcher(s);
		if (m.matches()) {
			start = Integer.parseInt(m.group(1));
			end = Integer.parseInt(m.group(2));
		}
		else
			freeform = s;
	}
	
	public Element getDOMrepresentation(Document d) {
		Element result = d.createElement("extent");
		result.setAttribute("unit","page");
		if (freeform != null) { 
			Node t = d.createTextNode(freeform);
			result.appendChild(t);
		}
		else {
			Element start = d.createElement("start");
			Element end = d.createElement("end");
			start.appendChild(d.createTextNode("" + this.start));
			end.appendChild(d.createTextNode("" + this.end));
			result.appendChild(start);
			result.appendChild(end);			
		}
		return result;
	}
	
    public String toString(String seperator) {
    	if (freeform != null)
    		return freeform; 
		return (start+seperator+end);
    }

	public String toString() {
		return toString("--");
    }
 
}

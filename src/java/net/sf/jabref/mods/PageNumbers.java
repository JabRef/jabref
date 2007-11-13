/*
 * Created on Oct 29, 2004
 * Updated on May 03, 2007
 *
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

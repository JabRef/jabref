/*
 * Created on Oct 29, 2004
 * Updated on May 03, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref.mods;
import net.sf.jabref.export.layout.format.*;
import net.sf.jabref.export.layout.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.util.regex.*;
/**
 * @author Michael Wrighton
 * @author S M Mahbub Murshed
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PageNumbers {
	String freeform = null;
	int start, end;
	
	public PageNumbers(String s) {
		parsePageNums(s);
	}
	
	protected void parsePageNums(String s) {
		s = s.replaceAll("–","-"); // Remove special dash that looks like same but is not
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

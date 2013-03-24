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
package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * 
 * Warning -- it is not a generic filter, only read is implemented!
 *
 * Note: this is just a quick port of the original SPIRESBibtexFilterReader.
 * 
 * @author Fedor Bezrukov
 * @author Sheer El-Showk
 * 
 * @version $Id$
 * 
 * TODO: Fix grammar in bibtex entries -- it ma return invalid bibkeys (with space)
 * 
 */
public class INSPIREBibtexFilterReader extends FilterReader {

    protected BufferedReader in;

    private String line;
    private int pos;
    private boolean pre;

    INSPIREBibtexFilterReader(Reader _in) { 
    	super(_in);
    	in = new BufferedReader(_in);
    	pos=-1;
    	pre=false;
    }

    private String readpreLine() throws IOException {
    	String l;
    	do {
    		l=in.readLine();
    		if (l==null)
    			return null;
    		if (l.equals("<pre>")) {
    			pre = true;
    			l=in.readLine();
    		}
    		if (l.equals("</pre>"))
    			pre = false;
    	} while (!pre);
    	return l;
    }
    
    private String fixBibkey(String in) {
    	if (in== null)
    		return null;
    	//System.out.println(in);
    	if ( in.matches("@Article\\{.*,") ) {
    		//System.out.println(in.replace(' ','_'));
    		return in.replace(' ', '_');
    	} else
    		return in;
    }

    public int read() throws IOException {
    	if ( pos<0 ) {
    		line=fixBibkey(readpreLine());
    		pos=0;
	    	if ( line == null )
	    		return -1;
    	}
    	if ( pos>=line.length() ) {
    		pos=-1;
    		return '\n';
    	}
    	return line.charAt(pos++);
    }

}

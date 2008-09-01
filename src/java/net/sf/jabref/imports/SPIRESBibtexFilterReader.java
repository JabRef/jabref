package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * 
 * Warning -- it is not a generic filter, only read is implemented!
 * 
 * @author Fedor Bezrukov
 * 
 * @version $Id$
 * 
 * TODO: Fix grammar in bibtex entries -- it ma return invalid bibkeys (with space)
 * 
 */
public class SPIRESBibtexFilterReader extends FilterReader {

    protected BufferedReader in;

    private String line;
    private int pos;
    private boolean pre;

    SPIRESBibtexFilterReader(Reader _in) { 
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

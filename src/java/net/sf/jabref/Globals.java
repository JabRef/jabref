/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JFileChooser;
import java.util.logging.*;
import java.io.IOException;
public class Globals {

    private static String resourcePrefix = "resource/JabRef";
    private static String logfile= "jabref.log";
    public static ResourceBundle messages;
	
    public void logThis(String s){
	Logger.global.info(s);
    }
    
    public void turnOffLogging(){ // only log exceptions
	Logger.global.setLevel(java.util.logging.Level.SEVERE); 
    }

    // should be only called ones
    public void turnOnConsoleLogging(){
	Logger.global.addHandler( new java.util.logging.ConsoleHandler());
    }

    public void turnOnFileLogging(){
	Logger.global.setLevel(java.util.logging.Level.ALL);	
	java.util.logging.Handler handler;
	try{
	    handler = new FileHandler(logfile);// this will overwrite
	}catch (IOException e){ //can't open log file so use console
	    handler =  new ConsoleHandler() ; 
	}
	Logger.global.addHandler( handler);

	handler.setFilter(new Filter() { // select what gets logged
		public boolean isLoggable(LogRecord record) {
		    return true;
		}
	    });
    }

    /**
     * String constants.
     */
    public static final String
	KEY_FIELD = "bibtexkey",
	SEARCH = "search",
	GROUPSEARCH = "groupsearch",
	// Using this when I have no database open when I read
	// non bibtex file formats (used byte ImportFormatReader.java
		DEFAULT_BIBTEXENTRY_ID="__ID";

    public static void setLanguage(String language, String country) {
	messages = ResourceBundle.getBundle(resourcePrefix,
					    new Locale(language,
						       country));

    }

    public static String lang(String key){
	String translation;
	try{
	    translation=Globals.messages.getString(key.replaceAll(" ","_"));
	}catch(MissingResourceException ex){
	    translation= key;
	    System.err.println("Warning: could not get translation for \""
			       + key +"\"");
	}
	return translation.replaceAll("_"," ");
    }
    //============================================================
    // this is incomplete...i need to add all the types here
    //============================================================    
    static BibtexEntryType getEntryType(String type){
	// decide which entryType object to return
	
	if(type.equals("article"))
	    return BibtexEntryType.ARTICLE;
	else if(type.equals("book"))
	    return BibtexEntryType.BOOK;
	else if(type.equals("inproceedings"))
	    return BibtexEntryType.INPROCEEDINGS;
	else //if(type.equals("other"))
	    return BibtexEntryType.OTHER;
    }

    //========================================================
    // lot of abreviations in medline
	// PKC etc convert to {PKC} ... 
    //========================================================
    static Pattern titleCapitalPattern=Pattern.compile("[A-Z]+");    

    static String putBracesAroundCapitals(String title){
		StringBuffer buf = new StringBuffer();
		
		Matcher mcr=Globals.titleCapitalPattern.matcher(title.substring(1));
		boolean found =false;
		while((found=mcr.find())){
			String replaceStr = mcr.group();
			mcr.appendReplacement(buf,"{"+replaceStr+"}");
		}
		mcr.appendTail(buf);
		String titleCap=title.substring(0,1)+buf.toString();
		return titleCap;
    }
	
	
	/*    public static void setupKeyBindings(JabRefPreferences prefs) {
		  
	
	}*/
}

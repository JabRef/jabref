/*
Copyright (C) 2003 Nathan Dunn, Morten O. Alver, Nizar N. Batada

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
package net.sf.jabref.label;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import java.util.StringTokenizer ; 

public class BookLabelRule extends DefaultLabelRule {

    // this is the rule used handle articles
    // we try (first author)/(year)
    public String applyRule(BibtexEntry oldEntry){
        String oldLabel = (String) (oldEntry.getField(Globals.KEY_FIELD)) ; 
        String newLabel = "" ; 


        StringTokenizer authorTokens = null ; 
        // use the author token
        try{ 
            if((String) oldEntry.getField("author")!= null){
                authorTokens= new StringTokenizer((String) oldEntry.getField("author"),",") ; 
            }else
            if((String) oldEntry.getField("editor")!= null){
                authorTokens= new StringTokenizer((String) oldEntry.getField("editor"),",") ; 
            }
            newLabel += authorTokens.nextToken().toLowerCase() ; 
        }catch(Throwable t){
			System.out.println("error getting author/editor: "+t) ; 
        }

        // use the year token
        try{
            if( oldEntry.getField("year")!= null){
                newLabel += String.valueOf( oldEntry.getField("year")) ;  
            }
        }catch(Throwable t){
			System.out.println("error getting author: "+t) ; 
        }

        newLabel += "book" ;
        
	//	oldEntry.setField(Globals.KEY_FIELD,newLabel) ; 
	return newLabel; 
    }


//    public static void main(String args[]){
//        
//        System.out.println(args[0]) ; 
//        BibtexEntry entry = new BibtexEntry("1",BibtexEntryType.ARTICLE) ; 
//        entry.setField("journal",args[0]) ; 
//        entry.setField("author","jones, b") ; 
//        entry.setField("year","1984") ; 
//        BookLabelRule rule = new BookLabelRule() ; 
//        entry = rule.applyRule(entry) ; 
////        System.out.println(entry.getField("journal") ); 
//        System.out.println(entry.getField(BibtexBaseFrame.KEY_PROPERTY) ); 
//
//    }
    
}




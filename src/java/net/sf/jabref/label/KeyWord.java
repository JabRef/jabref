/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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

import java.util.Hashtable ; 

public class KeyWord{

   
    private KeyWord(){
        // puts all keywords in 
        setKeyWords() ; 
    }

    private static void setKeyWords(){
        keyWordTable.put("society","society") ; 
        keyWordTable.put("transaction","transaction") ; 
        keyWordTable.put("transactions","transactions") ; 
        keyWordTable.put( "journal" , "journal" )  ; 
        keyWordTable.put( "review" , "review" )  ; 
        keyWordTable.put( "revue" , "revue" )  ; 
        keyWordTable.put( "communication" , "communication" )  ; 
        keyWordTable.put( "communications" , "communications" )  ; 
        keyWordTable.put( "letters" , "letters" )  ; 
        keyWordTable.put( "advances" , "advances" )  ; 
        keyWordTable.put( "proceedings" , "proceedings" )  ; 
        keyWordTable.put( "proceeding" , "proceeding" )  ; 
        keyWordTable.put( "international" , "international" )  ; 
        keyWordTable.put( "joint" , "joint" )  ; 
        keyWordTable.put( "conference" , "conference" )  ; 
    }


    // accessors, if users, or anyone would want to change these defaults
    // later
    public static void addKeyWord(String newKeyWord){
		keyWordTable.put(newKeyWord,newKeyWord) ; 
    }

    public static String removeKeyWord(String newKeyWord){
		return (String) keyWordTable.remove(newKeyWord) ; 

    }

    
   public static boolean isKeyWord(String matchWord){
       if(keyWordTable.size()==0){
		   setKeyWords() ; 
       }
       if(keyWordTable.containsKey(matchWord.toLowerCase())) {
            return true ; 
       }
       return false ; 
   }

   public static boolean isKeyWordMatchCase(String matchWord){
       if(keyWordTable.size()==0){
		   setKeyWords() ; 
       }
       if(keyWordTable.containsKey(matchWord)) {
            return true ; 
       }
       return false ; 
   }

   private static Hashtable keyWordTable = new Hashtable() ; 

}


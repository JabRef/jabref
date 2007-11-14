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

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;

public class ArticleLabelRule extends DefaultLabelRule {

    // this is the rule used handle articles
    // we try (first author last name)/(year)/(first unique journal word)
    public String applyRule(BibtexEntry oldEntry){
        String oldLabel = (oldEntry.getField(BibtexFields.KEY_FIELD)) ;
        String newLabel = "" ;

        String author="";

        //## to be done: i need to check if the key is unique else need to make another one with suffix
        try{
            author=oldEntry.getField("author");
            String[] tokens= author.split("\\band\\b");
            if( tokens.length > 0){ // if author is empty
                if(tokens[0].indexOf(",") > 0)
                    tokens[0] = AuthorList.fixAuthor_firstNameFirst( tokens[0] ); // convert lastname, firstname to firstname lastname
                String[] firstAuthor = tokens[0].replaceAll("\\s+"," ").split(" ");
                // lastname, firstname

                newLabel += firstAuthor[ firstAuthor.length-1];
            }
        }catch(Throwable t){
            System.out.println("error getting author: "+t) ;
        }

        // use the year token
        try{
            if( ! newLabel.equals("")){
                if( oldEntry.getField("year")!= null){
                    newLabel += String.valueOf( oldEntry.getField("year")) ;
                }
            }else
                newLabel=oldLabel; // don't make a key since there is no author
        }catch(Throwable t){
            System.out.println("error getting year: "+t) ;
        }

// now check for uniqueness
// i need access to basepanes: checkForDuplicateKey

//oldEntry.setField(Globals.KEY_FIELD,newLabel) ;
        return newLabel ;


/*
// use the journal name
// return the first token 4 wrds or longer, that's not journal
// , society, or the like (using the Keyword class)
try{

if(oldEntry.getField("journal") != null) {
authorTokens = new StringTokenizer( ((String) oldEntry.getField("journal")).replaceAll(","," ").replaceAll("/"," ")) ;
String tempString = authorTokens.nextToken() ;
tempString = tempString.replaceAll(",","") ;
boolean done = false ;
while(tempString!=null && !done ){
tempString = tempString.replaceAll(",","").trim() ;
if(tempString.trim().length() > 3 && !KeyWord.isKeyWord(tempString))  {
done = true ;
}
else{

if(authorTokens.hasMoreTokens()){
tempString = authorTokens.nextToken() ;
}else{
done = true ;
}
}
}

if(tempString!=null && (tempString.indexOf("null")<0) ){
newLabel += String.valueOf( tempString.toLowerCase()) ;
}
}
}
catch(Throwable t){  System.err.println(t) ; }
*/

    }


//    public static void main(String args[]){
//
//        System.out.println(args[0]) ;
//        BibtexEntry entry = new BibtexEntry("1",BibtexEntryType.ARTICLE) ;
//        entry.setField("journal",args[0]) ;
//        entry.setField("author","jones, b") ;
//        entry.setField("year","1984") ;
//        ArticleLabelRule rule = new ArticleLabelRule() ;
//        entry = rule.applyRule(entry) ;
//        System.out.println(entry.getField(BibtexBaseFrame.KEY_PROPERTY) );
//
//    }

}




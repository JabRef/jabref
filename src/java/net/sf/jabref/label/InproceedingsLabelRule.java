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
package net.sf.jabref.label;

import java.util.StringTokenizer;

import net.sf.jabref.BibtexEntry;

public class InproceedingsLabelRule extends DefaultLabelRule {

    // this is the rule used handle articles
    // we try (first author)/(year)/(first unique booktitle word)
    public String applyRule(BibtexEntry oldEntry){
        String newLabel = "" ;

        StringTokenizer authorTokens = null ;
        // use the author token
        try{
            authorTokens= new StringTokenizer(oldEntry.getField("author"),",") ;
            newLabel += authorTokens.nextToken().toLowerCase().replaceAll(" ","").replaceAll("\\.","")   ;
        }catch(Throwable t){
                        System.out.println("error getting author: "+t) ;
        }

        // use the year token
        try{
            if( oldEntry.getField("year")!= null){
                newLabel += String.valueOf( oldEntry.getField("year")) ;
            }
        }catch(Throwable t){
                        System.out.println("error getting year: "+t) ;
        }

        // use the booktitle name
        // return the first token 4 wrds or longer, that's not a keyword
        try{

          if(oldEntry.getField("booktitle") != null) {
            authorTokens = new StringTokenizer( (oldEntry.getField("booktitle")).replaceAll(","," ").replaceAll("/"," ")) ;
            String tempString = authorTokens.nextToken() ;
            tempString = tempString.replaceAll(",","") ;
            boolean done = false ;
            while(tempString!=null && !done ){
                tempString = tempString.replaceAll(",","").trim() ;
                if(tempString.trim().length() > 3 && !KeyWord.getKeyWord().isKeyWord(tempString))  {
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

        //	oldEntry.setField(Globals.KEY_FIELD,newLabel) ;
        return newLabel;
    }



}

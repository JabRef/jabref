/*
Copyright (C) 2004 R. Nagel

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

// created by : r.nagel 27.10.2004
//
// function : check all bibtex items and report errors, inconsistencies and others
//
//     todo : find equal authors: e.g.: D. Knuth = Donald Knuth = Donald E. Knuth
//            and try to give all items an identically look
//
// modified :



package net.sf.jabref.wizard.integrity ;

import net.sf.jabref.*;
import java.util.*;

public class IntegrityCheck
{
  public IntegrityCheck()
  {
  }

  public Vector checkBibtexDatabase(BibtexDatabase base)
  {
    Vector back = new Vector() ;
    if (base != null)
    {
      Collection col = base.getEntries() ;
      for( Iterator myIt = col.iterator() ; myIt.hasNext() ;)
      {
        Object dat = myIt.next() ;
        if (dat != null)
        {
          BibtexEntry item = ( BibtexEntry ) dat ;

          Vector result = checkBibtexEntry(item) ;

          if (result != null)
            back.addAll(result);
        }
      }

    }

    return back ;
  }

  public Vector checkBibtexEntry(BibtexEntry entry)
  {
    if (entry == null)
      return null ;

    Vector back = new Vector() ;

    Object name = entry.getField("author") ;
    if (name != null)
      back.addAll( authorCheck( name.toString())) ;

    return back ;
  }

  private Vector authorCheck(String authors)
  {
    Vector back = new Vector() ;

    // try to extract the structure of author tag
    // N = name, ","= seperator, "a" = and
    StringBuffer structure = new StringBuffer() ;
    int len = authors.length() ;
    int mode = -1 ;
    for (int t = 0 ; t < len ; t++)
    {
      char ch = authors.charAt(t) ;
      switch (ch)
      {
        case ',' :
          if (mode == 5) // "and"
            structure.append('a') ;
          else
            structure.append('N') ;

          structure.append(',') ;
          mode = 0 ;
          break ;

        case ' ' :
          if (mode == 5) // "and"
            structure.append('a') ;
          else
            if (mode != 0)
              structure.append('N') ;
          mode = -1 ; // blank processed
          break ;
       case 'a' :
         if (mode == -1)
           mode = 2 ;
         break ;
       case 'n' :
         if (mode == 2)
           mode = 3 ;
         break ;
       case 'd' :
         if (mode == 3)
           mode = 5 ;
         break ;
       default :
         mode = 1 ;
      }
    }
    if (mode == 5) // "and"
      structure.append('a') ;
    else
      if (mode != 0)
        structure.append('N') ;

    // Check
    len = structure.length() ;
    if (len > 0)
    {
      boolean failed = false ;
      char z1 = structure.charAt(0) ;

      if (structure.charAt(0) != 'N')  // must start by name
      {
        back.add("beginning of author field");
        failed = true ;
      }

      if (structure.charAt( structure.length() -1) != 'N')  // end without seperator
      {
        back.add("bad end (author field)");
        failed = true ;
      }
      if (structure.indexOf("NN,NN") > -1)
      {
        back.add("something could be wrong in author field") ;
        failed = true ;
      }

//      if (failed)
//        System.out.println(authors +" #" +structure.toString() +"#") ;
    }

    return back ;
  }


}

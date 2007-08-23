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
// function : check all bibtex items and report errors, inconsistencies,
//            warnings, hints and ....
//
//     todo : find equal authors: e.g.: D. Knuth = Donald Knuth = Donald E. Knuth
//            and try to give all items an identically look
//
// modified :



package net.sf.jabref.wizard.integrity ;

import java.util.Vector;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

public class IntegrityCheck
{
  private Vector<IntegrityMessage> messages ;

  public IntegrityCheck()
  {
    messages = new Vector<IntegrityMessage>() ;
  }

  public Vector<IntegrityMessage> checkBibtexDatabase(BibtexDatabase base) {
		messages.clear();
		if (base != null) {
			for (BibtexEntry entry : base.getEntries()) {
				checkSingleEntry(entry);
			}
		}
		return new Vector<IntegrityMessage>(messages);
	}

	public Vector<IntegrityMessage> checkBibtexEntry(BibtexEntry entry) {
		messages.clear();
		checkSingleEntry(entry);
		return new Vector<IntegrityMessage>(messages);
	}

  public void checkSingleEntry(BibtexEntry entry)
  {
    if (entry == null)
      return ;

    Object data = entry.getField("author") ;
    if (data != null)
      authorNameCheck( data.toString(), "author", entry) ;

    data = entry.getField("editor") ;
    if (data != null)
      authorNameCheck( data.toString(), "editor", entry) ;

    data = entry.getField("title") ;
    if (data != null)
      titleCheck( data.toString(), "title", entry) ;

    data = entry.getField("year") ;
    if (data != null)
      yearCheck( data.toString(), "year", entry) ;
  }

 /** fills the class Vector (of IntegrityMessage Objects) which did inform about
  *  failures, hints....
  *  The Authors or Editors field could be invalid -> try to detect it!
  *  Knuth, Donald E. and Kurt Cobain and A. Einstein = N,NNaNNaNN
  */
  private void authorNameCheck(String names, String fieldName, BibtexEntry entry)
  {
    // try to extract the structure of author tag
    // N = name, ","= seperator, "a" = and
    StringBuffer structure = new StringBuffer() ;
    int len = names.length() ;
    int mode = -1 ;
    for (int t = 0 ; t < len ; t++)
    {
      char ch = names.charAt(t) ;
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
      if (structure.charAt(0) != 'N')  // must start by name
      {
        messages.add( new IntegrityMessage( IntegrityMessage.NAME_START_WARNING,
                                            entry, fieldName, null))  ;
//        back.add("beginning of " +fieldName +" field");
      }

      if (structure.charAt( structure.length() -1) != 'N')  // end without seperator
      {
        messages.add( new IntegrityMessage( IntegrityMessage.NAME_END_WARNING,
                                            entry, fieldName, null))  ;
//        back.add("bad end (" +fieldName +" field)");
      }
      /*if (structure.indexOf("NN,NN") > -1)
      {
        messages.add( new IntegrityMessage( IntegrityMessage.NAME_SEMANTIC_WARNING,
                                            entry, fieldName, null))  ;

//        back.add("something could be wrong in " +fieldName +" field") ;
      } */
    }
//    messages.add( new IntegrityMessage( IntegrityMessage.NAME_END_WARNING,
//                                        entry, fieldName, null))  ;

  }



  private void titleCheck(String title, String fieldName, BibtexEntry entry)
  {
    int len = title.length() ;
    int mode = 0 ;
    int upLowCounter = 0 ;
//    boolean lastWasSpace = false ;
    for (int t = 0 ; t < len ; t++)
    {
      char ch = title.charAt( t ) ;
      switch (ch)
      {
        case '}' : // end of Sequence
          if (mode == 0)
          {
            // closing brace '}' without an opening
            messages.add( new IntegrityMessage( IntegrityMessage.UNEXPECTED_CLOSING_BRACE_FAILURE,
                                            entry, fieldName, null))  ;
          }
          else  // mode == 1
          {
            mode-- ;
//            lastWasSpace = true ;
          }
          break ;

        case '{' :  // open {
          mode++ ;
          break ;

        case ' ' :
//          lastWasSpace = true ;
          break ;

        default :
          if (mode == 0) // out of {}
          {
            if ( Character.isUpperCase(ch) && (t > 1))
            {
              upLowCounter++ ;
            }
          }
      }
    }
    if (upLowCounter > 0)
    {

        /*
        Morten Alver (2006.10.10):

        Disabling this warning because we have a feature for automatically adding
        braces when saving, which makes this warning misleading. It could be modified
        to suggest to use this feature if not enabled, and not give a warning if the
        feature is enabled.

        messages.add( new IntegrityMessage( IntegrityMessage.UPPER_AND_LOWER_HINT,
                                        entry, fieldName, null))  ;*/

    }
  }

  /** Checks, if the number String contains a four digit year */
  private void yearCheck(String number, String fieldName, BibtexEntry entry)
  {
    int len = number.length() ;
    int digitCounter = 0 ;
    boolean fourDigitsBlock = false ;
    boolean containsFourDigits = false ;

    for (int t = 0 ; t < len ; t++)
    {
      char ch = number.charAt( t ) ;
      if ( Character.isDigit(ch))
      {
        digitCounter++ ;
        if (digitCounter == 4)
          fourDigitsBlock = true ;
        else
          fourDigitsBlock = false ;
      } else
      {
        if (fourDigitsBlock)
          containsFourDigits = true ;

        digitCounter = 0 ;
      }
    }

    if ((!containsFourDigits) && (!fourDigitsBlock))
    {
      messages.add( new IntegrityMessage( IntegrityMessage.FOUR_DIGITS_HINT,
                                      entry, fieldName, null))  ;
    }
  }
}

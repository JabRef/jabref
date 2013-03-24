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

// created by : r.nagel 13.10.2004
//
// function : handles the subdatabase from aux command line option
//
// modified :

package net.sf.jabref.wizard.auximport ;

import java.util.Vector;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

public class AuxCommandLine
{
  private String auxName ;
  private BibtexDatabase bib ;

  public AuxCommandLine(String auxFileName, BibtexDatabase refDBase)
  {
    auxName = Util.getCorrectFileName(auxFileName, "aux") ;
    bib = refDBase ;
  }

  public BibtexDatabase perform()
  {
    BibtexDatabase back = null ;
    if ( (auxName.length() > 0) && (bib != null) )
    {
      AuxSubGenerator auxParser = new AuxSubGenerator(bib) ;
      Vector<String> returnValue = auxParser.generate(auxName, bib) ;
      back = auxParser.getGeneratedDatabase() ;

      // print statistics
//      System.out.println(Globals.lang( "Results" ));
      System.out.println( Globals.lang("keys_in_database") +" " +bib.getEntryCount() ) ;
      System.out.println( Globals.lang("found_in_aux_file") +" "+auxParser.getFoundKeysInAux());
      System.out.println( Globals.lang("resolved") +" " +auxParser.getResolvedKeysCount());
      if (auxParser.getNotResolvedKeysCount() > 0)
      {
        System.out.println( Globals.lang( "not_found" ) + " " +
                            auxParser.getNotResolvedKeysCount() ) ;
        System.out.println( returnValue ) ;
      }
      int nested = auxParser.getNestedAuxCounter() ;
      if (nested > 0)
        System.out.println( Globals.lang("nested_aux_files") +" " +nested);

    }
    return back ;
  }
}

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

// created by : r.nagel 23.08.2004
//
// modified :

package net.sf.jabref.wizard.auximport ;

import java.io.* ;
import java.util.regex.* ;
import java.util.* ;

import net.sf.jabref.* ;
import net.sf.jabref.imports.* ;

/**
 * <p>Title: Latex Aux to Bibtex</p>
 *
 * <p>Description: generates a sub-database which contains only bibtex entries
 * from input aux file</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p>Company: </p>
 *
 * @version 1.0
 * @author r.nagel
 *
 * @todo Redesign of dialog structure for an assitent like feeling....
 *   Now the unknown bibtex entries cannot inserted into the reference database
 *   without closing the dialog.
 */

public class AuxSubGenerator
{

  private HashSet mySet ; // all unique bibtex keys in aux file

  private Vector notFoundList ; // all not solved bibtex keys

  private BibtexDatabase db ; // reference database
  private BibtexDatabase auxDB ; // contains only the bibtex keys who found in aux file


  public AuxSubGenerator(BibtexDatabase refDBase)
  {
    mySet = new HashSet(20) ;
    notFoundList = new Vector() ;
    db = refDBase ;
  }

  public final void setReferenceDatabase(BibtexDatabase newRefDB)
  {
    db = newRefDB ;
  }

  /**
   * parseAuxFile
   * read the Aux file and fill up some intern data structures
   *
   * @param filename String : Path to LatexAuxFile
   * @return boolean, true = no error occurs
   */
  public final boolean parseAuxFile(String filename)
  {
    // regular expressions
    Pattern pattern ;
    Matcher matcher ;

    // while condition
    boolean weiter = false ;

    // return value -> default: no error
    boolean back = true ;

    // the important tag
    pattern = Pattern.compile( "\\\\citation\\{.+\\}" ) ;

    // input-file-buffer
    BufferedReader br = null ;

    try
    {
      br = new BufferedReader( new FileReader( filename ) ) ;
      weiter = true ;
    }
    catch ( FileNotFoundException fnfe )
    {
      System.out.println( "Cannot locate input file! " + fnfe.getMessage() ) ;
      // System.exit( 0 ) ;
      back = false ;
    }

    while ( weiter )
    {
      String line ;
      try
      {
        line = br.readLine() ;
      }
      catch ( IOException ioe )
      {
        line = null ;
        weiter = false ;
      }

      if ( line != null )
      {
        matcher = pattern.matcher(line) ;

        while ( matcher.find() )
        {
          // extract the bibtex-key(s) XXX from \citation{XXX} string
          int len = matcher.end() - matcher.start() ;
          if ( len > 11 )
          {
            String str = matcher.group().substring( matcher.start() + 10,
                                                    matcher.end() - 1 ) ;
            // could be an comma separated list of keys
            String keys[] = str.split(",") ;
            if (keys != null)
            {
              int keyCount = keys.length ;
              for ( int t = 0 ; t < keyCount ; t++ )
              {
                String dummyStr = keys[t] ;
                if (dummyStr != null)
                {
                  // delete all unnecessary blanks and save key into an set
                  mySet.add( dummyStr.trim() ) ;
//                System.out.println("found " +str +" in AUX") ;
                }
              }
            }
          }
        }
      }
      else weiter = false ;
    }  // end of while

    if (back) // only close, if open sucessful
    {
      try
      {
        br.close() ;
      }
      catch ( IOException ioe )
      {}
    }

    return back ;
  }

  /**
   * resolveTags
   * Try to find an equivalent bibtex entry into reference database for all keys
   * (found in aux file). This methode will fill up some intern data structures.....
   */
  public final void resolveTags()
  {
    auxDB = new BibtexDatabase() ;
    notFoundList.clear();

    Iterator it = mySet.iterator() ;

    // forall bibtex keys (found in aux-file) try to find an equivalent
    // entry into reference database
    while (it.hasNext())
    {
      String str = (String) it.next() ;
      BibtexEntry entry = db.getEntryByKey(str);

      if (entry == null)
      {
        notFoundList.add(str) ;
      } else
      {
        try
        {
          auxDB.insertEntry( entry ) ;
        }
        catch (Exception e) {}
      }
    }
  }

  /**
   * generate
   * Shortcut methode for easy generation.
   *
   * @param auxFileName String
   * @param bibDB BibtexDatabase - reference database
   * @return Vector - contains all not resolved bibtex entries
   */
  public final Vector generate(String auxFileName, BibtexDatabase bibDB)
  {
    setReferenceDatabase(bibDB);
    parseAuxFile(auxFileName) ;
    resolveTags();

    return notFoundList ;
  }

  public BibtexDatabase getGeneratedDatabase()
  {
    if (auxDB == null)
      auxDB = new BibtexDatabase() ;

    return auxDB ;
  }

  public final int getFoundKeysInAux()
  {
    return mySet.size() ;
  }

  public final int getResolvedKeysCount()
  {
    return auxDB.getEntryCount() ;
  }

  public final int getNotResolvedKeysCount()
  {
    return notFoundList.size() ;
  }

  /** reset used all datastructures */
  public final void clear()
  {
    mySet.clear() ;
    notFoundList.clear();
  }

  /** returns a vector off all not resolved bibtex entries found in auxfile */
  public Vector getNotFoundList()
  {
    return notFoundList ;
  }

}

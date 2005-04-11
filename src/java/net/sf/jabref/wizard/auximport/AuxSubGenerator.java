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
 *   Now - the unknown bibtex entries cannot inserted into the reference
 *   database without closing the dialog.
 */

// created by : r.nagel 23.08.2004
//
// modified : - 11.04.2005
//              handling \\@input{file.aux} tag in aux files (nested aux files)


package net.sf.jabref.wizard.auximport ;

import java.io.* ;
import java.util.regex.* ;
import java.util.* ;

import net.sf.jabref.* ;
import net.sf.jabref.imports.* ;

public class AuxSubGenerator
{

  private HashSet mySet ; // all unique bibtex keys in aux file

  private Vector notFoundList ; // all not solved bibtex keys

  private BibtexDatabase db ; // reference database
  private BibtexDatabase auxDB ; // contains only the bibtex keys who found in aux file

  private int nestedAuxCounter ;  // counts the nested aux files


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
   * read the Aux file and fill up some intern data structures.
   * Nested aux files (latex \\include) supported!
   *
   * @param filename String : Path to LatexAuxFile
   * @return boolean, true = no error occurs
   */

  // found at comp.text.tex
  //  > Can anyone tell be the information held within a .aux file?  Is there a
  //  > specific format to this file?
  //
  // I don't think there is a particular format. Every package, class
  // or document can write to the aux file. The aux file consists of LaTeX macros
  // and is read at the \begin{document} and again at the \end{document}.
  //
  // It usually contains information about existing labels
  //  \\newlabel{sec:Intro}{{1}{1}}
  // and citations
  //  \citation{hiri:conv:1993}
  // and macros to write information to other files (like toc, lof or lot files)
  //  \@writefile{toc}{\contentsline {section}{\numberline
  // {1}Intro}{1}}
  // but as I said, there can be a lot more

  // aux file :
  //
  // \\citation{x}  x = used reference of bibtex library entry
  //
  // \\@input{x}  x = nested aux file
  //
  // the \\bibdata{x} directive contains information about the
  // bibtex library file -> x = name of bib file
  //
  // \\bibcite{x}{y}
  //   x is a label for an item and y is the index in bibliography

  public final boolean parseAuxFile(String filename)
  {
    // regular expressions
    Pattern pattern ;
    Matcher matcher ;

    // while condition
    boolean weiter = false ;

    // return value -> default: no error
    boolean back = true ;

    // fileopen status
    boolean loopFileOpen = false ;

    // the important tag
    pattern = Pattern.compile( "\\\\citation\\{.+\\}" ) ;

    // input-file-buffer
    BufferedReader br = null ;

    // filelist, used for nested aux files
    Vector fileList = new Vector(5) ;
    fileList.add( filename );

    // get the file path
    File dummy = new File( filename ) ;
    String path = dummy.getParent() ;
    if (path != null)
      path = path + dummy.separator ;
    else
      path = "" ;

    nestedAuxCounter = -1 ;  // count only the nested reads

    // index of current file in list
    int fileIndex = 0 ;

    while (fileIndex < fileList.size())
    {
      String fName = (String) fileList.elementAt( fileIndex ) ;
      try
      {
//        System.out.println("read #"+fName +"#") ;
        br = new BufferedReader( new FileReader( fName ) ) ;
        weiter = true ;
        loopFileOpen = true ;
      }
      catch ( FileNotFoundException fnfe )
      {
        System.out.println( "Cannot locate input file! " + fnfe.getMessage() ) ;
        // System.exit( 0 ) ;
        back = false ;
        weiter = false ;
        loopFileOpen = false ;
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
          matcher = pattern.matcher( line ) ;

          while ( matcher.find() )
          {
            // extract the bibtex-key(s) XXX from \citation{XXX} string
            int len = matcher.end() - matcher.start() ;
            if ( len > 11 )
            {
              String str = matcher.group().substring( matcher.start() + 10,
                  matcher.end() - 1 ) ;
              // could be an comma separated list of keys
              String keys[] = str.split( "," ) ;
              if ( keys != null )
              {
                int keyCount = keys.length ;
                for ( int t = 0 ; t < keyCount ; t++ )
                {
                  String dummyStr = keys[t] ;
                  if ( dummyStr != null )
                  {
                    // delete all unnecessary blanks and save key into an set
                    mySet.add( dummyStr.trim() ) ;
//                System.out.println("found " +str +" in AUX") ;
                  }
                }
              }
            }
          }
          // try to find a nested aux file
          int index = line.indexOf( "\\@input{" ) ;
          if ( index >= 0 )
          {
            int start = index + 8 ;
            int end = line.indexOf( "}", start ) ;
            if ( end > start )
            {
              String str = path + line.substring( index + 8, end ) ;

              // if filename already in filelist
              if (!fileList.contains( str ) )
              {
                 fileList.add(str);   // insert file into filelist
              }
            }
          }
        } // line != null
        else weiter = false ;
      } // end of while

      if ( loopFileOpen ) // only close, if open sucessful
      {
        try
        {
          br.close() ;
          nestedAuxCounter++ ;
        }
        catch ( IOException ioe )
        {}
      }

      fileIndex++ ; // load next file
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

  /** reset all used datastructures */
  public final void clear()
  {
    mySet.clear() ;
    notFoundList.clear();
    // db = null ;  ???
  }

  /** returns a vector off all not resolved bibtex entries found in auxfile */
  public Vector getNotFoundList()
  {
    return notFoundList ;
  }

  /** returns the number of nested aux files, read by the last call of
   *  generate method */
  public int getNestedAuxCounter()
  {
    return this.nestedAuxCounter ;
  }

/*
  public class FileNameString extends String
  {
    public boolean equals(Object anObject)
    {
      if (anObject == null)
        return false ;

      if (anObject.hashCode() == this.hashCode())
        return true ;

      return false ;
    }
  }
*/
}

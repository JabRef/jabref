/*
 Copyright (C) 2005 R. Nagel

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

// created by : r.nagel 01.06.2005
//
// function : read build informations from build.properies file
//
//
// modified:
//

package net.sf.jabref.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TBuildInfo
{
  private String BUILD_DATE = "" ;
  private String BUILD_VERSION = "devel - 1st edition family" ;
  private String BUILD_NUMBER = "1" ;

//  private TBuilderInfo runtime = new TBuildInfo() ;
  public TBuildInfo(String path)
  {
    readBuildVersionData(path) ;
  }

// --------------------------------------------------------------------------
  // some informations from extern build file
  private void readBuildVersionData(String path)
  {
    String buf = null ;
    int sep = 0 ;
    String Key, Value ;
    BufferedReader input = null ;

    try
    {

      input = new BufferedReader(
          new InputStreamReader( getClass().getResourceAsStream( path) ), 100 ) ;
    }
    catch ( Exception e1 )
    {
//      System.out.println( e1 ) ;
//      e1.printStackTrace();
//      Logger.global.info( e1.getMessage() ) ;
        return ;
    }

    try
    {
      while ( ( buf = input.readLine() ) != null )
      {
        if ( buf.length() > 0 )
        { // no empty lines
          if ( buf.charAt( 0 ) != '#' )
          { // data line, comments - first char = #
            sep = buf.indexOf( '=' ) ;
            if ( sep > 0 )
            { // = found
              Key = buf.substring( 0, sep ) ;
              Value = buf.substring( sep + 1 ) ;
              if ( Key.equals( "builddate" ) )
              {
                BUILD_DATE = Value ;
              }
              else if ( Key.equals( "build" ) )
              {
                BUILD_NUMBER = Value ;
              }
              else if ( Key.equals( "version" ) )
              {
                BUILD_VERSION = Value ;
              }

            }
          } // data line
        }
      } // while
    }
    catch ( IOException iex )
    {
//      System.err.println(iex.getMessage());
//      Logger.global.info( iex.getMessage() ) ;
    }

    try
    {
      input.close() ;
    }
    catch ( Exception e )
    {
//      System.out.println(e.getMessage());
//      Logger.global.info( e.getMessage() ) ;
    }
  }

  // --------------------------------------------------------------------------

  public String getBUILD_DATE()
  {
    return BUILD_DATE;
  }

  public String getBUILD_VERSION()
  {
    return BUILD_VERSION;
  }

  public String getBUILD_NUMBER()
  {
    return BUILD_NUMBER;
  }


// --------------------------------------------------------------------------

}

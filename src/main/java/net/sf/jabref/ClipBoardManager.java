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
// created by : r.nagel 14.09.2004
//
// function : handle all clipboard action
//
// modified :


package net.sf.jabref ;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipBoardManager implements ClipboardOwner
{
  public static ClipBoardManager clipBoard = new ClipBoardManager() ;

  /**
   * Empty implementation of the ClipboardOwner interface.
   */
  public void lostOwnership( Clipboard aClipboard, Transferable aContents )
  {
    //do nothing
  }

  /**
   * Place a String on the clipboard, and make this class the
   * owner of the Clipboard's contents.
   */
  public void setClipboardContents( String aString )
  {
    StringSelection stringSelection = new StringSelection( aString ) ;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard() ;
    clipboard.setContents( stringSelection, this ) ;
  }

  /**
   * Get the String residing on the clipboard.
   *
   * @return any text found on the Clipboard; if none found, return an
   * empty String.
   */
  public String getClipboardContents()
  {
    String result = "" ;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard() ;
    //odd: the Object param of getContents is not currently used
    Transferable contents = clipboard.getContents( null ) ;
    if ( ( contents != null ) &&
        contents.isDataFlavorSupported( DataFlavor.stringFlavor ) )
    {
      try
      {
        result = ( String ) contents.getTransferData( DataFlavor.stringFlavor ) ;
      }
      catch ( UnsupportedFlavorException ex )
      {
        //highly unlikely since we are using a standard DataFlavor
        System.out.println( ex ) ;
      }
      catch ( IOException ex )
      {
        System.out.println( ex ) ;
      }
    }
    return result ;
  }
}

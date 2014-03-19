/*  Copyright (C) 2003-2011 Raik Nagel
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
// function : simple xml reader functions

package net.sf.jabref.util ;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public class TXMLReader
{
  private Document config ; // XML data

    private boolean ready = false ;

  public TXMLReader(String resPath)
  {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try
    {
        DocumentBuilder builder = factory.newDocumentBuilder();

      InputStream stream = null ;
      if (resPath != null)
      {
        stream = TXMLReader.class.getResourceAsStream( resPath ) ;
      }
      // not found, check the src/ directory (IDE mode)
      if (stream == null)
      {
        try
        {
          stream = new FileInputStream( "src" +resPath ) ;
        }
        catch (Exception ignored)
        {

        }
      }

      if (stream != null)
      {
        config = builder.parse(stream) ;
        ready = true ;
      }
    }
    catch (Exception oe)
    {
      oe.printStackTrace();
    }
  }

  // ---------------------------------------------------------------------------

  public boolean isReady()
  {
    return ready ;
  }


  public NodeList getNodes( String name )
  {
    return config.getElementsByTagName( name ) ;
  }

  // ---------------------------------------------------------------------------

  private Element getFirstElement( Element element, String name )
  {
    NodeList nl = element.getElementsByTagName( name ) ;
    if ( nl.getLength() < 1 )
    {
      throw new RuntimeException(
          "Element: " + element + " does not contain: " + name ) ;
    }
    return ( Element ) nl.item( 0 ) ;
  }

  /** returns all "plain" data of a subnode with name <name> */
  public String getSimpleElementText( Element node, String name )
  {
    Element namedElement = getFirstElement( node, name ) ;
    return getSimpleElementText( namedElement ) ;
  }

  /** collect all "plain" data of a xml node */
  public String getSimpleElementText( Element node )
  {
    StringBuffer sb = new StringBuffer() ;
    NodeList children = node.getChildNodes() ;
    for ( int i = 0 ; i < children.getLength() ; i++ )
    {
      Node child = children.item( i ) ;
      if ( child instanceof Text )
      {
        sb.append( child.getNodeValue().trim() ) ;
      }
    }
    return sb.toString() ;
  }

  // ---------------------------------------------------------------------------
  // read some attributes
  // --------------------------------------------------------------------------
  public int readIntegerAttribute( Element node, String attrName, int defaultValue )
  {
    int back = defaultValue ;
    if ( node != null )
    {
      String data = node.getAttribute( attrName ) ;
      if ( data != null )
      {
        if ( data.length() > 0 )
        {
          try
          {
            back = Integer.parseInt( data ) ;
          }
          catch (Exception ignored) {}
        }
      }
    }
    return back ;
  }

  public String readStringAttribute( Element node, String attrName, String defaultValue )
  {
    if ( node != null )
    {
      String data = node.getAttribute( attrName ) ;
      if ( data != null )
      {
        if ( data.length() > 0 )
        {
          return data ;
        }
      }
    }
    return defaultValue ;
  }

  public double readDoubleAttribute( Element node, String attrName, double defaultValue )
  {
    if ( node != null )
    {
      String data = node.getAttribute( attrName ) ;
      if ( data != null )
      {
        if ( data.length() > 0 )
        {
          return Double.parseDouble( data ) ;
        }
      }
    }
    return defaultValue ;
  }

}

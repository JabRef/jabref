/*
 Copyright (C) 2006 Raik Nagel <kiar@users.sourceforge.net>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 * Neither the name of the author nor the names of its contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// created by : r.nagel 19.04.2006
//
// function : simple xml reader functions
//
//
// modified :

package net.sf.jabref.util ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class TXMLReader
{
  private Document config ; // XML data
  private DocumentBuilderFactory factory ;
  private DocumentBuilder builder ;

  private boolean ready = false ;

  public TXMLReader(String resPath)
  {
    factory = DocumentBuilderFactory.newInstance() ;
    try
    {
      builder = factory.newDocumentBuilder() ;

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
        catch (Exception e)
        {

        }
      }

      if (stream != null)
      {
        config = builder.parse( stream ) ;
        ready = true ;
      }
    }

    catch ( SAXException sxe )
    {
      sxe.printStackTrace() ;
    }
    catch ( ParserConfigurationException pce )
    {
      pce.printStackTrace() ;
    }
    catch ( IOException ioe )
    {
      ioe.printStackTrace() ;
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
          catch (Exception e) {}
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

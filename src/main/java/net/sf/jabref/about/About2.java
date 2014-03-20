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
// created by : r.nagel 05.05.2005
//
// function : new about dialog
//

package net.sf.jabref.about ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.jabref.Globals;

public class About2 extends JDialog implements ActionListener
{
  private static final long serialVersionUID = 1L ;

  // AboutDialog constructor
  public About2( JFrame parent )
  {
    super( parent, Globals.lang("About JabRef"), true ) ;

//    setDefaultCloseOperation( EXIT_ON_CLOSE ) ;

    JPanel contentPanel = new JPanel( new BorderLayout() ) ;
    contentPanel.setBackground( Color.white);
//    content.setBorder( new EmptyBorder( 12, 12, 12, 12 ) ) ;
    setContentPane( contentPanel ) ;

    contentPanel.add( BorderLayout.CENTER, new ExtendedInfoPanel(this) ) ;

    this.setBackground( Color.white);

    pack() ;
    setResizable( false ) ;
    setLocationRelativeTo( parent ) ;
    setVisible( true ) ;
  }

  protected void processWindowEvent( WindowEvent e )
  {
    super.processWindowEvent( e ) ;
    if ( e.getID() == WindowEvent.WINDOW_CLOSING )
    {
      System.exit( 0 ) ;
    }
  }

  public void actionPerformed( ActionEvent e )
  {
    String cmd = e.getActionCommand() ;
    if ( cmd.equals( "close" ) )
    {

      setVisible( false ) ;
      dispose() ;
//      System.exit( 0 ) ;
    }
    else if ( cmd.equals( "license" ) )
    {
//      showLicense() ;
    }
  }

// ----------------------------------------------------------------------------

}

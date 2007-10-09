/*
 animated about dialog

Copyright (C) 2005 Raik Nagel <kiar@users.sourceforge.net>
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

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
// function : content for the about dialog
//
// modified :
//            28.07.2005
//            - hide license button

package net.sf.jabref.about ;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class ExtendedInfoPanel extends JPanel implements AnimationListener, ActionListener
{
  private JEditorPane textPane ;
  private JScrollPane scroller ;
  private CardLayout cards ;

  private JButton close ;
  private JButton license ;

  private AboutPanel about ;
  private JPanel infoPanel ;

  private boolean animationIsRunning = true ;

  private ActionListener mainListener ;

  public ExtendedInfoPanel(ActionListener mainFrame)
  {
    mainListener = mainFrame ;

    // animated area
    about = new AboutPanel();
    about.addAnimationListener(this);

    // scrallable html infos
    textPane = new JEditorPane() ;

    textPane.setEditable( false ) ;
//    URL helpURL = getClass().getResource( "/help/About.html" ) ;
    // try to load about.html for the locale language
    URL helpURL = getClass().getResource( GUIGlobals.getLocaleHelpPath() + GUIGlobals.aboutPage) ;
    // about.html could not detected => try to load the default version
    if (helpURL == null)
    {
      helpURL = getClass().getResource( GUIGlobals.helpPre + GUIGlobals.aboutPage) ;
    }

    if ( helpURL != null )
    {
      try
      {
        textPane.setPage( helpURL ) ;
      }
      catch ( IOException e )
      {
        System.err.println( "Attempted to read a bad URL: " + helpURL ) ;
      }
    }
    else
    {
      System.err.println( "Couldn't find file: About.html" ) ;
    }

    scroller = new JScrollPane(textPane) ; //, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) ;
    scroller.setPreferredSize( about.getSize());

    // overlapped animated/scrollable area
    infoPanel = new JPanel() ;
    cards = new CardLayout() ;
    infoPanel.setLayout( cards);

    infoPanel.add(about, "ani") ;
    infoPanel.add(scroller, "inf") ;

     // Buttons
    JPanel buttonPanel = new JPanel() ;
    buttonPanel.setBackground( Color.white);
    buttonPanel.setLayout( new GridLayout(1, 2, 10, 20) );
    buttonPanel.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED));

    buttonPanel.add( Box.createGlue() ) ;
    close = new JButton( Globals.lang("Skip") ) ;
    close.addActionListener( this ) ;
    close.setActionCommand( "close" ) ;
    close.setFocusable( false ) ;

    license = new JButton( Globals.lang("License") ) ;
    license.addActionListener( this ) ;
    license.setActionCommand( "license" ) ;
    license.setFocusable( false ) ;

    buttonPanel.add( close ) ;
//    buttonPanel.add( license ) ;
    buttonPanel.add( Box.createGlue() ) ;


    // main panel
    this.setLayout( new BorderLayout(0, 0));

    this.add(infoPanel, BorderLayout.CENTER) ;
    this.add(buttonPanel, BorderLayout.SOUTH) ;

//    about.startAnimation();
  }

  public void animationReady()
  {
    animationIsRunning = false ;
    cards.show(infoPanel, "inf");
    close.setText(Globals.lang("Close"));
  }

  public void actionPerformed( ActionEvent e )
  {
    String cmd = e.getActionCommand() ;
    if ( cmd.equals( "close" ) )
    {
      if (animationIsRunning)
      {
         about.skipAnimation(); // implicit call of AnimationListener.animationReady()
      }
      else
      {
        setVisible( false ) ;
        mainListener.actionPerformed(e);
      }
    }
    else if ( cmd.equals( "license" ) )
    {
//      showLicense() ;
      mainListener.actionPerformed(e);
    }
  }

}

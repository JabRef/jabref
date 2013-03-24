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

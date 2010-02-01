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
// modified :


package net.sf.jabref.about ;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

// AboutPanel class
public class AboutPanel extends JComponent
{
  private Vector<TextBlock> textBlocks ;
  private String versionStr ;
  private String buildStr ;
  private AnimationThread thread;
  private ImageIcon image;


  // animated text positions
  public int TOP = 300 ; // offset from top - hide
  public int BOTTOM = 0 ; // show

  public int HEIGHT = 500 ;
  public int WIDTH = 500 ;

  private int borders[] ;  // Border-Coordinates of paintarea (xLeft, xRight, yTop, yBottom)
  private int paintWidth ;

  private Font font1 ;
  private Font font2 ;
  private Font font3 ;

  private AnimationListener aniListener ;
  private ImageProducer iProducer ;

  AboutPanel()
  {
    Font font = loadFont("ASTROLYT.TTF") ;
    font1 = font.deriveFont(Font.BOLD, (float) 14.0) ;
    font2 = font.deriveFont(Font.BOLD, (float) 20.0) ;

    font = loadFont("AUGIE.TTF") ;
    font3 = font.deriveFont(Font.BOLD, (float) 14.0) ;

    versionStr = "Version " + Globals.VERSION ;
    buildStr = " build " + Globals.BUILD ;

    image = new ImageIcon(getClass().getResource("/images/autumn.png"));

    HEIGHT = image.getIconHeight() ;
    WIDTH = image.getIconWidth() ;

    FontMetrics fm = getFontMetrics( font2 ) ;
    TOP = 2*fm.getHeight() ;
    fm = getFontMetrics( font1 ) ;
    BOTTOM = 2*fm.getHeight() ;

    borders = new int[4] ;
    borders[0] = 0 ;
    borders[1] = WIDTH ;
    borders[2] = TOP ;
    borders[3] = HEIGHT - TOP - BOTTOM ;


    paintWidth = borders[1] - borders[0] ;

    setForeground( Color.black) ;
    setBackground( Color.white) ;

    setBorder ( BorderFactory.createBevelBorder( BevelBorder.RAISED)) ;

    textBlocks = new Vector<TextBlock>( 50 ) ;

    loadAboutText() ;

    this.setDoubleBuffered(true);

    thread = new AnimationThread();
  }
// ----------------------------------------------------------------------------

  public void addAnimationListener(AnimationListener listener)
  {
    aniListener = listener ;
  }

// ----------------------------------------------------------------------------

  // returns
  private int getMiddleX(String text, Font font)
  {
    FontMetrics fm = getFontMetrics(font);
    return ( paintWidth/2 - ((fm.stringWidth(text) + 10) / 2)) ;
  }

// ----------------------------------------------------------------------------

  private Font loadFont(String fontName)
  {
    Font back = UIManager.getFont("Label.font") ;
    try
    {
      InputStream myStream = getClass().getResourceAsStream(GUIGlobals.fontPath + fontName) ;
      back = Font.createFont(Font.TRUETYPE_FONT, myStream) ;
    }
    catch (Exception e) { System.out.println(e) ; }

    return back ;
  }

// ----------------------------------------------------------------------------

  private final void loadAboutText()
  {
    TextBlock block = null ;
    AboutTextLine aLine = null ;

    int index = -3 ;
    FontMetrics fm = getFontMetrics(font3);
    try
    {
      InputStream stream = getClass().getResourceAsStream(GUIGlobals.getLocaleHelpPath() + "credits.txt" ) ;
      if (stream == null)
      {
        stream = getClass().getResourceAsStream( GUIGlobals.helpPre +
                                                 "credits.txt" ) ;
      }
      InputStreamReader reader = new InputStreamReader( stream ) ;
      BufferedReader input = new BufferedReader(reader, 1000) ;

      while ( input.ready() )
      {
        String line = input.readLine() ;

        if (line != null)
        {
          line = line.trim() ;

          if (line.length() > 0)
          {
            if (line.charAt(0) == '#')  // new Block....
            {
              if (block != null)  //insert previous block
              {
                textBlocks.add(block) ;
                index+=2 ;
              }

              aLine = new AboutTextLine( line.substring(1).trim()) ;
              aLine.setTag(2);
              aLine.setPos(getMiddleX(aLine.getText(), font2), borders[0] -fm.getHeight()*(index+3)*1.5);
              aLine.setDirection(0.0, 1.0);
              aLine.setFont(font2);

              block = new TextBlock() ;
              block.setHeading(aLine);
              block.setVisible(true);

            }
            else  // Blocklines
            {
              aLine = new AboutTextLine( line.trim() ) ;
              aLine.setPos( getMiddleX( line, font3 ),
                            borders[3] + ( index * fm.getHeight() * 1.5 ) ) ;
              aLine.setTag( 10 ) ;
              aLine.setDirection( 0.0, -1.0 ) ;
              aLine.setFont( font3 ) ;

              block = new TextBlock() ;
              block.add( aLine ) ;
              block.setVisible(true);
              
              index++ ;
            }
          }
        }
      }
      input.close() ;
    }

    catch ( Exception e )
    {
      block = new TextBlock() ;
      block.setHeading( new AboutTextLine("failure") );
      String line = "no infos available" ;
      aLine = new AboutTextLine( line ) ;
      block.add( aLine ) ;
    }

    textBlocks.add(block);  // insert last block
  }

// ----------------------------------------------------------------------------

  public void paintComponent( Graphics g )
  {
    if (thread.mode == 0)
    {
      thread.start();
//      thread.setEnabled(true);
    }
    else
    if (thread.mode == 1)
    {
      image.paintIcon(this, g, 0, 0);

      FontMetrics fm = g.getFontMetrics( font1 ) ;

      int x1 = ( getWidth() - fm.stringWidth( versionStr ) ) / 2 ;
      int y1 = getHeight() - fm.getHeight() - 4 ;
      int y2 = getHeight() - 5 ;
/*
      int x1 = ( getWidth() - fm.stringWidth( versionStr ) ) / 2 ;
      int y1 = 4 ;
      int y2 = fm.getHeight() +4 ;
*/
      g.setFont( font1 ) ;

      g.setColor( Color.black ) ;
      g.drawString( versionStr, x1, y1 ) ;
      g.drawString( buildStr, x1, y2 ) ;

      g.setFont( font2) ;
      fm = g.getFontMetrics( font2 ) ;
      g.drawString( "JabRef", (getWidth() - fm.stringWidth("JabRef")) /2, fm.getHeight()+10) ;


      for ( TextBlock block : textBlocks){
        if (block.isVisible()) // only if Block is marked as visible
        {
          // print Heading
          AboutTextLine head = block.getHeading() ;
          drawLine(head, g) ;

          for (AboutTextLine line : block){
            drawLine(line, g) ;
          }
        }
      }
    }
    else
    {
      image.paintIcon(this, g, 0, 0);
    }
  }
// ----------------------------------------------------------------------------

  private void drawLine(AboutTextLine line, Graphics g)
  {
    int x = line.getPosX() ;
    int y = line.getPosY() ;
    if ( ( x > borders[0] - 10 ) && ( x < borders[1] + 10 ) &&
         ( y > borders[2] - 10 ) && ( y < borders[3] + 10 ) )
    {
      if ( line.getVisible() )
      {
        g.setFont( line.getFont() ) ;
        g.setColor( line.getColor() ) ;
        g.drawString( line.getText(), line.getPosX(), line.getPosY() ) ;
      }
    }
  }
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

  public Dimension getPreferredSize()
  {
    return new Dimension( WIDTH, HEIGHT ) ;
  }

  public void removeNotify()
  {
    super.removeNotify();
    thread.kill();
  }

  public void skipAnimation()
  {
    thread.kill() ;
    if (aniListener != null) aniListener.animationReady();
  }

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

  class AnimationThread extends Thread
  {
    private boolean running = true ;
    private double help01 = 1.0 ;
    private int mode = 0 ;
    public int sleepTime = 50 ;
    private int zone2Counter = 0 ;
    private boolean runMode = true ;

    AnimationThread()
    {
      super( "About box animation thread" ) ;
      setPriority( Thread.MIN_PRIORITY ) ;
    }

    public void kill()
    {
      running = false ;
    }

    public void setEnabled(boolean onOff) { runMode = onOff ; }

    public synchronized void setMode(int newMode) { mode = newMode ; }

    public void run()
    {
      Object mutex = new Object() ;

      mode = 1 ;
      runMode = true ;
      while ( running )
      {

        synchronized(mutex)
        {
          try
          {
            do
            {
              mutex.wait( sleepTime ) ;
            } while (!runMode) ;
          }
          catch ( Exception e )
          {
          }
        }
        if (mode == 1)  // Textanimation
        {
          int counter = 0 ;

          for ( Enumeration<TextBlock> myE = textBlocks.elements() ; myE.hasMoreElements() ; )
          {
            TextBlock block = myE.nextElement() ;
            AboutTextLine head = block.getHeading() ;
            counter = performStep(head) ;

            for (AboutTextLine line : block){
            	counter += performStep( line ) ;
            }
          }
          if (counter < 1)
          {
            mode = 2 ;
          }
          repaint(borders[0]-10, borders[2]-10, borders[1]+10, borders[3]+10) ;
        }
        else if (mode == 2)  // Picture animation
        {
          if (sleepTime < 2)
            sleepTime = 5 ;
          else
            sleepTime -= sleepTime / 3 ;

          image.setImage( createImage( iProducer) );
          repaint(0, 0, WIDTH, HEIGHT) ;
        }
      }
    }

    private int performStep(AboutTextLine line)
    {
      int back = 0 ;

      line.performTimeStep( 1.0 ) ;

      if ( line.getTag() == 2 )  // Heading
      {
        int zone = (int) (HEIGHT / 3.5) ;
        if ( line.getPosY() > zone )
        {
          line.setSpeed( 0.0 ) ;
          line.setTag( 4 ) ;
          zone2Counter = 0 ;
        } else
        if ( line.getPosY() > ( zone - 10) )
        {
          zone2Counter=1 ;
        }
        back++ ;
      }
      else if ( line.getTag() == 4) // Heading Blender
      {
        if (zone2Counter < 1)
        {
          Color col = line.getColor() ;
          int rgb = col.getRGB() + 1023 ;
          line.setColor( new Color( rgb ) ) ;
        }
        else
        {
          line.setVisible(false);
          line.setTag(5);
        }
      }
      else if ( line.getTag() == 10 )  // scrolling text
      {
        if ( line.getPosY() < ( HEIGHT / 3 ) )
        {
          line.setDirection( help01, 0.0 ) ;
          line.setAccel( 0.5 ) ;
          line.setTag( 11 ) ;
          help01 = help01 * -1.0 ;
        }
        back = 1 ;
      }
      else if (line.getTag() == 11) // text line out
      {
         if ((line.getPosX() < -100) || (line.getPosX() > WIDTH+100))
         {
           line.setTag(12);
           line.setVisible(false);
         }
         back = 1 ;
      }

      return back ;
    }
  }

}


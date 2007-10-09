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
// function : a animated line for about dialog
//


package net.sf.jabref.about ;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;

import javax.swing.UIManager;

// a single About Line
public class AboutTextLine
{
  private String text ;
  private double direction[] ;
  private double pos[] ;
  private double speed ;
  private double accel ;
  private int tag ;  // additional user information
  private Font font ;
  private Color color ;

  private boolean visible ;

  public AboutTextLine(String txt)
  {
    Random rand = new Random(System.currentTimeMillis()* this.hashCode()) ;

    text = txt ;

    pos = new double[2] ;
    pos[0] = rand.nextDouble() *100.0 ;
    pos[1] = rand.nextDouble() *100.0 ;

    direction = new double [2] ;
    direction[0] = rand.nextDouble() ;
    direction[1] = rand.nextDouble() ;

    accel = 0.0 ;
    speed = 1.0 ;

    tag = 0 ;

    color = Color.black ;

    visible = true ;

    font = UIManager.getFont("Label.font") ;
  }

// ------------------------------------------------------------------------

  public void performTimeStep(double time)
  {
    accel = accel * time ;
    speed += accel ;

    double move = speed * time ;  //

    pos[0] += (move * direction[0]) ;
    pos[1] += (move * direction[1]) ;
  }

// ------------------------------------------------------------------------

  public String toString()
  {
    return (text + "<" +pos[0] +", " +pos[1] +">"
                 + "<" +direction[0] +", " +direction[1] +">" ) ;
  }

// ------------------------------------------------------------------------

  public int getPosX()
  {
    return (int) pos[0] ;
  }

  public int getPosY()
  {
    return (int) pos[1] ;
  }

  public double[] getPos()
  {
    return pos;
  }

  public void setPos(double posX, double posY)
  {
    this.pos[0] = posX ;
    this.pos[1] = posY ;
  }

 // ------------------------------------------------------------------------

  public String getText()
  {
    return text;
  }

  public void setText(String pText)
  {
    this.text = pText;
  }

// ------------------------------------------------------------------------

  public double[] getDirection()
  {
    return direction;
  }

  public void setDirection(double dirX, double dirY)
  {
    this.direction[0] = dirX ;
    this.direction[1] = dirY ;
  }

// ------------------------------------------------------------------------

  public double getSpeed()
  {
    return speed;
  }

  public void setSpeed(double pSpeed)
  {
    this.speed = pSpeed;
  }

// ------------------------------------------------------------------------

  public double getAccel()
  {
    return accel;
  }

  public void setAccel(double pAccel)
  {
    this.accel = pAccel;
  }

// ------------------------------------------------------------------------

  public int getTag()
  {
    return tag;
  }

  public void setTag(int pTag)
  {
    this.tag = pTag;
  }

// ------------------------------------------------------------------------

  public Font getFont()
  {
    return font;
  }
  public void setFont(Font pFont)
  {
    this.font = pFont;
  }

// ------------------------------------------------------------------------

  public Color getColor()
  {
    return color;
  }
  public void setColor(Color pColor)
  {
    this.color = pColor;
  }

// ------------------------------------------------------------------------

  public boolean getVisible()
  {
    return visible;
  }
  public void setVisible(boolean pVisible)
  {
    this.visible = pVisible;
  }
// ------------------------------------------------------------------------
// ------------------------------------------------------------------------

}

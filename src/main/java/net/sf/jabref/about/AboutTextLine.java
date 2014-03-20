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

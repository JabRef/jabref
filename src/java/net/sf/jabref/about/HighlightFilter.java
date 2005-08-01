/*
 * @(#)HighlightFilter.java	1.6 98/03/18
 *
 * Copyright (c) 1995-1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package net.sf.jabref.about ;

import java.awt.* ;
import java.awt.image.* ;

/**
 * An image filter to highlight an image by brightening or darkening
 * the pixels in the images.
 *
 * @author 	Jim Graham
 * @version 	1.6, 03/18/98
 */
class HighlightFilter extends RGBImageFilter
{
  boolean brighter ;
  int percent ;
  private int middleX ;
  private int middleY ;
  private int dimX ;
  private int dimY ;
  private int distance = 0 ;
  private int startSize = 10 ;

  private int white = Color.white.getRGB() ;
  private int black = Color.black.getRGB() ;

  public HighlightFilter( boolean b, int p )
  {
    brighter = b ;
    percent = p ;
    canFilterIndexColorModel = true ;
  }

  public void setMiddle(int x, int y)
  {
    middleX = x/2 ;
    middleY = y/2 ;
    dimX = x ;
    dimY = y ;
    distance = startSize ;
  }

  public final void nextStep() { distance+= distance/1.5 +1; }

  public boolean isReady()
  {
    boolean back = false ;
    if ((dimX < distance) && (dimY < distance))
      back = true ;

    return back ;
  }

  public final int filterRGB( int x, int y, int rgb )
  {
    int back = rgb ;

    int x1 = x - middleX ;
    int y1 = y - middleY ;

    int dist = (int) Math.sqrt( Math.abs(2*x1*y1) ) ;

    if ( ((dist < distance) && (x != middleX) && (y != middleY)) ||
         (((x == middleX) || (y == middleY)) && (distance > 30)) )
    {
       back = white ;
    } else if ((dist == distance) && (dist > 20)) // Black border
    {
      back = black ;
    }

    return back ;
  }
}

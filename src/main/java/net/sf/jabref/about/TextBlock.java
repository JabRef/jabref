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
// function : set of animated lines
//
// modified :

package net.sf.jabref.about ;

import java.util.Iterator;
import java.util.Vector;

public class TextBlock implements Iterable<AboutTextLine> {

  private Vector<AboutTextLine> textLines ;
  private AboutTextLine headLine ;
  private boolean visible ;

  public TextBlock()
  {
    textLines = new Vector<AboutTextLine>() ;
    visible = false ;
  }

// ---------------------------------------------------------------------------

  public void add(AboutTextLine line)
  {
    textLines.add(line);
  }

  public Iterator<AboutTextLine> iterator() { 
	  return textLines.iterator(); 
  }

// ---------------------------------------------------------------------------
  public void setHeading(AboutTextLine head)
  {
    headLine = head ;
  }

  public AboutTextLine getHeading() { return headLine ; }

// ---------------------------------------------------------------------------
  public boolean isVisible()
  {
    return visible;
  }

  public void setVisible(boolean pVisible)
  {
    this.visible = pVisible;
  }


}

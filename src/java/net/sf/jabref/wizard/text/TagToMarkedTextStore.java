/*
 Copyright (C) 2004 R. Nagel

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */

// created by : r.nagel 06.10.2004
//
// function : save the textposition for tags in a recent TextInputDialog context
//
// todo     :
//
// modified :

package net.sf.jabref.wizard.text ;

import java.util.*;
import javax.swing.* ;
import javax.swing.text.*;


public class TagToMarkedTextStore
{
  private class TMarkedStoreItem
  {
    int start ;
    int end ;
  }

  private HashMap tagMap ;

  public TagToMarkedTextStore()
  {
    tagMap = new HashMap(10) ;
  }

  /** appends a selection propertie for tag */
  public void appendPosition(String tag, int start, int end)
  {
    LinkedList ll ;
    Object dummy = tagMap.get(tag) ;
    if (dummy == null)
    {
      ll = new LinkedList() ;
      tagMap.put(tag, ll) ;
    }
    else
    {
      ll = (LinkedList) dummy ;
    }

    TMarkedStoreItem item = new TMarkedStoreItem() ;
    ll.add(item);
    item.end = end ;
    item.start = start ;
  }

  /** insert selection propertie for tag, old entries were deleted */
  public void insertPosition(String tag, int start, int end)
  {
    LinkedList ll ;
    Object dummy = tagMap.get(tag) ;
    if (dummy == null)
    {
      ll = new LinkedList() ;
      tagMap.put(tag, ll) ;
    }
    else
    {
      ll = (LinkedList) dummy ;
      ll.clear();
    }

    TMarkedStoreItem item = new TMarkedStoreItem() ;
    ll.add(item);
    item.end = end ;
    item.start = start ;
  }

  /** set the Style for the tag if an entry is available */
  public void setStyleForTag(String tag, String style, StyledDocument doc)
  {
    Object dummy = tagMap.get(tag) ;
    if (dummy != null)
    {
      LinkedList ll = (LinkedList) dummy ;

      // iterate over all saved selections
      for (ListIterator lIt = ll.listIterator() ; lIt.hasNext() ; )
      {
        Object du2 = lIt.next() ;
        if (du2 != null)
        {
          TMarkedStoreItem item = ( TMarkedStoreItem ) du2 ;
          doc.setCharacterAttributes( item.start, item.end - item.start,
                                      doc.getStyle( style ), true ) ;
        }
      }
    }
  }


}

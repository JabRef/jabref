/*
 Copyright (C) 2004 R. Nagel
 Copyright (C) 2016 JabRef Contributors.

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
// modified :

package net.sf.jabref.gui.plaintextimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.StyledDocument;

public class TagToMarkedTextStore {

    private final Map<String, List<TMarkedStoreItem>> tagMap;

    public TagToMarkedTextStore() {
        tagMap = new HashMap<>();
    }

    /** appends a selection property for tag */
    public void appendPosition(String tag, int start, int end) {
        List<TMarkedStoreItem> ll = tagMap.get(tag);
        if (ll == null) {
            ll = new ArrayList<>();
            tagMap.put(tag, ll);
        }

        ll.add(new TMarkedStoreItem(start, end));
    }

    /** insert selection properties for tag, old entries were deleted */
    public void insertPosition(String tag, int start, int end) {
        List<TMarkedStoreItem> ll = tagMap.get(tag);

        if (ll == null) {
            ll = new ArrayList<>();
            tagMap.put(tag, ll);
        } else {
            ll.clear();
        }

        ll.add(new TMarkedStoreItem(start, end));
    }

    /** set the Style for the tag if an entry is available */
    public void setStyleForTag(String tag, String style, StyledDocument doc) {
        List<TMarkedStoreItem> ll = tagMap.get(tag);

        if (ll != null) {
            // iterate over all saved selections
            for (TMarkedStoreItem item : ll) {
                if (item != null) {
                    doc.setCharacterAttributes(item.getStart(), item.getLength(), doc.getStyle(style), true);
                }
            }
        }
    }


    private static class TMarkedStoreItem {

        private final int start;
        private final int end;


        public TMarkedStoreItem(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getLength() {
            return end - start;
        }

    }

}

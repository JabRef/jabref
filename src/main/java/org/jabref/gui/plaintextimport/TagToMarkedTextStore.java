package org.jabref.gui.plaintextimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.StyledDocument;

/**
 * Save the textposition for tags in a recent TextInputDialog context
 */
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

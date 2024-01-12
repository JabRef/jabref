package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

import java.util.List;

/**
 * Used for GSON
 */
public class CitationsResponse {
    private int offset;
    private int next;
    private List<CitationDataItem> data;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public List<CitationDataItem> getData() {
        return data;
    }

    public void setData(List<CitationDataItem> data) {
        this.data = data;
    }
}

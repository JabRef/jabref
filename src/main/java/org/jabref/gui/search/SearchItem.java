package org.jabref.gui.search;

public class SearchItem {
    private String itemType;
    private String item;

    public SearchItem(String itemType, String item) {
        this.itemType = itemType;
        this.item = item;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}

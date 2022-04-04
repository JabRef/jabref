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

    public boolean isQuery() {
        return this.itemType.equals("query");
    }

    public boolean isAttribute() {
        return this.itemType.equals("attribute");
    }

    public boolean isLogical() {
        return this.itemType.equals("logical");
    }

    public boolean isBracket() {
        return this.itemType.equals("bracket");
    }

    public boolean isLeftBracket() {
        return this.itemType.equals("bracket") && this.item.equals("(");
    }

    public boolean isRightBracket() {
        return this.itemType.equals("bracket") && this.item.equals(")");
    }
}

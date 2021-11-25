package org.jabref.gui.search;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.controlsfx.control.textfield.CustomTextField;

public class SearchFieldSynchronizer {
    ObservableList<SearchItem> searchItemList = FXCollections.observableList(new ArrayList<SearchItem>());
    CustomTextField searchField;
    public SearchFieldSynchronizer(CustomTextField searchField) {
        this.searchField = searchField;

    }

    public void addSearchItem(String itemType, String item) {
        SearchItem newItem = new SearchItem(itemType, item);
        if (isValid(newItem)) {
            searchItemList.add(newItem);
        }
    }

    public boolean isValid(SearchItem newItem) {
        if (newItem.getItemType().equals("query")) {
            if (searchItemList.isEmpty()) {
                return true;
            }
            if (this.returnLatest().isQuery()) {
                return false;
            }
            if (this.returnLatest().isAttribute()) {
                return true;
            }
            if (this.returnLatest().isLogical()) {
                return false;
            }
            if (this.returnLatest().isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        if (newItem.getItemType().equals("attribute")) {
            if (searchItemList.isEmpty()) {
                return true;
            }
            if (this.returnLatest().isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }
            if (this.returnLatest().isAttribute()) {
                return false;
            }
            if (this.returnLatest().isLogical()) {
                return true;
            }
            if (this.returnLatest().isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        if (newItem.getItemType().equals("logical")) {
            if (searchItemList.isEmpty()) {
                return false;
            }
            if (this.returnLatest().isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }
            if (this.returnLatest().isAttribute()) {
                return false;
            }
            if (this.returnLatest().isLogical()) {
                return false;
            }
            if (this.returnLatest().isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }
        // TODO: Think about how to manage brackets
        if (newItem.getItemType().equals("bracket")) {
            if (searchItemList.isEmpty()) {
                return false;
            }
            if (this.returnLatest().getItemType().equals("query")) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return false;
            }
            if (this.returnLatest().getItemType().equals("attribute")) {
                return false;
            }
            if (this.returnLatest().getItemType().equals("logicalOperator")) {
                return false;
            }
            if (this.returnLatest().getItemType().equals("bracket")) {
                return false; // TODO: Think about how to manage brackets
            }
        }
        return false;
    }

    public boolean isFirstQuery() {
        if (searchItemList.isEmpty()) {
            return false;
        }
        return searchItemList.get(0).getItemType().equals("query");
    }

    public SearchItem returnLatest() {
        return searchItemList.get(searchItemList.size() - 1);
    }

    public void synchronize() {
        if (searchItemList.size() == 1) {
            if (searchItemList.get(0).getItem().equals("") || searchItemList.get(0).getItem().equals(" ")) {
                searchItemList.clear();
            }
        }
        this.clearEmptyItems();
        this.searchItemListToString();
        searchField.setText(this.searchStringBuilder());
        searchField.positionCaret(searchField.getText().length());
    }

    public String searchStringBuilder() {
        StringBuilder searchString = new StringBuilder();
        for (SearchItem item : searchItemList) {

            if (item.isQuery()) {
                searchString.append(item.getItem());
                searchString.append(" ");
            }
            if (item.isLogical()) {
                searchString.append(item.getItem());
                searchString.append(" ");
            }
            if (item.isAttribute()) {
                searchString.append(item.getItem());
            }
        }
        return searchString.toString();
    }

    public void updateSearchItemList(ArrayList<String> list) {
        searchItemList.clear();
        if (!list.isEmpty()) {
            for (String s : list) {
                if (s.endsWith(":")) {
                    searchItemList.add(new SearchItem("attribute", s));
                } else if (s.equals("AND")) {
                    searchItemList.add(new SearchItem("logical", s));
                } else if (s.equals("OR")) {
                    searchItemList.add(new SearchItem("logical", s));
                } else if (s.equals("(")) {
                    searchItemList.add(new SearchItem("bracket", s));
                } else if (s.equals(")")) {
                    searchItemList.add(new SearchItem("bracket", s));
                } else {
                    searchItemList.add(new SearchItem("query", s));
                }
            }
            searchStringBuilder();
        }
        searchItemListToString();
    }

    public void searchItemListToString() {
        for (SearchItem item : searchItemList) {
            System.out.print("| ");
            System.out.print("Type: " + item.getItemType() + ", ");
            System.out.print("Value: " + item.getItem());
            System.out.print(" |");
        }
    }

    public void clearEmptyItems() {
        searchItemList.removeIf(item -> item.getItem().equals(""));
        searchItemList.removeIf(item -> item.getItem().equals(" "));
    }
}

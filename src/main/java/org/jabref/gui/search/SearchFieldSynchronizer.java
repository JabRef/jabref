package org.jabref.gui.search;

import java.util.ArrayList;
import java.util.Arrays;

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

        if (!searchItemList.isEmpty()) {
            if (searchItemList.get(0).getItem().equals("")) {
                searchItemList.remove(searchItemList.get(0));
            }
        }
        if (isValid(this.searchItemList, newItem)) {
            searchItemList.add(newItem);
        }
    }

    public void addSearchItem(SearchItem newItem) {
        if (isValid(this.searchItemList, newItem)) {
            searchItemList.add(newItem);
        }
    }

    public boolean isValid(ObservableList<SearchItem> searchItemList, SearchItem newItem) {
        searchItemListToString(searchItemList);
        if (newItem.getItemType().equals("query")) {
            if (searchItemList.isEmpty()) {
                return true;
            }
            if (this.returnLatest(searchItemList).isQuery()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isAttribute()) {
                return true;
            }
            if (this.returnLatest(searchItemList).isLogical()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        if (newItem.getItemType().equals("attribute")) {
            if (searchItemList.isEmpty()) {
                return true;
            }
            if (this.returnLatest(searchItemList).isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }
            if (this.returnLatest(searchItemList).isAttribute()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isLogical()) {
                return true;
            }
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        if (newItem.getItemType().equals("logical")) {
            if (searchItemList.isEmpty()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }
            if (this.returnLatest(searchItemList).isAttribute()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isLogical()) {
                return false;
            }
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }
        // TODO: Think about how to manage brackets
        if (newItem.getItemType().equals("bracket")) {
            if (searchItemList.isEmpty()) {
                return false;
            }
            if (this.returnLatest(searchItemList).getItemType().equals("query")) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return false;
            }
            if (this.returnLatest(searchItemList).getItemType().equals("attribute")) {
                return false;
            }
            if (this.returnLatest(searchItemList).getItemType().equals("logicalOperator")) {
                return false;
            }
            if (this.returnLatest(searchItemList).getItemType().equals("bracket")) {
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

    public SearchItem returnLatest(ObservableList<SearchItem> searchItemList) {
        return searchItemList.get(searchItemList.size() - 1);
    }

    public void synchronize() {
        String searchString = this.searchStringBuilder();
        searchField.clear();
        searchField.setText(searchString);
        searchField.positionCaret(searchField.getText().length());
        syntaxHighlighting();
    }

    public String searchStringBuilder() {
        StringBuilder searchString = new StringBuilder();
        int i = 0;
        for (SearchItem item : searchItemList) {
            if (item.isQuery()) {
                if (i > 0) {
                    if (!searchItemList.get(i - 1).isAttribute()) {
                        searchString.append(" ");
                    }
                }
                searchString.append(item.getItem());
            }
            if (item.isLogical()) {
                if (i > 0) {
                    searchString.append(" ");
                }
                searchString.append(item.getItem());
                if (returnLatest(searchItemList).equals(item)) {
                    searchString.append(" ");
                }
            }
            if (item.isAttribute()) {
                if (i > 0) {
                    searchString.append(" ");
                }
                searchString.append(item.getItem());
            }
            i = i + 1;
        }
        return searchString.toString();
    }

    public void updateSearchItemList(ArrayList<String> list) {
        searchItemList.clear();
        if (!list.isEmpty()) {
            for (String s : list) {
                System.out.println(s);
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
                } else if (s.equals(" ")) {
                    continue;
                } else {
                    searchItemList.add(new SearchItem("query", s));
                }
            }
            searchItemListToString(searchItemList);
            searchStringBuilder();
        }
    }

    public void searchItemListToString(ObservableList<SearchItem> searchItemList) {
        for (SearchItem item : searchItemList) {
            System.out.print("| ");
            System.out.print("Type: " + item.getItemType() + ", ");
            System.out.print("Value: " + item.getItem());
            System.out.print(" |");
        }
    }

    public boolean isValidSearch(ObservableList<SearchItem> searchItemListCheck) {
        ObservableList<SearchItem> searchItemListNew = FXCollections.observableList(new ArrayList<SearchItem>());

        for (SearchItem item : searchItemListCheck) {
            if (this.isValid(searchItemListNew, item)) {
                searchItemListNew.add(item);
            } else {
                System.out.println("not valid");
                return false;
            }
        }

        return true;
    }

    public void syntaxHighlighting() {
        if (this.isValidSearch(this.searchItemList)) {
            this.searchField.setStyle("-fx-border-color: green");
        } else {
            this.searchField.setStyle("-fx-border-color: red");
        }

        if (!this.searchItemList.isEmpty()) {
            if (this.searchItemList.get(searchItemList.size() - 1).isAttribute()) {
                this.searchField.setStyle("-fx-border-color: red");
            }
            if (this.searchItemList.get(searchItemList.size() - 1).isLogical()) {
                this.searchField.setStyle("-fx-border-color: red");
            }
        }
    }

    public ArrayList<String> textFieldToList() {
        String str = searchField.getText();
        // splits a string "author:luh AND year:2013 OR author:\"lee smith\"" into
        // [author:] [luh] [AND] [year:] [2013] [OR] [author:] ["lee smith"]
        String[] words = str.split("(?<=:)|\\ ");
        ArrayList<String> list = new ArrayList<>();
        list = new ArrayList<>(Arrays.asList(words));

//        // ARRAY TEST
//        System.out.print("Textfeld Array: ");
//        for (String word : words) {
//            System.out.print(word + " | ");
//        }
//        System.out.println();

//        for (int i = 0; i < words.length; i++) {
//            if (words[i].startsWith("\"")) {
//                boolean isWordAfterwards = i + 1 < words.length;
//                if (isWordAfterwards && words[i + 1].endsWith("\"") && !words[i].endsWith(":")) {
//                    String str2 = words[i] + " " + words[i + 1];
//                    list.add(str2);
//                    i++;
//                } else {
//                    list.add(words[i]);
//                }
//            } else {
//                list.add(words[i]);
//            }
//        }

/*
        // TEXTFELD TEST
        System.out.print("Textfeld Liste: ");
        for (String word : list) {
            System.out.print(word + " | ");
        }
        System.out.println();
*/

        return list;
    }
}

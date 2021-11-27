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

    /* Add item to list if valid */
    public void addSearchItem(String itemType, String item) {
        SearchItem newItem = new SearchItem(itemType, item);

        // Remove empty query item (appears after clearing the searchBar text
        // and prevents you from clicking buttons if not removed)
        if (!searchItemList.isEmpty()) {
            if (searchItemList.get(0).getItem().equals("")) {
                searchItemList.remove(searchItemList.get(0));
            }
        }

        // Add item, if valid according to isValid function
        if (isValid(this.searchItemList, newItem)) {
            searchItemList.add(newItem);
        }
    }

    /* Does the same as the function above but accepts SearchItems directly*/
    public void addSearchItem(SearchItem newItem) {
        if (!searchItemList.isEmpty()) {
            if (searchItemList.get(0).getItem().equals("")) {
                searchItemList.remove(searchItemList.get(0));
            }
        }
        if (isValid(this.searchItemList, newItem)) {
            searchItemList.add(newItem);
        }
    }

    /* Used mainly by the DropDownMenu to check if a button press
    should add an item to the searchItemList or not. */
    public boolean isValid(ObservableList<SearchItem> searchItemList, SearchItem newItem) {

        // new item is of type query
        if (newItem.getItemType().equals("query")) {

            // if list is empty it's okay to add a query
            if (searchItemList.isEmpty()) {
                return true;
            }

            // a query should usually not follow a query (only okay if searchField consists of only queries)
            if (this.returnLatest(searchItemList).isQuery()) {
                return false;
            }

            // query is allowed to follow attribute
            if (this.returnLatest(searchItemList).isAttribute()) {
                return true;
            }

            // query is not allowed to follow logical
            if (this.returnLatest(searchItemList).isLogical()) {
                return false;
            }

            //
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        // new item is of type "attribute"
        if (newItem.getItemType().equals("attribute")) {

            // if list is empty it's okay to add attribute
            if (searchItemList.isEmpty()) {
                return true;
            }

            // attribute is allowed to follow query but not when search starts with a query
            if (this.returnLatest(searchItemList).isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }

            // attribute is not allowed to follow attribute
            if (this.returnLatest(searchItemList).isAttribute()) {
                return false;
            }

            // attribute is allowed to follow logical
            if (this.returnLatest(searchItemList).isLogical()) {
                return true;
            }

            //
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        // new item is of type "logical"
        if (newItem.getItemType().equals("logical")) {

            // logical is not allowed as first item in list
            if (searchItemList.isEmpty()) {
                return false;
            }

            // logical is allowed to follow query but not if search starts with a query
            if (this.returnLatest(searchItemList).isQuery()) {
                if (this.isFirstQuery()) {
                    return false;
                }
                return true;
            }

            // logical is not allowed to follow attribute
            if (this.returnLatest(searchItemList).isAttribute()) {
                return false;
            }

            // logical is not allowed to follow logical
            if (this.returnLatest(searchItemList).isLogical()) {
                return false;
            }

            //
            if (this.returnLatest(searchItemList).isBracket()) {
                return false; // TODO: Think about how to manage brackets
            }
        }

        // new item is of type "bracket"
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

    /* Returns true if the searchItemList is not empty
    and the first item in it is an item of type query */
    public boolean isFirstQuery() {
        if (searchItemList.isEmpty()) {
            return false;
        }
        return searchItemList.get(0).getItemType().equals("query");
    }

    /* Returns the last SearchItem from a searchItemList */
    public SearchItem returnLatest(ObservableList<SearchItem> searchItemList) {
        return searchItemList.get(searchItemList.size() - 1);
    }

    /* builds a new searchString by calling searchStringBuilder,
    sets the searchField text and positions caret at the end */
    public void synchronize() {
        String searchString = this.searchStringBuilder();
        searchField.clear();
        searchField.setText(searchString);
        searchField.positionCaret(searchField.getText().length());
        syntaxHighlighting();
    }

    /* Builds a searchString from the searchItemList */
    public String searchStringBuilder() {

        // create StringBuilder that later becomes our search String
        StringBuilder searchString = new StringBuilder();

        // count the item index
        int i = 0;

        // loop over all SearchItem in searchItemList
        for (SearchItem item : searchItemList) {

            if (item.isQuery()) {
                // item is query

                // if it's not the first item append " " if the item before the current one was not an attribute
                if (i > 0) {
                    // not first item
                    if (!searchItemList.get(i - 1).isAttribute()) {
                        // item before was not attribute
                        searchString.append(" ");
                    }
                }

                // add item to the end of searchString
                searchString.append(item.getItem());
            }

            if (item.isLogical()) {
                // item is logical
                if (i > 0) {
                    // not first item
                    searchString.append(" ");
                }
                searchString.append(item.getItem());

                // if item is last in list append " " after item
                if (returnLatest(searchItemList).equals(item)) {
                    // item is last in list
                    searchString.append(" ");
                }
            }

            if (item.isAttribute()) {
                // item is attribute
                if (i > 0) {
                    // not first item
                    searchString.append(" ");
                }
                searchString.append(item.getItem());
            }
            // raise index counter by one
            i = i + 1;
        }

        // return String from StringBuilder
        return searchString.toString();
    }

    /* Updates the searchItemList from a string ArrayList.
    * In reality this string ArrayList is created by textFieldToList */
    public void updateSearchItemList(ArrayList<String> list) {
        // clear the current searchItemList as we rebuild it from the input ArrayList
        searchItemList.clear();

        if (!list.isEmpty()) {
            // list is not empty

            // loop over all Strings in ArrayList
            for (String s : list) {
                if (s.endsWith(":")) {
                    // s is attribute
                    // add attribute to ItemList
                    searchItemList.add(new SearchItem("attribute", s));
                } else if (s.equals("AND")) {
                    // s is logical
                    // add logical to ItemList
                    searchItemList.add(new SearchItem("logical", s));
                } else if (s.equals("OR")) {
                    // s is logical
                    // add logical to ItemList
                    searchItemList.add(new SearchItem("logical", s));
                } else if (s.equals("(")) {
                    // s is left bracket
                    // add bracket to list
                    searchItemList.add(new SearchItem("bracket", s));
                } else if (s.equals(")")) {
                    // s is right bracket
                    // add bracket to list
                    searchItemList.add(new SearchItem("bracket", s));
                } else if (s.equals(" ")) {
                    // s is " "
                    // continue without adding s
                    continue;
                } else {
                    // s is none of the above -> s is query
                    // add query to list
                    searchItemList.add(new SearchItem("query", s));
                }
            }
        }
    }

    // Prints a searchItemList to the console for testing purposes.
    public void searchItemListToString(ObservableList<SearchItem> searchItemList) {
        for (SearchItem item : searchItemList) {
            System.out.print("| ");
            System.out.print("Type: " + item.getItemType() + ", ");
            System.out.print("Value: " + item.getItem());
            System.out.print(" |");
        }
        System.out.println("----");
    }

    /* Checks if a given searchItemList is a valid list by adding every single item in order
    to a new list and checking if every step is valid according to the isValid function

    return true, if valid
    return false, if not valid
     */
    public boolean isValidSearch(ObservableList<SearchItem> searchItemListCheck) {
        ObservableList<SearchItem> searchItemListNew = FXCollections.observableList(new ArrayList<SearchItem>());

        // Every time we add one item check if it would be valid to do so or not.
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

    // Highlights the searchField green if the search query is valid and red if it is invalid
    public void syntaxHighlighting() {

        // Highlight if some new SearchItem is not a valid new SearchItem according to isValid function
        if (this.isValidSearch(this.searchItemList)) {
            this.searchField.setStyle("-fx-border-color: green");
        } else {
            this.searchField.setStyle("-fx-border-color: red");
        }

        // Highlight red if only SearchItem is attribute or logical or if last SearchItem is attribute or logical
        if (!this.searchItemList.isEmpty()) {
            if (this.searchItemList.get(searchItemList.size() - 1).isAttribute()) {
                this.searchField.setStyle("-fx-border-color: red");
            }
            if (this.searchItemList.get(searchItemList.size() - 1).isLogical()) {
                this.searchField.setStyle("-fx-border-color: red");
            }
        }

        // Highlight red if attribute is followed by a space (" ")
        for (SearchItem item : this.searchItemList) {
            if (item.isAttribute()) {
                if (searchItemList.indexOf(item) + 1 < searchItemList.size()) {
                    if (Character.isWhitespace(searchItemList.get(searchItemList.indexOf(item) + 1).getItem().charAt(0))) {
                        this.searchField.setStyle("-fx-border-color: red");
                    }
                }
            }
        }

        // Highlight green if only queries in list
        boolean onlyQueries = true;
        for (SearchItem item : this.searchItemList) {
            if (!item.isQuery()) {
                onlyQueries = false;
                break;
            }
        }

        if (onlyQueries) {
            this.searchField.setStyle("-fx-border-color: green");
        }

    }

    public ArrayList<String> textFieldToList() {
        String str = searchField.getText();

        // splits a string "author:luh AND year:2013 OR author:\"lee smith\"" into
        // [author:] [luh] [AND] [year:] [2013] [OR] [author:] ["lee smith"]
        String[] words = str.split("(?<=:)|\\ ");
        ArrayList<String> list = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            if (words[i].startsWith("\"")) {
                boolean isWordAfterwards = i + 1 < words.length;
                if (isWordAfterwards && words[i + 1].endsWith("\"") && !words[i].endsWith(":")) {
                    String str2 = words[i] + " " + words[i + 1];
                    list.add(str2);
                    i++;
                } else {
                    list.add(words[i]);
                }
            } else {
                list.add(words[i]);
            }
        }

        return list;
    }
}

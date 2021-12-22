package org.jabref.gui.search;

import java.util.ArrayList;
import java.util.Stack;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
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

        // Remove empty query item (appears after clearing the searchBar text)
        if (!searchItemList.isEmpty()) {
            if (searchItemList.get(0).getItem().equals("")) {
                searchItemList.remove(searchItemList.get(0));
            }
        }

        // if last item is ) bracket, add new item before
        if (latestIsBracket()) {
            searchItemList.remove(searchItemList.size() - 1);
            searchItemList.add(newItem);
            searchItemList.add(new SearchItem("bracket", ")"));
            if (!isValid()) {
                System.out.println("NOT VALID");
                searchItemList.remove(searchItemList.size() - 1);
                searchItemList.remove(searchItemList.size() - 1);
                searchItemList.add(new SearchItem("bracket", ")"));
            }
        } else {
            if (isValid(this.searchItemList, newItem)) {
                searchItemList.add(newItem);
            }
        }
    }

    /* Does the same as the function above but accepts SearchItems objects directly*/
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

    public boolean isValid() {
        StandardQueryParser queryParser = new StandardQueryParser();

        boolean valid = true;
        try {
            queryParser.parse(this.searchStringBuilder(), "");
        } catch (QueryNodeException e) {
            valid = false;
        }

        if (valid) {
            return true;
        }

        valid = true;
        SearchItem Foo = new SearchItem("query", "foo");

        if (searchItemList.get(searchItemList.size() - 1).getItem().equals(")")) {
            searchItemList.remove(searchItemList.size() - 1);
            searchItemList.add(Foo);
            searchItemList.add(new SearchItem("bracket", ")"));
        } else {
            searchItemList.add(Foo);
        }
        try {
            queryParser.parse(this.searchStringBuilder(), "");
        } catch (QueryNodeException e) {
            valid = false;
        }

        searchItemList.remove(Foo);

        return valid;
    }

    /* Used mainly by the DropDownMenu to check if a button press
    should add an item to the searchItemList or not. */
    public boolean isValid(ObservableList<SearchItem> searchItemList, SearchItem newItem) {
        StandardQueryParser queryParser = new StandardQueryParser();

        boolean valid = true;
        try {
            queryParser.parse(this.searchStringBuilder(), "");
        } catch (QueryNodeException e) {
            valid = false;
        }

        if (valid) {
            return true;
        }

        valid = true;
        SearchItem Foo = new SearchItem("query", "foo");
        searchItemList.add(newItem);
        searchItemList.add(Foo);
        try {
            queryParser.parse(this.searchStringBuilder(), "");
        } catch (QueryNodeException e) {
            valid = false;
        }

        searchItemList.remove(newItem);
        searchItemList.remove(Foo);

        return valid;
    }

    /* Returns the last SearchItem from a searchItemList */
    public SearchItem returnLatest(ObservableList<SearchItem> searchItemList) {
        return searchItemList.get(searchItemList.size() - 1);
    }

    public boolean latestIsBracket() {
        if (searchItemList.size() == 0) {
            return false;
        }
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("bracket")
                && searchItemList.get(searchItemList.size() - 1).getItem().equals(")");
    }

    /* builds a new searchString by calling searchStringBuilder,
    sets the searchField text and positions caret at the right position */
    public void synchronize() {
        String searchString = this.searchStringBuilder();
        searchField.clear();
        searchField.setText(searchString);
        searchField.positionCaret(caretPos());
        syntaxHighlighting();
    }

    private int caretPos() {
        if (searchField.getText().endsWith(")")) {
            return searchField.getText().length() - 1;
        }
        return searchField.getText().length();
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
                String lastChar = "empty";
                if (searchString.length() != 0) {
                    lastChar = searchString.substring(searchString.length() - 1);
                }
                if (i > 0 && !lastChar.equals("(")) {
                    // not first item and last character not (
                    searchString.append(" ");
                }
                searchString.append(item.getItem());
            }

            if (item.isBracket()) {
                if (i > 0) {
                    // not first item
                    if (item.getItemType().equals("(")) {
                        searchString.append(" ");
                    }
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

    // Highlights the searchField green if the search query is valid and red if it is invalid
    public void syntaxHighlighting() {
        if (isValidLucene()) {
            this.searchField.setStyle("-fx-border-color: green");
        } else {
            this.searchField.setStyle("-fx-border-color: red");
        }

    }

    public ArrayList<String> textFieldToList() {
        String str = searchField.getText();

        // splits a string "author:luh AND year:2013 OR author:\"lee smith\"" into
        // [(] [author:] [luh] [AND] [year:] [2013] [)] [OR] [(] [author:] ["lee smith" [)]]
//        String[] words = str.split("(?<=:)|\\ ");
        String[] words = str.split("(?<=:)|(?<=\\()|(?=\\))|\\ ");
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

    /* Check if brackets are balanced */
    public boolean bracketsBalanced(ObservableList<SearchItem> searchItemList) {
        Stack<SearchItem> bracketStack = new Stack<>();

        for (SearchItem item : searchItemList) {
            // if it's a left bracket -> push to Stack
            // if it's a right bracket -> pop from stack and check if we get a left bracket
            if (item.isLeftBracket()) {
                bracketStack.push(item);
            } else if (item.isRightBracket()) {
                if (bracketStack.isEmpty()) {
                    return false;
                } else {
                    if (bracketStack.pop().isRightBracket()) {
                        return false;
                    }
                }
            }
        }
        return bracketStack.isEmpty();
    }

    // Uses StandardQueryParser to parse searchString. Throws exception if not valid syntax
    public boolean isValidLucene() {
        StandardQueryParser queryParser = new StandardQueryParser();
        try {
            queryParser.parse(this.searchStringBuilder(), "");
        } catch (QueryNodeException e) {
            return false;
        }
        return true;
    }

    // Deletes all entries in the List
    public void deleteAllEntries() {
        searchItemList.clear();
    }
}

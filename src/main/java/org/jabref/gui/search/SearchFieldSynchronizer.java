package org.jabref.gui.search;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.jfoenix.controls.JFXChipView;
import org.controlsfx.control.textfield.CustomTextField;

public class SearchFieldSynchronizer {
    ObservableList<SearchItem> searchItemList = FXCollections.observableList(new ArrayList<>());
    String searchString;

    public SearchFieldSynchronizer(CustomTextField searchField) {
        searchItemList.addListener(new ListChangeListener<SearchItem>() {
            @Override
            public void onChanged(Change<? extends SearchItem> c) {
//                synchronize(chipView);

            }
        });
    }

    public void addSearchItem(String itemType, String item) {
        SearchItem newItem = new SearchItem(itemType, item);
//        if (isValid(newItem)) {
            searchItemList.add(newItem);
//        }
    }

    public boolean isPrevAttribute() {
        if (searchItemList.isEmpty()) {
            return false;
        }
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("attribute");
    }

    public boolean isPrevOperator() {
        if (searchItemList.isEmpty()) {
            return true; // no previous operator technically, but search shouldnt start with an operator
        }
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("OR") || searchItemList.get(searchItemList.size() - 1).getItemType().equals("AND");
    }

    public boolean isPrevBracket() {
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("bracket");
    }

    public boolean isValid(SearchItem newItem) {
        if (searchItemList.size() != 0) {
            return switch (newItem.getItemType()) {
                case "attribute" -> !(isPrevAttribute());
                case "logicalOperator" -> !(isPrevOperator());
                case "bracket" -> !(isPrevBracket());
                default -> false;
            };
        } else {
            return true;
        }
    }

    public void synchronize(JFXChipView<SearchItem> chipView) {
        chipView.getChips().removeAll();
        chipView.getChips().addAll(searchItemList);

    }

    public String searchStringBuilder() {
        StringBuilder str = new StringBuilder();

        for (SearchItem current : searchItemList) {
            boolean isLogicalOp = current.getItemType().equals("OR") || current.getItemType().equals("AND");
//                    || current.getItemType().equals("title:") || current.getItemType().equals("year:");
            if (!str.isEmpty()) {
                str.append(" ");
            }
            if (current.getItemType().endsWith(":")) {
                str.append(current.getItemType());
                str.append(current.getItem());
            } else if (isLogicalOp && (current == searchItemList.get(searchItemList.size() - 1))) {
                str.append(current.getItemType());
                str.append(" ");
            } else {
                str.append(current.getItemType());
            }
        }
        searchString = str.toString();
        System.out.println("searchStringBuilder: " + searchString);
        return searchString;
    }

    // Takes a string, splits it up into attributes, search strings and operators. Updates search item list.
    public void updateSearchItemList(ArrayList<String> list) {
        searchItemList.clear();
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).endsWith(":")) {
                    if (i + 1 != list.size()) { // ensures item is not last element in list
                        searchItemList.add(new SearchItem(list.get(i), list.get(i + 1)));
                        i++;
                    } else {
                        searchItemList.add(new SearchItem(list.get(i), ""));
                    }
//                } else if (list.get(i).equals("AND")) {
//                    searchItemList.add(new SearchItem("AND", ""));
//                } else if (list.get(i).equals("OR")) {
//                    searchItemList.add(new SearchItem("OR", ""));
                } else {
                    searchItemList.add(new SearchItem(list.get(i), ""));
                }
            }
            searchStringBuilder();
        }
    }

    public String getSearchString() {
        return searchString;
    }

//    public String getLastItem() {
//        SearchItem item = searchItemList.get(searchItemList.size() - 1);
//        String lastItem = item.getItemType();
//
//        if (lastItem.endsWith(":")) {
//
//        }
//    }

}

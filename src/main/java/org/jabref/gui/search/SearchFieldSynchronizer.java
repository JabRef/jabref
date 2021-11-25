package org.jabref.gui.search;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.jfoenix.controls.JFXChipView;

public class SearchFieldSynchronizer {
    ObservableList<SearchItem> searchItemList = FXCollections.observableList(new ArrayList<SearchItem>());
    String searchString;
    public SearchFieldSynchronizer(JFXChipView<SearchItem> chipView) {
        searchItemList.addListener(new ListChangeListener<SearchItem>() {
            @Override
            public void onChanged(Change<? extends SearchItem> c) {
                synchronize(chipView);
            }
        });
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

    public void synchronize(JFXChipView<SearchItem> chipView) {
        chipView.getChips().removeAll();
        chipView.getChips().addAll(searchItemList);

    }
}

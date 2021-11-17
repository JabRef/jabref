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

    public boolean isPrevAttribute() {
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("attribute");
    }

    public boolean isPrevLogical() {
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("logicalOperator");
    }

    public boolean isPrevBracket() {
        return searchItemList.get(searchItemList.size() - 1).getItemType().equals("bracket");
    }

    public boolean isValid(SearchItem newItem) {
        if (searchItemList.size() != 0) {
            return switch (newItem.getItemType()) {
                case "attribute" -> !(isPrevAttribute());
                case "logicalOperator" -> !(isPrevLogical());
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
}

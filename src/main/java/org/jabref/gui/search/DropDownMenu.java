package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.textfield.CustomTextField;

public class DropDownMenu {
    public PopOver searchbarDropDown;
    public Button authorButton;
    public Button journalButton;
    public Button titleButton;
    public Button yearButton;
    public Button yearRangeButton;
    public Button andButton;
    public Button orButton;
    public Button andGroupButton;
    public Button orGroupButton;
    public Button leftBracketButton;
    public Button rightBracketButton;
    public Button deleteButton;
    public Button searchStart;
    public Button addString;
    public RecentSearch recentSearch;
    public TextField searchString;

    @SuppressWarnings("checkstyle:NoWhitespaceBefore")
    public DropDownMenu(CustomTextField searchField, GlobalSearchBar globalSearchBar, SearchFieldSynchronizer searchFieldSynchronizer) {

        authorButton = new Button("Author");
        journalButton = new Button("Journal");
        titleButton = new Button("Title");
        yearButton = new Button("Year");
        yearRangeButton = new Button("Year-Range");
        andButton = new Button("AND");
        orButton = new Button("OR");
        andGroupButton = new Button("AND^-1");
        orGroupButton = new Button("OR^-1");
        leftBracketButton = new Button("(");
        rightBracketButton = new Button(")");
        deleteButton = IconTheme.JabRefIcons.DELETE_ENTRY.asButton();
        searchStart = IconTheme.JabRefIcons.SEARCH.asButton();
        addString = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        searchString = new TextField();

        Text titleLucene = new Text(" Lucene Search");
        Text titleRecent = new Text(" Recent Searches");
        recentSearch = new RecentSearch(globalSearchBar);
        TextField searchString = new TextField();
        searchString.setPrefWidth(200);

        // yearRangeSlider horizontal
        Text titelYearRangeSlider = new Text("      Year-Range");
        final RangeSlider hSlider = new RangeSlider(1800, 2022, 10, 90);
        hSlider.setShowTickMarks(true);
        hSlider.setShowTickLabels(true);
        hSlider.setBlockIncrement(10);
        hSlider.setPrefWidth(100);
        hSlider.setMajorTickUnit(100);
        hSlider.setMinorTickCount(10);
        hSlider.showTickMarksProperty();
        Label label = new Label();

        HBox luceneString = new HBox(searchString, addString, searchStart, deleteButton);
        HBox recentSearchBox = recentSearch.getHBox();
        HBox buttonsLucene = new HBox(2, authorButton, journalButton, titleButton,
                yearButton, yearRangeButton);
        HBox andOrButtonsWithYearRangeSlider = new HBox(2, andButton, orButton, titelYearRangeSlider, hSlider, label);
        HBox bracketButtons = new HBox(2, leftBracketButton, rightBracketButton);

        VBox mainBox = new VBox(4, titleLucene, luceneString, buttonsLucene, andOrButtonsWithYearRangeSlider, bracketButtons, titleRecent, recentSearchBox);
        // mainBox.setMinHeight(500);
        // mainBox.setMinWidth(500);
        Node buttonBox = mainBox;

        searchField.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (searchbarDropDown == null || !searchbarDropDown.isShowing()) {
                searchbarDropDown = new PopOver(buttonBox);
                searchbarDropDown.setWidth(searchField.getWidth());
                searchbarDropDown.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
                searchbarDropDown.setContentNode(buttonBox);
                searchbarDropDown.setDetachable(false); // not detachable
                searchbarDropDown.show(searchField);
                searchString.setFocusTraversable(false);
            }
            searchbarDropDown.setOnHiding(event1 -> {
                recentSearch.add(searchField.getText());
            });
        });

        // addString action
        addString.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            String current = searchField.getText();
            String adder = searchString.getText();
            String newString = "";
            int pos = current.length() - 1;
            int pos2 = current.length() - 1;
            while (pos > 0) {
                char ch = current.charAt(pos);
                if (ch == ':') {
                    break;
                }
                pos--;
            }
            while (pos2 > 0) {
                char cha = current.charAt(pos2);
                if (cha == ')') {
                    break;
                }
                pos2--;
            }
            if (searchField.getText().isEmpty()) {
                searchField.setText(adder);
                searchField.positionCaret(searchField.getText().length());
                searchString.clear();
            } else {
                if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isLogical()) {
                    newString = current + adder;
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isRightBracket() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isLeftBracket()) {
                    String subi = current.substring(0, pos2);
                    newString = subi + adder + ")";
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isRightBracket() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isLogical()) {
                    String subs = current.substring(0, pos2);
                    newString = subs + adder + ")";
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isQuery() && pos == 0 && !searchString.getText().isEmpty()) {
                    newString = current + " " + adder;
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isRightBracket() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isQuery() && !searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 3).isAttribute()) {
                    String subs = current.substring(0, pos2);
                    newString = subs + " " + adder + ")";
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isAttribute()) {
                    newString = current + adder;
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isRightBracket() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isAttribute()) {
                    String subidu = current.substring(0, pos2);
                    newString = subidu + adder + ")";
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isRightBracket() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isQuery() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 3).isAttribute()) {
                    String sub = current.substring(0, pos + 1);
                    newString = sub + adder + ")";
                } else if (searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 1).isQuery() && searchFieldSynchronizer.searchItemList.get(searchFieldSynchronizer.searchItemList.size() - 2).isAttribute()) {
                    String nsub = current.substring(0, pos + 1);
                    newString = nsub + adder;
                }
                searchField.setText(newString);
                searchField.positionCaret(searchField.getText().length());
                searchString.clear();
            }
            searchString.setFocusTraversable(false);
        });

        // searchStart action
        searchStart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            globalSearchBar.focus();
            globalSearchBar.performSearch();
            searchbarDropDown.hide();
        });

        // deleteButton action
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchField.clear();
            searchString.clear();
            searchFieldSynchronizer.deleteAllEntries();
            searchString.setFocusTraversable(false);
        });

        // authorButton action
        authorButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "author:");
            searchFieldSynchronizer.synchronize();
        });

        // journalButton action
        journalButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "journal:");
            searchFieldSynchronizer.synchronize();
        });

        // titleButton action
        titleButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "title:");
            searchFieldSynchronizer.synchronize();
        });

        // yearButton action
        yearButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "year:");
            searchFieldSynchronizer.synchronize();
        });

        // yearRangeButton action
        yearRangeButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.synchronize();
        });

        // yearRangeSlider action
        hSlider.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            int pos = 0;
            boolean bol = false;
            SearchItem item = null;
            if (!searchField.getText().isEmpty()) {
                for (int i = 0; i < searchFieldSynchronizer.searchItemList.size(); i++) {
                    item = searchFieldSynchronizer.searchItemList.get(i);
                    if (item.getItemType().equals("attribute")) {
                        if (item.getItem().equals("year-range:")) {
                            pos = i;
                            bol = true;
                        }
                    }
                }
            }
            if (!bol) {
                searchFieldSynchronizer.addSearchItem("attribute", "year-range:");
                searchFieldSynchronizer.addSearchItem("query", Integer.toString((int) hSlider.getLowValue()) + "to" + Integer.toString((int) hSlider.getHighValue()));
                searchFieldSynchronizer.synchronize();
            } else {
               searchFieldSynchronizer.searchItemList.get(pos + 1).setItem(Integer.toString((int) hSlider.getLowValue()) + "to" + Integer.toString((int) hSlider.getHighValue()));
               searchFieldSynchronizer.synchronize();
            }
        });

        // yearRangeSlider output on action
        hSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
            label.setText("search from " + Integer.toString((int) hSlider.getLowValue()) + " to " + Integer.toString((int) hSlider.getHighValue()));
        });

        // andButton action
        andButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("logical", "AND");
            searchFieldSynchronizer.synchronize();
        });

        // orButton action
        orButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("logical", "OR");
            searchFieldSynchronizer.synchronize();
        });

        // leftBracketButton action
        leftBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("bracket", "(");
            searchFieldSynchronizer.synchronize();
        });

        // rightBracketButton action
        rightBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("bracket", ")");
            searchFieldSynchronizer.synchronize();
        });

        // andGroupButton action
        andGroupButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addBrackets();
            searchFieldSynchronizer.addSearchItem("logical", "AND");
            searchFieldSynchronizer.synchronize();
        });

        // orGroupButton action
        orGroupButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addBrackets();
            searchFieldSynchronizer.addSearchItem("logical", "OR");
            searchFieldSynchronizer.synchronize();
        });
    }

}

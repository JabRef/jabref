package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.control.PopOver;
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
    public Button leftBracketButton;
    public Button rightBracketButton;
    public Button deleteButton;
    public Button searchStart;
    public Button addString;
    public RecentSearch recentSearch;
    public TextField searchString;

    public DropDownMenu(CustomTextField searchField, GlobalSearchBar globalSearchBar, SearchFieldSynchronizer searchFieldSynchronizer) {

        authorButton = new Button("Author");
        journalButton = new Button("Journal");
        titleButton = new Button("Title");
        yearButton = new Button("Year");
        yearRangeButton = new Button("Year-Range");
        andButton = new Button("AND");
        orButton = new Button("OR");
        leftBracketButton = new Button("(");
        rightBracketButton = new Button(")");
        deleteButton = IconTheme.JabRefIcons.DELETE_ENTRY.asButton();
        searchStart = IconTheme.JabRefIcons.SEARCH.asButton();
        addString = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        searchString = new TextField();

        Text titleLucene = new Text(" Lucene Search");
        Text titleRecent = new Text(" Recent Searches");
        recentSearch = new RecentSearch(globalSearchBar);
        searchString.setPrefWidth(200);
        HBox luceneString = new HBox(searchString, addString, searchStart, deleteButton);
        HBox recentSearchBox = recentSearch.getHBox();
        HBox buttonsLucene = new HBox(2, authorButton, journalButton, titleButton,
                yearButton, yearRangeButton);
        HBox andOrButtons = new HBox(2, andButton, orButton);
        HBox bracketButtons = new HBox(2, leftBracketButton, rightBracketButton);

        VBox mainBox = new VBox(4, titleLucene, luceneString, buttonsLucene, andOrButtons, bracketButtons, titleRecent, recentSearchBox);
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
        });

        // addString action
        addString.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            String current = searchField.getText();
            String adder = searchString.getText();
            String newString = "";
            int pos = current.length() - 1;
            while (pos > 0) {
                char ch = current.charAt(pos);
                if (ch == ':') {
                    break;
                }
                pos--;
            }
            if (searchField.getText().isEmpty()) {
                searchField.setText(adder);
                searchField.positionCaret(searchField.getText().length());
                searchString.clear();
            } else {
                if (pos == 0) {
                    newString = current + " " + adder;
                } else if (pos == current.length() - 1) {
                    newString = current + adder;
                } else {
                    String sub = current.substring(0, pos + 1);
                    newString = sub + adder;
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

        // orButton action
        rightBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("bracket", ")");
            searchFieldSynchronizer.synchronize();
        });
    }

}

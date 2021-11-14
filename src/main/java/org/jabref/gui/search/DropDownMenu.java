package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;

public class DropDownMenu {
    // DropDown Searchbar
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
    public RecentSearch recentSearch;
    private boolean isPrevAttribute;
    // private final Button articleButton;
    // private final Button bookButton;
    // private final Button citationKeyButton;

    // test buttons
    // private final Button testButton;

    public DropDownMenu(CustomTextField searchField, GlobalSearchBar globalSearchBar) {
        // Testing dropdown for searchbar

        authorButton = new Button("Author");
        journalButton = new Button("Journal");
        titleButton = new Button("Title");
        yearButton = new Button("Year");
        yearRangeButton = new Button("Year-Range");
        andButton = new Button("AND");
        orButton = new Button("OR");
        leftBracketButton = new Button("(");
        rightBracketButton = new Button(")");
        // articleButton = new Button("Article");
        // bookButton = new Button("Book");
        // citationKeyButton = new Button("CitationKey");
        Text titleLucene = new Text(" Lucene Search");
        Text titleRecent = new Text(" Recent Searches");
        recentSearch = new RecentSearch(globalSearchBar);
        HBox recentSearchBox = recentSearch.getHBox();
        HBox buttonsLucene = new HBox(2, authorButton, journalButton, titleButton,
                yearButton, yearRangeButton);
        HBox andOrButtons = new HBox(2, andButton, orButton);
        HBox bracketButtons = new HBox(2, leftBracketButton, rightBracketButton);

        VBox mainBox = new VBox(4, titleLucene, buttonsLucene, andOrButtons, bracketButtons, titleRecent, recentSearchBox);
        mainBox.setMinHeight(500);
        mainBox.setMinWidth(searchField.getWidth());
        Node buttonBox = mainBox;

        searchField.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (searchbarDropDown == null || !searchbarDropDown.isShowing()) {
                searchbarDropDown = new PopOver(buttonBox);
                searchbarDropDown.setWidth(searchField.getWidth());
                searchbarDropDown.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
                searchbarDropDown.setContentNode(buttonBox);
                searchbarDropDown.setDetachable(false); // not detachable
                searchbarDropDown.show(searchField);
//            } else if (searchbarDropDown.isShowing()) {
//                searchbarDropDown.hide();  // this makes the dropdown disappear if you re-click on searchbar
            } else {
//                searchbarDropDown.setContentNode(buttonBox);
                // this makes the drop down reappear every time you click on search bar, even if its shown already
//                searchbarDropDown.show(searchField);
            }
        });

        // authorButton action
        authorButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!isPrevAttribute(searchField)) { // checks if the search term prior is an attribute and wont queue another if so
                checkAndAddSpace(searchField); // checks if there is a space prior and if not adds it
                searchField.insertText(searchField.getCaretPosition(), "author:");
                searchField.positionCaret(searchField.getText().length());
            }
        });

        // journalButton action
        journalButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (!isPrevAttribute(searchField)) {
                checkAndAddSpace(searchField);
                searchField.insertText(searchField.getCaretPosition(), "journal:");
                searchField.positionCaret(searchField.getText().length());
            }
        });

        // titleButton action
        titleButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (!isPrevAttribute(searchField)) {
                checkAndAddSpace(searchField);
                searchField.insertText(searchField.getCaretPosition(), "title:");
                searchField.positionCaret(searchField.getText().length());
            }
        });

        // yearButton action
        yearButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (!isPrevAttribute(searchField)) {
                checkAndAddSpace(searchField);
                searchField.insertText(searchField.getCaretPosition(), "year:");
                searchField.positionCaret(searchField.getText().length());
            }
        });

        // yearRangeButton action
        yearRangeButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{

        });

        // andButton action
        andButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (searchField.getCaretPosition() != 0) {
                if (!searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(" ")) {
                    searchField.insertText(searchField.getCaretPosition(), " ");
                    searchField.positionCaret(searchField.getText().length());
                }
            }
            searchField.insertText(searchField.getCaretPosition(), "AND ");
            searchField.positionCaret(searchField.getText().length());
        });

        // orButton action
        orButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (searchField.getCaretPosition() != 0) {
                if (!searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(" ")) {
                    searchField.insertText(searchField.getCaretPosition(), " ");
                    searchField.positionCaret(searchField.getText().length());
                }
            }
            searchField.insertText(searchField.getCaretPosition(), "OR ");
            searchField.positionCaret(searchField.getText().length());
        });

        // leftBracketButton action
        leftBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (searchField.getCaretPosition() != 0) {
                if (!searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(" ")) {
                    searchField.insertText(searchField.getCaretPosition(), " ");
                    searchField.positionCaret(searchField.getText().length());
                }
            }
            searchField.insertText(searchField.getCaretPosition(), "( ");
            searchField.positionCaret(searchField.getText().length());
        });

        // orButton action
        rightBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->{
            if (searchField.getCaretPosition() != 0) {
                if (!searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(" ")) {
                    searchField.insertText(searchField.getCaretPosition(), " ");
                    searchField.positionCaret(searchField.getText().length());
                }
            }
            searchField.insertText(searchField.getCaretPosition(), ") ");
            searchField.positionCaret(searchField.getText().length());
        });
    }

    private void checkAndAddSpace(CustomTextField searchField) {
        if (searchField.getCaretPosition() != 0) {
            if (!searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(" ")) {
                searchField.insertText(searchField.getCaretPosition(), " ");
                searchField.positionCaret(searchField.getText().length());
            }
        }
    }

    private boolean isPrevAttribute(CustomTextField searchField) {
        isPrevAttribute = false;
        if (searchField.getCaretPosition() != 0) {
            if (searchField.getText(searchField.getCaretPosition() - 1, searchField.getCaretPosition()).equals(":")) {
                isPrevAttribute = true;
            }
        }
        return isPrevAttribute;
    }
}

package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import com.jfoenix.controls.JFXChipView;
import org.controlsfx.control.PopOver;

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

    public DropDownMenu(JFXChipView<SearchItem> searchField, GlobalSearchBar globalSearchBar) {
        // Testing dropdown for searchbar
        SearchFieldSynchronizer searchFieldSynchronizer = new SearchFieldSynchronizer(searchField);

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
            searchFieldSynchronizer.addSearchItem("attribute", "author:");
        });

        // journalButton action
        journalButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "journal:");
        });

        // titleButton action
        titleButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "title:");
        });

        // yearButton action
        yearButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("attribute", "year:");
        });

        // yearRangeButton action
        yearRangeButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {

        });

        // andButton action
        andButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("logicalOperator", "AND");
        });

        // orButton action
        orButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("logicalOperator", "OR");
        });

        // leftBracketButton action
        leftBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("bracket", "(");
        });

        // orButton action
        rightBracketButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            searchFieldSynchronizer.addSearchItem("bracket", ")");
        });
    }

}

# OCL Constraints

| Constraint | OCL Constraint  |
|---|---|
| The Search Query is not empty    | **context** `GlobalSearchBar:performSearch()` **inv**: `self.SearchField != null`  |
| After performing a search,  the search query gets added to the recent search list| **context** `GlobalSearchBar:performSearch()` **post**: `RecentSearch.contains(GlobalSearchBar.searchField)`|
| If Lucene Syntax is not obeyed, search is not possible | |
| If the GlobalSearchBar gets clicked the DropDownMenu will be activated | **context** `GlobalSearchBar:searchField.Mousevent.MOUS_CLICKED` **inv**: `GlobalSearchBar.dropDownMenu.show(searchField)` |
| If some button "Example" in the DropDownMenu is clicked the Searchbar gets updated with the input "example:"| **context** `DropDownMenu.ExampleButton` **inv**: `GlobalSearchBar.searchField.insertText()`|
| Only one "-1" logical operator is allowed | **context** `SearchFieldSynchronizer::SearchItemList` **inv**: `self.amountMinusOneLogical <= 1`| 
| SearchItem object with itemType "attribute" must have a non empty item attribute | **context** `SearchItem` **inv**: `SearchItem.itemType == attribute` **implies** `SearchItem.item != null`|
| If button Year is clicked and the searchbar asks for input maximum 4 digits are allowed after "year:"| **context** `DropDownMenu.YearButton` **post**: `GlobalSearchBar:searchField.insertText().maximum = 9999`|t**: `GlobalSearchBar:searchField.insertText().maximum = 9999`|
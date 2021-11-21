# Technical Documentation

| Project Overview  | | | | | |
| :---: | :---: | :---: | :---: | :---: | :---: |
| Version | Project Name | Authors | Status | Date | Comment |
| 0.1 | Better Search | MS, DM, JB, AJ | In Progress | 2021-11-20 | |

| Project Members | | | |
| :---: | :---: | :---: | :---: |
| Julian Bopp | Albert Jasari | Daniel Madoery | Mark Starzynski



## 1 Introduction

### 1.1 Purpose
- The goal of this document is to provide a good overview of the project that explains the use, functionality, creation and architecture of the product. It aims to function as a guide for its current and future developers.
kv
### 1.2 References
- Functional Specification Document: [functional-specification-document.md](https://github.com/josphstar/jabref/blob/designdoc/docs/sweng/functional-specification-document.md)
- Issue [#341](https://github.com/koppor/jabref/issues/341)

## 2 System Overview
<!-- Hier sollte eine kurze Übersicht über das System gegeben werden. Das Ziel dieses Abschnitts ist, dass der Leser weiss, was entwickelt wird. Also zum Beispiel sollte man erwähnen, dass es sich um eine Erweiterung für Ganttproject handelt, und was das Ziel dieser Erweiterung ist. -->
The project goal is to implement an extension for the open-sourced citation and reference management software JabRef. The goal of this extension is to improve upon the existing global search function and make it more user-friendly by adding a dropdown menu with additional search functionalities.

## 3 Design Goals
<!-- Es gibt kein absolutes Mass für gutes oder schlechtes Design. Das Design ist nur gut oder schlecht bezüglich den Anforderungen der Stakeholder. Hier sollten die Ziele/Anforderungen kurz beschrieben werden. Beispiele sind:

Das Design soll künftige Erweiterbarkeit gewährleisten
Das Design soll zu minimalen Entwicklungszeit/Kosten führen
Das Design soll maximale Performance gewährleisten
… -->
For the design of the extension, at its core a GUI implementation of a dropdown menu, future extensability will be ensured because JabRef already has dropdown menus implemented with JavaFX. Those will be re-used, which will also satisfy low development costs and good performance.

## 4 System Performance
<!-- Um die Designlösung die nachfolgend beschrieben wird einzuführen, sollten an dieser Stelle nochmals das gewünschte Verhalten des Systems (abgeleitet aus dem Pflichtenheft) kurz beschrieben werden. Idealerweise sollte hier genügend Information gegeben werden, so dass man die Diagramme und Spezifikationen die im nächsten Abschnitt beschrieben werden verstehen kann, ohne zuvor das Pflichtenheft im Detail gelesen zu haben. -->
The extension implements a dropdown menu for the existing global search bar. This dropdown menu will consist of clickable buttons which allows the user to search for specific fields (i.e. author, title) without the need of having to type in a complex search syntax. Furthermore the implemented dropdown menu has the option to browse through and re-search the most recent search queries.

## 5 Design
<!-- An dieser Stelle wird nun das eigentliche Softwaredesign (die technische Lösung) beschrieben. In grösseren Systemen wird typischerweise zwischen High-level Design (Architektur) sowie Mid-level Design (UML Klassen- und Sequenzdiagramme) sowie Detaildesign (Detaillierte Beschreibung von einigen Schlüsselklassen) unterschieden. Für diese kleine Änderung, muss diese Unterscheidung aber nicht gemacht werden. Jedoch wollen wir explizit zwischen Statik, Dynamik und Logik zu unterscheiden. -->


### 5.1 Statics
<!-- An dieser Stelle sollten die statischen Aspekte, zum Beispiel mit Hilfe von UML Klassendiagrammen oder Paketdiagrammen beschrieben werden. -->
"Better Search" implementation will mainly evolve around the SeearchFieldSynchronizer class, which task it will be to keep user inputs up-to-date with the live search query and ensure proper syntax.

For that an ArrayList will be kept and updated in accordance with the - by the user via search field or dropdown menu - intended live search query.

RecentSearch will keep track of previously searched for important attributes - mainly authors and titles - in separate lists and also display them in the dropdown menu at the end.

![Class Diagram](/docs/sweng/diagrams/class-diagram-withLegend.png "class-diagram.png")

### 5.2 Dynamics
<!-- An dieser Stelle sollten die dynamische Aspekte, zum Beispiel mit Hilfe von UML Sequenz/Kollaborationsdiagrammen, oder Akivitätsdiagrammen beschrieben werden. -->
#### 5.2.1 User Search Input
With the implementation of a search dropdown menu the user can input information for search queries mainly in two ways: As usual via search field or - additionaly - via newly implemented dropdown menu. 

Handling synchronization between user interaction and proper display of the complete search string will be the class SearchFieldSynchronizer. 
- When interacting with the dropdown menu it will check and add new items (attributes, logical operators, brackets) to the ArrayList searchItemList and build a full search string to update the search field.
- When typing directly into the search field it will fetch changes or additions to the ArrayList searchItemList and update the search field accordingly. 

![User Search Input](/docs/sweng/diagrams/user-search-input.png "user-search-input.png")

#### 5.2.2 Recent Search

The Recent Search functionality shows the user a record of past searches inside the dropdown menu.

- When performSearch() is called, the query gets added to the RecentSearches List.
- Clicking on an entry in Recent Search performs a corresponding search.
- The List is ordered from bottom to top.

![Recent Search](/docs/sweng/diagrams/recent-search.png "recent-search.png")

#### 5.2.3 ChipView

The ChipView transforms coherent search strings into a single chip.

- Pressing a button in DropDownMenu creates a new chip.
- The resulting chip can then be filled with a search query.

![ChipView](/docs/sweng/diagrams/chip-view.png "chip-view.png")

#### 5.2.4 Autocomplete

Autocomplete takes care of the automatically generated completion in the GlobalSearchBar which is done by the bindAutoCompletion() method in the AutoCompletePopup class. 
The recommended strings are linked together and can thus be accepted or ignored by the user.
The input is tracked as far as possible.

- A pop-up menu appears with all the recommended entries which you can click on.

![Autocomplete](/docs/sweng/diagrams/autocomplete.png "autocomplete.png")

### 5.3 Logic
<!-- An dieser Stelle können noch logische Aspekte, wie zum Beispiel logische Einschränkungen spezifiziert werden. Hierzu kann zum Beispiel OCL verwendet werden. -->
#### 5.3.1 Logic Operators
The buttons in the dropdown menu have to function differently under certain conditions. I.e. it is preferred to not allow two logical operators in a row without an attribute in between or to not allow adding search items while an attribute search string is still empty.

Furthermore a rudimentary weighted logic operating option is depicted in the following activity diagram. A logic operator can be weighted normally (1) or weaker (-1) once, which allows for a little more complexity in the drop down menu.

![Logic Operators](/docs/sweng/diagrams/logic-operators.png "logic-operators.png")

#### 5.3.2 OCL Constraints

| Constraint | OCL Constraint  |
|---|---|
| The Search Query is not empty    | **context** `GlobalSearchBar:performSearch()` **inv**: `self.SearchField != null`  |
| After performing a search,  the search query gets added to the recent search list| **context** `GlobalSearchBar:performSearch()` **post**: `RecentSearch.contains(GlobalSearchBar.searchField)`|
| AutoCompleter is reading searchfield and proposes different endings | **context** `GlobalSearchBar:AutoComplete.readCurrent(searchField)` **inv**: `AutoComplete.popup()`|
| If the GlobalSearchBar gets clicked the DropDownMenu will be activated | **context** `GlobalSearchBar:searchField.Mousevent.MOUS_CLICKED` **inv**: `GlobalSearchBar.dropDownMenu.show(searchField)` |
| If some button "Example" in the DropDownMenu is clicked the Searchbar gets updated with the input "example:"| **context** `DropDownMenu.ExampleButton` **inv**: `GlobalSearchBar.searchField.insertText()`|
| Only one "-1" logical operator is allowed | **context** `SearchFieldSynchronizer::SearchItemList` **inv**: `self.amountMinusOneLogical <= 1`| 
| SearchItem object with itemType "attribute" must have a non empty item attribute | **context** `SearchItem` **inv**: `SearchItem.itemType == attribute` **implies** `SearchItem.item != null`|
| If button Year is clicked and the searchbar asks for input maximum 4 digits are allowed after "year:"| **context** `DropDownMenu.YearButton` **post**: `GlobalSearchBar:searchField.insertText().maximum = 9999`|

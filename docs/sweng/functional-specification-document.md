# Functional Specification Document "Better Search" Feature for JabRef

## 1. Introduction

### 1.1 Purpose

- The open-sourced citation and reference management software JabRef is in need of a more functional global search and a user-friendly interface thereof.
- The users of JabRef should be able to conveniently search in their citations library and perform complex search querys, also via GUI.
- The main purpose of this feature is to add a UI for the global search bar. 

### 1.2 Definitions

- Bracketing: Lucene allows for bracketing of multiple fields
- Chipview: A box design for search terms which allows easier separation
- Chips: A box in the ChipView. May contain search words, Boolean operators, ...
- Complex search query: JabRef specific search query
- Dropdown menu: Graphical menu which shows up below the item which triggers the menu
- GUI: Graphical user interface
- JabRef: Citation and reference management software
- Lucene: Open-source search engine library. [More details](http://www.lucenetutorial.com/lucene-query-syntax.html)
- Query: Is a request for data or information from a database table or combination of tables
- Search field: The textfield from the global search bar.
- Search user interface: Main graphical user interface for the developed extension
- Search key: Every search string typed into the global search bar which isn't a field. Search strings are separated by spaces in general but can be extended to more than one word by enclosing the search term within "".

### 1.3 Applications & Goals

- "Better Search" is a feature implementation for the software JabRef.
- Target group of the JabRef software are researchers, PhD candidates, university members, students and the scientific community in general.
- An internet connection is not needed for this feature.
- Search queries in global search should have the same syntax as the already implemented web search. 
- Search queries need to be performed reliably and without any bugs or errors.
- Improve the view of the search bar.
- Improve the use of the search bar.
- Improve the search function of the search bar.
- Improve the autocomplete function of the search bar.

### 1.4 References
* /R00/: <https://github.com/koppor/jabref/issues/341>

### 1.5 Overview

- The search user interface is activated upon click on the search term in the search box.
- Autocomplete is implemented into the global search bar and shows suggestions for fields and string searches in the search user interface. 
- A chipview design for the fields in the global search bar shall show the search key in separated boxes.
- Search strings in the search field are already parsed with Lucene.


## 2. General description

### 2.1 Embedding

- The search category fields from the dropdown menu can be chosen via mouseclick and added with a search key, they will be used in the global search bar.
- The search field uses Lucene query syntax to run the search.
- The search field uses the internal autocompletion which proposes search key results from the local database.


### 2.2 Functional requirements

| Features | | | |
| :---: | :---: | :---: | :---: | 
| MUST: | Search User Interface | Chipview Design | Autocompletion |
| SHOULD: | Highlighting | Recent Search |

- <s>Lucene syntax</s>
- Search user interface
- Chipview design
- Autocompletion
- Highlighting
- Recent search

### 2.3 User profile

- All JabRef users with basic know-how in field-based search.
- Target group of the JabRef software are researchers, PhD candidates, university members, students and the scientific community in general.
- For all supported operating systems (Linux, OS, Windows)

### 2.4 Limitations

- Lucene has to be used for global search bar.
- The speed of query evaluation depends on the current JabRef implementation and won't be altered.

### 2.5 Assumptions & Dependencies

- Search bar exists and is running properly.


## 3. Specific requirements

- /F00/ The global search bar functionality must be extended with new features.
  - /F01/ (/F10/) The global search bar must implement a search user interface.
  - <s>/F02/ (/F20/) The global search bar must must allow for a Lucene syntax.</s>
  - /F03/ (/F30/) The global search bar must implement a chipview design for the fields and search keys.
  - /F05/ (/F60/) The global search bar must implement a new button to the right which allows for a dropdown menu with recent searches.

- /F10/ The search user interface must be implemented as dropdown menu.
  - /F11/ The search user interface must popup below the search bar after clicking it.
  - /F12/ The search user interface must contain all relevant fields as clickable items.
  - /F13/ The search user interface must contain logical operators like OR/AND as clickable items.
  - /F14/ The search user interface must close if the user clicks somewhere other than the interface itself or the global search bar.
  - /F15/ The search user interface could implement a special section for recent search queries.
  - /F16/ The search user interface could implement an adjustment bar with two markers left and right to limit the search for papers within a specific period of years.
  - /F17/ The search bar interface could implement complex bracketing of search terms and boosting in form of clickable items.

- <s>/F20/ (/F00/) The Lucene query parser must be used in the global search bar.</s>
  - <s>/F21/ The Lucene query parser must function in the same way it was implemented in the web search bar.</s>
  - <s>/F22/ The Lucene query parser must take the complete global search string and fetch it to the complex search query.</s>

- /F30/ The global search bar must appear in a chipview design.
  - /F31/ (/F12/) Clicking on a field must open a new chip that allows entering a search key.
  - /F32/ Chips must be closable by clicking on a cross next to the keyword.
  - /F33/ (/F13/) Clicking on a field must open a new chip for AND/OR.
  - /F34/ The order of the chips should be changeable by drag and drop.
  - /F35/ The cursor should jump over the separate chips.

- /F40/ Autocomplete must complete missing characters/words in the interface.
  - /F41/ Autocomplete completed text must be displayed in a list below the search bar.
  - /F42/ Words suggested by autocomplete should be color-coded in the suggestions.
  - /F43/ Autocomplete could correct typos and overwrite them.

- /F50/ Red highlighting could be implemented for the search field if the search syntax is not valid.

- /F60/ The recent search should be implemented as a dropdown menu.
  - /F61/ A clickable item corresponding to the recent search should be inside the global search bar. 
  - /F62/ When selecting the clickable item it should open a dropdown menu right below the clickable item.
  - /F63/ The dropdown menu should contain a list of the last used search queries.
  - /F64/ Selecting a query from the list should perform a search with the selected query.

## 4. Acceptance criteria

- /A10/ JabRef users can search in the global search bar by using the search user interface.
- <s>/A20/ JabRef users can use Lucene syntax in the global search bar. <!-- using ":" not "=" --></s>
- /A30/ JabRef users gets suggestions for search (autocompletion).
- /A40/ JabRef users can see their current search keys as chips in the global search bar.
- /A50/ JabRef users gets feedback via highlighted search key.
- /A60/ JabRef users can see their recent searches in a separate dropdown menu.
- /A98/ Extension passes all unit tests.
- /A99/ The extension's new source code conforms to JabRefs coding style.

# Apendix

### Use-cases

### Use Case 1:
* Name: Click on global search bar
* Stakeholders: Users of JabRef
* Preconditions: JabRef must be started
* Procedure:
  * Click on the global search bar
* Postconditions: Dropdown menu shows up below the search bar with suggestions for syntax use. Last seen searches are listed.
* Postconditions exception: Dropdown menu shows up below the search bar with suggestions for syntax use. No prior searches show empty "Last searches" list

### Use Case 2:
* Name: Make a search
* Stakeholders: Users of JabRef
* Preconditions: JabRef must be started
* Procedure:
  * Click on the global search bar.
  * Choose the fields you would like to search in (i.e. author, title etc.).
  * Click on author field (button or text) in the interface.
  * Type in the author "Beat Schmutz" into the global search bar and press ENTER.
  * Click on the OR field in the interface.
  * Click on the title field in the interface.
  * Type in "Morphological analysis" and press ENTER.
  * Click on an entry match in the main library pane.
* Postconditions: 
  * After selection of the author field an author field shows up in the global search bar. The cursor will be positioned to the right of this field. 
  * After typing in the author and selecting the OR field the position of the cursor is to the right again. 
  * After selecting the title field the cursor position changes to the right of this field once again. Typing in the title and pressing ENTER starts the search query which will be parsed with Lucene and passed on to the complex search query. The matched entries will show up in the main library pane. After clicking on an entry Metadata will show up; "Beat Schmutz" and "Morphological analysis" will be highlighted. 
* Postconditions exception: The search finds no matching entries in the library. in this case the library pane stays empty.


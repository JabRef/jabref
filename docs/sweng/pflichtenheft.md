# Functional Specification Document "Better Search" Feature for JabRef

## 1. Introduction

### 1.1 Purpose

- The open-sourced citation and reference management software JabRef is in need of a more functional global search and a user-friendly interface thereof.
- The users of JabRef should be able to conveniently search in their citations library and perform complex search querys, also via GUI.
- The goal of this project is to develop an implementation that improves upon the current library search capabilities and enables users of the JabRef software to very easily navigate their libraries. 

### 1.2 Definitions

- JabRef: Citation and reference management software
- Lucene: Search Query Syntax
- Query: Is a request for data or information from a database table or combination of tables
- Chipview: A box design for search terms which allows easier separation
- Chips: box including a search key
- GUI: Graphical user interface
- Dropdown menu: Graphical menu which shows up below the item which triggers the menu
- Bracketing and Boost: Lucene allows for bracketing of multiple fields
- Search user interface: Main graphical user interface for the developed extension
- Complex search query: JabRef specific search query


### 1.3 Applications & Goals

- "Better Search" is a feature implementation for the software JabRef.
- Target group of the JabRef software are researchers, PHD candidates, university members, students and the scientific community in general.
- An internet conneciton is not needed for this feature.
- Search queries in global search must have the same syntax as the already implemented web search. 
- Search queries need to be performed reliably and without any bugs or errors.
- Improve the view of the search bar.
- Improve the use of the search bar.
- Improve the search function of the search bar.
- Improve the autocomplete function of the search bar.

### 1.4 References
* /R00/: [https://github.com/koppor/jabref/issues/341]()

### 1.5 Overview

- The search user interface is a dropdown menu.
- Lucene is implemented into the global search bar.
- Autocomplete is implemented into the global search bar and shows suggestions for fields and string searches in the search user interface. 
- A chipview design for the fields in the searchbar shall show the search key in separated boxes
- Search strings in the global search bar are parsed with Lucene.


## 2. General description

### 2.1 Embedding

- The searchcategoriefields from the dropdown menu can be chosen via mouseclick and added with a search key they will be used in the global searchbar.
- The globalsearchbar uses Lucene query syntax to run the search
- The global searchbar uses the internal autocompletion which propose search key results from the local database.


### 2.2 Functional requirements

| Features | | | | | |
| :---:   | :-: | :-: | :-: | :-: | :-: |
| MUST: | Global Search Bar | Lucene Syntax | Search User Interface | Chipview Design | Autocompletion |
| SHOULD: | Highlighting | Recent Search |

- Lucene syntax
- Search user interface
- Chipview design
- Autocompletion
- Highlighting
- Recent search


### 2.3 User profile

- For all JabRefusers in all Languages, without special knowhow.

### 2.4 Limitations

- Lucene has to be used for global search bar.
- Chipview is required in the search bar.
- Search bar is required to use autocomplete.

### 2.5 Assumptions & Dependencies

- Search bar exists and is running properly.


## 3. Specific requirements

- /F00/ The global search bar functionality must be extended with new features.
  - /F01/ (/F10/) The global search bar must implement a search user interface.
  - /F02/ (/F20/) The global search bar must must allow for a Lucene syntax.
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

- /F20/ (/F00/) The Lucene query parser must be used in the global search bar.
  - /F21/ The Lucene query parser must function in the same way it was implemented in the web search bar.
  - /F22/ The Lucene query parser must take the complete global search string and fetch it to the complex search query.

- /F30/ The searchbar must appear in a chipview design.
<<<<<<< HEAD
  - /F31/ (/F12/) Clicking on a field must open a new chip that allows entering a search key.
  - /F32/ Chips must be closable by clicking on a cross next to the keyword.
  - /F33/ (/F13/) Clicking on a fiel must open a new chip for AND/OR.
  - /F34/ The order of the chips should be changeable by drag and drop.
  - /F35/ The cursor should jump over the seperate chips.

- /F40/ Autocomplete completes missing characters/words in the interface.
  - /F41/ JabRef users can choose from a combobox of suggested datasets 
  - /F42/ Autocomplete completed text is displayed in a list below the search bar and is color-coded to match the search word entered
  - /F43/ Autocomplete corrects typos and overwrites them

- /F50/ highlighting the search key.
  - /F51/ highlights search keys red if not found in the global database.

- /F60/ The recent search should be implemented as a dropdown menu.
  - /F61/ A clickable item corresponding to the recent search shold be inside the searchbar. 
  - /F62/ When you select the clickable item it should open a dropdown menu right below the clickable item.
  - /F63/ The dropdown menu should contain a list of the last used search queries.
  - /F64/ Selecting a query from the list should perform a search with the selected query.

## 4. Acceptance criteria

- /A10/ JabRef users can search in the global searchbar by using the search user interface.
- /A20/ JabRef users can use Lucene syntax in the global search bar. <!-- using ":" not "=" -->
- /A30/ JabRef users gets suggestions for search (autocompletion).
- /A40/ JabRef users can see their current search keys as chips in the global search bar.
- /A50/ JabRef users gets feedback via highlighted search key.
- /A60/ JabRef users can see their recent searches in a separate dropdown menu.


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
* Postconditions exception: The search finds no matching entries in the library. in this case the library pane stays empty or a pop-up could suggest that no entries were found.


# Specification Book

| Version | Authors | Status | Date | Commentary |
| ------- | ----- | ------ | ----- | --------- |
|  0.1    |  Raphael Kreft| Abgeschlossen | 03.11.2020 | [Projectwebsite](https://unibas-marcelluethi.github.io/software-engineering/project/project-summary.html) , [JabRef Website](https://www.jabref.org/)|
||Olivier Mattmann||||
||Matias Carballo||||
||Tim Bachmann||||

This specification book captures the requirements of the extension/modification of JabRef, that takes place as a part of the course "Software Engineering" at the University of Basel. This project implements [**issue #6187 Add posibility to explore citation relations**](https://github.com/JabRef/jabref/issues/6187). It should implement a way to explore articles that cite or are cited by articles in the database.

## 1. Vision and Goals

* /V10/ JabRef users can explore and administrate relations between articles
* /Z10/ JabRef users have the possibility to explore their own references and find articles that reference or are refered to by own articles
* /Z20/ It is not a goal to change [Search related articles using Mr. DLib](https://docs.jabref.org/advanced/entryeditor#related-articles-tab)

## 2. Stakeholder

* JabRef Maintainer @koppor
* JabRef Maintainer reviewing the pull request
* Tutoren: Joe Zgraggen, Günes Aydin
* Dozent: Marcel Lüthi
* Project-Developers: Raphael Kreft, Olivier Mattmann, Matias Carballo, Tim Bachmann

## 3. Conditions

* /R10/ Scope of application: JabRef Software
* /R20/ Target group: JabRef users
* /R30/ Runs unattended as part of JabRef
* /R40/ Used Software on the target machines: JabRef, Java, OS: Windows/Linux/macOS
* /R50/ Software on the development systems: JabRef, Java, OS: depending on the developer Windows/Linux oder macOS
* /R60/ Organisational marginal conditions: Given by [Projektrichtlinien](https://unibas-marcelluethi.github.io/software-engineering/project/project-summary.html)
* /R70/ Documentation of changes and operational intructions in the way set by the course
* /R80/ Development-interface 1: The extension is part of JabRef and must adapt to its [Ecosystem](https://devdocs.jabref.org/getting-into-the-code/code-howtos)
* /R90/ Development-interface 2: The API that is used to get the citations: [Opencitaions API](https://opencitations.net/index/api/v1)

## 4. Context and Overview

* /K10/ [Opencitaions API](https://opencitations.net/index/api/v1), to search for published works that cite or are cited by a certain reference
* /K20/ JabRef, where the extension is embedded

## 5. Functional Requirements

* /F10/ The extension must allow the user to find articles cited by the selected article
* /F20/ The user should be able to use the entire functionality from the GUI
* /F30/ The extension must allow users to find articles that cite the selected article
* /F40/ The user should be informed, if no articles are found
* /F50/ The user should be able to select found articles and to add them to the database via a button
* /F60/ The user should be informed if there is no internet connection
* /F70/ The extension must ensure that no articles are added to the database if they're already present. Articles will be grey and the buttons will be deactivated (/F50/)
* /F80/ The extension must be reachable via a tab in the entry editor
* /F90/ The user should be informed if it is not possible to add an article to the database (/F70/)
* /F100/ The tab should be composed of a left and a right section
* /F110/ The left section must implement the functionality described in /F30/ and must contain a list with the search results given by /F30/
* /F120/ The right section must implement the functionality described in /F10/ and must contain a list with the search results given by /F10/
* /F130/ Articles listed as search results should be displayed grouped below each other using JabRef Standards of WebSearch (/F110/ und /F120/)
* /F140/ The search in /F110/ and /F120/ should be repeated by pressing the corresponding Refresh-Buttons
* /F170/ The extension must ensure that the selected article contains the needed informations for executing the search (DOI-Nummer des Eintrags)
* /F180/ If not enough information is available, the extension must inform the user via a message in the lower part of the screen
* /F190/ The extension should save the search results if the user leaves the tab and show them again when the user returns (/F150/)
* /F200/ The extension should search for citations using the Open Citations API (/R90/)
* /F210/ The extensions should display search results correctly (/F130/), that have at least a title and author linked to their DOI-Number
* /F230/ The left and right sections of the tab should be separated using a movable separator that is located in the middle by default (/F100/)
* /F250/ The extension should inform the user about the progress of the search query in this way: (#current articles from total #total articles)
* /F260/ The extension should display search results that only have a DOI-Number by that number (Secondary case of /F210/)
* /F270/ The extension should, as soon as a search query is running, have a button to cancel below the progress indicator (/F250/)
* /F280/ If an article is already part of the database, the user should be able to navigate to it by clicking the corresponding article
* /F290/ It should be possible to activate and deactivate the entire functionality of the tab in the settings
* /F300/ If the functionality is activated, the search should start automatically as soon as the user is on the tab

## 6. Non-functional requirements

### 6.1 Quality requirements


| System quality  | very good | good | normal | non relevant |
| ----------------| --------  | ---  | ------ | -------------|
| Functionality   |     x     |      |        |              |
| Reliability     |           |  x   |        |              |
| Usability       |     x     |      |        |              |
| Efficiency      |           |      |   x    |              |
| Mantainability  |           |      |   x    |              |
| Portability     |           |      |        |      x       |

* /QB10/ All operations should be executable from the UI
* /QB20/ All textelements of the extension should be available in english and german
* /QE10/ The time it take for a search query to complete is dependent on the number of results: The user is informed about the progress
* /QT10/ There should be test cases for the logic of the API use
* /QT20/ There should be test cases for adding to the database

### 6.2 Other

* /N10/ If the tab is left (/F80/) to another tab in the same article the results should be saved
* /N20/ If the tab is left (/F80/) to another article or the database is closed the results should not be saved

## 7. Acceptance criteria

* /A10/ The implemented extension offers the functionality of displaying a list of search results
* /A20/ The implemented extension should be operable from the GUI
* /A30/ Errors must not lead to program crashes and are displayed to the user
* /A40/ The quality requirement /QE10/ is fulfilled
* /A50/ The quality requirement /QB20/ is fulfilled
* /A60/ The quality requirement /QT10/ is fulfilled
* /A70/ The quality requirement /QT20/ is fulfilled

## 8. Appendix

## Appendix A. Use-cases

### Use Case 1

* Name: Finding the extension
* Actors: *JabRef user, JabRef*
* Preconditions:
  * JabRef is running
  * JabRef user has openned a database containing articles
* Default procedure
  * JabRef-user selects an articles
  * The article menu opens in the lower part of JabRef
  * The user selects the tab of the extension
* Postcondition success: *The view of the extension in the article menu below the tab*
* Postcondition failure: -

### Use Case 2

* Name: Search cited articles
* Actors: *JabRef user, JabRef, API*
* Preconditions:
  * JabRef is running
  * JabRef user has openned a database containing articles
  * User has selected an article
  * User is on the extension tab
  * There are no problems connecting to the API
* Default procedure
  * User presses the search button in the left section of the tab
  * JabRef checks if enough information is available for the search
  * JabRef sends a query to the API and waits for a response
* Postcondition success: *The search results are displayed as a list in the result section*
* Postcondition failure: *An error message is shown to the user if not enough information for a search*

### Use Case 3

* Name: Search citing articles
* Actors: *JabRef user, JabRef, API*
* Preconditions:
  * JabRef is running
  * JabRef user has openned a database containing articles
  * User has selected an article
  * User is on the extension tab
  * There are no problems connecting to the API
* Default procedure:
  * User presses the search button in the right section of the tab
  * JabRef checks if enough information is available for the search
  * JabRef sends a query to the API and waits for a response
* Postcondition success: *The search results are displayed as a list in the result section*
* Postcondition failure: *An error message is shown to the user if not enough information for a search*

### Use Case 4

* Name: Adding cited article
* Actors: *JabRef user, JabRef*
* Preconditions:
  * List of cited articles must be present
* Default procedure:
  * User selects at least one article from the list
  * Add button is pressed
  * Check if article is already in database
* Postcondition success: *Article is added to database*
* Postcondition failure: *Article is not added to database*

### Use Case 5

* Name: Adding cited by article
* Actors: *JabRef user, JabRef*
* Preconditions:
  * List of cited by articles must be present
* Default procedure
  * User selects at least one article from the list
  * Add button is pressed
  * Check if article is already in database
* Postcondition success: *Article is added to database*
* Postcondition failure: *Article is not added to database*

### Use Case 6

* Name: Start search offline
* Actors: *JabRef user, JabRef*
* Preconditions:
  * At least one article in database
  * There is no internet connection
* Default procedure:
  * Select article in database
  * Open the extension tab in the article menu
* Postcondition success: *The user is informed about missing internet connection, search can be restarted*

### Use Case 7

* Name: Navigation to cited article
* Actors: *JabRef user, JabRef*
* Preconditions:
  * Cited article has to be in database
  * Article in the list has a different color (already in database)
* Default procedure:
  * User is in the extension tab
  * User clicks on the article
* Postcondition success: *User is moved to the entry editor, list is updated*

### Use Case 8

* Name: Navigation to citing article
* Actors: *JabRef user, JabRef*
* Preconditions:
  * Citin article has to be in database
  * Article in the list has a different color (already in database)
* Default procedure:
  * User is in the extension tab
  * User clicks on the article
* Postcondition success: *User is moved to the entry editor, list is updated*

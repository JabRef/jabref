# Pflichtenheft 

## 1. Visions and Goals 

*  /V10/ Reduce the time users spend to add entrys to a library in jabref.
*  /V20/ It is more efficient to use the parser, than adding the entries   by hand.
*  /G10/ Users can easily add entries to a library by parsing a plain     text source.
*  /G20/ Users can change the parsing results if necessary.


## 2. General Conditions / Stakeholders

* /S10/ Private users who work with latex
* /S20/ The jabref developers
* /S30/ University of Basel
* /C10/ Project is based on the course Software engineering at the University of Basel
* /C20/ Jabref is an existing software which allows the user to manage text references
* /C30/ Jabref is an open source project that is developed freely
* /C40/ Jabref is running on Linux / Windows and MacOS
* /C50/ A group of 4 people is developing this feature
* /C60/ To accomplish the task an external parser should be used

## 3. Context and Overview

* /O10/ The software the feature should be added to is implemented with java
* /O20/ There is already an existing parser from .bib files to jabref entries 
* /O30/ There is an existing UI where the feature should be accessible
* /O40/ The external parser must be added to the jabref project as git submodule


## 4. Functional Requirements
Die Kernfunktionailt&auml;t des Systems ist aus Auftraggebersicht auf oberster Abstraktionsebene zu beschreiben. 
Auf Detailbeschreibung ist zu verzichten. 

* /F10/ The whole process can be done via the GUI
* /F20/ The User should have the possibility to add one or multiple Bibtex-Entries to Jabref by pressing on a Context Menu Button
* /F30/ The user should have the possibility to cancel the process while entering the text
* /F40/ The user is informed when a text could not be parsed
* /F50/ The user can change the parsed fields, according to his preferences
* /F60/ The user can overthrow the generated entry if he wants
* /F70/ The user can, after adding one entry, add another entry by pressing the corresponding button
* /F80/ The user is informed when the entry was successfully added
* /F90/ When the parsing process is invoken, an external parser must parse the input text
* /F100/ The output of the external parser must be translated into a .bib entry
* /F110/ The .bib entry must be displayed correctly to the user
* /F120/ Jabrefs internal duplication checker must recognize, that the user tries to add an entry, which is already in the library
* /F130/ The feature must use jabrefs internal .bib parser in order to create a new BibEntry
* /F140/ The created BibEntry must be added correctly to the current Database
* /F141/ The BibEntry is updated when the user made changes as specified in /F50/
* /F150/ The pop up window, which is used for the wholre process is displayed only once
* /F160/ If the parsing pipeline fails, an error message is displayed, that parsing was not possible
* /F170/ If the System unpredicatally crashes, nothing is saved in the Bibtex library
* /F180/ If any of the listed functions above is not working properly, an appropriate error message is shown and the process is closed

Erfolgt die spezifikation natürlichsprachlich, dann sollten Sprachtemplates verwendet werden. 

Die Funktionalen Anforderungen können mithilfe von Use-cases erhoben werden. Die Use-cases sollen in Anhang A detailliert beschrieben werden. 


## 5. Quality requirements 
Es sollte anhand einer Tabelle eine Qualit&auml;tszielbestimmung f&uuml;r das Systems vorgenommen werden. 

| System quality   | very good | good | normal | not relevant   |
| ---------------- | --------- | ---- | ------ | -------------- | 
| Functionality    |      x    |      |        |                |
| Reliability      |           |   x  |        |                |
| Usage            |      x    |      |        |                |
| Efficiency       |           |   x  |        |                |
| Maintainability  |           |      |    x   |                |
| Portability      |           |      |        |        x       |
| Reusability      |           |      |        |        x       |


Einzelne Anforderungen k&ouml;nnen wie folgt festgelegt werden:

* /QB10/ Qualit&auml;tsanforderung zur Benutzbarkeit des Systems
* /QE10/ Qualit&auml;tsanforderung zur Effizienz des Systems


## 6. Abnahmekriterien  
Abnahmekriterien legen fest, wie Anforderungen bei der Abnahme auf ihre Realisierung &uuml;berpr&uuml;ft werden k&ouml;nnen. 

* /A10/ Abnahmekriterium 1
* /A20/ Abnahmekriterium 2


# Anhang

## Anhang A. Use-cases


### Use Case 1:
* Name: *Adding bibtex entry with the parser*
* Agents: *Users of jabref*
* Preconditions: *A plain reference text is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain reference text
    * Press the "parse" button on the GUI
    * Check if the parser results are correct
    * Change bibtex entry (if needed)
    * Accept or decline bibtex entry
* Postcondition success: *The entry (with correctly parsed fields) has been successfully added to the library*
* Postcondition failure: *No entry has been added to the library. User can restart the process if needed*

### Use Case 2:
* Name: *Trying to parse a text which is not a reference*
* Agents: *Users of jabref*
* Preconditions: *A plain reference text is available*
* Standard process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain text (which is not a reference)
    * Pressing the parse button
* Postcondition success: *Show error message to the user that the text is invalid*
* Postcondition failure: *An entry with garbage value is being added*

### Use Case 3:
* Name: *Trying to use the parser on an empty reference text*
* Agents: *Users of jabref*
* Preconditions: *None*
*  Common process
    * Pressing the "Add entry from plain text" button on the GUI
    * (Insert nothing / empty text)
    * Pressing the parse button without entering a reference text (empty)
* Postcondition success: *A warning is shown to the user, input window stays open*
* Postcondition failure: *An entry is being added with empty fields*

### Use Case 4:
* Name: *Adding multiple valid bibtex entries with the parser*
* Agents: *Users of jabref*
* Preconditions: *A plain text with multiple valid references is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain text with multiple references
    * Press the "parse" button on the GUI
    * Check if the parser results are correct
    * Change bibtex entry (if needed)
    * Accept or decline bibtex entry
    * Repeat steps 3-6 until all entries are parsed
* Postcondition success: *The entries (with correctly parsed fields) have been successfully added to the library*
* Postcondition failure: *Several entries have not been added to the library. User can restart the process if needed*

### Use Case 5:
* Name: *Adding multiple bibtex entries with the parser*
* Agents: *Users of jabref*
* Preconditions: *A plain text with some valid and some invalid  references is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain text with multiple references
    * Press the "parse" button on the GUI
    * If the current text is valid:
        * Check if the parser results are correct
        * Change bibtex entry (if needed)
        * Accept or decline bibtex entry
        * Repeat step 4 until all entries are parsed
    * If the current text is invalid:
        * Repeat steps 4 until all entries are parsed
* Postcondition success: *The valid entries (with correctly parsed fields) have been successfully added to the library and the invalid texts are shown to the user in an error message*
* Postcondition failure: *Several entries have not been added / or were added with garbage values to the library without letting the user know.*

### Use Case 6:
* Name: *Adding multiple invalid bibtex entries with the parser*
* Agents: *Users of jabref*
* Preconditions: *A plain text with multiple invalid references is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain text with multiple invalid references
    * Press the "parse" button on the GUI
* Postcondition success: *An error message is displayed to the user that the entries were invalid*
* Postcondition failure: *Several entries have been added with garbage values*

### Use Case 7:
* Name: *User wants to add two entries one by one*
* Agents: *Users of jabref*
* Preconditions: *Two valid plain reference texts are available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI
    * Insert the plain reference text
    * Press the "parse" button on the GUI
    * Check if the parser results are correct
    * Change bibtex entry (if needed)
    * Accept or decline bibtex entry
    * Press "Add another entry" button on success dialog
    * Repeat steps 2-6
    * Press "close" Button on success dialog
* Postcondition success: *The two entries (with correctly parsed fields) have been successfully added to the library*
* Postcondition failure: *No or just one entry has been added to the library. User can restart the process if needed*

### Use Case 8:
* Name: *User accidentally pressed the "add entry from plain text" button on the GUI*
* Agents: *Users of jabref*
* Preconditions: *The "add entry from plain text" button shows up in the GUI*
* Common process 
    * Accidentally pressing "Add entry from plain text" button on the GUI
    * Press the cancel button on the popup window
* Postcondition success: *No entry was added to the library and the pop window is closed*
* Postcondition failure: *Popup window is not closed*
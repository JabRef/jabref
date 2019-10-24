# Specification Book

## 1. Visions and Goals 

* /V10/ Reduce the time users spend to add entries to a library in Jabref.
* /V20/ It is more efficient to use the parser, than adding the entries by hand.
* /G10/ Users can easily add entries to a library by parsing a plain text reference.
* /G20/ One or more entries can be parsed in one process.
* /G30/ Users can change the parsing results if necessary.

## 2. General Conditions / Stakeholders

* /S10/ Private users who work with latex
* /S20/ The Jabref developers
* /S30/ Us, the developers of the feature
* /S40/ The evaluators of our project (Tutors and Lecturers)
* /C10/ The project is based on the course Software Engineering at the University of Basel.
* /C10/ A group of 4 people is developing this feature.
* /C20/ The feature will be finished in 4 milestones.
* /C30/ Jabref is an existing software which allows the user to manage text references.
* /C40/ Jabref is an open source project that is developed freely.
* /C50/ Jabref is running on Linux / Windows and MacOS.
* /C60/ To accomplish the task an external parser should be used.

## 3. Context and Overview

* /O10/ Jabref is implemented with java.
* /O20/ There is already an existing parser from .bib files to Jabref entries.
* /O30/ There is an existing UI where the feature should be accessible.
* /O40/ The external parser must be added to the Jabref project as git submodule.

## 4. Functional Requirements

* /F10/ The feature must be accessible over the GUI.
* /F20/ The feature must start when the user presses on a context menu button.
* /F30/ The feature must give the user the possibility to add one or multiple .bib-entries to Jabref.
* /F40/ The feature must inform the user when an entry was successfully added.
* /F41/ This should be realized with a pop-up information window.
* /F50/ The feature must give the user the possibility to cancel the process while entering the plain text reference.
* /F60/ The feature must inform the user when a text could not be parsed.
* /F70/ The feature must give the user the possibility to change the parsed entry attributes.
* /F80/ The feature must give the user the possibility to overthrow the generated entry.
* /F90/ The feature must give the user the possibility to add another entry after successfully adding an entry.
* /F91/ This should be realized by a button on the success window described in /F41/.
* /F100/ The feature must use an external parser to extract the entry attributes.
* /F110/ The feature must convert the output of the external parser into a .bib-entry.
* /F120/ The feature must display the parsed .bib-entry with its attributes to the user.
* /F121/ This is also important for /F70/.
* /F130/ The feature should use Jabrefs internal duplication checker to ensure no duplicate is added to an library.
* /F140/ The feature must use Jabrefs internal .bib-entry parser in order to create a new BibEntry.
* /F141/ The BibEntry is updated when the user makes changes as specified in /F70/.
* /F150/ The feature must add the created BibEntry correctly to the currently opened library.
* /F160/ The feature should run only once at a time.
* /F161/ Therewith the windows the feature uses are displayed only once at a time.
* /F170/ Jabref should show an appropriate error message when the feature crashes.
* /F171/ Jabref should not save anything in the library when the feature unpredictably crashes.
* /F180/ The feature must be accessible again after it was closed.

## 5. Quality Requirements 

| Feature quality   | very good | good | normal | not relevant   |
| ----------------- |:---------:|:----:|:------:|:--------------:| 
| Functionality     |      x    |      |        |                |
| Reliability       |           |   x  |        |                |
| Usage             |      x    |      |        |                |
| Efficiency        |           |   x  |        |                |
| Maintainability   |           |      |    x   |                |
| Portability       |           |      |        |        x       |
| Reusability       |           |      |        |        x       |
| Security          |           |      |        |        x       |

## 6. Acceptance Criteria  

* /A10/ One plain reference text is passed to the feature.
* /A20/ Multiple plain reference texts are passed to the feature.
* /A30/ Garbage text is passed to the feature.
* /A40/ No text is passed to the feature.



# Attachment


## Attachment A. Use-cases

### Use Case 1:
* Name: *Adding bibtex entry with the feature*
* Agents: *Jabref user*
* Preconditions: *A plain reference text is available*
* Common process: 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert the plain reference text
    * Press the "parse" button on the GUI (pop-up window)
    * Check if the parser results are correct
    * (Change bibtex entry if needed)
    * Accept or decline bibtex entry
* Postcondition success: *The entry (with the correct attributes) has been successfully added to the library*
* Postcondition failure: *No entry has been added to the library. User can restart the process if needed*

### Use Case 2:
* Name: *Trying to parse a text which is not a reference*
* Agents: *Jabref user*
* Preconditions: *A plain reference text is available*
* Common process:
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert the plain text (which is not a reference)
    * Press the "parse" button on the GUI (pop-up window)
* Postcondition success: *Show error message to the user that the text is invalid*
* Postcondition failure: *An entry with garbage value is being added*

### Use Case 3:
* Name: *Trying to use the parser on an empty reference text*
* Agents: *Jabref user*
* Preconditions: *None*
*  Common process
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * (Insert nothing / empty text)
    * Press the "parse" button on the GUI (pop-up window)
* Postcondition success: *A warning is shown to the user, input window stays open*
* Postcondition failure: *An entry is being added with empty fields*

### Use Case 4:
* Name: *Adding multiple valid bibtex entries with the parser*
* Agents: *Jabref user*
* Preconditions: *A plain text with multiple valid references is available, one entry on each line*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert the plain text with multiple references
    * Press the "parse" button on the GUI (pop-up window)
    * Check if the parser results are correct
    * (Change bibtex entry if needed)
    * Accept or decline bibtex entry
    * Repeat steps 3-6 until all entries are parsed
* Postcondition success: *The accepted entries have been successfully added to the library*
* Postcondition failure: *Too many or too little entries or entries with wrongly parsed attributes were added to the library*

### Use Case 5:
* Name: *Adding multiple bibtex entries with the parser*
* Agents: *Jabref user*
* Preconditions: *A plain text with some valid and some invalid references is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert the plain text with multiple references
    * Press the "parse" button on the GUI (pop-up window)
    * If the current reference is valid:
        * Check if the parser results are correct
        * (Change bibtex entry if needed)
        * Accept or decline bibtex entry
        * Repeat step 4 until all entries are parsed
    * If the current reference is invalid:
        * Repeat steps 4 until all entries are parsed
* Postcondition success: *The valid accepted references have been successfully added to the library and the invalid references are shown to the user*
* Postcondition failure: *Several accepted entries have not been added or invalid references were added with garbage values to the library*

### Use Case 6:
* Name: *Adding multiple invalid bibtex entries with the parser*
* Agents: *Jabref user*
* Preconditions: *A plain text with multiple invalid references is available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert the plain text with multiple invalid references
    * Press the "parse" button on the GUI (pop-up window)
* Postcondition success: *An error message is displayed to the user that the entries were invalid*
* Postcondition failure: *Several entries have been added with garbage values*

### Use Case 7:
* Name: *User wants to add two entries one by one*
* Agents: *Jabref user*
* Preconditions: *Two valid plain reference texts are available*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Insert one plain reference text
    * Press the "parse" button on the GUI (pop-up window)
    * Check if the parser results are correct
    * (Change bibtex entry if needed)
    * Accept or decline bibtex entry
    * Press "Add another entry" button on success dialog
    * Repeat steps 2-6
    * Press "close" button on success dialog
* Postcondition success: *The two accepted entries have been successfully added to the library*
* Postcondition failure: *No or just one entry has been added to the library. User can restart the process if needed*

### Use Case 8:
* Name: *User accidentally pressed the "add entry from plain text" button on the GUI*
* Agents: *Jabref user*
* Preconditions: *The "add entry from plain text" button shows up in the GUI*
* Common process 
    * Pressing "Add entry from plain text" button on the GUI (Jabref context menu)
    * Press the "cancel" button on the popup window
* Postcondition success: *No entry was added to the library and the pop-up window is closed*
* Postcondition failure: *Pop-up window is not closed or a garbage entry was added to the library*

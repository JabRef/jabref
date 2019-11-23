# Testplan

## **Table of Contents**
  - [**1. Introduction**](#1-introduction)
    - [**1.1 Objective**](#11-objective)
    - [**1.2 Related Documents**](#12-related-documents)
  - [**2. System Overview**](#2-system-overview)
  - [**3. Features**](#3-features)
    - [**3.1 To be tested**](#31-to-be-tested)
      - [**3.1.1 Functional Requirements**](#311-functional-requirements)
    - [**3.2 Not to be tested**](#32-not-to-be-tested)
  - [**4. Procedure**](#4-procedure)
    - [**4.1 Components and Integrationtests**](#41-components-and-integrationtests)
    - [**4.2 Function tests**](#42-function-tests)
  - [**5. Hardware and Software Requirements**](#5-hardware-and-software-requirements)
  - [**6. Test cases**](#6-test-cases)
    - [**6.1 Module tests**](#61-module-tests)
    - [**6.2 Function tests**](#62-function-tests)


## **1. Introduction**

The feature enables access for the user to copy reference text into a box which is accessible over the gui. When hitting the "Add to new library" or "Add to current library" button, the whole text will be sent to an external parser which reads the reference text and returns the corresponding values for the specific fields for a new entry in to the current or new library. The user can repeat this process and when needed, change some values which might not be right by his own.

### **1.1 Objective**

The main objective of this document is to give an good overview about all important tests of the software. It shows the parts which have to be / not to be tested. The document refers to the users, as well to the developers of this feature.

### **1.2 Related Documents**

- Specification Book
- Technical Documentation

## **2. System Overview**

The main responsibility of the system is split into three important components : 
- **gui**: Access to the feature over the gui.
- **net**: The external parser which is being used for this feature is hosted by a server so a connection from the client / user is essential.
- **logic**: The pipeline of the parser from reference text to bibentry.

The testing should especially focus on the logic and net packages because their funcionalities are the core of the feature. With testing a working functionality can be guaranteed more reliable.<br>

## **3. Features**

### **3.1 To be tested**

As mentioned before the testing focuses on the logic component of the system since it's core functionality is implemented there.

#### **3.1.1 Functional Requirements**

**logic**:
- The user gets notification if the parser fails **(F60)**
- The user can overthrow parser results **(F80)**
- The user can *chain* his entries without having to start over again from the beginning of the process **(F90)**
- Working functionality of the external parser **(F110)**
- The feature considers duplication issues with the internal jabref duplication checker **(F130)**
- Successfully creating a new bibentry corresponding to the results of an reference test **(F140)**
- No saving after an unexpected system crash **(171)**
- The feature can never be runned multiple times at the same time, but it can be opened again after it is closed in the current session. **(F160, F180)**

**net**:
- Communication to the external parser which is hosted by a server is possible **(F100)**

### **3.2 Not to be tested**

The whole functionality of the gui is not being tested.

## **4. Procedure**

### **4.1 Components and Integrationtests**

For all the tests Junit is going to be used.
- Every test is going to be in a single class file and will be tested separately, but in the corresponding test package (logic / net).

### **4.2 Function tests**

To make sure that alle the functionalities are working properly all the tests are going to be tested by the developers / implementers of this feature.

## **5. Hardware and Software Requirements**
- It is required that an internet connection is available:
  - The external parser which is being used in the system is hosted by the GROBID server.

## **6. Test cases**

### **6.1 Module tests**

| Class           | Test    |
|-------          |------   |
| GrobidService  | parserFailsNotification|
| GrobidService  | parserFailsNotification|
| GrobidCitationFetcher  | parserFailsNotification|
| GrobidCitationFetcher  | userCanOverthrowResults|
| GrobidCitationFetcher  | parseTeiToBibSuccess|
| GrobidCitationFetcher  | grobidParseRequestWorks|
| GrobidCitationFetcher  | parsePlainReferenceText|
| GrobidCitationFetcher    | serverRequestSucceed|
| GrobidCitationFetcher | TODO |
| GrobidCitationFetcher | TODO |
| GrobidCitationFetcher  | TODO |


### **6.2 Function tests**

--------

**Testcase:/TF01/**

*Testgoal:* The window to enter reference text opens when it is accessed over the Tab "Library" -> list item "New entry from plain text".<br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text". <br>

*Result:* The window to use the jabref parser opens. <br>

*Dependencies:* None <br>

--------

**Testcase:/TF02/**

*Testgoal:* When the window for the text parser opens after it is accessed over the tab "Library" -> list item "New entry from plain text" and then the button "Cancel" is pressed, the window closes. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text" so the window opens to parse plain text references. Then click on the button "Cancel".<br>

*Result:* The window closes. <br>

*Dependencies:* The function which opens the window to parse plain text references needs to work. <br>

--------

**Testcase:/TF03/**

*Testgoal:* When the window for the text parser opens after it is accessed over the tab "Library" -> list item "New entry from plain text" and then the button "Add to new library" is pressed after a reference text is put in, then the corresponding entries will be entered in a new library. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text" so the window opens to parse plain text references. Put a plain reference text in the box. Then click on the button "Add to new library".  <br>

*Result:* The new entries are put into a new library. <br>

*Dependencies:* The window must open and a plain reference text has to be pun into the text box. <br>

--------

**Testcase:/TF04/**

*Testgoal:* When the window for the text parser opens after it is accessed over the tab "Library" -> list item "New entry from plain text" and then the button "Add to current library" is pressed after a reference text is put in, then the corresponding entries will be entered in a new library. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text" so the window opens to parse plain text references. Put a plain reference text in the box. Then click on the button "Add to current library".  <br>

*Result:* The new entries are put into the current library. <br>

*Dependencies:* The window must open and a plain reference text has to be put into the text box.<br>

--------

**Testcase:/TF05/**

*Testgoal:* When the window for the text parser opens after it is accessed over the tab "Library" -> list item "New entry from plain text" and then a plain reference text is put into the text box then the buttons "Add to new library" and "Add to current library" become clickable.<br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text" so the window opens to parse plain text references. Put a plain reference text in the box.  <br>

*Result:* The buttons "Add to new library" and "Add to current library" become clickable.<br>

*Dependencies:* The window must open and a plain reference text has to be put into the text box. <br>

--------

**Testcase:/TF06/**

*Testgoal:* When the window for the text parser opens after it is accessed over the tab "Library" -> list item "New entry from plain text" and then multiple text references are put into the text box it must show a popup with a list that contains all the reference texts  where the parser failed after the button "Add to current library" or "Add to new library" has been pressed. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the Tab "Library" and then on the list item "New entry from plain text" so the window opens to parse plain text references. Put multiple reference texts in the box (some of them cannot be parsed). Click either on the button "Add to current library" or "Add to new library". <br>

*Result:* It shows a popup with a list that contains all the reference texts where the parser failed. <br>

*Dependencies:* The window must open and multiple plain texts have to be entered in the text box. But some of them cannot be parsed so the list for the failed parsings is not empty. <br>

--------

**Testcase:/TF07/**

*Testgoal:* The window to enter reference text opens when it is accessed when the button "Parse with Grobid" is clicked on the toolbar. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" which can be found on the toolbar.

*Result:* The window to parse text references opens. <br>

*Dependencies:* None <br>

--------

**Testcase:/TF08/**

*Testgoal:* The window for the parser closes if the button "Cancel" is pressed. The window must be opened by clicking on the button "Parse with Grobid" on the toolbar. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" on the toolbar. After the window for the parser opens the button "Cancel" is pressed. <br>

*Result:* The window closes. <br>

*Dependencies:* The window for the parser opens when the button "Parse with Grobid" is pressed on the toolbar.  <br>

--------

**Testcase:/TF09/**

*Testgoal:* When the button "Add to new library" on the parser window is pressed then the corresponding entries are being added to a new library. The window must have been opened by clicking on the button "Parse with Grobid" on the toolbar. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" on the toolbar. Put a reference text in the box of the parser window which opens by clicking on the button. Then press on the button "Add to new library".  <br>

*Result:* The created new entries are being added to a new library.<br>

*Dependencies:* The window for the parser opens when the button "Parse with Grobid" is pressed on the toolbar. <br>

--------

**Testcase:/TF10/**

*Testgoal:* When the button "Add to current library" on the parser window is pressed then the corresponding entries are being added to the current library. The window must have been opened by clicking on the button "Parse with Grobid" on the toolbar. <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" on the toolbar. Put a reference text in the box of the parser window which opens by clicking on the button. Then press on the button "Add to current library".  <br>

*Result:* The created new entries are being added to the current library.<br>

*Dependencies:* The window for the parser opens when the button "Parse with Grobid" is pressed on the toolbar. <br>

--------
 
**Testcase:/TF11/**

*Testgoal:* When a text reference is put into the box of the parser window after it has been opened by clicking on the button "Parse with Grobid" on the toolbar then the buttons "Add to new library" and "Add to current library" become clickable.  <br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" on the toolbar. Put a reference text in the box of the parser window which opens by clicking on the button.  <br>

*Result:* The buttons "Add to new library" and "Add to current library" become clickable. <br>

*Dependencies:* The window for the parser opens when the button "Parse with Grobid" is pressed on the toolbar. <br>

--------

**Testcase:/TF12/**

*Testgoal:* When multiple text references which cannot be parsed are put into the text box of the parser window after it has been opened by clicking on the button "Parse with Grobid" on the GUI then a new window opens which shows all the text references where the parser failed.<br>

*Requirements:* Jabref is running. <br>

*Executed procedure:* Click on the button "Parse with Grobid" on the toolbar. Put a multiple text references in the box of the parser window which cannot be parsed in the text box of the window. Then click either on the button "Add to current library" or "Add to new library". <br>

*Result:* A window shows up containing all the reference texts where the parser failed. <br>

*Dependencies:* The window for the parser opens when the button "Parse with Grobid" is pressed on the toolbar. Multiple text references which cannot be parsed have to be added in the text box of the window, separated with ";;". <br>

--------

---
layout: default
title : Woche 6
---
# Testplan template

| Version | Projectname | Authors  | Status | Date | Comment |
| ------- | ----- | ------ | ------ | ----- | --------- |
|  0.1    |  Implement better search   | Julian Bopp, Albert Jasari, Daniel Madoery, Mark Starzynski | in Progress | 17.11.2021 | |

# 1. Introduction

- The open-sourced citation and reference management software JabRef is in need of a more functional global search and a user-friendly interface thereof.
- The users of JabRef should be able to conveniently search in their citations library and perform complex search querys, also via GUI.
- The goal of this project is to develop an implementation that improves upon the current library search capabilities and enables users of the JabRef software to very easily navigate their libraries. 

## 1.1 Purpose

- The test plan includes features about the management software JabRef which are tested in their function and the features are explained.
- In addition, a system overview with the components to be tested is listed, so you can get an overview of the functionality contained in JabRef (for example its complex query search).
- You can examine thereby its hardware and software requirements as well as modules. The functions of the methods are represented by automated unit tests.
- In this test plan you can find all important information about the purpose and use of the individual functions that merge into a unified product - better search.

## 1.2 References

- Technical Specification Document: [technical-documentation.md](https://github.com/josphstar/jabref/blob/designdoc/docs/sweng/technical-documentation.md)
- Functional Specification Document: [functional-specification-document.md](https://github.com/josphstar/jabref/blob/designdoc/docs/sweng/functional-specification-document.md)
- [Issue #341](https://github.com/koppor/jabref/issues/341)

## 2. Systemoverview

- The components which have to be tested are the methods in RecentSearch, the different Buttons in the DropDownMenu and its methods, the methods in SearchItem, ItemType, Highlighting and Autocomplete.

![Class Diagram](/w3-UML/out/w3-UML/better-search/class-diagram.png "class-diagram.png") 

## 3. Features

### 3.1 To be tested features (Features / Funktionen)

#### DropDownMenu

- GUI visible
- Buttons working on mouse clicked

#### Recent Search

- Recent search removes duplicates

#### SearchFieldSynchronizer

- Get search string
- Search string builder
- Search item list
- Update search item list
- Is previous attribute
- Is previous operator
- Return latest really returns latest
- Add item does not create invalid search
- Searchbar highlighting works
- Searchstring builder builds mixed strings correctly
- Update Search Itemlist

#### Autocomplete

- Autocomplete creates recommendations in searchbar/list

#### 3.1.1 Functional Requirements

- The tested functions can be found in this list under point 3. "Specific requirements" from the [functional specification.md](https://github.com/josphstar/jabref/blob/designdoc/docs/sweng/functional-specification-document.md).


### 3.2 Not to be tested features (Features / Functions)

- Funktion tests of the GUI are not tested.

## 4 Proceeding

### 4.1 Components and Integration Tests

- The tests are performed by unit tests.

### 4.2 Functional Tests

- The tests are performed by unit tests in which we check the implementation. To the assistance we have the technique of the cause-effect graphs in which we can execute the specification of the conditions. Since the tests run automatically, we need a so-called test oracle which we can implement from the existing specifications (see post-condition and pfand specify!!).
The modules are carried out on a separate test environment by a separate team.

### 5 Hardware and Software Requirements

- There are no special hardware or software requirements needed.

## 6 Testcases

### 6.1 Modultests

| Name of the Class | Name of the Testcase | Status |
| ------- | ----- | -----|
| SearchFieldSynchronizer | returnLatestReallyReturnsLatest  | |
| SearchFieldSynchronizer | addItemDoesNotCreateInvalidSearch | |
| SearchFieldSynchronizer | SearchBarHighlightingWorks | |
| SearchFieldSynchronizer | SearchStringBuilderBuildsMixedStringCorrectly| |
| SearchFieldSynchronizer | testGetSearchString() | |
| SearchFieldSynchronizer | testSearchItemList() | |
| SearchFieldSynchronizer | testIsPrevAttribute() | |
| SearchFieldSynchronizer | testIsPrevOperator() | |
| RecentSearch | RecentSearchRemovesDuplicates | |
| DropDownMenu | testButtonWorking | |
| DropDownMenu | testDropDownShowing | |
| DropDownMenu | testDropDownNotShowing | |
| GlobalSearchBar | completeSearchbarInDropdown() | in progress|
| GlobalSearchBar | completeWithoutAddingAnythingReturnsSomething() | in progress|
| GlobalSearchBar | completeReturnsMultipleResultsInDropdown() |in progress |

### 6.2 Funktionstests

| Name of the Class |  Name of the Testcase | Status | 
| ------- | ----- | ----- |
| SearchFieldSynchronizer | testSearchStringBuilder() | | 
| SearchFieldSynchronizer | testUpdateSearchItemList() | | 

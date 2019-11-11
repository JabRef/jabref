# Technical Documentation

## Table of Contents
1. [Introduction](#1-Introduction) <br>
1.1 [Objective](#1.1-Objective) <br>
1.2 [Related documents](#1.2-Related-documents)
2. [Project overview](#2-Project-overview)
3. [Design Goals](#3-Design-Goals)
4. [System Behaviour](#4-System-Behaviour)
5. [Design](#5-Design) <br>
5.1 [Static](#5.1-Static) <br>
5.2 [Dynamic](#5.2-Dynamic) <br>
5.3 [Logic](#5.3-Logic)

## **1 Introduction** 
### **1.1 Objective**
This document should help all developers of JabRef and the project by explaining the design and functionality of the feature. After reading this document, the reader should have a better understanding of how the design and functionality of the feature are justified, focusing on the most important concepts.

### **1.2 Related documents**
- Specification Book with Glossary
- MockUp

## **2 Project overview**
The feature is an expansion to the existing open source reference manager called **JabRef**. The main goal of the feature is that an user of JabRef is able to copy a text reference into a popup-window which is accessible over the GUI. After the user pasted the text reference into the box, the user can click on a parse-button. Following points should be fullfilled:
- The user has the option to also parse multiple text references
- The parser filters all relevant references from the text reference
- An appropriate Bibentry with re results of the parser should be created
    - If the Entry already exists, nothing happens

## **3 Design Goals**
- The design must work on every furter version of jabref
- The design mustn't take much time for the user
- The design can be used on multiple languages
- The design must ensure maximum performance
- The design must be very efficient for the user to use

## **4 System behaviour**
There are some tasks of the system behaviour related to different conditions. The most important ones are listed below:
- The user can access to the parser over the main GUI of jabref
    - The first window contains a box to put the whole reference text in with a button "Parse" and "Cancel".
    - The user can decide if he wants to keep / change the parser results.
    - The user can deceide to parse another after the parsers job is done or not.
- Empty reference text will be ignored so the parser does nothing.
- The parser returns a String containing the references to create the specific entry.
    - The creation of the new entry will be done separately and will only be done, if the entry doesn't already exist, independent of the result from the pasrser.
- The user can manually edit the corresponding entries if the result is not satisfying.


## **5 Design**
In the follwing the Design of the system is fully represented in UML-Diagrams. An UML-Class-Diagram is used to show the static aspects of the system. To describe the interaction / communication between the objects we decided to use an sequence diagram for the dynamic aspects of the system.

### **5.1 Static**

![image](classDiagram.png)

### **5.2 Dynamic**
To get a better understanding about the dynamic processes of the main classes / objects of the system, view the following Diagrams: <br>

**PipeLine Process**: <br>

![image](SequenceDiagramPipeLine.png)

**Client / Server parse request**: <br>

![image](SequenceDiagramHttpClass.png)

**Gui Interaction Process**: <br>

![image](StateDiagramGUI.png)

**Track plain text to bibEntry**: <br>

![image](ActivityDiagramTrackPlainEntry.png)

### **5.3 Logic**


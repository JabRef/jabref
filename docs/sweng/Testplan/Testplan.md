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

The feature enables access for the user to copy reference text into a box which is accessible over the gui. When hitting the "parse" button, the whole text will be sent to an external parser which reads the reference text and returns the corresponding values for the specific fields for a new entry in to the library. The user can repeat this process and when needed, change some values which might not be right by his own.

### **1.1 Objective**

The main objective of this document is to give an good overview about all important tests of the software. It shows the parts which have to be / not to be tested. The document is refers to the users, as well to the developers of this feature.

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
| ParserPipeline  | parserFailsNotification|
| ParserPipeline  | userCanOverthrowResults|
| ParserPipeline  | parseTeiToBibSuccess|
| ParserPipeline  | grobidParseRequestWorks|
| ParserPipeline  | parsePlainReferenceText|
| GrobidClient    | serverRequestSucceed|
| HttpPostService | TODO |
| HttpPostService | TODO |
| ParserPipeline  | TODO |


### **6.2 Function tests**

**Testcase:/TFXX/**

*Testgoal:* <br>

*Requirements:* <br>

*Input:* <br>

*Expected Output:* <br>

*Dependencies:* <br>

--------

**Testcase:/TFXX/**

*Testgoal:* <br>

*Requirements:* <br>

*Input:* <br>

*Expected Output:* <br>

*Dependencies:* <br>

--------
**Testcase:/TFXX/**

*Testgoal:* <br>

*Requirements:* <br>

*Input:* <br>

*Expected Output:* <br>

*Dependencies:* <br>

--------
**Testcase:/TFXX/**

*Testgoal:* <br>

*Requirements:* <br>

*Input:* <br>

*Expected Output:* <br>

*Dependencies:* <br>

--------
**Testcase:/TFXX/**

*Testgoal:* <br>

*Requirements:* <br>

*Input:* <br>

*Expected Output:* <br>

*Dependencies:* <br>

--------

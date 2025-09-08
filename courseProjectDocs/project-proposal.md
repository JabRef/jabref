## Project Overview
  JabRef is an cross platform citation and reference managment tool design to help mangage and find bilibographic data. Key Features include:
  - Collections: Users and import references for serveral online scientific catalogs such as Goog Scholar and PubMed, retrieve its metadata using DOI or ISBN,
  - Organization: The software allows research to be filtering, manage duplication , add custom data fields and has advance surch features/ Users can allso attach related documents together
  - Cite: With the intergrate of LaTeX editors, as more references are added , it can magemange the citations needed in serveral different styles
  - Share: Allows users to exort their files with sharign services such as Dropbox, and allows different databases to be sync using a sql database

# Key Metrics:
## Code Structure: 
### 1. LOC (per file/module)
### 2. Comment Density 
### 3. Cyclomatic Complexity
### 4. Number of Unit Tests

## **Modules**:
|    Module    |    Module    |
| ------------- | ------------- |
| build-logic   | build-support |
| jabgui        | jabkit        |
| jablib        | jabls         |
| jabsrv       | jabsrv-cli     |
| test-suport  |  jabls-cli     |

#### + build-logic
#### + build-support
#### + jabgui
#### + jabkit
#### + jablib
#### + jabls
#### + jabls-cli
#### + jabsrv
#### + jabsrv-cli
#### + test-support

![The Module Results for code metrics](metrics_1_results.png)

These *modules* were chosen as they contained the 'src' code for major contributions to the project, their file extensions include: .kts, .java, and .css


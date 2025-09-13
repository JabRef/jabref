# Requirements and Test Oracles

## Functional Requirements
1. The system shall allow users to import bibliographic data from multiple sources, including but not limited to, DOI, ISBN, PubMed, and arXiv-ID, and shall extract metadata from PDF documents.
2. The system shall enable users to organize references into collections, assign keywords, merge duplicate entries, and automatically move associated documents according to a specific rule set.
3. The system shall support native BibTex and BibLaTeX for importing and exoporting sources from files
4. The system shall shaw allow users to completed and fix their bilibgraphic data through comparision of online catalogs such as Google Scholar, Springer, or MathSciNet
5. The system shall ...
6. The system shall ...
...

## Non-Functional Requirements
1. The system shall be free of charge, open-source, and cross-platform to ensure accessibility and ease of use for users across different operating systems and environments.
2. The system shall shall be able to be run as a CLI application or built from the source to give users installion options. 
3. The system shall ...
...

## Test Oracles
1. Given a valid DOI entered by the user, the system shall fetch and populate the corresponding complete bibliographic information.
2. Given that a user organizes references into groups using, the system shall display those references in collections.
3. When the system is installed on any major operating system (e.g. Windows, macOS, or Linux), the system shall behave functionally equivalent and not require paid licensimg.
4. When givin an arctile through a BibTex or BibLaTeX format, the system shall create the appropriate entry in the database
5. Given there is a entry, the system shall fill missing fields based upoon finding in online catalogs
6. When the system is installed using the CLI option, the system and be run using jbang or Jabkit inorder to start up the application.
7. When ...
8. When ...
9. When ...
...

| Requirement ID | Requirement Description | Test Oracle (Expected Behavior) |
|-----------------------|-----------------------------------|---------------------------------------------|
| FR-1 | The system shall allow users to import bibliographic data from multiple sources, including but not limited to, DOI, ISBN, PubMed, and arXiv-ID, and shall extract metadata from PDF documents. | Given a valid DOI entered by the user, the system shall fetch and populate the corresponding complete bibliographic information. |
| FR-2 | The system shall enable users to organize references into collections, assign keywords, merge duplicate entries, and automatically move associated documents according to a specific rule set. | Given that a user organizes references into groups using, the system shall display those references in collections. |
| NFR-1 | The system shall be free of charge, open-source, and cross-platform to ensure accessibility and ease of use for users across different operating systems and environments. | When the system is installed on any major operating system (e.g. Windows, macOS, or Linux), the system shall behave functionally equivalent and not require paid licensimg. |
| FR-3 |The system shall support native BibTex and BibLaTeX for importing and exoporting sources from files|When givin an arctile through a BibTex or BibLaTeX format, the system shall create the appropriate entry in the database|
| FR-4 |The system shall shaw allow users to completed and fix their bilibgraphic data through comparision of online catalogs such as Google Scholar, Springer, or MathSciNet|Given there is a entry, the system shall fill missing fields based upoon finding in online catalogs|
| NFR-2 | The system shall shall be able to be run as a CLI application or built from the source to give users installion options. |  When the system is installed using the CLI option, the system and be run using jbang or Jabkit inorder to start up the application. |
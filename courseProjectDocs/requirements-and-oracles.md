# Requirements and Test Oracles

## Functional Requirements
1. The system shall allow users to import bibliographic data from multiple sources, including but not limited to, DOI, ISBN, PubMed, and arXiv-ID, and shall extract metadata from PDF documents.
2. The system shall enable users to organize references into collections, assign keywords, merge duplicate entries, and automatically move associated documents according to a specific rule set.
3. The system shall support native BibTex and BibLaTeX for importing and exporting sources from files
4. The system shall shaw allow users to complete and fix their bilibgraphic data through comparison of online catalogs such as Google Scholar, Springer, or MathSciNet
5. The system shall allow users to share their document library with other users as a .txt file that can be shared to Dropbox or Google Drive.
6. The system shall provide customizable export options, including exporting over 15 supported files.
7. The system shall synchronize user's content library with a SQL database.

## Non-Functional Requirements
1. The system shall be free of charge, open-source, and cross-platform to ensure accessibility and ease of use for users across different operating systems and environments.
2. The system shall shall be able to be run as a CLI application or built from the source to give users installion options. 
3. The SQL database for library syncing shall maintain 99.9% availability to ensure the user can always sync as needed.
4. The SQL database for library syncing shall implement encryption and access controls to ensure data security.
5. Upon pressing 'export' in JabRef GUI, the system shall process export filters accurately and efficiently, maintaining the integrity of user-customized export formats, given any.

## Test Oracles
1. Given a valid DOI entered by the user, the system shall fetch and populate the corresponding complete bibliographic information.
2. Given that a user organizes references into groups using, the system shall display those references in collections.
3. When the system is installed on any major operating system (e.g. Windows, macOS, or Linux), the system shall behave functionally equivalent and not require paid licensimg.
4. When given an arctile through a BibTex or BibLaTeX format, the system shall create the appropriate entry in the database
5. Given there is an entry, the system shall fill missing fields based upon finding in online catalogs
6. When the system is installed using the CLI option, the system can be run using jbang or Jabkit inorder to start up the application.
7. When the user clicks 'sync library', the system will securely connect to the remote storage and update the local library by merging changes from the server.
8. When exporting a .bib citation, the system will display an export dialog allowing the user to select or preview the citation formatted according to the selected export filter.
9. When exporting the entire library, the system will prompt the user with a save file dialog to choose the destination location, such as the desktop.


| Requirement ID | Requirement Description | Test Oracle (Expected Behavior) |
|-----------------------|-----------------------------------|---------------------------------------------|
| FR-1 | The system shall allow users to import bibliographic data from multiple sources, including but not limited to, DOI, ISBN, PubMed, and arXiv-ID, and shall extract metadata from PDF documents. | Given a valid DOI entered by the user, the system shall fetch and populate the corresponding complete bibliographic information. |
| FR-2 | The system shall enable users to organize references into collections, assign keywords, merge duplicate entries, and automatically move associated documents according to a specific rule set. | Given that a user organizes references into groups using, the system shall display those references in collections. |
| NFR-1 | The system shall be free of charge, open-source, and cross-platform to ensure accessibility and ease of use for users across different operating systems and environments. | When the system is installed on any major operating system (e.g. Windows, macOS, or Linux), the system shall behave functionally equivalent and not require paid licensimg. |
| FR-3 |The system shall support native BibTex and BibLaTeX for importing and exporting sources from files|Whengiven an arctile through a BibTex or BibLaTeX format, the system shall create the appropriate entry in the database|
| FR-4 |The system shall shaw allow users to  complete and fix their bilibgraphic data through comparison of online catalogs such as Google Scholar, Springer, or MathSciNet|Given there is an entry, the system shall fill missing fields based upon findings in online catalogs|
| NFR-2 | The system shall shall be able to be run as a CLI application or built from the source to give users installion options. |  When the system is installed using the CLI option, the system can be run using jbang or Jabkit inorder to start up the application. |
| NF-3 | The SQL database for library syncing shall implement encryption and access controls to ensure data security. | When the user clicks 'sync library', the system will securely connect to the remote storage and update the local library by merging changes from the server. | 
| FR-5 | The system shall allow users to share their document library with other users (including a .txt file) that can be shared to Dropbox or Google Drive. | When exporting the entire library, the system will prompt the user with a save file dialog to choose the destination location, such as the desktop. |

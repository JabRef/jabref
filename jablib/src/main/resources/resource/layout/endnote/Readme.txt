EndNote Export Filter for JabRef
version 1.0
2004-12-02

Written by Alex Montgomery (ahm@stanford.edu)

*********************************************************************************
Overview:
*********************************************************************************

The EndNote Export Filter for JabRef (when combined with the "EndNote Import from JabRef.enf" filter for EndNote, derived from the "EndNote Import" filter) allows for most of the default JabRef fields to be imported into the appropriate EndNote fields. Two export styles from EndNote ("BibTeX Export to JabRef") are also included that support the same fields. Note that the default EndNote Reference Types must be modified by the user to support the import of certain fields.

*********************************************************************************
Installation:
*********************************************************************************
EndNote Import from JabRef.eni
This file must be placed in your EndNote Filters directory. On a macOS system, the default directory is /Applications/EndNote 7/Filters. On a Windows XP system, the default directory is C:\Program Files\EndNote\Filters. The default EndNote Import filter will be able to import the files from JabRef, but supports fewer fields. You should then open up the Filter Manager (Edit->Import Filters->Open Filter Manager) and add it to your Favorites.

EndNote Preferences
The filter provided will only work if certain fields are added to EndNote's default Reference Types. Open up Preferences and click on Reference Types, then Modify Reference Types. The following table lists the field names that must be added to certain reference types for EndNote to support these fields. For example, the Publisher field for Journal Article is blank by default; Type in Publisher in this field. 

Tag	Generic Name 	New Field Name	Ref Types
%I	Publisher	Publisher	(Journal Article)
%& 	Section		Section 	(Book Section)
%9 	Type of Work	Type of Work 	(Book Section)
%8 	Date		Date 		(Book, Book Section, Thesis)
%1 	Custom 1	pdf		(All)
%2 	Custom 2	comment		(All)
%3 	Custom 3	entrytype	(All)
%4 	Custom 4	crossref	(All)
%# 	Custom 5	owner		(All)
%$ 	Custom 6	key		(All)
(All = Journal Article, Book, Book Section, Conference Proceedings, Report, Thesis)

EndNote.*.layout:
These files must be kept together in a single directory.
Start JabRef. In the "Options" menu you will find the button "Manage custom exports". The "Manage custom exports" interface will appear. Click the "Add new" button. You can choose a name for the export filter (e.g. "EndNote"). Specify the location of the main layout file (which is the file "EndNote.layout") by typing the full path or by using the "Browse" button. The file extension should be set to ".txt" Click "OK". Now you will find the new custom export filter in the "File" menu under the pop-up menu "Custom export". 

EndNote.tab:
This is the tab-delimited spreadsheet containing a list of all the Refer codes, how they map to the Generic EndNote fields, and how the JabRef fields for the default BibTeX types are mapped to the Generic EndNote fields.

BibTeX Export to JabRef, BibTeX Export to JabRef*
These file are optional for if you wish to re-export these entries to JabRef. They must be placed in your EndNote Styles directory. On a macOS system, the default directory is /Applications/EndNote 7/Styles. On a Windows XP system, the default directory is C:\Program Files\EndNote\Styles. You may then want to open up the Filter Manager (Edit->Output Styles->Open Style Manager) and add it to your Favorites.

*********************************************************************************
Usage
*********************************************************************************
To export the entries of your database select File->Custom Export->EndNote and type in the output filename.

To import the entries into Endnote, open up a (or create a new) database. Select File->Import, then select "EndNote Import from JabRef." Click on Choose File, then select the output file you created in the previous step.

To re-export to JabRef, two EndNote Styles are provided. Select a style (see "Notes" for the problems with each), then select File->Export. The exported text file is a BibTeX file ready to be read by JabRef.

*********************************************************************************
Notes:
*********************************************************************************
The export format implemented is the EndNote Import format, an extension of the Refer format.

Only two JabRef fields are unsupported due to a lack of Custom fields in EndNote: doi and citeseerurl. Enterprising users should be able to modify the enclosed files in order to swap out two other fields (e.g. pdf and owner) instead. Note that EndNote 8 has additional fields that could be ideal for doi and citeseerurl (part of the reason why these are excluded here). In particular, Electronic Resource Number (DOI) and Link to PDF. The latter is actually a URL field, not a relative field like the pdf field in JabRef, so it would actually be better for citeseerurl than pdf. Unfortunately, ISI ResearchSoft has established no new extension of the Refer standard to include these fields, so any immediate solution would be likely to break later.

This has been tested on Mac OS X 10.3.6 using JabRef 1.6 and EndNote 7. It should work on Windows XP using EndNote 7, or EndNote 8 on either platform.

BibTeX Export to JabRef munges together some BibTeX types (e.g. misc/unpublished/other/manual -> misc). Use BibTeX Export to JabRef* instead if you need better mapping - but only if you are using field Custom 3 (entrytype) to store the BibTeX entrytypes. Note that this export filter ONLY currently covers the reference types that the EndNote Export Filter uses (i.e. Journal Article, Book, Book Section, Conference Proceedings, Report, Thesis).

BibTeX Export to JabRef* is ONLY for use if you are re-exporting a file that was imported from JabRef. This is because it uses Custom 3 (entrytype) to store and output the entrytype rather than guessing from the EndNote reference type. If this field isn't filled in, it will export bad BibTeX code. You have been warned.

Booklet, when exported back to JabRef using either BibTeX Export to JabRef,
has \publisher filled instead of \howpublished; \lastchecked is not re-exported. This is due to the fact that EndNote doesn't have export filters sophisticated enough to fix this properly; however, the Book EndNote reference type is the only one appropriate for the "booklet" BibTeX type.

Corporate Authors (e.g. {Central Intelligence Agency} should be mapped to "Central Intelligence Agency," in EndNote, but aren't (this would require a new formatter in JabRef).
# Required Features

##Realization of the problem
Problem with the auto-complete function for the various fields.

For example, the list of journal abbreviations only works if the journal name matches exactly. This means that when I manually enter an entry, I have to find and copy and paste another entry from the same journal, or, if it's the first time a particular journal occurs, I have to open the abbreviation list and copy and paste from there.

Suggested solution:

It would be beneficial to start typing and the program would make suggestions for the end based on entries already in the database or abbreviation database, similar to what Firefox, Chrome and the like do with their text fields. It would also make sure that all entries conform to a consistent convention.


### CODE: AUTOCOMPLETE IMPLEMENTATION
- API [algoliasearch](https://github.com/algolia/algoliasearch-client-java-2)
- Implements a [ternary search tree](https://www.codeproject.com/articles/5819/ternary-search-tree-dictionary-in-c-faster-string?pageflow=Fluid&fid=31232&df=90&mpp=25&sort=Position&view=Normal&spc=Relaxed&prof=True&fr=26)


## pacakge: gui/search

- ### class:
    - Implements 
  
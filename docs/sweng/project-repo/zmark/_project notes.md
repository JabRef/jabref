# Packages and Classes

- org\jabref\logic\importer\
    - fetcher\ComplexSearchQuery.java
    - fetcher\CompositeSearchBasedFetcher.java
    - SearchBasedFetcher.java
    - SearchBasedParserFetcher.java

- org\jabref\logic\search
    - DatabaseSearcher.java
    - SearchQuery.java
- org\jabref\gui\search
    - rules.describer
    - GlobalSearchBar.java
    - GlobalSearchResultDialog.java
    - GlobalSearchResultDialogViewModel.java
    - RebuildFulltextSearchIndexAction.java
    - SearchDisplayMode.java
    - SearchResultsTable.java
    - SearchResultsTableDataModel.java
    - SearchTextField.java
- org\jabref\model\search
    - matchers
    - rules
    - GroupSearchQuery.java
    - SearchMatcher.java
- logic\pdf\search\ ???


# Lucene Notes
- [Description](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html) Gute Beschreibung für benutzung von Lucene
- AddDoc() --> AddDocuments()
- Searching needs an already built index; ...

# PopOver usage and JavaDoc
- ord.jabref.gui.JabrefFrame
- Zeile: 174, 949-959
- method: createTaskIndicator
- [JavaDoc](http://javadox.com/org.controlsfx/controlsfx/8.0.5/org/controlsfx/control/PopOver.html) 

# GlobalSearchbar
- org.jabref.gui.JabrefFrame
- initialisation: 160, 184
- case search: 248
- toolbar: 487
- method getGlobalSearchbar: 1181
- org jabref.gui.search.GlobalSearchBar

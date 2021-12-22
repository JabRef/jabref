# Required Features / Functional Overview

### MINOR BUGFIX
- possible use case scenario
  - [Is it really a problem if the user searches for an empty string, e.g. author = ""?](https://github.com/JabRef/jabref/pull/6687/files#r464082845)
  - ...(We should keep this in the back of our heads when the user interface is implemented. There you would like to show a helping message instead of an error dialog).

### CODE: LUCENE QUERY SYNTAX IMPLEMENTATION
- [lucene](https://lucene.apache.org/core/8_6_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html)
- [query syntax](http://www.lucenetutorial.com/lucene-query-syntax.html)
- [query syntax 2](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
- [#6799](https://github.com/JabRef/jabref/pull/6799/files)
  - [AST: abstract syntax tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree)
- class [QueryParser](https://lucene.apache.org/core/4_0_0/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html) is used to convert a lucene search input (in web search i.e.) to a complex query, which can be then used to fetch the results. it returns a complexSearchQuery object.
  - use this class to apply to lib search
- requested OR query in case of empty spaces in between search terms is already implemented

### CODE: GUI
- hint box adjustments
- red/green syntax indicator [#6805](https://github.com/JabRef/jabref/pull/6805/files)
- search interface example in [#7423](https://github.com/JabRef/jabref/issues/7423)
- 






# Appendix

### OPEN QUESTIONS
- should green/red highlighting as seen in web search be implemented into lib search? ([#6805](https://github.com/JabRef/jabref/pull/6805))?
- is chipview a requirement ([7806](https://github.com/JabRef/jabref/issues/7806))?
- 
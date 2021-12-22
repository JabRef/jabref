# Lucene Syntax Feature

* All terms in the query are whitespace separated and will be ORed
* Default and certain fielded terms are supported
* Fielded Terms:
  * `author`
  * `title`
  * `journal`
  * `year` (for single year)
  * `year-range` (for range e.g. `year-range:2012-2015`)
* The `journal`, `year`, and `year-range` fields should only be populated once in each query
* Example:
  * `author:"Igor Steinmacher" author:"Christoph Treude" year:2017` will be converted to
  * `author:"Igor Steinmacher" AND author:"Christoph Treude" AND year:2017`


### QueryParser
- Converts query string written in lucene syntax into a complex search query

- OPENQUESTION: Should AND or OR be implemented as standard? <!--????-->

- new: [Add support for Lucene as search syntax #8206](https://github.com/JabRef/jabref/pull/8206)
   - reg ex button in search bar should be removed. all regex searches should be handled by lucene based search.
  - search rule describer for Lucene has to be created. Currently only a dummy. <!--????-->
  - think of removing the search grammar completely <!--????-->


### CODE: LUCENE QUERY SYNTAX IMPLEMENTATION
- [lucene](https://lucene.apache.org/core/8_6_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html)
- [query syntax](http://www.lucenetutorial.com/lucene-query-syntax.html)
- [query syntax 2](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
- [#6799](https://github.com/JabRef/jabref/pull/6799/files)
  - [AST: abstract syntax tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree)
- class [QueryParser](https://lucene.apache.org/core/4_0_0/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html) is used to convert a lucene search input (in web search i.e.) to a complex query, which can be then used to fetch the results. it returns a complexSearchQuery object.
  - use this class to apply to lib search
- requested OR query in case of empty spaces in between search terms is already implemented
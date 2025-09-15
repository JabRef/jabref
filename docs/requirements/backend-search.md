---
parent: Requirements
---
# Backend Search
Search functionialities the backend needs to support.

## General Search
Standard search functions.

### Word Matching
`req~backend-search.general-search.word-matching~1`

Searching for a word retrieves all entries containing it. 
Searching for multiple words only returns entries containing all of them.

### Sequence Matching
`req~backend-search.general-search.sequence-matching~1`

Searching for a sequence of words only lists entries containing the words of the sequence in the given order. 
Searching for multiple sequences lists entries containing all sequences.

### Combinations
`req~backend-search.general-search.combinations~1`

Searching for words and sequences at the same time is allowed and results in a list of entries containing all words and sequences.
<!-- What should happen when searching for a word and a sequence containing the word? -->

### Case Sensitivity
`req~backend-search.general-search.case-sensitivity~1`

Allow searching for words and sequences with and without respecting case sensitivity.

### Normalisation of Terms
`req~backend-search.general-search.normalisation~1`

Terms with varying spelling get detected and are searched in all commonly known variations. E.g. Searching for `Düsseldorf` will also find entries containing `Duesseldorf`, `Dusseldorf` and `D\"{u}sseldorf` and vice versa.

## Field Search
Search functions using the fields that bibliographic data provides. E.g. Author, date, title, url and more.

### Field Contains
`req~backend-search.field-search.field-contains~1`

Fields are searchable for contained words and sequences. Listing entries with the specified field which contains the searched word or sequence.
<!-- Inherent anyfield search is not a requirement, but a neat implementation feature. -->

### Field Matches
`req~backend-search.field-search.field-matches~1`

Fields are searchable for exact matches. I.e. words or sequences.
E.g. `year == 1932` searches for entries with the year field being exactly 1932, assuming "==" is the symbol for "matches".

### Field Search Combinations
`req~backend-search.field-search.combinations~1`

Searches can be logically combined using boolean operations.  
Concatenate two search phrases with `and` and only results that fulfill both conditions are shown.  
Use `or` between two search phrases to list entries that satisfy either one or both.  
Prepend a search phrase with `not` to invert the resulting entry list, showing all entries that would have been hidden.  
Finally, using `(` and `)` to isolate a part of the logical operations to ensure its resolution before the standard left to right order.

Note that the symbols used here are just an example.

### Pseudo Fields
`req~backend-search.field-search.pseudo-fields~1`

The following four pseudo fields are supported: anyfield, anykeyword, key and entrytype.  
- Searching for a word or sequence in any field is possible and returns entries that have a field containing the searched word or sequence.  
- Search among the keywords is supported, listing entries when the given phrase is amongst or part of its keywords.  
- Searching for citation keys will show entries containing the phrase in their citation key or show the single entry whose key matches the phrase (in case of a exact match search).  
- Entries of a certain type can be listed. E.g. `entrytype = thesis` lists all entries whose type contains `thesis`, like `bachelorsthesis` or `mastersthesis`.

## Searching with Regular Expressions
Search function requirements regarding the support of regular expressions. In the case of JabRef, specifically refer to [java regular expressions](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/regex/Pattern.html).

### Regular Expressions Search
`req~backend-search.regex-search~1`

When searching, regular expressions as defined in Java can be used. 
This means the backend is searchable with special characters (e.g. tab: `\t`, carriage-return: `\r`), with character classes (e.g. `[a-z]`), with boundary matchers (e.g. `\b`, `\B`), greedy, reluctant and possessive quantifiers (e.g. `?`, `*`, `+`) and logical operators (e.g. braces: `(` `)`, or: `|`).  
Searching with special characters by escaping them is also supported.



<!-- markdownlint-disable-file MD022 -->

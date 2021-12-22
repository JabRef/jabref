# Issues
##### OPEN
- [Search for type does not work in biblatex mode #193](https://github.com/koppor/jabref/issues/193)
    - type=book and title=management: no hit
    - publisher=springer and year=2006 and title=management: one hit
- [Improve Search Interface](https://github.com/JabRef/jabref/issues/7423)
- [Add ChipView for web search interface #7806](https://github.com/JabRef/jabref/issues/7806)
- [Search highlighting broken at complex searches #8067](https://github.com/JabRef/jabref/issues/8067)
    - search title=TALEN and abstract=plant, the and will be highlighted, but the TALEN and plant do not.
- [Add support for Lucene as search syntax #8206](https://github.com/JabRef/jabref/pull/8206)
  - reg ex button in search bar should be removed. all regex searches should be handled by lucene based search.
  - search rule describer for Lucene has to be created. Currently only a dummy.
  - think of removing the search grammar completely ????
- 

##### MERGED
- [Feature/enable lucene query parsing #6799](https://github.com/JabRef/jabref/pull/6799)
- [Feature/add ui for query parsing #6805](https://github.com/JabRef/jabref/pull/6805)
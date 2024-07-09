---
parent: Code Howtos
---
# Frequently Asked Questions (FAQ)

## Resolving common development errors

Following is a list of common errors encountered by developers which lead to failing tests, with their common solutions:

* `org.jabref.architecture.MainArchitectureTest` `restrictStandardStreams` <span style="color:red">FAILED</span>
  * <span style="color:green">Fix</span> : Check if you've used ```System.out.println(...)``` (the standard output stream) to log anything into the console. This is an architectural violation, as you should use the Logger instead for logging. More details on how to log can be found [here](https://devdocs.jabref.org/code-howtos/logging.html).

* `org.jabref.architecture.MainArchitectureTest` `doNotUseLogicInModel` <span style="color:red">FAILED</span>
  * <span style="color:green">Fix</span> : One common case when this test fails is when you put any class purely containing business logic at some level inside the ```model``` directory (```org/jabref/model/```). To fix this, shift the class to a subdirectory within the ```logic``` directory (```org/jabref/logic/```).

* `org.jabref.logic.l10n.LocalizationConsistencyTest` `findMissingLocalizationKeys` <span style="color:red">FAILED</span>
  * <span style="color:green">Fix</span> : You have probably used Strings that are visible on the UI (to the user) but not wrapped them using ```Localization.lang(...)``` and added them to the [localization properties file](https://github.com/JabRef/jabref/blob/main/src/main/resources/l10n/JabRef_en.properties).
      Read more about the background and format of localization in JabRef [here](https://devdocs.jabref.org/code-howtos/localization.html).

<!-- markdownlint-disable-file MD033 -->

# Code Quality

We monitor the general source code quality at three places:

* [codacy](https://www.codacy.com/) is a hosted service to monitor code quality. It thereby combines the results of available open source code quality checkers such as [Checkstyle](https://checkstyle.sourceforge.io/) or [PMD](https://pmd.github.io/). The code quality analysis for JabRef is available at [https://app.codacy.com/gh/JabRef/jabref/dashboard](https://app.codacy.com/gh/JabRef/jabref/dashboard), especially the [list of open issues](https://app.codacy.com/gh/JabRef/jabref/issues/index). In case a rule feels wrong, it is most likely a PMD rule. The JabRef team can change the configuration at [https://app.codacy.com/p/306789/patterns/list?engine=9ed24812-b6ee-4a58-9004-0ed183c45b8f](https://app.codacy.com/p/306789/patterns/list?engine=9ed24812-b6ee-4a58-9004-0ed183c45b8f).
* [codecov](https://codecov.io/) is a solution to check code coverage of test cases. The code coverage metrics for JabRef are available at [https://codecov.io/github/JabRef/jabref](https://codecov.io/github/JabRef/jabref).
* [Teamscale](https://www.cqse.eu/de/produkte/teamscale/landing/) is a popular German product analyzing code quality. The analysis results are available at [https://demo.teamscale.com/findings.html\#/jabref/?](https://demo.teamscale.com/findings.html#/jabref/?).

We strongly recommend to read following two books on code quality:

* [Java by Comparison](http://java.by-comparison.com) is a book by three JabRef developers which focuses on code improvements close to single statements. It is fast to read and one gains much information from each recommendation discussed in the book.
* [Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/) is the standard book for advanced Java programming. Did you know that `enum` is the [recommended way to enforce a singleton instance of a class](https://learning.oreilly.com/library/view/effective-java-3rd/9780134686097/ch2.xhtml#lev3)? Did you know that one should [refer to objects by their interfaces](https://learning.oreilly.com/library/view/effective-java-3rd/9780134686097/ch9.xhtml#lev64)?

The principles we follow to ensure high code quality in JabRef is stated at our [Development Strategy](../getting-into-the-code/development-strategy.md).


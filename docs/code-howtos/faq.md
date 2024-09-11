---
parent: Code Howtos
---
# Frequently Asked Questions (FAQ)

Following is a list of common errors encountered by developers which lead to failing tests, with their common solutions:

## Failing tests

### Failing <b>Checkstyle</b> tests

JabRef follows a pre-defined style of code for uniformity and maintainability that must be adhered to during development. To set up warnings and auto-fixes conforming to these style rules in your IDE, follow [Step 3](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/intellij-13-code-style.html) of the process to set up a local workspace in the documentation. Ideally, follow all the [set up rules](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/) in the documentation end-to-end to avoid typical set-up errors.<br> <b>Note</b>: The steps provided in the documentation are for IntelliJ, which is the preferred IDE for Java development. The `checkstyle.xml` is also available for VSCode, in the same directory as mentioned in the steps.

### Failing <b>OpenRewrite</b> tests

Execute the Gradle task `rewriteRun` from the `rewrite` group of the Gradle Tool window in IntelliJ to apply the automated refactoring and pass the test:<br>
![Executing Gradle task rewriteRun](../images/rewriteRun.png)

Background: [OpenRewrite](https://docs.openrewrite.org/) is an automated refactoring ecosystem for source code.

### `org.jabref.logic.l10n.LocalizationConsistencyTest findMissingLocalizationKeys` <span style="color:red">FAILED</span>

You have probably used Strings that are visible on the UI (to the user) but not wrapped them using `Localization.lang(...)` and added them to the [localization properties file](https://github.com/JabRef/jabref/blob/main/src/main/resources/l10n/JabRef_en.properties).

Read more about the background and format of localization in JabRef [here](https://devdocs.jabref.org/code-howtos/localization.html).

### `org.jabref.logic.l10n.LocalizationConsistencyTest findObsoleteLocalizationKeys` <span style="color:red">FAILED</span>

Navigate to the unused key-value pairs in the file and remove them.
You can always click on the details of the failing test to pinpoint which keys are unused.

Background: There are localization keys in the [localization properties file](https://github.com/JabRef/jabref/blob/main/src/main/resources/l10n/JabRef_en.properties) that are not used in the code, probably due to the removal of existing code.
Read more about the background and format of localization in JabRef [here](https://devdocs.jabref.org/code-howtos/localization.html).

### `org.jabref.logic.citationstyle.CitationStyle discoverCitationStyles` <span style="color:red">ERROR: Could not find any citation style. Tried with /ieee.csl.</span>

Check the directory `src/main/resources/csl-styles`.
If it is missing or empty, run `git submodule update`.
Now, check inside if `ieee.csl` exists.
If it does not, run `git reset --hard` **inside that directory**.

### `java.lang.IllegalArgumentException`: Unable to load locale en-US <span style="color:red">ERROR: Could not generate BibEntry citation. The CSL engine could not create a preview for your item.</span>

Check the directory `src/main/resources/csl-locales`.
If it is missing or empty, run `git submodule update`.
If still not fixed, run `git reset --hard` **inside that directory**.

### `org.jabref.architecture.MainArchitectureTest restrictStandardStreams` <span style="color:red">FAILED</span>

Check if you've used `System.out.println(...)` (the standard output stream) to log anything into the console.
This is an architectural violation, as you should use the Logger instead for logging.
More details on how to log can be found [here](https://devdocs.jabref.org/code-howtos/logging.html).

### `org.jabref.architecture.MainArchitectureTest doNotUseLogicInModel` <span style="color:red">FAILED</span>

One common case when this test fails is when you put any class purely containing business logic inside the `model` package (i.e., inside the directory `org/jabref/model/`).
To fix this, shift the class to a sub-package within the `logic` package (i.e., the directory`org/jabref/logic/`).
An efficient way to do this is to use IntelliJ's built-in refactoring capabilities - right-click on the file, go to "Refactor" and use "Move Class".
The import statement for all the classes using this class will be automatically adjusted according to the new location.<br>
![Moving a file using refactor](../images/refactor-moving.png)<br>

More information on the architecture can be found at [../getting-into-the-code/high-level-documentation.md](High-level documentation).

## Gradle outpus

### `ANTLR Tool version 4.12.0 used for code generation does not match the current runtime version 4.13.1`

Execute the Gradle task `clean` from the `build` group of the Gradle Tool Window in IntelliJ:<br>
![Executing Gradle task clean](../images/clean.png)<br>

### `BstVMVisitor.java:157: error: package BstParser does not exist`

Execute gradle task `clean` from the `build` group of the Gradle Tool Window in IntelliJ.

<!-- markdownlint-disable-file MD033 -->

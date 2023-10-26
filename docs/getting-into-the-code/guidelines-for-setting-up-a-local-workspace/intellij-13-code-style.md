---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 13
---

# Step 3: Using JabRef's code style

Contributions to JabRef's source code need to have a code formatting that is consistent with existing source code.
For that purpose, JabRef relies on both the [Prettier Java](https://github.com/jhipster/prettier-java#prettier-java) and [checkstyle](https://checkstyle.sourceforge.io/).
On each push, the [GitHub Prettier Action](https://github.com/marketplace/actions/prettier-action#github-prettier-action) runs and reformats the code.
JabRef's IntelliJ configuration ensures a close-enough configuration.
In case you use IntelliJ ultimate, you can also configure prettier to do the formatting.

## Checkstyle

### Install checkstyle plugin

Install the [Checkstyle-IDEA plugin](http://plugins.jetbrains.com/plugin/1065?pr=idea), it can be found via the plug-in repository:
Navigate to **File > Settings... > Plugins"**.
On the top, click on "Marketplace".
Then, search for "Checkstyle".
Click on "Install" choose "Checkstyle-IDEA".

{% figure caption:"Install Checkstyle" %}
![Install Checkstyle](guidelines-intellij-install-checkstyle.png)
{% endfigure %}

After clicking, IntelliJ asks for confirmation:

{% figure caption:"Third Party Plugin Privacy Notice" %}
![Third Party Plugin Privacy Notice](guidelines-intellij-checkstyle-confirmation.png)
{% endfigure %}

If you agree, click on "Agree" and you can continue.

Afterwards, use the "Restart IDE" button to restart IntelliJ.

{% figure caption:"IntelliJ restart IDE" %}
![IntelliJ restart IDE](guidelines-intellij-checkstyle-restart-ide.png)
{% endfigure %}

Click on "Restart" to finally restart.

Wait for IntelliJ coming up again.

Go to **File > Settings... > Editor > Code Style**

Click on the settings wheel (next to the scheme chooser),
then click "Import Scheme >",
then click "IntelliJ IDEA code style XML"

{% figure caption:"Location of “Import Scheme > IntelliJ IDEA code style XML”" %}
![Location of IntelliJ IDEA code style XML](guidelines-intellij-codestyle-import.png)
{% endfigure %}

You have to browse for the directory `config` in JabRef's code.
There is an `IntelliJ Code Style.xml`.

{% figure caption:"Browsing for `config/IntelliJ Code Style.xml`" %}
![Browsing for config/IntelliJ Code Style.xml](guidelines-intellij-codestyle-import-select-xml-file.png)
{% endfigure %}

Click "OK".

At following dialog is "Import Scheme".
Click there "OK", too.

{% figure caption:"Import to JabRef" %}
![Import to JabRef](guidelines-intellij-codestyle-import-as-jabref.png)
{% endfigure %}

Click on "Apply" to store the preferences.

### Put JabRef's checkstyle configuration in place

Now, put the checkstyle configuration file is in place:

Go to **File > Settings... > Tools > Checkstyle > Configuration File**

Trigger the import dialog of a CheckStyle style by clicking the \[+] button:

{% figure caption:"Trigger the rule import dialog" %}
![Trigger the rule import dialog](guidelines-intellij-checkstyle-start-import.png)
{% endfigure %}

Then:

* Put "JabRef" as description.
* Browse for `config/checkstyle/checkstyle.xml`
* Tick "Store relative to project location"
* Click "Next"

{% figure caption:"Filled Rule Import Dialog" %}
![Filled Rule Import Dialog](guidelines-intellij-checkstyle-import-file.png)
{% endfigure %}

Click on "Finish"

Activate the CheckStyle configuration file by ticking it in the list

{% figure caption:"JabRef's checkstyle config is activated" %}
![JabRef's checkstyle config is activated](guidelines-intellij-checkstyle-jabref-active.png)
{% endfigure %}

Ensure that the [latest CheckStyle version](https://checkstyle.org/releasenotes.html) is selected (10.3.4 or higher).
Also, set the "Scan Scope" to "Only Java sources (including tests)".

{% figure caption:"Checkstyle is the highest version - and tests are also scanned" %}
![Checkstyle is the highest version - and tests are also scanned](guidelines-intellij-checkstyle-final-settings.png)
{% endfigure %}

Save settings by clicking "Apply" and then "OK"

In the lower part of IntelliJ's window, click on "Checkstyle".
In "Rules", change to "JabRef".
Then, you can run a check on all modified files.

{% figure caption:"JabRef's style is active - and we are ready to run a check on all modified files" %}
![JabRef's style is active - and we are ready to run a check on all modified files](guidelines-intellij-checkstyle-window.png)
{% endfigure %}

## Enable proper import cleanup

To enable "magic" creation and auto cleanup of imports, go to **File > Settings... > Editor > General > Auto Import**.
There, enable both "Add unambiguous imports on the fly" and "Optimize imports on the fly"
(Source: [JetBrains help](https://www.jetbrains.com/help/idea/creating-and-optimizing-imports.html#automatically-add-import-statements)).

{% figure caption:"Auto import enabled" %}
![Enable auto import](guidelines-intellij-editor-autoimport.png)
{% endfigure %}

Press "OK".

## Disable too advanced code folding

Go to **File > Settings... > Editor > General > Code Folding**.
At "Java", disable "General > File header", "General > Imports", and "Java > One-line methods".

{% figure caption:"Code foldings disabled" %}
![Code foldings disabled](guidelines-settings-intellij-code-foldings.png)
{% endfigure %}

Press "OK".

## Final comments

{: .highlight }
> Now you have configured IntelliJ completely.
> You can run the main application using Gradle and the test cases using IntelliJ.
> The code formatting rules are imported - and the most common styling issue at imports is automatically resolved by IntelliJ.
> Finally, you have Checkstyle running locally so that you can check for styling errors before submitting the pull request.

Got it running? GREAT! You are ready to lurk the code and contribute to JabRef. Please make sure to also read our [contribution guide](https://devdocs.jabref.org/contributing#contribute-code).

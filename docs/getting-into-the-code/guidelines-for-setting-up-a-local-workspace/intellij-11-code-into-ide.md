---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 11
---

# Step 1: Get the code into IntelliJ

## IntelliJ Startup

Start IntelliJ IDEA.

IntelliJ shows the following window:

{% figure caption:"IntelliJ Start Window" %}
![IntelliJ Start Window](guidelines-intellij-start-window.png)
{% endfigure %}

## Open the project

Click on "Open"

Choose `build.gradle.kts` in the root of the `jabref` source folder:

{% figure caption:"Choose `build.gradle.kts` in the “Open Project or File” dialog" %}
![Open File or Project dialog](11-3-choose-build-gradle-kts.png)
{% endfigure %}

After clicking "Open," IntelliJ asks how that file should be opened.
Answer: "Open as Project"

{% figure caption:"Choose “Open as Project” in the Open Project dialog" %}
![Open Project dialog](11-4-guidelines-choose-open-as-project.png)
{% endfigure %}

Then, trust the project:

{% figure caption:"Choose “Trust Project” in the “Trust and Open Project” dialog" %}
![Trust and Open Project dialog](12-05-guidelines-trust-project.png)
{% endfigure %}

## Confirm JDK Downloading

IntelliJ asks for JDK downloading.
Keep the suggested Java version and choose "Eclipse Temurin" as Vendor.
Click "Download".

{% figure caption:"Choose “Eclipse Temurin” in the “Download JDK” dialog" %}
![Choose Eclipse Temurin](12-06-download-jdk-temurin.png)
{% endfigure %}

## Allow JDK to access the internet

Allow also access for both cases and click "Allow access".

{% figure caption:"Trust JDK" %}
![Windows Firewall JDK](12-07-trust-firewall.png)
{% endfigure %}

## Wait for IntelliJ IDEA to import the gradle project

IntelliJ shows "Importing 'jabref' Gradle Project" at the lower right corner.
This will take several minutes.
Wait until this disappears.

{% figure caption:"Importing 'jabref' Gradle Project" %}
![Importing 'jabref' Gradle Project](12-08-importing-project.png)
{% endfigure %}

## IntelliJ IDEA will report low memory

{% figure caption:"Low memory pop up" %}
![alt text](12-09-low-memory.png)
{% endfigure %}

1. Click on "Configure".
2. Set "2500" MB (instead of 1262) and click on "Save and Restart".
3. Wait until IntelliJ is up and running again.

## Ensure that committing via IntelliJ works

Unfortunately, IntelliJ has no support for ignored sub modules [[IDEA-285237](https://youtrack.jetbrains.com/issue/IDEA-285237/ignored-changes-in-submodules-are-still-visible-in-the-commit-window)].
Fortunately, there is a workaround:

Go to **File > Settings... > Version Control > Directory Mappings**.<br>
**Note:** In some MacBooks, `Settings` can be found at the "IntelliJ" button of the app menu instead of at "File".

Currently, it looks as follows:

{% figure caption:"Directory Mappings unmodified" %}
![Directory Mappings including sub modules](12-12-intellij-directory-mappings-unmodified.png)
{% endfigure %}

You need to tell IntelliJ to ignore the submodules `jablib\src\main\abbrv.jabref.org`, `jablib\src\main\resources\csl-locales`, and `jablib\src\main\resources\csl-styles`.
Select all three (holding the <kbd>Ctrl</kbd> key).
Then press the minus button on top.

This will make these directories "Unregistered roots:", which is fine.

{% figure caption:"Directory Mappings having three unregistered roots" %}
![Directory Mappings having three repositories unregistered](12-13-intellij-directory-mappings-unregistered-roots.png)
{% endfigure %}

Click "OK"

<!-- markdownlint-disable-file MD033 -->

---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 11
---

# Step 1: Get the code into IntelliJ

Start IntelliJ.

IntelliJ shows the following window:

{% figure caption:"IntelliJ Start Window" %}
![IntelliJ Start Window](guidelines-intellij-start-window.png)
{% endfigure %}

Click on "Open"

Choose `build.gradle` in the root of the JabRef source folder:

{% figure caption:"Choose `build.gradle` in the “Open Project or File” dialog" %}
![Open File or Project dialog](guidelines-intellij-choose-build-gradle.png)
{% endfigure %}

After clicking "Open," IntelliJ asks how that file should be opened.
Answer: "Open as Project"

{% figure caption:"Choose “Open as Project” in the Open Project dialog" %}
![Open Project dialog](guidelines-choose-open-as-project.png)
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

Allow also access in private networks and click "Allow access".

{% figure caption:"Trust JDK" %}
![Windows Firewall JDK](12-07-trust-firewall.png)
{% endfigure %}

## Wait for IntelliJ to import the gradle project

IntelliJ shows "Importing 'JabRef' Gradle Project" at the lower right corner.
Wait until this disappears.

{% figure caption:"Importing 'JabRef' Gradle Project" %}
![Importing 'JabRef' Gradle Project](12-08-importing-project.png)
{% endfigure %}

## IntelliJ will crash

IntelliJ will close.

Restart IntelliJ.

```text
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (malloc) failed to allocate 1048576 bytes. Error detail: AllocateHeap
```

Click on "Report and ClearAll".

1. Click on the burger menu
2. Go to "Help" and then "Change Memory Settings"
3. Set "2000" MB (instead of 750) and click on "Save and Restart"

## Ensure that committing via IntelliJ works

IntelliJ offers committing using the UI.
Press <kbd>Alt</kbd>+<kbd>0</kbd> to open the commit dialog.
If this works, everything is fine.

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
![Directory Mappings having three repositories unregsitered](12-13-intellij-directory-mappings-unregistered-roots.png)
{% endfigure %}

Click "OK"

## Ensure that committing with other tools work

Open a "git bash".
On Windows, navigate to `C:\git-repositories\JabRef`.
Open the context menu of the file explorer (using the right mouse button), choose "Open Git Bash here".

Execute following command:

```shell
git update-index --assume-unchanged jablib/src/main/abbrv.jabref.org jablib/src/main/resources/csl-styles jablib/src/main/resources/csl-locales
```

{: .tip }
If you do not see the context menu, re-install git following the steps given at [StackOverflow](https://stackoverflow.com/a/50667280/873282).

<!-- markdownlint-disable-file MD033 -->

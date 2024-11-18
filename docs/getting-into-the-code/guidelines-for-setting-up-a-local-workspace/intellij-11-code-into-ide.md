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

Choose `build.gradle` in the root of the jabref source folder:

{% figure caption:"Choose `build.gradle` in the “Open Project or File” dialog" %}
![Open File or Project dialog](guidelines-intellij-choose-build-gradle.png)
{% endfigure %}

After pressing "OK", IntelliJ asks how that file should be opened.
Answer: "Open as Project"

{% figure caption:"Choose “Open as Project” in the Open Project dialog" %}
![Open Project dialog](guidelines-choose-open-as-project.png)
{% endfigure %}

Then, trust the project:

{% figure caption:"Choose “Trust Project” in the “Trust and Open Project” dialog" %}
![Trust and Open Project dialog](guidelines-trust-project.png)
{% endfigure %}

## Ensure that committing via IntelliJ works

IntelliJ offers committing using the UI.
Press <kbd>Alt</kbd>+<kbd>0</kbd> to open the commit dialog.

Unfortunately, IntelliJ has no support for ignored sub modules [[IDEA-285237](https://youtrack.jetbrains.com/issue/IDEA-285237/ignored-changes-in-submodules-are-still-visible-in-the-commit-window)].
Fortunately, there is a workaround:

Go to **File > Settings... > Version Control > Directory Mappings**.<br>
**Note:** In some MacBooks, `Settings` can be found at the "IntelliJ" button of the app menu instead of at "File".

Currently, it looks as follows:

{% figure caption:"Directory Mappings unmodified" %}
![Directory Mappings including sub modules](intellij-directory-mappings-unmodified.png)
{% endfigure %}

You need to tell IntelliJ to ignore the submodules `buildres\abbrv.jabref.org`, `src\main\resources\csl-locales`, and `src\main\resources\csl-styles`.
Select all three (holding the <kbd>Ctrl</kbd> key).
Then press the red minus button on top.

This will make these directories "Unregistered roots:", which is fine.

{% figure caption:"Directory Mappings having three unregistered roots" %}
![Directory Mappings having three repositories unregsitered](intellij-directory-mappings-unregistered-roots.png)
{% endfigure %}

## Ensure that committing with other tools work

Open a "git bash".
On Windows, navigate to `C:\git-repositories\JabRef`.
Open the context menu of the file explorer (using the right mouse button), choose "Open Git Bash here".

Execute following command:

```shell
git update-index --assume-unchanged buildres/abbrv.jabref.org src/main/resources/csl-styles src/main/resources/csl-locales
```

{: .tip }
If you do not see the context menu, re-install git following the steps given at [StackOverflow](https://stackoverflow.com/a/50667280/873282).

<!-- markdownlint-disable-file MD033 -->

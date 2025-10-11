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
![IntelliJ Start Window](11-01-intellij-start-window.png)
{% endfigure %}

## Open the project

Click on "Open"

Choose `build.gradle.kts` in the root of the `jabref` source folder:

{% figure caption:"Choose `build.gradle.kts` in the “Open Project or File” dialog" %}
![Open File or Project dialog](11-02-choose-build-gradle-kts.png)
{% endfigure %}

After clicking "Open," IntelliJ asks how that file should be opened.
Answer: "Open as Project"

{% figure caption:"Choose “Open as Project” in the Open Project dialog" %}
![Open Project dialog](11-03-guidelines-choose-open-as-project.png)
{% endfigure %}

Then, trust the project:

{% figure caption:"Choose “Trust Project” in the “Trust and Open Project” dialog" %}
![Trust and Open Project dialog](11-04-guidelines-trust-project.png)
{% endfigure %}

## Confirm JDK Downloading

IntelliJ asks for JDK downloading.
Keep the suggested Java version and choose "Eclipse Temurin" as Vendor.
Click "Download".

{% figure caption:"Choose “Eclipse Temurin” in the “Download JDK” dialog" %}
![Choose Eclipse Temurin](11-05-download-jdk-temurin.png)
{% endfigure %}

## Allow JDK to access the internet

Allow also access for both cases and click "Allow access".

{% figure caption:"Trust JDK" %}
![Windows Firewall JDK](11-06-trust-firewall.png)
{% endfigure %}

## Wait for IntelliJ IDEA to import the gradle project

IntelliJ shows "Importing 'jabref' Gradle Project" at the lower right corner.
This will take several minutes.
Wait until this disappears.

{% figure caption:"Importing 'jabref' Gradle Project" %}
![Importing 'jabref' Gradle Project](11-07-importing-project.png)
{% endfigure %}

## Respond to notifications

You can disregard notifications

* offering to reopen the project in a container
* announcing the project JDK
* suggesting that you install the plugin WireMock

## IntelliJ IDEA may report low memory

{% figure caption:"Low memory pop up" %}
![Low memory pop up](11-08-low-memory.png)
{% endfigure %}

1. Click on "Configure".
2. Set "2500" MB (instead of 1262) and click on "Save and Restart".
3. Wait until IntelliJ is up and running again.

<!-- markdownlint-disable-file MD033 -->

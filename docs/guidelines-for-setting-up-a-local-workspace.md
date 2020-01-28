# Setup a local workspace

This guide explains how to set up your environment for development of JabRef. It includes information about prerequisites, configuring your IDE, and running JabRef locally to verify your setup.

For a complete step-by-step guide (using IntellJ as the IDE), have a look at the following video instructions:

<p align="center">
  <a href="http://www.youtube.com/watch?v=FeQpygT0314"><img src="http://img.youtube.com/vi/FeQpygT0314/0.jpg" /></a>
</p>

## Prerequisites

### Java Development Kit 13

A working Java 13 installation is required. In the command line (terminal in Linux, cmd in Windows) run `javac -version` and make sure that the reported version is Java 13 (e.g `javac 13.0.1`). If `javac` is not found or a wrong version is reported, check your PATH environment variable, your JAVA_HOME environment variable or install the most recent JDK.

### git

It is strongly recommend that you have git installed: [official installation instructions](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git).

* In Debian-based distros: `sudo apt-get install git`
* In Windows: [Download the installer](http://git-scm.com/download/win) and install it. For more advanced tooling, you may use [Git Extensions](http://gitextensions.github.io/) or [SourceTree](https://www.sourcetreeapp.com/). - See also our [tool recommendations](tools.md) for installation hints including [chocolatey](https://chocolatey.org/).

If you do not yet have a GitHub account, please [create one](https://github.com/join).

### IDE

We suggest [IntelliJ](https://www.jetbrains.com/idea/) or [Eclipse](https://eclipse.org/) (`2019-06` or newer).

Under Ubuntu Linux, you can follow the [documentation from the Ubuntu Community](https://help.ubuntu.com/community/EclipseIDE#Download_Eclipse) or the [step-by-step guideline from Krizna](www.krizna.com/ubuntu/install-eclipse-in-ubuntu-12-04/) to install Eclipse. Under Windows, download it from [www.eclipse.org](http://www.eclipse.org/downloads/) and run the installer.

## Get the code

### Fork JabRef into your GitHub account

1. Log into your GitHub account
2. Go to <https://github.com/JabRef/jabref>
3. Create a fork by clicking at fork button on the right top corner
4. A fork repository will be created under your account (https://github.com/YOUR_USERNAME/jabref)

### Clone your forked repository on your local machine

* In a command line, navigate to the folder where you want to place the source code (parent folder of `jabref/`). To prevent issues along the way, it is strongly recommend to choose a path that does not contain any special (non-ASCII or whitespace) characters.
* Run `git clone --depth=10 https://github.com/YOUR_USERNAME/jabref.git`. The `--depth--10` is used to limit the download to ~20 MB instead of downloading the complete history (~800 MB). If you want to dig in our commit history, feel free to download everything.
* Go to the newly created jabref folder: `cd jabref`
* Generate additional source code: `./gradlew assemble`
* Start JabRef: `./gradlew run`

## Configure your IDE

### IntelliJ

1. Open `jabref/build.gradle` as a project
2. Enable annotation processors:
   * File -> Settings -> Compiler -> Annotation processors -> Check "Enable annotation processing"
3. Configure module settings: Right click on project -> Open Module Settings
   * Ensure that the projects SDK is Java 13: Project Settings -> Project -> Project SDK: Choose Java 13
   * Ensure that standard SDK is Java 13: Platform Settings -> SDK -> Choose Java 13
4. Specify additional compiler arguments: File -> Settings -> Build, Execution, Deployment -> Compiler -> Java Compiler -> Under "Override compiler parameters per-module" add the following compiler arguments for the `JabRef.main` module:
   ```text
   --patch-module test=fastparse_2.12-1.0.0.jar
   --patch-module test2=fastparse-utils_2.12-1.0.0.jar
   --patch-module test3=sourcecode_2.12-0.1.4.jar
   --add-exports javafx.controls/com.sun.javafx.scene.control=org.jabref
   --add-exports org.controlsfx.controls/impl.org.controlsfx.skin=org.jabref
   --add-exports javafx.graphics/com.sun.javafx.scene=org.controlsfx.controls
   --add-exports javafx.graphics/com.sun.javafx.scene.traversal=org.controlsfx.controls
   --add-exports javafx.graphics/com.sun.javafx.css=org.controlsfx.controls
   --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=org.controlsfx.controls
   --add-exports javafx.controls/com.sun.javafx.scene.control=org.controlsfx.controls
   --add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=org.controlsfx.controls
   --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls
   --add-exports javafx.base/com.sun.javafx.collections=org.controlsfx.controls
   --add-exports javafx.base/com.sun.javafx.runtime=org.controlsfx.controls
   --add-exports javafx.web/com.sun.webkit=org.controlsfx.controls
   --add-exports javafx.graphics/com.sun.javafx.css=org.controlsfx.controls
   --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=com.jfoenix
   --patch-module org.jabref=build/resources/main
   ```
4. Use IntellJ to build and run (instead of gradle): File -> Settings -> Build, Execution, Deployment ->  Build Tools -> Gradle -> At "Build and run using" and "Run tests using" choose "Intellj IDEA"
4. Ensure that JDK13 is enabled for Gradle: Use IntellJ to build and run (instead of gradle): File -> Settings -> Build, Execution, Deployment ->  Build Tools -> Gradle -> Gradle -> Gradle JVM
5. Use the provided code style:
   1. Install the [CheckStyle-IDEA plugin](http://plugins.jetbrains.com/plugin/1065?pr=idea), it can be found via plug-in repository (File > Settings > Plugins > Marketplace -> Search for "Checkstyle" and choose "CheckStyle-IDEA). Close the settings afterwards and restart IntelliJ.
   2. Go to File > Settings > Editor > Code Style, choose a code style (or create a new one) 
   3. Click on the settings wheel (next to the scheme chooser), then click "Import Scheme" and choose "IntelliJ Code Style xml". Select the IntelliJ configuration file `config/IntelliJ Code Style.xml`. Click OK.
   4. Go to File -> Settings -> Checkstyle and import the CheckStyle configuration file. Activate it.
6. Use the provided run configuration: Run -> Run "JabRef Main"

### Set-up Eclipse

1. Run `./gradlew run` to generate all resources and to check if jabref runs. (This step is only required once)
2. Run `./gradlew eclipse` (This has to be always execute, when there are new upstream changes)
7. Copy the file Log4jPlugins.java from `build\generated\sources\annotationProcessor\java\main\org\jabref\gui\logging\plugins` to `org.jabref.gui.logging.plugins`
8. Create a run/debug configuration for main class `org.jabref.JabRefLauncher` 
9. In the arguments tab enter the same runtime arguments as above for intellij.
10. Optional: Install the [e(fx)clipse plugin](http://www.eclipse.org/efxclipse/index.html) from the eclipse marketplace

## Final comments

Got it running? GREAT! You are ready to lurk the code and contribute to JabRef. Please make sure to also read our [contribution guide](https://github.com/JabRef/jabref/blob/master/CONTRIBUTING.md).

## Common issues

### Java installation

An indication that `JAVA_HOME` is not correctly set or no JDK 13 is installed is following error message:

```text
compileJava FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileJava'.
> java.lang.ExceptionInInitializerError (no error message)
```

Another indication is following output

```text
java.lang.UnsupportedClassVersionError: org/javamodularity/moduleplugin/ModuleSystemPlugin has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0
```

### Problems with generated source files

In rare cases you might encounter problems due to out-dated automatically generated source files. Running `./gradlew clean` deletes these old copies. Do not forget to run at least `./gradlew eclipse` or `./gradlew build` afterwards to regenerate the source files.

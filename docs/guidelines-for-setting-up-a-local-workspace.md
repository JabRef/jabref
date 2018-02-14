## Prerequisites:
* Java Development Kit ([Download JDK from Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html?ssSourceSiteId=otnjp) - or execute `choco install jdk8` when using [chocolatey](https://chocolatey.org/))

An indication that `JAVA_HOME` is not correctly set or no JDK is installed is following error message:

```
compileJava FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileJava'.
> java.lang.ExceptionInInitializerError (no error message)
```



### Ensure you have a Java SDK installed
* In the command line (terminal in Linux, cmd in Windows) run `javac -version`
* If `javac` is not found, check your PATH environment variable, your JAVA_HOME environment variable or install the most recent SDK

### GitHub account
If you do not have a GitHub account, create it at https://github.com.

### Get an IDE in place
We suggest [Eclipse](https://eclipse.org/) or [IntelliJ](https://www.jetbrains.com/idea/).

#### IntelliJ
The community edition should be enough.
If not, a developer key for the JabRef project for the full version is available upon request.

You can find a IntelliJ Codestyle configuration file in the folder `config`

#### Eclipse
Please install [EclEmma](http://eclemma.org/) for code coverage.

In Ubuntu Linux, you can follow one of these guidelines to Install Eclipse:

* [Documentation from Ubuntu Community](https://help.ubuntu.com/community/EclipseIDE#Download_Eclipse)
* [Step-by-step from Krizna](www.krizna.com/ubuntu/install-eclipse-in-ubuntu-12-04/)

In Windows download it from [www.eclipse.org](http://www.eclipse.org/downloads/) and run the installer.

### Get git in place
* In Debian-based distros: `sudo apt-get install git`
* In Windows: Go to http://git-scm.com/download/win download and install it. For more advanced tooling, you may use [Git Extensions](http://gitextensions.github.io/) or [SourceTree](https://www.sourcetreeapp.com/).

## Get the code
### Fork JabRef into your GitHub account
1. Log into your GitHub account
2. Go to https://github.com/JabRef/jabref 
3. Create a fork by clicking at fork button on the right top corner
4. A fork repository will be created under your account (https://github.com/YOUR_USERNAME/jabref)

### Clone your forked repository on your local machine.

* In command line go to a folder you want to place the source code locally (parent folder of `jabref/`)
* Run `git clone --depth=10 https://github.com/YOUR_USERNAME/jabref.git`. The `--depth--10` is used to limit the download to ~20 MB instead of downloading the complete history (~800 MB). If you want to dig in our commit history, feel free to download everything.

### Generating additional source codes and getting dependencies using Gradle:
1. Go to the jabref folder (the repo you just cloned, if you are following this tutorial, just execute `cd jabref`)
2. Execute the following steps from the git-bash:
  - Run `./gradlew assemble` 
  - If you use Eclipse: Additionally run `./gradlew eclipse` 
  - If you use IntelliJ: No further setup is required
3. In rare cases you might encounter problems due to out-dated automatically generated source files. Running `./gradlew clean` deletes these old copies. Do not forget to run at least `./gradlew eclipse` or `./gradlew build` afterwards to regenerate the source files. 
4. `./gradlew tasks` shows many other runnable tasks.  

### Building it into your IDE
* Open Eclipse (or your preferred IDE)
* To Import your jabref project go to menu File --> Import

1. Choose General --> Existing projects in the workspace and "next"
2. For "select root directory", browse until the root folder of your jabref just cloned from your repo (e.g., `/home/user/<YOU>/jabref`)
3. Click on "Finish" and voilá!
4. In Eclipse, right click on the project and choose Refresh workspace and then choose Run as --> Java application (Forget about the existing errors)
5. Choose JabRefMain as the main class to be executed

### Set-up your IDE (IntelliJ)
1. Install the [CheckStyle-IDEA plugin](http://plugins.jetbrains.com/plugin/1065?pr=idea), it can be found via plug-in repository (File > Settings > Plugins > Browse repositories).
2. Go to File > Settings > Editor > Code Style, choose a code style (or create a new one) 
3. Click on the settings wheel (next to the scheme chooser), then Import scheme and choose "CheckStyle Configuration". Select the CheckStyle configuration file `config\checkstyle\checkstyle.xml`. Click OK and restart IntelliJ.
4. Go to File > Settings > Checkstyle and import the above CheckStyle configuration file. Activate it.

Got it running? GREAT! You are ready to lurk the code and contribute to JabRef :books: . In the latter case, please read [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/master/CONTRIBUTING.md).

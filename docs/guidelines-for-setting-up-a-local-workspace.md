## Prerequisites:
* Java Development Kit ([Download Java DevKit from Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html?ssSourceSiteId=otnjp))

### Ensure you have a Java SDK installed
* In the command line (terminal in Linux, cmd in Windows) run `javac -version`
* If `javac` is not found, check your PATH environment variable, your JAVA_HOME environment variable or install the most recent SDK

### GitHub account
If you do not have a GitHub account, create it at https://github.com/.

### Get an IDE in place
I suggest [Eclipse](https://eclipse.org/). Other developers like and use [IntelliJ](https://www.jetbrains.com/idea/).

In Ubuntu Linux, you can follow one of these guidelines:

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
* Go to the jabref folder (the repo you just cloned, if you are following this tutorial, just execute `cd jabref`)
* Run `gradlew generateSource`
* If you use Eclipse: run `gradlew eclipse` (`./gradlew eclipse` in Linux)
* If you use IntelliJ: No extra setup is required

### Building it into your IDE
* Open Eclipse (or your preferred IDE)
* To Import your jabref project go to menu File --> Import

1. Choose General --> Existing projects in the workspace and "next"
2. For "select root directory", browse until the root folder of your jabref just cloned from your repo (e.g., /home/user/<YOU>/jabref)
3. Click on "Finish" and voilá!
4. In Eclipse, right click on the project and choose Run as --> Java application (Forget about the existing errors)
5. Choose JabRefMain as the main class to be executed

Got it running? GREAT! You are ready to lurk the code and contribute to JabRef :books:

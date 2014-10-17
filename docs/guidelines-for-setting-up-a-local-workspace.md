## Prerequisites:
* Java Development Kit ([Download Java DK from Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html?ssSourceSiteId=otnjp))
### Ensure you have java SDK installed in your machine
* in the command line (terminal in linux, cmd in windows) run "javac -version"
* if javac is not found, check your PATH environment variable, your JAVA_HOME environment variable or install the most recent SDK

### Install gradle in your machine
* for debian based linux distros:

1. "sudo apt-get install gradle"
2. test it by running gradle -version

* for windows

1. download from Gradle website http://www.gradle.org/downloads
2. Unzip the Gradle download to the folder to which you would like to install Gradle, eg. “C:\Program Files”. The subdirectory gradle-x.x will be created from the archive, where x.x is the version.
3. Add location of your Gradle “bin” folder to your path (e.g. "C:\Program Files\gradle-x.x\bin"). Open the system properties (WinKey + Pause), select the “Advanced” tab, and the “Environment Variables” button, then add “C:\Program Files\gradle-x.x\bin” (or wherever you unzipped Gradle) to the end of your “Path” variable under System Properties. Be sure to omit any quotation marks around the path even if it contains spaces. Also make sure you separated from previous PATH entries with a semicolon “;”.
4. Open a new command prompt (type cmd in Start menu) and run gradle –version to verify that it is correctly installed.

### get git in place. To install it:
* in debian based linux distros
1. "sudo apt-get install git"

### in Windows
1. Go to http://git-scm.com/download/win download and install it

### Github account
* If you do not have github account, create it at https://github.com/

### Get an IDE in place.
* I suggest eclipse. 
* In ubuntu linux, you can follow one of these guidelines:

1. [Documentation from Ubuntu Community](https://help.ubuntu.com/community/EclipseIDE#Download_Eclipse)
1. [Step-by-step from Krizna](www.krizna.com/ubuntu/install-eclipse-in-ubuntu-12-04/)

* In windows download it from [www.eclipse.org](http://www.eclipse.org/downloads/) and run the install

## Getting the code
### Fork jabref into your github account
1. Log into your github account
2. Go to https://github.com/JabRef/jabref 
3. Create a fork by clicking at fork button on the right top corner
4. a fork repository will be created under your account (https://github.com/YOUR_USERNAME/jabref)

### Clone your forked repo into your local machine.
* In command line go to a folder you want to place the source code locally (parent folder of jabref/)
* run "git clone https://github.com/YOUR_USERNAME/jabref.git jabref"

### Generating additional source codes and getting dependencies using Gradle:
* Go to the jabref folder (the repo you just cloned, if you are following this tutorial, just execute "cd jabref")
* run "gradlew generateSource" ("./gradlew generateSource" in linux) to generate additional source and download libraries (it will take some minutes)
* run "gradlew eclipse" (./"gradlew eclipse" in linux) if you are using eclipse IDE (alternatively you can use inteliJIdea running "gradlew idea")

### building it into your IDE
* open eclipse (or your prefered IDE)
* to import your jabref project go to menu File--> Import

1. choose General --> Existing projects in the workspace and "next"
2.for "select root directory" Browse until the root folder of your jabref just cloned from your repo (e.g. /home/user/<YOU>/jabref
3. click on "Finish" and voilá!
4. In eclipse, right click on the project and choose Run as ---> Java application (Forget about the existing errors)
5. Choose JabRefMain as the main class to be executed

Got it running? GREAT! You are ready to lurk the code and contribute to JabRef
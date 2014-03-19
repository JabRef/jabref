@echo off
:: Simple Windows batch script to compile Java-classes that are part 
:: of the jabref-extensions.
:: We don't use ant here, 'cause anyone just with a out-of-the-box
:: Java-installation should be able to compile
:: extension files.
:: For UNIX there is a corresponding sh-script

:: Make sure, jabref and java can be found
if exist ..\lib\jabref.jar set JABREF_JAR=..\lib\jabref.jar
if exist ..\lib\JabRef.jar set JABREF_JAR=..\lib\JabRef.jar
if exist ..\lib\jabref-@version@.jar set JABREF_JAR=..\lib\jabref-@version@.jar
if exist ..\lib\JabRef-@version@.jar set JABREF_JAR=..\lib\JabRef-@version@.jar
if exist ..\jabref.jar set JABREF_JAR=..\jabref.jar
if exist ..\JabRef.jar set JABREF_JAR=..\JabRef.jar
if exist ..\jabref-@version@.jar set JABREF_JAR=..\jabref-@version@.jar
if exist ..\JabRef-@version@.jar set JABREF_JAR=..\JabRef-@version@.jar
if exist lib\jabref.jar set JABREF_JAR=lib\jabref.jar
if exist lib\JabRef.jar set JABREF_JAR=lib\JabRef.jar
if exist lib\jabref-@version@.jar set JABREF_JAR=lib\jabref-@version@.jar
if exist lib\JabRef-@version@.jar set JABREF_JAR=lib\JabRef-@version@.jar
if exist jabref.jar set JABREF_JAR=jabref.jar
if exist JabRef.jar set JABREF_JAR=JabRef.jar
if exist jabref-@version@.jar set JABREF_JAR=jabref-@version@.jar
if exist JabRef-@version@.jar set JABREF_JAR=JabRef-@version@.jar
if exist %JABREF_HOME%\jabref.jar set JABREF_JAR=%JABREF_HOME%\jabref.jar
if exist %JABREF_HOME%\JabRef.jar set JABREF_JAR=%JABREF_HOME%\JabRef.jar
if exist %JABREF_HOME%\jabref-@version@.jar set JABREF_JAR=%JABREF_HOME%\jabref-@version@.jar
if exist %JABREF_HOME%\JabRef-@version@.jar set JABREF_JAR=%JABREF_HOME%\jabref-@version@.jar

if NOT exist "%JABREF_JAR%"=="" goto nojabref

if "%JAVA_HOME%"=="" goto nojava

:: Compile Java-extensions
%JAVA_HOME%\bin\javac -classpath %JABREF_JAR% net\sf\jabref\imports\*.java
%JAVA_HOME%\bin\javac -classpath %JABREF_JAR% *.java
%JAVA_HOME%\bin\javac -classpath %JABREF_JAR% net\sf\jabref\export\layout\format\*.java
goto done

:nojava
echo Please set the environment variable JAVA_HOME to the base path of your Java-installation (e.g. set JAVA_HOME=C:\j2sdk1.4.2_07).
goto done

:nojabref
echo Please set the environment variable JABREF_JAR to the path where your jabref.jar resides (e.g. set JABREF_HOME=C:\Programme\JabRef-2.0.1).
goto done

:done


# Build Instructions

Use the Gradle wrapper (`./gradlew`) from the project root. After any Gradle file change, reload Gradle in your IDE.

---

##  Generate API documentation
```bash
./gradlew javadoc
```
> Note: In the current JabRef version this may fail; that is expected for the exercise.

---

## List libraries (dependencies)
Project modules (examples):
```bash
./gradlew :jabgui:dependencies --configuration runtimeClasspath
./gradlew :jablib:dependencies --configuration runtimeClasspath
```
Inspect a specific dependency:
```bash
./gradlew :jabgui:dependencyInsight --configuration runtimeClasspath --dependency <name-or-group>
```

---

## Check style for unit tests
```bash
./gradlew :jablib:checkstyleTest
./gradlew :jabgui:checkstyleTest
```

---

##  Create a runnable ZIP distribution
Root distribution:
```bash
./gradlew distZip
```
Module distribution (example):
```bash
./gradlew :jabgui:distZip
```
Output:
```
build/distributions/            # or <module>/build/distributions/
```

---

## Generate sources (JabRef specific)
LTWA MVStore generator:
```bash
./gradlew :jablib:generateLtwaListMV
```
ANTLR grammar to Java sources:
```bash
./gradlew :jablib:generateGrammarSource
```

---

##  Discover all available tasks
```bash
./gradlew tasks --all
```

---

##  Add `swenglib` (version 2.0)

###  Add repository
File: `build-logic/src/main/kotlin/org.jabref.gradle.base.repositories.gradle.kts`
```kotlin
repositories {
    // existing repositories...
    maven { url = uri("https://shapemodelling.cs.unibas.ch/repo") }
}
```

###  Add dependency
File: `jabgui/build.gradle.kts`
```kotlin
dependencies {
    implementation("ch.unibas.informatik.sweng:swenglib:2.0")
}
```

###  Add Java module requirement
File: `jabgui/src/main/java/module-info.java`
```java
module org.jabref {
    requires swenglib;
    // other requires/exports...
}
```

###  Verify (optional)
Add to `main` (or any reachable code path) and run:
```java
import ch.unibas.informatik.sweng.HelloSweng;

String greeting = HelloSweng.greeting();
System.out.println(greeting);
```
CTRL + Left-Click on `HelloSweng` should open library sources.

---

##  Add `opencsv` from Maven Central
File: `jabgui/build.gradle.kts`
```kotlin
dependencies {
    implementation("com.opencsv:opencsv:5.9")
}
```

---

##  Add Gradle License Report plugin
File: **root** `build.gradle.kts`
```kotlin
plugins {
    id("com.github.jk1.dependency-license-report") version "2.8"
}

// This plugin generates a license report for all dependencies,
// useful for compliance and third-party license tracking.
licenseReport {
    outputDir = "$buildDir/reports/license"
}
```
Run and open the report:
```bash
./gradlew generateLicenseReport
open build/reports/license
```

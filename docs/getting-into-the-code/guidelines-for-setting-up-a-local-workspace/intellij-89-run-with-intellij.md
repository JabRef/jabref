---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 89
---

# Advanced: Build and run using IntelliJ IDEA

In "Step 2: Setup the build system: JDK and Gradle", IntelliJ was configured to use Gradle as tool for launching JabRef.
It is also possible to use IntelliJ's internal build and run system to launch JabRef.
Due to [IDEA-119280](https://youtrack.jetbrains.com/issue/IDEA-119280), it is a bit more work.

1. Navigate to **File > Settings... > Build, Execution, Deployment > Build Tools > Gradle**.
2. Change the setting "Build an run using:" to "IntelliJ IDEA".
3. **Build > Build Project**
4. Open the project view (<kbd>Alt</kbd>+<kbd>1</kbd>)
5. Copy all build resources to the folder of the build classes
   1. Navigate to the folder `out/production/resources`
   2. Select all folders below (`bst`, `csl-locales`, ...)
   3. Press <kbd>Ctrl</kbd>+<kbd>C</kbd> to mark them for copying
   4. Select the folder `classes`
   5. Press <kbd>Ctrl</kbd>+<kbd>V</kbd> to start the copy process
6. Locate the class `Launcher` (e.g., by <kbd>ctrl</kbd>+<kbd>N</kbd> and then typing `Launcher`)
7. Click on the green play button next to the `main` method to create a Launch configuration. IntelliJ will fail in launching.
8. On the top right of the IntelliJ window, next to the newly created launch configuration, click on the drop down
9. Click on "Edit Configurations..."
10. On the right, click on "Modify options"
11. Ensure that "Use classpath of module" is checked
12. Select "Add VM options"
13. In the newly appearing field for VM options, insert:

   ```text
   --add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref
   --add-opens=org.controlsfx.controls/org.controlsfx.control.textfield=org.jabref
   --add-exports=org.controlsfx.controls/impl.org.controlsfx.skin=org.jabref
   --add-reads org.jabref=org.fxmisc.flowless
   --add-reads org.jabref=org.apache.commons.csv
   ```

14. Click "Apply"
15. Click "Run"

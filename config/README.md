# IntelliJ IDEA Code Style Configuration

IntelliJ IDEA comes with a powerful code formatter that helps you to keep the formatting consistent with the style JabRef uses.
Style-checks are done for each pull request and installing this cody style configuration helps you to ensure that this test passes. To install it, you need to do the following steps:

1. Goto *Preferences* or press <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd> (<kbd>Cmd</kbd> + <kbd>,</kbd> on Mac OS X)
2. Go to "Editor > Code Style"
3. Click the gear (right of "Scheme: ...")
4. Click "Import Scheme >"
5. Choose `IntelliJ IDEA code style XML`
6. Select the file `config\IntelliJ Code Style.xml`
7. Press "OK"
8. Press "OK"
9. Press "Close"
10. Press "OK"

* Please let `.editorconfig` override the settings of IntelliJ


# Eclipse:

The Eclipse code formatter style is stored in the `eclipse.gradle` file and gets imported automatically.
In case the formatter style needs to be adapted, configure it and export in in eclipse.

1. Right click on the eclipse project "JabRef"
2. Select "Export > General > Preferences"
3. Select "Java Code Style preferences"
4. Choose output file
5. Compare the formatter settings in the epf file with the ones in the eclipse.gradle file (`org.eclipse.jdt.core.formatter.`)
6. Replace the Eclipse Code Style.epf with the exported epf file

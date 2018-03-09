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

In case the Eclipse style needs to be adjusted for the eclipse.gradle file:

1. Export the style file xml
2. Open the Style file in an editor which supports RegEx search and replacement
3. Enter for search regex: `<setting id="(org\.eclipse\.jdt\.core\.formatter\.[a-zA-Z_0-9\.]*)"\s*value="([a-zA-Z@0-9\s_:]*)"\/>`
4. Enter for replacement regex: `\1=\2`
5. Remove the lines with jdt.compiler from hand
6. Escape `@formatter:on` and `@formatter:off` with`@formatter\\:on` and `@formatter\\:off`
7. Replace existing lines in eclipse.gradle starting with `org.eclipse.jdt.core.formatter`

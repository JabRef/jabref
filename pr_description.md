Closes #13275

Refactored jabkit CLI to improve output formatting and code organization. Changes include clearing unused imports, skipping redundant format footers for check-consistency command, reorganizing method order, adding documentation, and improving usage footer generation with export format support.

### Steps to test

1. Build the project: `./gradlew build`
2. Run jabkit commands to verify output formatting:
   - `./gradlew :jabkit-cli:run --args="check-consistency"`
   - `./gradlew :jabkit-cli:run --args="export --help"`
3. Verify that redundant footers are no longer displayed
4. Check that usage information is properly formatted

### Mandatory checks

- [x] I own the copyright of the code submitted and I license it under the [MIT license](https://github.com/JabRef/jabref/blob/main/LICENSE)
- [x] I manually tested my changes in running JabRef (always required)
- [/] I added JUnit tests for changes (if applicable)
- [/] I added screenshots in the PR description (if change is visible to the user)
- [/] I described the change in `CHANGELOG.md` in a way that is understandable for the average user (if change is visible to the user)
- [/] I checked the [user documentation](https://docs.jabref.org/): Is the information available and up to date? If not, I created an issue at <https://github.com/JabRef/user-documentation/issues> or, even better, I submitted a pull request updating file(s) in <https://github.com/JabRef/user-documentation/tree/main/en>.
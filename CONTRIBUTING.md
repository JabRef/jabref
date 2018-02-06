## Understanding the basics
We welcome contributions to JabRef and encourage to create a fork, clone, **create a new branch** (such as `fix-for-issue-121`), **work on the new branch — not master**, and create a pull request.
Be sure to create a **separate branch** for each improvement you implement.
Take a look at GitHub's excellent [help documentation] for a detailed explanation and the explanation of [Feature Branch Workflow](https://de.atlassian.com/git/tutorials/comparing-workflows#feature-branch-workflow) for the idea behind this kind of development.

We also have [code howtos](https://github.com/JabRef/jabref/wiki/Code-Howtos) and [guidelines for setting up a local workspace](https://github.com/JabRef/jabref/wiki/Guidelines-for-setting-up-a-local-workspace).

In case you have any question, do not hesitate to write one of our [JabRef developers](https://github.com/orgs/JabRef/teams/developers) an email.
We should also be online at [gitter](https://gitter.im/JabRef/jabref).


## Formal requirements for a pull request
The main goal of the formal requirements is to provide credit to you and to be able to understand the patch.

### Add your change to CHANGELOG.md
You should edit the [CHANGELOG.md](CHANGELOG.md) located in the root directory of the JabRef source.
Add a line with your changes in the appropriate section.

If you did internal refactorings or improvements not visible to the user (e.g., UI, .bib file), then you don't need to put an entry there.


#### Key format
Example: `<kbd>Ctrl</kbd> + <kbd>Enter</kbd>`

In case you add keys to the changelog, please follow these rules:

- `<kbd>` tag for each key
- First letter of key capitalized
- Combined keys separated by `+`
- Spaces before and after separator `+`


### Author credits
You will be given credit in the `AUTHORS` file in the root of the repository and the 'About' pages inside the main application.
We will periodically update the contributors list inside `AUTHORS`.
This is done by an automatic shell script `scripts/generate-authors.sh`.

If you want to add yourself directly with your pull request please run this script.
Please make sure there are no duplicates or alternate spellings of your name listed.
If you need to merge different Git usernames or email addresses you can do so by editing `.mailmap`.
More information on this can be found via `man git-shortlog`.

Please, **do not add yourself at JavaDoc's `@authors`**.
The contribution information is tracked via the version control system.

Your contribution is considered being made under [MIT license](https://tldrlegal.com/license/mit-license).


### Write a good commit message
See [good commit message] or [commit guidelines section of Pro Git].


### Test your code
We know that writing test cases causes a lot of time.
Nevertheless, we rely on our test cases to ensure that a bug fix or a feature implementation doesn't break anything.
In case you do not have time to add a test case, we nevertheless ask you to run `gradlew check` to ensure that your change doesn't break anything else.


### When adding a library
Please try to use a version available at jCenter and add it to `build.gradle`.
In any case, describe the library at [external-libraries.txt](external-libraries.txt).
We need that information for our package maintainers (e.g., those of the [debian package](https://tracker.debian.org/pkg/jabref)).
Also add a txt file stating the license in `libraries/`.
It is used at `gradlew processResources` to generate the About.html files.
You can see the result in `build\resources\main\help\en\About.html` or when clicking Help -> About.


### When making an architectural decision
In case you add a library or do major code rewrites, we ask you to document your decision.
Recommended reading: <https://adr.github.io/>.

We simply ask to create a new markdown file in `docs/adr` following the template presented at <https://adr.github.io/madr/>.

In case you want to directly add a comment to a class, simply use following template (based on [sustainable architectural decisions](https://www.infoq.com/articles/sustainable-architectural-design-decisions)):

```
In the context of <use case/user story u>,
facing <concern c>
we decided for <option o>
and neglected <other options>,
to achieve <system qualities/desired consequences>,
accepting <downside / undesired consequences>,
because <additional rationale>.
```


### When adding a new Localization.lang entry
Add new `Localization.lang("KEY")` to Java file.
Tests fail. In the test output a snippet is generated which must be added to the English translation file.

Example:

```
java.lang.AssertionError: DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE
PASTE THESE INTO THE ENGLISH LANGUAGE FILE
[
Opens\ JabRef's\ Twitter\ page=Opens JabRef's Twitter page
]
Expected :[]
Actual   :[Opens\ JabRef's\ Twitter\ page (src\main\java\org\jabref\gui\JabRefFrame.java LANG)]
```

Add snippet to English translation file located at `src/main/resources/l10n/JabRef_en.properties`.
[Crowdin](http://translate.jabref.org/) will automatically pick up the new string and add it to the other translations.

You can also directly run the specific test in your IDE.
The test "LocalizationConsistencyTest" is placed under `src/test/java/net.sf.jabref.logic.l10n/LocalizationConsistencyTest.java`
Find more information in the [JabRef Wiki](https://github.com/JabRef/jabref/wiki/Code-Howtos#using-localization-correctly).


### Create a pull request
Create a pull request on GitHub.
For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).

You can add the prefix `[WIP]` to indicate that the pull request is not yet complete, but you want to discuss something or inform about the current state of affairs.


[commit guidelines section of Pro Git]: http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines
[good commit message]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[help documentation]: https://help.github.com/articles/about-pull-requests/

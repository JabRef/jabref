# Contributing

After reading through this guide, check out some good first issues to contribute to by clicking here: [Good First Issues](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)

In case you are aiming to contribute other improvements, please head over to our general [JabRef contribution page](https://docs.jabref.org/faqcontributing).

In case you are an instructor and want to use **JabRef as a software engineering example**, please head to [https://devdocs.jabref.org/teaching](https://devdocs.jabref.org/teaching).

## Contribute code

### Understanding the basics of code contributions

We welcome contributions to JabRef and encourage you to follow the GitHub workflow specified below. If you are not familiar with this type of workflow, take a look at GitHub's excellent overview on the [GitHub flow](https://guides.github.com/introduction/flow/index.html) and the explanation of [Feature Branch Workflow](https://atlassian.com/git/tutorials/comparing-workflows#feature-branch-workflow) for the idea behind this kind of development.

1. Get the JabRef code on your local machine. Detailed instructions about this step can be found in our [guidelines for setting up a local workspace](getting-into-the-code/guidelines-for-setting-up-a-local-workspace.md).
   1. Fork the JabRef into your GitHub account.
   2. Clone your forked repository on your local machine.
2. **Create a new branch** (such as `fix-for-issue-121`). Be sure to create a **separate branch** for each improvement you implement.
3. Do your work on the **new branch — not the master branch.** Refer to our [code how-tos](https://devdocs.jabref.org/getting-into-the-code/code-howtos) if you have questions about your implementation.
4. Create a pull request. For an overview of pull requests, take a look at GitHub's [pull request help documentation](https://help.github.com/articles/about-pull-requests/).
5. In case your pull request is not yet complete or not yet ready for review, consider creating a [draft pull request](https://github.blog/2019-02-14-introducing-draft-pull-requests/) instead.

In case you have any questions, do not hesitate to write one of our [JabRef developers](https://github.com/orgs/JabRef/teams/developers) an email. We should also be online at [gitter](https://gitter.im/JabRef/jabref).

### Formal requirements for a pull request

The main goal of the formal requirements is to provide credit to you and to be able to understand the patch.

#### Add your change to `CHANGELOG.md`

You should edit the [`CHANGELOG.md`](https://github.com/JabRef/jabref/blob/master/CHANGELOG.md#changelog) file located in the root directory of the JabRef source. Add a line with your changes in the appropriate section.

If you did internal refactorings or improvements not visible to the user (e.g., UI, .bib file), then you don't need to put an entry there.

#### **Format of keyboard shortcuts**

Example: `<kbd>Ctrl</kbd> + <kbd>Enter</kbd>`

In case you add keys to the changelog, please follow these rules:

* `<kbd>` tag for each key
* First letter of key capitalized
* Combined keys separated by `+`
* Spaces before and after separator `+`

#### Author credits

Please, **do not add yourself at JavaDoc's `@authors`**. The contribution information is tracked via the version control system and shown at [https://github.com/JabRef/jabref/graphs/contributors](https://github.com/JabRef/jabref/graphs/contributors). We also link to the contributors page in our about dialog.

Your contribution is considered being made under [MIT license](https://tldrlegal.com/license/mit-license).

#### Write a good commit message

See [good commit message](https://github.com/joelparkerhenderson/git_commit_message) or [commit guidelines section of Pro Git](http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines). The first line of your commit message is automatically taken as the title for the pull-request. All other lines make up the body of the pull request. Add the words `fixes #xxx` to your PR to auto-close the corresponding issue.

#### Test your code

We know that writing test cases takes a lot of time. Nevertheless, we rely on our test cases to ensure that a bug fix or a feature implementation doesn't break anything. In case you do not have time to add a test case, we nevertheless ask you to at least run `gradlew check` to ensure that your change doesn't break anything else.

#### When adding a new `Localization.lang` entry

Add new `Localization.lang("KEY")` to a Java file. The tests will fail. In the test output a snippet is generated, which must be added to the English translation file.

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

Add the above snippet to the English translation file located at `src/main/resources/l10n/JabRef_en.properties`. [Crowdin](https://crowdin.com/project/jabref) will automatically pick up the new string and add it to the other translations.

You can also directly run the specific test in your IDE. The test "LocalizationConsistencyTest" is placed under `src/test/java/net.sf.jabref.logic.l10n/LocalizationConsistencyTest.java`. Find more information in the [JabRef developer docs](https://devdocs.jabref.org/getting-into-the-code/code-howtos#using-localization-correctly).

#### When adding a library

Please try to use a version available at JCenter and add it to `build.gradle`. In any case, describe the library at [`external-libraries.md`](https://github.com/JabRef/jabref/blob/master/external-libraries.md#external-libraries). We need that information for our package maintainers (e.g., those of the [debian package](https://tracker.debian.org/pkg/jabref)). Also add a txt file stating the license in `libraries/`. It is used at `gradlew processResources` to generate the About.html files. You can see the result in `build\resources\main\help\en\About.html` or when clicking Help -> About.

#### When making an architectural decision

In case you add a library or do major code rewrites, we ask you to document your decision. Recommended reading: [https://adr.github.io/](https://adr.github.io).

We simply ask to create a new markdown file in `docs/adr` following the template presented at [https://adr.github.io/madr/](https://adr.github.io/madr/).

In case you want to directly add a comment to a class, simply use the following template (based on [sustainable architectural decisions](https://www.infoq.com/articles/sustainable-architectural-design-decisions)):

```
In the context of <use case/user story u>,
facing <concern c>
we decided for <option o>
and neglected <other options>,
to achieve <system qualities/desired consequences>,
accepting <downside / undesired consequences>,
because <additional rationale>.
```

### Create a pull request

Create a pull request on GitHub following GitHub's guide "[Creating a pull request from a fork](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request-from-a-fork)". For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).

If you want to indicate that a pull request is not yet complete **before** creating the pull request, you may consider creating a [draft pull request](https://github.blog/2019-02-14-introducing-draft-pull-requests/). Alternatively, once the PR has been created, you can add the prefix `[WIP]` (which stands for "Work in Progress") to indicate that the pull request is not yet complete, but you want to discuss something or inform about the current state of affairs.

## How to improve the developer's documentation

For improving developer's documentation, go on at the [docs/ subdirectory of JabRef's code](https://github.com/JabRef/jabref/tree/master/docs) and edit the file. GitHub offers a good guide at [Editing files in another user's repository](https://help.github.com/en/github/managing-files-in-a-repository/editing-files-in-another-users-repository).

In case you use some gitbook special features, and you want to test them, checkout JabRef's code locally, and execute following steps:

1. `npm install -g gitbook`
2. `cd docs`
3. `gitbook serve`

Then, you can see a near-to-reality rendering of the development documentation at [http://localhost:4000](http://localhost:4000).

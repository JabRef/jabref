# Contributing

General overview about contributing for non-programmers is available at <https://docs.jabref.org/contributing>.

We welcome contributions to JabRef and encourage you to follow the GitHub workflow specified below.
If you are not familiar with this type of workflow, take a look at GitHub's excellent overview on the [GitHub flow](https://docs.github.com/en/get-started/using-github/github-flow) and the explanation of [Feature Branch Workflow](https://atlassian.com/git/tutorials/comparing-workflows#feature-branch-workflow) for the idea behind this kind of development.

Before you start, get the JabRef code on your local machine.
Detailed instructions about this step can be found in our [guidelines for setting up a local workspace](getting-into-the-code/guidelines-for-setting-up-a-local-workspace/).

## Table of Contents

* [Choosing a task](#choosing-a-task-)
* [Getting a task assigned](#getting-a-task-assigned)
* [Pull Request Process](#pull-request-process)
  * [Requirements on the pull request and code](#requirements-on-the-pull-request-and-code)
  * [After submission of a pull request](#after-submission-of-a-pull-request)
  * [Development hints](#development-hints)

## Choosing a task [![Join the chat at https://gitter.im/JabRef/jabref](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/JabRef/jabref)

In general, we offer small issues perfect for aspiring developers.
These tasks provide an opportunity to learn how to set up your local workspace, create your first pull request on GitHub, and contribute towards solving minor problems or making small enhancements in JabRef.

It is essential to note that JabRef's issues vary in difficulty.
Some are simpler, while others are more complex. Our primary aim is to guide you through the code, ensuring that the understanding scope remains manageable. Sometimes, grasping the code might demand more effort than actually writing lines of code.

### I am a student and I want to start with something easy

We collect good issues to start with at our [list of good first issues](https://github.com/orgs/JabRef/projects/5/views/1).

### I am a student and I want to choose from a curated list of university projects

Take a look at [JabRef's candidates for university projects](https://github.com/orgs/JabRef/projects/3). There, a list of possible projects to work on during a teaching period is offered.

### I am a lecturer

If you ask yourself how to integrate JabRef into your class, please read the [documentation about how to integrate JabRef into a class of software engineering training](https://devdocs.jabref.org/teaching.html#jabref-and-software-engineering-training).
As student, you may notify your lecturer about this possibility.

### I want something with huge impact

Look at the discussions in our forum about [new features](https://discourse.jabref.org/c/features/6).
Find an interesting topic, discuss it and start contributing.
Alternatively, you can check out [JabRef's projects page at GitHub](https://github.com/JabRef/jabref/projects?query=is%3Aopen).
Although, of course, you can choose to work on ANY issue, choosing from the projects page has the advantage that these issues have already been categorized, sorted and screened by JabRef maintainers.
A typical subclassifications scheme is "priority" (high, normal and low). Fixing high priority issues is preferred.

### I want to know how to contribute code and set up my workspace

Check out the [documentation for developers](https://devdocs.jabref.org/contributing.html#contribute-code)

### I want to improve the developer's documentation

For improving developer's documentation, go on at the [docs/ subdirectory of JabRef's code](https://github.com/JabRef/jabref/tree/main/docs) and edit the file.
GitHub offers a good guide at [Editing files in another user's repository](https://help.github.com/en/github/managing-files-in-a-repository/editing-files-in-another-users-repository).
One can also add [callouts](https://just-the-docs.github.io/just-the-docs-tests/components/callouts/).

## Getting a task assigned

Comment on the issue you want to work at with `/assign-me`.
GitHub will then automatically assign you.

## Pull Request Process

1. Follow the steps at [Pre Condition 3: Code on the local machine](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/pre-03-code.html) to a) create a fork and b) have the fork checked out on your local machine
2. Ensure that you followed the [steps to set up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/) to have the code running properly in IntelliJ.
3. **Create a new branch** (such as `fix-for-issue-121`). Be sure to create a **separate branch** for each improvement you implement.
4. Work on the **new branch — not the `main` branch.** Refer to our [code how-tos](https://devdocs.jabref.org/code-howtos) if you have questions about your implementation.
5. Create a [pull request to JabRef main repository](https://github.com/JabRef/jabref/pulls).
   For an overview on the concept of pull requests, take a look at GitHub's [pull request help documentation](https://help.github.com/articles/about-pull-requests/).
   1. Ensure that you followed the requirements listed below. They are not too hard, they merely support the maintainers to focus on supportive feedback than just stating the obvious.
   2. For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).
   3. In case your pull request is not yet complete or not yet ready for review, create a [draft pull request](https://github.blog/2019-02-14-introducing-draft-pull-requests/) instead.
6. Wait for feedback of the developers
7. Address the feedback of the developers
8. After two developers gave their green flag, the pull request will be merged.

In case you have any questions, please

1. comment on the issue,
2. show up in our [Gitter chat](https://gitter.im/JabRef/jabref), or
3. show your current code using a draft pull request and ask questions.

We favor looking into your code using a draft pull request, because we can then also load the code into our IDE.
As counterexample, if you provide us with a screenshot of your changes, we cannot run it in our IDE.

### Requirements on the pull request and code

#### Test your code

We know that writing test cases takes a lot of time.
Nevertheless, we rely on our test cases to ensure that a bug fix or a feature implementation does not break anything.

For UI changes, we know that test cases are hard to write.
Therefore, you can omit them.
However, please at least add a screenshot showing your changes to the request.

<!-- In case you do not have time to add a test case, we nevertheless ask you to at least run `gradlew check` to ensure that your change does not break anything else. -->

#### Write a good commit message

See [good commit message](https://github.com/joelparkerhenderson/git-commit-message) or [commit guidelines section of Pro Git](http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines). For the curious: [Why good commit messages matter!](https://cbea.ms/git-commit/). The first line of your commit message is automatically taken as the title for the pull-request. All other lines make up the body of the pull request. Add the words `fixes #xxx` to your PR to auto-close the corresponding issue.

#### Add your change to `CHANGELOG.md`

You should edit the [`CHANGELOG.md`](https://github.com/JabRef/jabref/blob/main/CHANGELOG.md#changelog) file located in the root directory of the JabRef source. Add a line with your changes in the appropriate section.

If you did internal refactorings or improvements not visible to the user (e.g., UI, .bib file), then you don't need to put an entry there.

#### Author credits

Please, **do not add yourself at JavaDoc's `@authors`**.
The contribution information is tracked via the version control system and shown at [https://github.com/JabRef/jabref/graphs/contributors](https://github.com/JabRef/jabref/graphs/contributors).
We also show all contributors in our blog posts. See [Release 5.15 blog post](https://blog.jabref.org/2024/07/16/JabRef5-15/) for an example.

Your contribution is considered being made under [MIT license](https://tldrlegal.com/license/mit-license).

#### Notes on AI usage

Please keep these two principles in mind when you contribute:

1. Never let an LLM speak for you.
2. Never let an LLM think for you.

More reading on that is available at <https://roe.dev/blog/using-ai-in-open-source>.

We reserve the right to reject pull requests that contain little or no genuine and original contribution from the contributor.

### After submission of a pull request

After you submitted a pull request, automated checks will run.
You will maybe see "Some checks were not successful".
Then, please look into them and handle accordingly.

Afterwards, you will receive comments on it.
Maybe, the pull request is approved immediatly, maybe changes are requested.
In case changes are requested, please implement them.

After changing your code, commit on the branch and push.
The pull request will update automatically.

Never ever close the pull request and open a new one.
This causes much load on our side and is not the style of the GitHub open source community.

### Development hints

#### When adding an external dependency

Please try to use a version available at JCenter and add it to `build.gradle`.
In any case, describe the library at [`external-libraries.md`](https://github.com/JabRef/jabref/blob/main/external-libraries.md#external-libraries).
We need that information for our package maintainers (e.g., those of the [debian package](https://tracker.debian.org/pkg/jabref)).

#### When making an architectural decision

In case you add a library or do major code rewrites, we ask you to document your decision. Recommended reading: [https://adr.github.io/](https://adr.github.io).

We simply ask to create a new markdown file in `docs/adr` following the template presented at [https://adr.github.io/madr/](https://adr.github.io/madr/).
You can link that ADR using `@ADR({num})` as annotation.

#### When adding a new `Localization.lang` entry

Add new `Localization.lang("KEY")` to a Java file. The tests will fail. In the test output a snippet is generated, which must be added to the English translation file.

Example:

```text
java.lang.AssertionError: DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE
PASTE THESE INTO THE ENGLISH LANGUAGE FILE
[
Opens\ JabRef's\ Twitter\ page=Opens JabRef's Twitter page
]
Expected :[]
Actual   :[Opens\ JabRef's\ Twitter\ page (src\main\java\org\jabref\gui\JabRefFrame.java LANG)]
```

Add the above snippet to the English translation file located at `src/main/resources/l10n/JabRef_en.properties`.
[Crowdin](https://crowdin.com/project/jabref) will automatically pick up the new string and add it to the other translations.

You can also directly run the specific test in your IDE.
The test "`LocalizationConsistencyTest`" is placed under `src/test/java/org.jabref.logic.l10n/LocalizationConsistencyTest.java`.
Find more information in the [JabRef developer docs](https://devdocs.jabref.org/code-howtos/localization.html).

#### **Format of keyboard shortcuts**

In Markdown files (e.g., `CHANGELOG.md`), sometimes keyboard shortcuts need to be added.
Example: `<kbd>Ctrl</kbd> + <kbd>Enter</kbd>`

In case you add keys to the changelog, please follow these rules:

* `<kbd>` tag for each key
* First letter of key capitalized
* Combined keys separated by `+`
* Spaces before and after separator `+`

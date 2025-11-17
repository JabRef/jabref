# Contributing

We welcome contributions to JabRef and encourage you to follow the [GitHub workflow](https://docs.github.com/en/get-started/using-github/github-flow).
You can also check out the explanation of [Feature Branch Workflow](https://atlassian.com/git/tutorials/comparing-workflows#feature-branch-workflow) for the idea behind this kind of development.

JabRef regards its contributors as **[software engineers, not just programmers](https://www.phoenix.edu/blog/programmer-vs-software-engineer-key-differences.html)**.
As one consequence, for non-basic issues, you will have to work on the requirements side, too.

> [!TIP]
> If you are a newcomer, the two most helpful sections to navigate through are the [guidelines for setting up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/) (to get JabRef's source code into your local machine and get the development version running), and [frequently asked questions](https://devdocs.jabref.org/code-howtos/faq) - where you will find solutions to issues that are most commonly faced by new contributors.
<!-- markdownlint-disable-next-line MD028 -->
> [!NOTE]
> For non-programmers, a general overview on contributing is available at <https://docs.jabref.org/contributing>.

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

### I am a student (or a beginner to Open Source)

* Select an issue to work on from the [Issues Page](https://github.com/JabRef/jabref/issues). If you are a newcomer, we have a few issues labeled as <https://github.com/JabRef/jabref/labels/good%20first%20issue> to help you get started. You can start with any of the [unassigned good first issues](https://github.com/JabRef/jabref/issues?q=sort%3Aupdated-desc%20is%3Aissue%20is%3Aopen%20label%3A%22good%20first%20issue%22%20no%3Aassignee).
* Once you get your first PR merged, you can move on to <https://github.com/JabRef/jabref/labels/good%20second%20issue>, <https://github.com/JabRef/jabref/labels/good%20third%20issue>, and finally <https://github.com/JabRef/jabref/labels/good%20fourth%20issue> before taking up some more major bug fixes or features. Note that not all beginner friendly issues are labeled, so you may find suitable untagged issues to solve as well.

Always make sure that the issue you select is not presently assigned to anyone.

### I am a student and I want to choose from a curated list of university projects

Apart from "good first issues", we also offer collections of curated issues to work on.
We categorize them into "small", "medium", and "large".
All of them are intended to
a) bring you closer to a larger code base with a dedicated issue and
b) be clear in their description of work.
Sometimes, you may need to refine the requirements:
We see contributors as software engineers and [not just programmers](https://www.phoenix.edu/blog/programmer-vs-software-engineer-key-differences.html).
Thus, requirement engineering inherently comes up as a part of the process.

Take a look at [JabRef's candidates for university projects](https://github.com/orgs/JabRef/projects/3).
Here, you will get a table of curated issues.
The table offers filtering for small, medium, and large projects.
You can check the main focus (UI, logic, or both), the issue understanding effort, the implementation effort, and testing effort.
The "issue understanding effort" is an indication of how much time you will need to understand the issue before you can do any coding. It may depend on how easy it is to reproduce the issue, how much background knowledge is needed, etc.
The "implementation effort" is based on our experience of JabRef development.
Note that there may be issues with a high effort in understanding, but low implementation effort.
The challenge of these issues is to understand **where** in the code base something needs to be modified.

### I am a lecturer

If you ask yourself how to integrate JabRef into your class, please read the [documentation about how to integrate JabRef into a class of software engineering training](https://devdocs.jabref.org/teaching#jabref-and-software-engineering-training).
As student, you may notify your lecturer about this possibility.

### I want something with huge impact

Look at the discussions in our forum about [new features](https://discourse.jabref.org/c/features/6).
Find an interesting topic, discuss it and start contributing.
Alternatively, you can check out [JabRef's projects page at GitHub](https://github.com/JabRef/jabref/projects?query=is%3Aopen).
Although, of course, you can choose to work on ANY issue, choosing from the projects page has the advantage that these issues have already been categorized, sorted and screened by JabRef maintainers.
A typical subclassifications scheme is "priority" (high, normal and low). Fixing high priority issues is preferred.

### I want to know how to contribute code and set up my workspace

Check out the [documentation for developers](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/)

### I want to improve the developer's documentation

For improving developer's documentation, go on at the [docs/ subdirectory of JabRef's code](https://github.com/JabRef/jabref/tree/main/docs) and edit the file.
GitHub offers a good guide at [Editing files in another user's repository](https://help.github.com/en/github/managing-files-in-a-repository/editing-files-in-another-users-repository).
One can also add [callouts](https://just-the-docs.github.io/just-the-docs-tests/components/callouts/).

## Getting a task assigned

Comment on the issue you want to work at with `/assign-me`.
GitHub will then automatically assign you.

<!-- markdownlint-disable-next-line MD026 -->
## Give JabRef a Star!

> [!IMPORTANT]
> JabRef is completely free and used by students and researchers all over the world.
> It is actively developed and maintained primarily by volunteers in their free time.
> Keep them motivated by giving the project a GitHub star:
> Simply navigate to <https://github.com/jabref/jabref/> and click on the Star button!

## Pull Request Process

1. Follow the steps at [Pre Condition 3: Code on the local machine](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/pre-03-code) to a) create a fork and b) have the fork checked out on your local machine
2. Ensure that you followed the [steps to set up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/) to have the code running properly in IntelliJ.
3. Read about our [architecture and components](https://devdocs.jabref.org/architecture-and-components).
4. **Create a new branch** (such as `fix-for-issue-121`). Be sure to create a **separate branch** for each improvement you implement.
5. Refer to our [code how-tos](https://devdocs.jabref.org/code-howtos) if you have questions about your implementation.
6. Implement and test your changes.
   * Create JUnit tests for your changes, apart from manual testing. Maybe even use [Test-driven Development](https://en.wikipedia.org/wiki/Test-driven_development) to speed up your development.
   * Have fun. Learn. Communictate.
7. Create a [pull request to JabRef main repository](https://github.com/JabRef/jabref/pulls).
   * For an overview on the concept of pull requests, take a look at GitHub's [pull request help documentation](https://help.github.com/articles/about-pull-requests/).
   * For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).
   * Note that submission of a pull request takes time. There is also a [checklist](https://github.com/JabRef/jabref/blob/main/.github/PULL_REQUEST_TEMPLATE.md?plain=1), which takes time to check - and maybe update your branch.
   * In case your pull request is not yet complete or not yet ready for review, create a [draft pull request](https://github.blog/2019-02-14-introducing-draft-pull-requests/) instead.
8. Wait for automatic checks to run and bots commenting.
9. Address the feedback of the automated checks. To find solutions to the most common errors that lead to such failures, check our [FAQ page](https://devdocs.jabref.org/code-howtos/faq).
10. Wait for feedback of one of the maintainers. Since this is a hobby project for all of them, this may take a while. You can speed up things by reducing their load. You can, for instance, try out other pull requests, review other pull requests, and answer questions on JabRef in the forum.
11. Address the feedback of the maintainer. In case your pull request changed something significant, we might ask you to create or update [`docs/requirements`](https://devdocs.jabref.org/requirements/) to include a description of the requirement stemming from the issue - and link that with your implementation.
12. Wait for feedback of a second maintainer.
13. Address the feedback of the second maintainer.
14. After two maintainers gave their green flag, the pull request will be merged.

We view pull requests as a collaborative process.
Submit a pull request early to enable feedback from the team while you continue working.
Please also remember to discuss bigger changes early with the core maintainers to ensure more fruitful investment of time and work, and lesser friction in acceptance later.
Some fundamental design decisions can be found within our list of [Architectural Decision Records](https://devdocs.jabref.org/decisions/).
After a pull request is ready for review, we will discuss improvements with you and agree to merge them once the [maintainers](https://github.com/JabRef/jabref/blob/main/MAINTAINERS) approve.

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

#### AI Usage Policy

This project does *not* accept fully AI-generated contributions.
AI tools may be used assistively only. As a contributor, you should be able to understand and take responsibility for changes you make to the codebase.

Agents and AI coding assistants must follow the guidelines in [`./AGENTS.md`](./AGENTS.md).

Please read the [AI Usage Policy](./AI_USAGE_POLICY.md) before proceeding.
In short, please keep these two principles in mind when you contribute:

> [!IMPORTANT]
>
> 1. Never let an LLM speak for you.
> 2. Never let an LLM think for you.

More reading on that is available at <https://roe.dev/blog/using-ai-in-open-source>.

We reserve the right to reject pull requests that contain little or no genuine and original contribution from the contributor.

### After submission of a pull request

Once you submit a pull request, automated checks will run and bots will perform a preliminary review on your code.
You will get automated comments on your pull request within about 5 minutes.
Acting on them in a timely manner is expected.

You may also see "Some checks were not successful".
You can click on failing checks to see more information about why they failed.
Please look into them and handle accordingly.

After implementing changes, commit to the branch your pull request is *from* and push.
The pull request will automatically be updated with your changes.
To maintain a clean git history, your commits will also be automatically squashed upon acceptance of the pull request, during merging.
Thus, no need to worry about WIP commits or [fixing git submodule issues](https://devdocs.jabref.org/code-howtos/faq#submodules).
As a concequece, force-pushing is not required - and must be **avoided**.

After all the basic checks are green, maintainers will look at your pull request.
Since JabRef is driven by volunteers in their spare time, reviews may take more time than a project with full time developers.
The pull request may be approved immediatly, or a reviewer may request changes and/or have discussions regarding your approach.
In that case, you are expected to answer any questions and implement the requested changes.

Please – **never ever close a pull request and open a new one** -
This causes unnecessary work on our side, and is not in the style of the GitHub Open Source community.
You can push any changes you need to make to the branch your pull request is *from*.
These changes will be automatically reflected in your pull request.

> [!CAUTION]
> **If you close your pull request, you will be unassigned from the issue automatically.**

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
Find more information in the [JabRef developer docs](https://devdocs.jabref.org/code-howtos/localization).

#### **Format of keyboard shortcuts**

In Markdown files (e.g., `CHANGELOG.md`), sometimes keyboard shortcuts need to be added.
Example: `<kbd>Ctrl</kbd> + <kbd>Enter</kbd>`

In case you add keys to the changelog, please follow these rules:

* `<kbd>` tag for each key
* First letter of key capitalized
* Combined keys separated by `+`
* Spaces before and after separator `+`

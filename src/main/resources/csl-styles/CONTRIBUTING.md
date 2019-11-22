# Guide to Submitting CSL Styles (and CSL Locales)

The CSL style repository is the central location on the web for collecting and maintaining CSL styles.
Software products like Zotero, Mendeley, and Papers all draw their styles from our repository.

We welcome style submissions (and corrections), and are particularly interested in styles for journals and published style guides.
If you wish to submit a different type of style, please first check our [Criteria for Accepting Styles](https://github.com/citation-style-language/styles/wiki/Criteria-for-Accepting-Styles).

To submit a style, please follow the following steps (for locale files, follow the same steps in the  [locales](https://github.com/citation-style-language/locales) repository):

#### 1. Check that your style meets all our [style requirements](https://github.com/citation-style-language/styles/wiki/Style-Requirements)

#### 2. [Validate](https://github.com/citation-style-language/styles/wiki/Validation) your style against the CSL schema, and correct any validation errors

#### 3. Create a pull request

The fastest way to get a style accepted into our repository is by creating a **pull request** on the GitHub website.
To start, create a GitHub account and sign in.

##### 3a. Submitting a new style

1. Visit https://github.com/citation-style-language/styles and click the "Create new file" button.
   When submitting a [dependent style](https://github.com/citation-style-language/styles/wiki/Requesting-Styles#dependent-styles), first navigate to the [dependent](https://github.com/citation-style-language/styles/tree/master/dependent) subdirectory.
2. Type in the file name of the style in the "Name your file..." text field at the top.
   Don't forget to add the ".csl" extension (e.g., "journal-of-results.csl" instead of just "journal-of-results")!
3. Paste the style code into the "<> Edit new file" tab below.
4. Click the "Propose new file" button.
5. In the next window, click the "Create pull request" button.
   Describe the changes you've made, and click the "Create pull request" button once more.

(for more help, see GitHub's instructions on [Creating new files](https://help.github.com/articles/creating-new-files))

##### 3b. Submitting changes to an existing style

1. Visit https://github.com/citation-style-language/styles and find the style you wish to change (dependent styles are stored in the [dependent](https://github.com/citation-style-language/styles/tree/master/dependent) subdirectory).
   To search by file name press "t" (this activates GitHub’s [File Finder](https://github.com/blog/793-introducing-the-file-finder)) and start typing.
2. Click on the style name, which will bring you to a page like https://github.com/citation-style-language/styles/blob/master/apa.csl.
3. Click the edit icon (in the shape of a pencil).
   This opens a code editor in which you can make your changes (you can for example copy and paste your modified style into this window).
4. Once you have made your changes in the "<> Edit file" tab (and previewed the changes in the "Preview changes" tab), click the "Propose file change" button.
5. In the next window, click the "Create pull request" button.
   Describe the changes you've made, and click the "Create pull request" button once more.

(for more help, see GitHub's instructions on [Editing files in another user's repository](https://help.github.com/articles/editing-files-in-another-user-s-repository))

Instead of relying solely on the GitHub website, you can also use a git client, such as [GitHub Desktop](https://desktop.github.com/) for Mac and Windows, or [SmartGit](http://www.syntevo.com/smartgit/).
When using a client, [fork](https://help.github.com/articles/fork-a-repo/) the [style repository](https://github.com/citation-style-language/styles), create a branch off of "master", commit your changes, and then create a [pull request](https://help.github.com/articles/using-pull-requests/).

#### 4. Update your pull request

We often request you to make some changes to your pull request.
Don't create a new pull request right away if your original submission has issues.
It's much easier for us if you update your original pull request instead.

After you create a pull request, our @csl-bot will first automatically run a number of tests to check whether your style validates and follows our style requirements.
These tests usually finish within a few minutes.
If there are problems, @csl-bot will give a link to the test report, which contains the identified errors.
If you have trouble understanding these errors, please post a comment in the pull request and ask for help.

We also have a handful of volunteers who review pull requests, usually within a day or two.

To update your pull request:

1. Visit the "Files changed" tab of your pull request.
2. Click the edit (pencil) icon at the top-right of your style.
3. Make your changes, and click the "Commit changes" button.

Any changes made this way will automatically appear in the pull request, and each commit will trigger a new test report.

Once we are satisfied, we will **merge** your pull request.
This can take hours or days, depending on how busy we are, and on how much back and forth is needed to get your style in acceptable shape.

#### 5. Make additional changes after merging

Once a pull request has been merged, it can no longer be used to submit further changes.
So, if you wish to make additional changes, just create a new pull request.

Please always start from the latest version of the style from our repository, because we often perform maintenance on the repository styles.

## Asking for help

If you get stuck at a particular step, don't hesitate to ask for help by opening a GitHub issue at https://github.com/citation-style-language/styles/issues (click the "New issue" button).

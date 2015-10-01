## Understanding the basics
We welcome contributions to JabRef and encourage to create a fork, make a patch, and create a pull request.
Be sure to create a separate branch for each improvement you implement.
Take a look at GitHub's excellent [help documentation] for a detailed explanation.

We also have [guidelines for setting up a local workspace](https://github.com/JabRef/jabref/wiki/Guidelines-for-setting-up-a-local-workspace).

For newcomers, [FLOSS Coach](http://www.flosscoach.com/) might be helpful.
It contains steps to get started with JabRef development.

In case you have any questions, you can use our [GITTER channel](https://gitter.im/JabRef/jabref) or use our [developers mailinglist](https://lists.sourceforge.net/lists/listinfo/jabref-devel).


## Formal requirements for a pull request
The main goal of the formal requirements is to provide credit to you and to be able to understand the patch.
Nevertheless we aim to keep the code consistently formatted, therefore we additionally have a requirement regarding the source formatter.


### Ensure consistent formatting
Ensure your code is formatted according the JabRef formatting guidelines.
These are provided as Eclipse formatting configuration in [formatter_settings.xml](ide-settings/formatter_settings.xml).
Ensure that JabRef's code cleanup settings are activated.
Import [cleanup_settings.xml](ide-settings/cleanup_settings.xml).
You can also run `gradlew format` to let the [Gradle Format plugin](https://github.com/youribonnaffe/gradle-format-plugin) do the formatting.


### Add your change to the CHANGELOG
You should edit the [CHANGELOG](CHANGELOG) located in the root directory of the JabRef source.
Add a line with your changes and your name.
Nicknames are OK.


### Author credits
You will be given credit in the `AUTHORS` file in the root of the repository and the 'About' pages inside the main application.
We will periodically update the contributors list inside `AUTHORS`.
This is done by an automatic shell script `scripts/generate-authors.sh`.

If you want to add yourself directly with your pull request please run this script.
Please make sure there are no duplicates or alternate spellings of your name listed.
If you need to merge different Git usernames or email addresses you can do so by editing `.mailmap`.
More information on this can be found via `man git-shortlog`.


### Add yourself to the header
The headers of each `.java` file state the authors.
These entries should match the modifications done.
If you do not want to add your real name, add yourself as `JabRef contributors`.

For instance,

```plain
/*  Copyright (C) 2003-2011 JabRef contributors.
```

gets

```plain
/*  Copyright (C) 2003-2011 JabRef contributors.
 *  Copyright (C) 2015 Stefan Jauch
```


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


### Create a pull request
Create a pull request on GitHub.
For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).

You can add the prefix `[WIP]` to indicate that the pull request is not yet complete, but you want to discuss something or inform about the current state of affairs.


[commit guidelines section of Pro Git]: http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines
[good commit message]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[help documentation]: https://help.github.com/articles/using-pull-requests/

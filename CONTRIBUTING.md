## Understanding the basics
We welcome contributions to JabRef and encourage to create a fork, make a patch, and create a pull request.
Be sure to create a separate branch for each improvement you implement.
Take a look at GitHub's excellent [help documentation] for a detailed explanation.

We also have [guidelines for setting up a local workspace](https://github.com/JabRef/jabref/wiki/Guidelines-for-setting-up-a-local-workspace).

For newcomers, [FLOSS Coach](http://www.flosscoach.com/) might be helpful.
It contains steps to get start with JabRef development.

Please keep in mind that JabRef relies on Java 6 due to the availability of Java 6 on older Mac OS X operating system.

In case you have any questions, you can use our [GITTER channel](https://gitter.im/JabRef/jabref) or use our [developers mailinglist](https://lists.sourceforge.net/lists/listinfo/jabref-devel).


## Formal requirements for a pull request

The main goal of the formal requirements is to provide credit to you and to be able to understand the patch.
Nevertheless we aim to keep the code consistently formatted, therefore we additionally have a requirement regarding the source formatter.


### Ensure consistent formatting

Ensure your code is formatted according the JabRef formatting guidelines.
These are provided as Eclipse formatting configuration in [formatter_settings.xml](formatter_settings.xml).
Ensure that JabRef's code cleanup settings are activated.
Import [cleanup_settings.xml](cleanup_settings.xml).


### Add your change to the CHANGELOG
You should edit the [CHANGELOG](CHANGELOG) located in the root directory of the JabRef source.
Add a line with your changes and your name.
Nicknames are OK.


### Add yourself to src/main/resources/help/About.html
We try to keep an updated list of contributors in `About.html`.
Open `About.html` and add yourself below `Contributions from:`.


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

[commit guidelines section of Pro Git]: http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines
[good commit message]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[help documentation]: https://help.github.com/articles/using-pull-requests/

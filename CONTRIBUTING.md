## Understanding the basics
Not sure what a pull request is, or how to submit one?  Take a look at GitHub's excellent [help documentation] first.


## Add your change to the CHANGELOG
You should edit the [CHANGELOG](CHANGELOG) located in the root directory of the JabRef source.
Add a line with your changes and your name.
Nicknames are OK


## Adapt the year in the header

The years stated in the header of each .java file should match the years where the file has been modified.

For instance,

```plain
/*  Copyright (C) 2003-2011 JabRef contributors.
```

gets

```plain
/*  Copyright (C) 2003-2014 JabRef contributors.
```


## Write a good commit message.
See [good commit message] or [commit guidelines section of Pro Git].

[commit guidelines section of Pro Git]: http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines
[good commit message]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[help documentation]: http://help.github.com/send-pull-requests

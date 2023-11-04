---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 3
---

# Pre Condition 3: Code on the local machine

This section explains how you get the JabRef code onto your machine in a form allowing you to make contributions.

## Fork JabRef into your GitHub account

1. Log into your GitHub account
2. Go to [https://github.com/JabRef/jabref](https://github.com/JabRef/jabref)
3. Create a fork by clicking at fork button on the right top corner
4. A fork repository will be created under your account `https://github.com/YOUR_USERNAME/jabref`.

## Clone your forked repository on your local machine

In a command line, navigate to the folder where you want to place the source code (parent folder of `jabref`).
To prevent issues along the way, it is strongly recommend choosing a path that does not contain any special (non-ASCII or whitespace) characters.
In the following, we will use `c:\git-repositories` as base folder:

```cmd
cd \
mkdir git-repositories
cd git-repositories
git clone --recurse-submodules https://github.com/JabRef/jabref.git JabRef
cd JabRef
git remote rename origin upstream
git remote add origin https://github.com/YOUR_USERNAME/jabref.git
git fetch --all
git branch --set-upstream-to=origin/main main
```

{: .important }
> `--recurse-submodules` is necessary to have the required files available to JabRef. (Background: It concerns the files from [citation-style-language/styles](https://github.com/citation-style-language/styles) and more).
>
> Note that putting the repo JabRef directly on `C:\` or any other drive letter on Windows causes compile errors (**negative example**: `C:\jabref`).
>
> Please really ensure that you pass `JabRef` as parameter. Otherwise, you will get `java.lang.IllegalStateException: Module entity with name: jabref should be available`. See [IDEA-317606](https://youtrack.jetbrains.com/issue/IDEA-317606/Changing-only-the-case-of-the-Gradle-root-project-name-causes-exception-while-importing-project-java.lang.IllegalStateException) for details.

{: .note-title }
> Background
>
> Initial cloning might be very slow (`27.00 KiB/s`).
>
> To prevent this, first the `upstream` repository is cloned.
> This repository seems to live in the caches of GitHub.
>
> Now, you have two remote repositories, where `origin` is yours and `upstream` is the one of the JabRef organization.
>
> You can see it with `git remote -v`:
>
> ```cmd
> c:\git-repositories\jabref> git remote -v
> origin     https://github.com/YOURUSERNAME/jabref.git (fetch)
> origin     https://github.com/YOURUSERNAME/jabref.git (push)
> upstream     https://github.com/jabref/jabref.git (fetch)
> upstream     https://github.com/jabref/jabref.git (push)
> ```

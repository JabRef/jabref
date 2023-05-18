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
git clone --depth=10 https://github.com/JabRef/jabref.git
cd jabref
git remote rename origin upstream
git remote add origin https://github.com/YOUR_USERNAME/jabref.git
git branch --set-upstream-to=origin/main main
```

{: .important }
> Note that putting the repo JabRef directly on `C:\` or any other drive letter on Windows causes compile errors (**negative example**: `C:\jabref`).
>
> Further, if you are building on Windows, make sure that the absolute path to the location of the clone does not contain folders starting with '`u`' (**negative example**: `C:\university\jabref`) as this may currently also cause [compile errors](https://github.com/JabRef/jabref/issues/9783).

{: .note-title }
> Background
>
> Initial cloning might be very slow (`27.00 KiB/s`).
>
> To prevent this, first the `upstream` repository is cloned.
> This repository seems to live in the caches of GitHub.
>
> The `--depth--10` is used to limit the download to \~20 MB instead of downloading the complete history (\~800 MB).
> If you want to dig in our commit history, feel free to download everything.
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

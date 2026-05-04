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

A longer explanation is available at <https://help.github.com/en/articles/fork-a-repo>.

## Disable GitHub actions

JabRef's CI checks take much time.
They could hit your fee limit.
Therefore, we advise to disable them - unless you know what you are doing.

1. Go to your JabRef repository.
2. Go to Settings
3. On the left, click on "Actions"
4. Click on "General"
5. Select "Disable actions"
6. Click on "Save"

## Clone your forked repository on your local machine

In a command line, navigate to the folder where you want to place the source code (parent folder of `jabref`).

### Linux and macOS

```bash
git clone --recurse-submodules https://github.com/JabRef/jabref.git
cd jabref
git remote rename origin upstream
git remote add origin https://github.com/YOUR_USERNAME/jabref.git
git fetch --all
```

### Windows

To prevent issues along the way, it is strongly recommend choosing a path that does not contain any special (non-ASCII or whitespace) characters.
In the following, we will use `c:\git-repositories` as base folder.

Open the "Command Prompt".

```cmd
cd \
mkdir git-repositories
cd git-repositories
git clone --recurse-submodules https://github.com/JabRef/jabref.git
cd jabref
git remote rename origin upstream
git remote add origin https://github.com/YOUR_USERNAME/jabref.git
git fetch --all
```

{: .important }
> `--recurse-submodules` is necessary to have the required files available to JabRef. (Background: It concerns the files from [citation-style-language/styles](https://github.com/citation-style-language/styles) and more).
>
> Note that putting the JabRef repository directly on `C:\` or any other drive letter on Windows causes compile errors (**negative example**: `C:\jabref`).

### Background

Initial cloning of your fork might be very slow (`27.00 KiB/s`).
To prevent this, first the `upstream` repository is cloned.
This repository seems to live in the caches of GitHub.

Now, you have two remote repositories, where `origin` is yours and `upstream` is the one of the JabRef organization.

You can see it with `git remote -v`:

```cmd
c:\git-repositories\jabref> git remote -v
origin     https://github.com/YOURUSERNAME/jabref.git (fetch)
origin     https://github.com/YOURUSERNAME/jabref.git (push)
upstream     https://github.com/JabRef/jabref.git (fetch)
upstream     https://github.com/JabRef/jabref.git (push)
```

## Have `git blame` working

You need to tell git to ignore some commits when doing `git blame`:

```terminal
git config --global blame.ignoreRevsFile .git-blame-ignore-revs
```

<!-- background: https://docs.github.com/en/repositories/working-with-files/using-files/viewing-and-understanding-files#ignore-commits-in-the-blame-view -->

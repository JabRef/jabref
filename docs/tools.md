This page lists some software we consider useful.

## Browser plugins
* [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub

## git hints
Here, we collect some helpful git hints

* https://github.com/blog/2019-how-to-undo-almost-anything-with-git
* https://github.com/RichardLitt/docs/blob/master/amending-a-commit-guide.md

### Rebase everything as one commit on master
* Precondition: `JabRef/jabref` is [configured as upstream](https://help.github.com/articles/configuring-a-remote-for-a-fork/). 

1. Fetch recent commits and prune non-existing branches: `git fetch upstream --prune`
2. Merge recent commits: `git merge upstream/master`
3. If there are conflicts, resolve them
4. Reset index to upstream/master: `git reset upstream/master`
5. Review the changes and create a new commit using git gui: `git gui&`
6. Do a force push: `git push -f origin`

See also: https://help.github.com/articles/syncing-a-fork/

## General git tooling on Windows

* Use [git for windows](https://git-for-windows.github.io/), no additional git tooling required
* [Use notepad++ as editor](http://stackoverflow.com/a/2486342/873282) for `git rebase -i`
* [Git Credential Manager for Windows](https://github.com/Microsoft/Git-Credential-Manager-for-Windows) - Aim: Store password for GitHub permanently for https repository locations
  * <s>Execute `"c:\Program Files\Git\bin\git.exe" push origin` once using **cmd** not `git bash` - reason: https://github.com/Microsoft/Git-Credential-Manager-for-Windows/issues/50.</s>

## Better console applications

* [Cmder] or ConEmu plus clink.

### ConEmu plus clink

* [ConEmu] -> Preview Version  - Aim: Colorful console with tabs
  * At first start:
    * "Choose your startup task ...": `{Bash::Git bash}}
    * `OK`
    * Upper right corner: "Settings..." (third entrry Eintrag)
    * Startup/Tasks: Choose task no. 7 ("Bash::Git bash"). At "Task parameters" `/dir C:\git-repositories\jabref\jabref`
    * `Save Settings`
* [clink] - Aim: Unix keys (Alt+b, Ctrl+s, etc.) also available at the prompt of `cmd.exe`

## Some useful keyboard shortcuts

* [AutoHotkey](http://autohotkey.com/) - Preparation for the next step
* https://github.com/koppor/autohotkey-scripts - Aim: Have Windows+`v` opening ConEmu
 1. Clone the repository locally.
 2. Then link `ConEmu.ahk` and `WindowsExplorer.ahk` at the startup menu (Link creation works with drag'n'drop using the right mouse key and then choosing "Create link" when dropping). Hint: Startup is in the folder `Startup` (German: `Autostart`) at `%APPDATA%\Microsoft\Windows\Start Menu\Programs\` - accessible via `Win+r`: `shell:startup`

  [ConEmu]: http://conemu.github.io/
  [clink]: http://mridgers.github.io/clink/
  [Cmder]: http://cmder.net/
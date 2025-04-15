---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 2
---

# Pre Condition 2: Required Software

## git

It is strongly recommended that you have git installed.

### Linux

* On Debian-based distros: `sudo apt-get install -y git gitk`

### macOS

* Use [homebrew](https://brew.sh/))
* `brew install git git-gui`

### Windows

* Use [git for windows](https://git-for-windows.github.io), no additional git tooling required
  * [Download the installer](http://git-scm.com/download/win) and install it.
  * Activate the option "Use Git and optional Unix tools from the Windows Command Prompt".
  * [Git Credential Manager for Windows](https://github.com/Microsoft/Git-Credential-Manager-for-Windows) is included. Ensure that you include that in the installation. Aim: Store password for GitHub permanently for `https` repository locations
  * Note: Using [chocolatey](https://chocolatey.org/), you can run `choco install git.install -y --params "/GitAndUnixToolsOnPath /WindowsTerminal` to a) install git and b) have Linux commands such as `grep` available in your `PATH`.
* [Configure using Visual Studio Code as editor](https://code.visualstudio.com/docs/sourcecontrol/overview#_vs-code-as-git-editor) for any git prompts (commit messages at merge, ...)

## Installed IDE

We highly encourage using [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=jabref), because all other IDEs work less good.
Especially using Visual Studio Code has issues.

IntelliJ's Community Edition works well.
Most contributors use the Ultimate Edition, because they are students getting that edition for free.

Install IntelliJ using [JetBrain's Toolbox App](https://www.jetbrains.com/toolbox-app/).

## Other Tooling

We collected some other tooling recommendations.
We invite you to read on at our [tool recommendations](../../code-howtos/tools.md).

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

* Use [homebrew](https://brew.sh/)
* `brew install git git-gui`

### Windows

* Install [Git for Windows](https://git-for-windows.github.io), no additional git tooling required. Git for Windows includes Git Bash.
  * [Download the installer](http://git-scm.com/download/win) and install it.
  * Activate the option "Use Git and optional Unix tools from the Windows Command Prompt".
  * [Git Credential Manager for Windows](https://github.com/Microsoft/Git-Credential-Manager-for-Windows) is included. Ensure that you include that in the installation. Aim: Store password for GitHub permanently for `https` repository locations
  * [Configure using Visual Studio Code as editor](https://code.visualstudio.com/docs/sourcecontrol/overview#_vs-code-as-git-editor) for any git prompts (commit messages at merge, ...)
* We recommend using [Windows Terminal](https://aka.ms/terminal), [configured to start Git Bash](https://www.timschaeps.be/post/adding-git-bash-to-windows-terminal/).
* Install [WixToolSet](https://github.com/wixtoolset/wix).

{: note}
You can use [chocolatey](https://chocolatey.org/) to install git more smoothly.
First, install chocolatey, then run `choco install git.install -y --params "/GitAndUnixToolsOnPath /WindowsTerminal /Editor:VisualStudioCode` to a) install git and b) have Linux commands such as `grep` available in your `PATH`.

## Installed IDE

We highly encourage using [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=jabref), as it provides the most reliable experience for this project.
Other IDEs may have compatibility issues, particularly Visual Studio Code.

IntelliJ IDEA Community Edition works well.
Most contributors use the Ultimate Edition, because they are students getting that edition for free.

{: .highlight }
Install IntelliJ using [JetBrain's Toolbox App](https://www.jetbrains.com/toolbox-app/?from=jabref).
This helps you keeping your JetBrains tools updated and allows for switching between different products easily.
Especially on Linux, the toolbox ensures a smooth start of IntelliJ IDEA.
Note that you need to scroll down the list of available IDEs to find "IntelliJ IDEA Community Edition".

{: note}
In case you have less than 6 GiB of RAM, IntelliJ won't work.
We recommend upgrading your system.

## Other Tooling

We collected some other tooling recommendations.
We invite you to read on at our [tool recommendations](../../code-howtos/tools.md).

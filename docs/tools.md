This page lists some software we consider useful.

## Browser plugins
* [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub
* [ZenHub Browser Extension](https://www.zenhub.io/) - `+1` for GitHub and much more

## General GIT tooling on Windows

* Use [git for windows](https://git-for-windows.github.io/), no additional git tooling required
* [Git Credential Manager for Windows](https://github.com/Microsoft/Git-Credential-Manager-for-Windows) - Aim: Store password for GitHub permanently for https repository locations
  * <s>Execute `"c:\Program Files\Git\bin\git.exe" push origin` once using **cmd** not `git bash` - reason: https://github.com/Microsoft/Git-Credential-Manager-for-Windows/issues/50.</s>

### Better console applications

* [Cmder] or ConEmu plus clink.

#### ConEmu plus clink

* [ConEmu] -> Preview Version  - Aim: Colorful console with tabs
  * At first start:
    * "Choose your startup task ...": `{Bash::Git bash}}
    * `OK`
    * Upper right corner: "Settings..." (third entrry Eintrag)
    * Startup/Tasks: Choose task no. 7 ("Bash::Git bash"). At "Task parameters" `/dir C:\git-repositories\jabref\jabref`
    * `Save Settings`
* [clink] - Aim: Unix keys (Alt+b, Ctrl+s, etc.) also available at the prompt of `cmd.exe`

### Some useful keyboard shortcuts

* [AutoHotkey](http://autohotkey.com/) - Preparation for the next step
* https://github.com/koppor/autohotkey-scripts - Aim: Have Windows+`v` opening ConEmu
 1. Clone the repository locally.
 2. Then link `ConEmu.ahk` and `WindowsExplorer.ahk` at the startup menu (Link creation works with drag'n'drop using the right mouse key and then choosing "Create link" when dropping). Hint: Startup is in the folder `Startup` (German: `Autostart`) at `%APPDATA%\Microsoft\Windows\Start Menu\Programs\` - accessible via `Win+r`: `shell:startup`

  [ConEmu]: http://conemu.github.io/
  [clink]: http://mridgers.github.io/clink/
  [Cmder]: http://cmder.net/
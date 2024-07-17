---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 91
---

# Advanced: VS Code as IDE

We are working on supporting VS Code for development.
There is basic support, but important things such as our code conventions are not in place.
Thus, use at your own risk.

Quick howto:

1. Start VS Code in the JabRef directory: `code .`.
2. There will be a poup asking "Reopen in Container". Click on that link.
3. VS Code restarts. Wait about 3 minutes until the dev container is build. You can click on "Starting Dev Container (show log)" to see the progress.
4. Afterwards, the Java project is imported. You can open the log (Click on "Check details"). Do that.
5. The terminal (tab "Java Build Status") will show some project synchronization and hang at `80% [797/1000]`.
   It keeps hanging at `Importing root project: 80% Refreshing '/jabref'`.
   Just wait.
   Then it hangs at `Synchronizing Gradle build at /workspaces/jabref: 80%`.
   Just wait.
   Then it takes long for `Refreshing workspace:`.
   Just wait.
   **Note:** If you had the project opened in IntelliJ before, this might cause issues (as outlined at <https://issuetracker.google.com/issues/255903901?pli=1>).
   Close everything, ensure that you committed your changes (if any), then execute `git clean -xdf` to wipe out all changes and created files - and start from step 1 again.
6. On the left, you will see a gradle button.
7. Click on the gradle button and open **JabRef -&gt; Tasks -&gt; application**.
8. Double click on **run**.
9. In the terminal, a new tab "run" opens.
10. On your desktop machine, open <http://127.0.0.1:6080/> in a web browser.
   Do not open the proposed port `6050`.
   This is JabRef's remote command port.
11. Use `vscode` as password.
12. You will see an opened JabRef.

Alternative to steps 9 to 10:

In case interaction using the web browser is too slow, you can use a VNC connection:

1. Install [VNC Connect](https://www.realvnc.com/en/connect/)
2. Use `vscode` as password

## Trouble shooting

In case there are reading errors on the file system, the docker container probably is out of order.
Close VS Code.
Stop the docker container, kill docker process in the Task Manager (if necessary).
Start docker again.
Start VS Code again.

## Background

We use VS Code's [Dev Containers](https://code.visualstudio.com/docs/devcontainers/containers) feature.
Thereby, we use [desktop-lite](https://github.com/devcontainers/features/tree/main/src/desktop-lite#options) to enable viewing the JabRef app.

---
parent: Set up a local workspace
grand_parent: Getting into the code
nav_order: 81
---

# Advanced: Use Prettier to format the code

{ .note}
This works on IntelliJ Ultimate only

This howto guides you to to configure [Prettier Java](https://github.com/jhipster/prettier-java/tree/main#prettier-java) for  code autoformatting on save and when pressing <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>L</kbd>.

1. Ensure that you have installed [Node.js](https://nodejs.org/en#downloadhttps://nodejs.org/en#download).
2. Install prettier: `npm install --global prettier`
3. **File > Settings... > Languages & Frameworks > JavaScript > Prettier**
4. Select "Automatic Prettier Configuration"
5. Change "Run for files" to `{**/*,*}.{java}`
6. Select "Run on save"

{: .note }
This howto is based on <https://www.jetbrains.com/help/idea/prettier.html>.

# Script usage

The Script has following commands available
- `$ python scripts/syncLang.py status [--extended]`
  - prints the current status to the terminal
  - `[--extended]` if the translations keys which create problems should be printed
  - usable with Gradle tasks
    - `$ gradle localizationStatus`
    - `$ gradle localizationStatusExtended`


- `$ python scripts/syncLang.py markdown`
  - Creates a markdown file of the current status and opens it
  - usable with Gradle tasks
    - `$ gradle localizationStatusWithMarkdown`


- `$ python scripts/syncLang.py update [--extended]`
  - compares all the localization files against the English one and fixes unambiguous duplicates, removes obsolete keys, adds missing keys, and sorts them
  - `[--extended]` if the translations keys which create problems should be printed
  - usable with Gradle tasks
    - `$ gradle localizationUpdate`
    - `$ gradle localizationUpdateExtended`

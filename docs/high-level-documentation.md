# High Level Documentation

Describes relevant information about the code structure of JabRef in a very precise and succinct way. 

We are currently transitioning from a spaghetti to an onion architecture with the `model` in the center, and the `logic` as an intermediate layer towards the `gui` which is the outer shell. The dependencies are only directed towards the center. We have JUnit tests to detect violations, and the build will fail automatically in these cases.

The `model` can only represent the most important data structures and has only a little bit of logic attached. The `logic` is responsible for reading/writing/importing/exporting and manipulating the `model`, and it is structured often as an API the `gui` can call and use. Only the `gui` knows the user and his preferences, and can interact with him to help him solve tasks. For each onion layer, we form packages according to their responsibility, i.e., vertical structuring.

We use an event bus to publish events from the `model` to the other onion layers.
This allows us to keep the onion architecture but still react upon changes within the core in the outer layers.




## Package Structure

```
gui --> logic --> model
gui ------------> model
# all packages and classes which are not part of logic or model 
# are considered gui classes from a dependency stand of view
```

## Most Important Classes and their Relation

Both GUI and CLI are started via the `JabRefMain` which will in turn call `JabRef` which then decides whether the GUI (`JabRefFrame`) or the CLI (`JabRefCLI` and a lot of code in `JabRef`) will be started. The `JabRefFrame` represents the Window which contains a `SidePane` on the left used for the fetchers/groups and a `DragDropPopupPane` extending a `JTabbedPane` on the right. Each tab is a `BasePanel` which has a `SearchBar` at the top, a `MainTable` at the center and a `PreviewPanel` or an `EntryEditor` at the bottom. Any right click in the `MainTable` is handled by the `RightClickMenu`. Each `BasePanel` holds a `BibDatabaseContext` consisting of a `BibDatabase` and the `MetaData`, which are the only relevant data of the currently shown database. A `BibDatabase` has a list of `BibEntries`. Each `BibEntry` has a key, a bibtex key and a key/value store for the fields with their values. Interpreted data (such as the type or the file field) is stored in the `TypedBibentry` type. The user can change the `JabRefPreferences` through the `PreferencesDialog` which uses a `JTabbedPane` to structure the preferences.

![Class Diagram](http://yuml.me/20975ef4)

Visualization as a class diagram: http://yuml.me/edit/20975ef4
---
nav_order: 1
layout: home
---
# Overview on Developing JabRef

This page presents all development information around JabRef.
In case you are an end user, please head to the [user documentation](https://docs.jabref.org) or to the [general homepage](https://www.jabref.org) of JabRef.

## Starting point for new developers

On the page [Setting up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace), we wrote about the initial steps to get your IDE running.
We strongly recommend continuing reading there.
After you successfully cloned and build JabRef, you are invited to continue reading here.

## JabRef's development strategy

We aim to keep up to high-quality code standards and use code quality tools wherever possible.

To ensure high code-quality,

* We follow the principles of [Java by Comparison](https://java.by-comparison.com).
* We follow the principles of [Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/).
* We use [Design Patterns](https://java-design-patterns.com/patterns/) when applicable.
* We document our design decisions using the lightweight architectural decision records [MADR](https://adr.github.io/madr/).
* Each external pull request is reviewed by at least two [JabRef Core Developers](https://github.com/JabRef/jabref/blob/main/MAINTAINERS).

## Continuous integration

JabRef has automatic checks using [GitHub actions](https://github.com/features/actions) in place.
One of them is checking for proper formatting of the code.
Consistent formatting ensures easier reading of the code.
Thus, we ensure that all of JabRef's code follows the same code style.

Binaries are created using [gradle](https://gradle.org).
In case of an internal pull request, they are uploaded to <https://builds.jabref.org>.
These binaries are created without any checks to have them available as quickly as possible, even if the localization or some fetchers are broken.
You can fnd the deployment workflow runs at: <https://github.com/JabRef/jabref/actions?workflow=Deployment>.

## Branches

The [main](https://github.com/JabRef/jabref/tree/main) branch is the main development line ("trunk") and is intended to incorporate fixes and improvements as soon as possible and to move JabRef forward to modern technologies such as the latest Java version.

Other branches are used for working on and discussing improvements with the help of [pull requests](https://github.com/JabRef/jabref/pulls). One can see the binaries of each branch at [https://builds.jabref.org/](https://builds.jabref.org). Releases mark milestones and are based on the `main` branch at that point in time.

## How JabRef acquires contributors

* We participate in [Hacktoberfest](https://www.hacktoberfest.com).
* We participate in [Google Summer of Code](https://developers.google.com/open-source/gsoc/).
* We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings.
  Please head to [Teaching](teaching.md) for more information on using JabRef as a teaching object and on previous courses where JabRef was used.

## High-level architecture

In JabRef's code structure,

The `model` package encompasses the most important data structures (`BibDatases`, `BibEntries`, `Events`, and related aspects) and has minimal logic attached.
The `logic` package is responsible for business logic such as reading/writing/importing/exporting and manipulating the `model`, and it is structured often as an API the `gui` can call and use.
Only the `gui` knows the user and their preferences and can interact with them to help them solving tasks.
For each layer, we form packages according to their responsibility, i.e., vertical structuring.
The `model` classes should have no dependencies to other classes of JabRef and the `logic` classes should only depend on `model` classes.
The `cli` package bundles classes that are responsible for JabRef's command line interface.
The `preferences` package represents all information customizable by a user for their personal needs.

We use an event bus to publish events from the `model` to the other layers.
This allows us to keep the architecture but still react upon changes within the core in the outer layers.
Note that we are currently switching to JavaFX's observables, as we aim for a stronger coupling to the data producers.

### Package Structure

Permitted dependencies in our architecture are:

```monospaced
gui --> logic --> model
gui ------------> model
gui ------------> preferences
gui ------------> cli
gui ------------> global classes

logic ------------> model

global classes ------------> everywhere

cli ------------> model
cli ------------> logic
cli ------------> global classes
cli ------------> preferences
```

All packages and classes which are currently not part of these packages (we are still in the process of structuring) are considered as gui classes from a dependency standpoint.

### Most Important Classes and their Relation

Both GUI and CLI are started via the `JabRefMain` which, in turn, calls `JabRef` which then decides whether the GUI (`JabRefFrame`) or the CLI (`JabRefCLI` and a lot of code in `JabRef`) will be started. The `JabRefFrame` represents the Window which contains a `SidePane` on the left used for the fetchers/groups. Each tab is a `BasePanel` which has a `SearchBar` at the top, a `MainTable` at the center and a `PreviewPanel` or an `EntryEditor` at the bottom. Any right click on the `MainTable` is handled by the `RightClickMenu`. Each `BasePanel` holds a `BibDatabaseContext` consisting of a `BibDatabase` and the `MetaData` (which is relevant data of the currently focussed database). A `BibDatabase` has a list of `BibEntries`. Each `BibEntry` has an ID, a citation key and a key/value store for the fields with their values. Interpreted data (such as the type or the file field) is stored in the `TypedBibentry` type. The user can change the `JabRefPreferences` through the `PreferencesDialog`.

## Architectural Decision Records

JabRef collects core architecture decisions using "Architectural Decision Records".
They are available at <https://devdocs.jabref.org/decisions/>.

For new ADRs, please use [adr-template.md](https://github.com/JabRef/jabref/blob/main/docs/decisions/adr-template.md) as basis.
More information on MADR is available at <https://adr.github.io/madr/>.
General information about architectural decision records is available at <https://adr.github.io/>.

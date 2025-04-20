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
* We review each external pull request by at least two [JabRef Core Developers](https://github.com/JabRef/jabref/blob/main/MAINTAINERS).

Read on about our automated quality checks at [Code Quality](../code-howtos/code-quality.md).

## Continuous integration

JabRef has automatic checks using GitHub actions in place.
One of them is checking for the formatting of the code.
Consistent formatting ensures more easy reading of the code.
Thus, we pay attention that JabRef's code follows the same code style.

Binaries are created using [gradle](https://gradle.org) and are uploaded to [https://builds.jabref.org](https://builds.jabref.org).
These binaries are created without any checks to have them available as quickly as possible, even if the localization or some fetchers are broken.
Deep link to the action: [https://github.com/JabRef/jabref/actions?workflow=Deployment](https://github.com/JabRef/jabref/actions?workflow=Deployment).

## Branches

The branch [main](https://github.com/JabRef/jabref/tree/main) is the main development line and is intended to incorporate fixes and improvements as soon as possible and to move JabRef forward to modern technologies such as the latest Java version.

Other branches are used for discussing improvements with the help of [pull requests](https://github.com/JabRef/jabref/pulls). One can see the binaries of each branch at [https://builds.jabref.org/](https://builds.jabref.org). Releases mark milestones and are based on the `main` branch at a point in time.

## How JabRef acquires contributors

* We participate in [Hacktoberfest](https://www.hacktoberfest.com).
* We participate in [Google Summer of Code](https://developers.google.com/open-source/gsoc/).
* We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings.
  Please head to [Teaching](teaching.md) for more information on using JabRef as a teaching object and on previous courses where JabRef was used.

## High-level architecture

The `model` represents the most important data structures (`BibDatases`, `BibEntries`, `Events`, and related aspects) and has only a little bit of logic attached.
The `logic` is responsible for reading/writing/importing/exporting and manipulating the `model`, and it is structured often as an API the `gui` can call and use.
Only the `gui` knows the user and their preferences and can interact with them to help them solving tasks.
For each layer, we form packages according to their responsibility, i.e., vertical structuring.
The `model` should have no dependencies to other classes of JabRef and the `logic` should only depend on `model` classes.
The `cli` package bundles classes that are responsible for JabRef's command line interface.
The `preferences` package represents all information customizable by a user for her personal needs.

We use an event bus to publish events from the `model` to the other layers.
This allows us to keep the architecture but still react upon changes within the core in the outer layers.
Note that we are currently switching to JavaFX's observables, as this concepts seems as we aim for a stronger coupling to the data producers.

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

All packages and classes which are currently not part of these packages (we are still in the process of structuring) are considered as gui classes from a dependency stand of view.

### Most Important Classes and their Relation

Both GUI and CLI are started via the `JabRefMain` which will in turn call `JabRef` which then decides whether the GUI (`JabRefFrame`) or the CLI (`JabRefCLI` and a lot of code in `JabRef`) will be started. The `JabRefFrame` represents the Window which contains a `SidePane` on the left used for the fetchers/groups Each tab is a `BasePanel` which has a `SearchBar` at the top, a `MainTable` at the center and a `PreviewPanel` or an `EntryEditor` at the bottom. Any right click on the `MainTable` is handled by the `RightClickMenu`. Each `BasePanel` holds a `BibDatabaseContext` consisting of a `BibDatabase` and the `MetaData`, which are the only relevant data of the currently shown database. A `BibDatabase` has a list of `BibEntries`. Each `BibEntry` has an ID, a citation key and a key/value store for the fields with their values. Interpreted data (such as the type or the file field) is stored in the `TypedBibentry` type. The user can change the `JabRefPreferences` through the `PreferencesDialog`.

## Architectural Decision Records

JabRef collects core architecture decision using "Architectural Decision Records".
They are available at <https://devdocs.jabref.org/decisions/>.

For new ADRs, please use [adr-template.md](https://github.com/JabRef/jabref/blob/main/docs/decisions/adr-template.md) as basis.
More information on MADR is available at <https://adr.github.io/madr/>.
General information about architectural decision records is available at <https://adr.github.io/>.

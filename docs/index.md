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

## Next reading

Please head to [Architecture and Components](architecture-and-components.md) to read on the high-level architecture and JabRef's (logical) components.

# Offer jabref-nightly as chocolatey pacakge

## Context and Problem Statement

[@OlafHaag](https://github.com/OlafHaag) maintains the [JabRef chocolatey package](https://chocolatey.org/packages/JabRef).
It offers the stable version and the beta versions as "pre releases".

JabRef builds on each push into `master` an new version, which gets published at <https://builds.jabref.org/master/>.
This offers the possibility for users to test the current development snapshots.
Having the chocolatey mechansim also for development snapshots in place, enables users to automatically update the development snapshots.

## Considered Options

* Offer `jabref-nightly` as chocolatey package
* Use `--prerelease` of the `jabref` package as channel for nightly builds

## Decision Outcome

Chosen option: "Offer `jabref-nightly` as chocolatey package", because we want to distingish between development versions, beta versions, and releases. Thus, we need three different distributions. When using the chocolatey eco system, there are two different flavours possible for each package.
This is also in line with [vlc-nightly](https://chocolatey.org/packages/vlc-nightly) chocolatey package.

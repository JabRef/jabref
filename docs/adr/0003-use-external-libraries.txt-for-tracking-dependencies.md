# Use external-libraries.txt for tracking dependencies

Technical Story: https://github.com/JabRef/jabref/pull/3906#issuecomment-377597588 and https://github.com/JabRef/jabref/issues/3897

## Context and Problem Statement

External libraries need to be tracked, because Linux distributions have a demand on tracking down the dependencies to ensure proper licensing.
For instance, see [The main archive area of Debian](https://www.debian.org/doc/debian-policy/#the-main-archive-area) and [Dependencies](https://www.debian.org/doc/debian-policy/#dependencies).

## Decision Drivers

* Correct statements only
* Complete
* Only used libraries, not the removed ones
* Easy to maintain
* High automation

## Considered Options

* No list maintenance
* Maintain `external-libraries.txt` manually
* Use [Gradle-License-Report plugin](https://github.com/jk1/Gradle-License-Report)

## Decision Outcome

Chosen option: "Maintain `external-libraries.txt` manually", because 

- Dependencies not tracked in `build.gradle` can also be tracked
- Comments can be added
- Additional information such as the project repository can be added
- Not all libraries have complete POMs (license statement, project homepage, project repository)

Positive Consequences:
* Simple text file can be distributed

Negative consequences:
* Manual effort required to keep consistency to `build.gradle`

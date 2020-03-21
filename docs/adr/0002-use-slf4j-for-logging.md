# Use slf4j together with log4j2 for logging

## Context and Problem Statement

Up to version 4.1 JabRef uses apache-commons-logging 1.2 for logging errors and messages. However, this is not compatible with java 9 and is superseded by log4j.

## Decision Drivers

* SLF4J provides a façade for several logging frameworks, including log4j and supports already java 9
* Log4j is already defined as dependency and slf4j has already been required by a third party dependency

## Considered Alternatives

* [Log4j2](https://logging.apache.org/log4j/2.x/)
* [SLF4J with Log4j2 binding](https://logging.apache.org/log4j/2.x/maven-artifacts.html)
* [SLF4J with Logback binding](https://logback.qos.ch/)

## Decision Outcome

Chosen option: "SLF4J with Log4j2 binding", because comes out best \(see below\).

## Pros and Cons of the Options

### Log4j2

* Good, because dependency already exists
* Good, because Java 9 support since version 2.10
* Bad, because direct dependency

### SLF4J with log4j2 binding

* Good, because it only requires minimal changes to our logging infrastructure
* Good, because Apache Log4j 2 is an upgrade to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides many of the improvements available in Logback while fixing some inherent problems in Logback’s architecture.
* Good, because supports other loggers as well
* Good, because Java 9 support
* Good, because already defined
* Good, because migration tool available
* Good, because it is a façade for several loggers. Thus, the underlying implementation can easily be changed in the future.
* Bad, because logger statements require a slight different syntax

### SLF4J with Logback binding

* Good, because migration tool available
* Good, because native implementation of slf4j
* Bad, because Java 9 support only available in alpha
* Bad, because different syntax than log4j/commons logging


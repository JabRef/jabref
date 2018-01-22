# Use slf4j together with log4j2 for logging

* Up to version 4.1 JabRef uses apache-ommons-logging 1.2 for logging errors and messages. However, this is not compatible with java 9 and is superseded by log4j.
* SLF4J provides a facade for several logging frameworks, including log4j and supports already java 9
* Log4j is already defined as dependency and slf4j has already been required by a third party dependency

## Considered Alternatives

* [Log4j2](https://logging.apache.org/log4j/2.x/)
* [SLF4J with Log4j2 binding](https://logging.apache.org/log4j/2.x/maven-artifacts.html)
* [SLF4J with Logback binding](https://logback.qos.ch/)

## Decision Outcome
* We chose slf4j with log4j2 binding, because it only requires minimal changes to our logging infrastructure and it is claimed that  
> Apache Log4j 2 is an upgrade to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and      provides many of the improvements available in Logback while fixing some inherent problems in Logback’s architecture.
* Furthermore, as slf4j is a facade for several loggers, the underlying implementation can easily be changed in the future


## Pros and Cons of the Alternatives 

### Log4j2

* `+` Dependency already exists
* `+` Java 9 support since version 2.10
* `-` Direct dependency

### SLF4J with log4j2 binding

* `+` Supports other loggers as well
* `+` Java 9 support
* `+` Already defined
* `+` Migration tool available
* `-` Logger statements require a slight different syntax

### SLF4J with Logback binding

* `+` Migration tool available
* `+` Native implementation of slf4j
* `-` Java 9 support only available in alpha
* `-` Different syntax than log4j/commons logging




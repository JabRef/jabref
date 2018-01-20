# Use slf4j together with log4j2 for logging

* Currently JabRef uses apache commons logging 1.2 for logging errors and messages. However, this is not compatible with java 9 and is superseeded by log4j.
* SLF4J provides a facade for several logging frameworks, including log4j and supports already java 9
* Log4j is already defined as dependency and slf4j has already been required by a third party dependency

## Considered Alternatives

* [Log4j2](https://logging.apache.org/log4j/2.x/)
* [SLF4J](https://www.slf4j.org/)


## Decision Outcome

## Pros and Cons of the Alternatives <!-- optional -->

### *Log4j2*

* `+` *Already defined*
* `+` *Java 9 support*
* `-` *Direct dependency*
* *[...]* <!-- numbers of pros and cons can vary -->

### *SLF4J*

* `+` *Supports other loggers as well*
* `+` *Java 9 support*
* `+` *Already defined*
* `+` *Migration tool avaiable*
* `-` *Logger statements require a slight different syntax


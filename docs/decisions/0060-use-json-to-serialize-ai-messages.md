---
nav_order: 0060
parent: Decision Records
---
# Use JSON to serialize AI chat messages

<!-- dsn->req~ai.chat.entries.history-storage~1 -->

## Context and Problem Statement

We need to choose the serialization and deserialization method for AI chat messages, as the chat history is persisted.

## Decision Drivers

* The API should be simple and easy to use
* It should be easy to perform migrations
* Preferably, use methods that are already used in JabRef

## Considered Options

* Use Java native serialization
* Use JSON format
* Use XML format
* Use binary format
* Use YAML
* Use database
* Use a custom format

## Decision Outcome

Chosen option: "Use JSON format" with Jackson, because JSON is a simple and widely used format, and it is easy to use with Jackson. Jackson is already used in JabRef, and it supports polymorphic types.

However, if a database is integrated to AI features, then preferably a database will be used.

## Pros and Cons of the Options

### Use Java native serialization

* Good, because it is simple and already builtin to Java
* Bad, because it is not easily extensible
* Bad, because it is not easily migratable

### Use JSON format

* Good, because it is widely used
* Good, because it is easy to use with Jackson
* Good, because it is easy to migrate via a custom script/function
* Good, because it is already used in JabRef
* Bad, because JSON is a dynamic format
* Bad, because JSON does not utilize the space as effectively as binary formats

### Use XML format

* Good, because it has a well-defined structure
* Good, because it is easily migratable via a custom script/function
* Good, because there are some builtin libraries for XML in Java
* Bad, because it is old-fashioned
* Bad, because XML does not utilize the space as effectively as binary formats

### Use binary format

* Good, because it is highly efficient
* Neutral, because it is not easily readable
* Bad, because it is not easily migratable

### Use YAML

* Good, because it is widely used
* Good, because it is easy to migrate via a custom script/function
* Good, because it is already used in JabRef
* Bad, because YAML is a dynamic format
* Bad, because YAML does not utilize the space as effectively as binary formats

### Use database

* Good, because it is structured
* Good, because it is highly efficient
* Good, because it is (highly probably) used for other purposes as well
* Good, because it is easy to migrate
* Bad, because it is available only through a database (a custom export feature should be implemented into some other format)
* Bad, because it required running or connecting to a database

### Use a custom format

* Good, because we are in full control of the data
* Bad, because it requires a lot of effort to implement

## More information

It is hard to decide between JSON and YAML, as they both suffit well. But because JSON is more used generally, more used in AI applications, it was chosen.

Previously, Java native serialization was used, but it caused too many problems. It was a mistake that happened because of time constraints.

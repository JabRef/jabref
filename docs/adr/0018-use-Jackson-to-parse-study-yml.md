# Use Jackson to parse study.yml

## Context and Problem Statement

The study definition file is formulated as a YAML document.
To accessed the definition within JabRef this document has to be parsed.
What parser should be used to parse YAML files?

## Considered Options

* Jackson
* SnakeYAML Engine
* yamlbeans
* eo-yaml
* Own parser

## Decision Outcome

Chosen option: Jackson, because as it is a dedicated library for parsing YAML. yamlbeans also seem to be viable. They all offer similar functionality

## Pros and Cons of the Options

### Jackson

* Good, because established YAML parser library
* Good, because supports YAML 1.2
* Good, because it can parse LocalDate

### SnakeYAML Engine

* Good, because established YAML parser library
* Good, because supports YAML 1.2
* Bad, because cannot parse YAML into Java DTOs

### yamlbeans

* Good, because established YAML parser library
* Good, because [nice getting started page](https://github.com/EsotericSoftware/yamlbeans)
* Bad, because objects need to be annotated in the yaml file to be parsed into Java objects

### eo-yaml

* Good, because established YAML parser library
* Good, because supports YAML 1.2
* Bad, because cannot parse YAML into Java DTOs?

### Own parser

* Good, because easily customizable
* Bad, because high effort
* Bad, because has to be tested extensively

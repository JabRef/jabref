# Use Jackson to parse study.yml

## Context and Problem Statement

The study definition file is formulated as a YAML document.
To accessed the definition within JabRef this document has to be parsed.
What parser should be used to parse YAML files?

## Considered Options

* [Jackson](https://github.com/FasterXML/jackson-dataformat-yaml)
* [SnakeYAML Engine](https://bitbucket.org/asomov/snakeyaml)
* [yamlbeans](https://github.com/EsotericSoftware/yamlbeans)
* [eo-yaml](https://github.com/decorators-squad/eo-yaml)
* Self-written parser

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
* Bad, because cannot parse YAML into Java DTOs, only into [basic Java structures](https://bitbucket.org/asomov/snakeyaml-engine/src/master/), this then has to be assembled into DTOs

### yamlbeans

* Good, because established YAML parser library
* Good, because [nice getting started page](https://github.com/EsotericSoftware/yamlbeans)
* Bad, because objects need to be annotated in the yaml file to be parsed into Java objects

### eo-yaml

* Good, because established YAML parser library
* Good, because supports YAML 1.2
* Bad, because cannot parse YAML into Java DTOs

### Own parser

* Good, because easily customizable
* Bad, because high effort
* Bad, because has to be tested extensively

## Links

* [Winery's ADR-0009](https://github.com/eclipse/winery/blob/master/docs/adr/0009-manual-tosca-yaml-serialisation.md)
* [Winery's ADR-0010](https://github.com/eclipse/winery/blob/master/docs/adr/0010-tosca-yaml-deserialisation-using-snakeyaml.md)

---
title: Use of Dependency Injection in the Application
nav_order: 100
parent: Decision Records
---

## Context and Problem Statement

JabRef uses JavaFX for its graphical interface and plain Java for its logic layer. We need a consistent and maintainable strategy for dependency injection (DI). 

JavaFX imposes certain constraints on controllers and view models, especially regarding how FXML loads classes. At the same time, the rest of the app benefits from clear, explicit, and testable construction patterns. However, sometimes there are too much arguments in a constructor. The question is how to balance JavaFX requirements with the architectural clarity of constructor-based DI.

## Decision Drivers

* JavaFX requires empty constructors for classes instantiated through FXML (or very simple arguments like integers or text).
* JabRef prefers clarity, type safety, and ease of reasoning over magic.
* Testing the logic layer should be simple and isolated.
* DI should not add unnecessary framework complexity.

## Considered Options

* Use DI framework for everything.
* Use constructor-based DI everywhere.
* Use a mix of a DI framework and constructor-based DI.

## Decision Outcome

Chosen option: "Use a mix of a DI framework and constructor-based DI.", because this approach works with the constraints of JavaFX while preserving explicitness and testability in the rest of the system.

### Consequences

* Good, because the GUI stays compatible with FXML, empty constructors.
* Good, because the logic layer remains clean, explicit, and easy to unit-test.
* Good, because the amount of DI configuration stays minimal.
* Bad, because there are two DI styles in the codebase, increasing conceptual overhead.

### Confirmation

Compliance can be verified by code review:  
JavaFX views should not expose required constructor parameters, classes that are necessary for the view model are injected as a fields in view class.
Core logic classes should expose dependencies through constructors, with no field injection or framework-specific annotations.  

## Pros and Cons of the Options

### Use DI framework for everything

* Good, because dependency creation is centralized.
* Good, because configuration can be standardized across all components.
* Bad, because the whole application becomes more dependent on a framework and harder to test without it.

### Use constructor-based DI everywhere

* Good, because constructors clearly express dependencies.
* Good, because testing is straightforward.
* Bad, because JavaFX does not allow passing complex constructor arguments (like `Services` or `Preferences`) to view in FXML.
* Bad, because nesting views in FXML becomes harder without a DI mechanism.

### Use DI for JavaFX view models; constructor-based DI for core

* Good, because it respects JavaFX requirements.
* Good, because core components remain simple and testable.
* Good, because the DI surface area stays small.
* Bad, because the team must remember the rule boundary.

## More information

This ADR focuses on the DI approach: constructor-based or with framework. However, an ADR of the chosen DI framework is to be done.

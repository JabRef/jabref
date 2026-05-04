---
nav_order: 0056
parent: Decision Records
---
# Implementation of embeddings in JabRef

<!-- dsn->feat~ai.answer-engines.embeddings-search~1 -->

## Context and Problem Statement

JabRef needs to implement embedding models to perform Retrieval-Augmented Generation (RAG) by generating embeddings for chunks of papers. The AI ecosystem in Java is not as diverse or developed as it is in Python, which limits the available tools for this task.

We need to decide how to design this integration to balance ease of development with the constraints of a desktop application.

The features that we need are:

1. Ability to find models.
2. Ability to download models.
3. Ability to execute inference on models.

## Decision Drivers

* The approach should not require additional setup from the user side
* It should be cross-platform
* It should support a wide variety of model architectures
* It should have an easy-to-use API
* The request that embedding libraries make should be known and controlled
* We should know how and where some library downloads and stores models

## Considered Options

* Custom implementation
* Use a mix of custom implementation and an inference library
* Use a comprehensive library

## Decision Outcome

Chosen option: "Use a comprehensive library", because it is the standard approach in software engineering to rely on specialized libraries for complex tasks rather than re-implementing them. It allows us to delegate the heavy lifting of model management and inference to a dedicated tool.

### Consequences

* Good, because it reduces the maintenance burden on the JabRef team
* Good, because we do not have to implement complex inference algorithms ourselves
* Bad, because we are dependent on the external library for updates and maintenance
* Bad, because it is not easy to find such a library in Java ecosystem

## Pros and Cons of the Options

### Custom implementation

* Good, because it will just work (no strange issues with PyTorch or ONNX, etc.)
* Bad, because it is a lot of work
* Bad, because for each architecture of embedding models we would have to write code

### Use a mix of custom implementation and an inference library

In this approach we manually implement the features 1 and 2, while relying on an inference library for 3.

* Good, because complex computations are delegated to another library
* Good, because we are in full control of data (network rquests, model storage)
* Bad, because an inference library might be too scientifically centered with a complex API
* Bad, because we have to write code to find the models, which is not easy
* Bad, because the inference engine might not provide every tool needed

### Use a comprehensive library

* Good, because this doesn't require custom code
* Good, because this is the right approach from software engineering POV
* Good, because it implements all features
* Good, because we delegate responsibilities to another library
* Neutral, because there is a small number of libraries for Java for these tasks
* Bad, because it might make untraceable requests to network
* Bad, because the storage of the models might be not customizable

## More information

This ADR is highly related to [ADR 0037 - RAG Architecture Implementation](./0037-rag-architecture-implementation.md).

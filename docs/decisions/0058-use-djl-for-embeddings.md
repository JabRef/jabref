---
nav_order: 0058
parent: Decision Records
---
# Use Deep Java Library for embeddings in AI features

<!-- dsn->feat~ai.answer-engines.embeddings-search~1 -->

## Context and Problem Statement

JabRef needs to use embedding models to perform Retrieval-Augmented Generation (RAG) by generating embeddings for chunks of papers.

The Java AI ecosystem is not as diverse as the Python AI ecosystem, so the choice must be careful to ensure stability and ease of use for end users.

Which library to choose?

## Decision Drivers

* The library should not require additional setup from the user side
* It should be cross-platform
* It should support a wide variety of model architectures
* It should have an easy-to-use API
* The request that the library makes should be known and controlled
* We should know how and where the library downloads and stores models

## Considered Options

* LangChain4j
* ONNX Runtime
* Deep Java Library (DJL)
* DeepLearning4j

## Decision Outcome

Chosen option: "Deep Java Library (DJL)", because it satisfies all our requirements for an all-in-one solution that handles model management and inference.

However, users have reported problems with the PyTorch engine integration and unstable behavior. Moreover, its API is a bit complex.

### Consequences

* Good, because it has an API to show available models
* Good, because it handles model downloading automatically
* Neutral, because the API is complex
* Bad, because users have reported problems with the PyTorch engine integration and unstable behavior

## Pros and Cons of the Options

### LangChain4j

* Good, because it offers a high-level abstraction for LLM workflows
* Neutral, because it actually wraps other libraries like DJL or ONNX Runtime for the embeddings
* Bad, because it is a general LLM framework

### ONNX Runtime

* Good, because it is fast and efficient
* Bad, because it is a low-level inference engine and does not provide model management or downloading features out of the box
* Bad, because it supplies all binaries for different platforms at once and also supply debugging symbols, which makes it larger than necessary (see [this issue in LangChain4j repository](https://github.com/langchain4j/langchain4j/issues/1492) and [this issue in ONNX repository](https://github.com/langchain4j/langchain4j/issues/1492))

### Deep Java Library (DJL)

* Good, because it supports multiple engines including PyTorch and ONNX
* Good, because it has a built-in model zoo for downloading models
* Neutral, because its API is a bit complex
* Bad, because of reported stability issues with certain engines

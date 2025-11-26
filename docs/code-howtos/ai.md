---
parent: Code Howtos
---

# AI

The AI feature of JabRef is built on [LangChain4j](https://github.com/langchain4j/langchain4j) and [Deep Java Library](https://djl.ai/).

## Architectural Decisions

See [ADR-0037](../decisions/0037-rag-architecture-implementation.md) for the decision regarding the RAG infrastructure.

## Feature "Chat with PDF(s)"

This is implemented mainly in the class [org.jabref.logic.ai.chatting.AiChatLogic].
From there, one will find preferences and other required infrastructure.

## Feature "Summarize PDF(s)"

This is implemented in the class [org.jabref.logic.ai.summarization.GenerateSummaryTask].

## Feature "BibTeX from Reference Text"

The general interface is [org.jabref.logic.importer.plaincitation.PlainCitationParser].
The class implementing it using AI is [org.jabref.logic.importer.plaincitation.LlmPlainCitationParser].

## Feature "Reference Extractor"

Extracts the list of references (Section "References") from the last page of the PDF to a List of BibEntry.

The general interface is [org.jabref.logic.importer.fileformat.pdf.BibliographyFromPdfImporter].
The class implementing it using AI is [org.jabref.logic.importer.plaincitation.LlmPlainCitationParser].

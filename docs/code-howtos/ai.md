---
parent: Code Howtos
---

# AI

The JabRef has next AI features:

- Chatting with entries,
- Chatting with groups,
- Summarization of entries,
- Parsing of plain citations using LLMs
- Extracting "References" section from PDFs with the help of LLMs.

The features are built on [LangChain4j](https://github.com/langchain4j/langchain4j) and [Deep Java Library](https://djl.ai/).

## Architectural Decisions

See [ADR-0037](./../decisions/0037-rag-architecture-implementation.md) for the decision regarding the RAG infrastructure.

The [ADR-0032](./../decisions/0032-store-chats-in-local-user-folder.md) and [ADR-0033](./../decisions/0033-store-chats-in-mvstore.md) are important ones, because they explain the decisions regarding the storage of AI artifacts (summaries, chat histories, embeddings, etc.).

## Requirements

See [the requirements page of AI features](./../requirements/ai.md).

## Features

### Feature "Chat with PDF(s)"

The interface with all of the features (chat history, regeneration, follow up questions, etc.) is implemented in the class [org.jabref.gui.ai.chat.AiChatView]. From there, one will find preferences and other required infrastructure.

The RAG entry point is located in [org.jabref.logic.ai.chatting.tasks.GenerateRagResponseTask].

### Feature "Summarize PDF(s)"

This is implemented in the class [org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask].

### Feature "BibTeX from Reference Text"

The general interface is [org.jabref.logic.importer.plaincitation.PlainCitationParser].
The class implementing it using AI is [org.jabref.logic.importer.plaincitation.LlmPlainCitationParser].

### Feature "Reference Extractor"

Extracts the list of references (Section ["References"](../glossary/references.md)) from the last page of the PDF to a List of BibEntry.

The general interface is [org.jabref.logic.importer.fileformat.pdf.BibliographyFromPdfImporter].
The class implementing it using AI is [org.jabref.logic.importer.plaincitation.LlmPlainCitationParser].

## Code organization

As every JabRef feature, AI is divided into 3 layers: GUI, logic, and model. Inside the `logic` package the AI code is split by feature (each feature has its own package).

The GUI code strongly follows [MVVM pattern](./javafx.md). Though, the GUI code is a bit complicated as:

1. Most of the core GUI components (chat and summary components) are designed as a state machine. Typical states include: loading, presenting the result, error, etc.
2. These core GUI components are also made that way so it would be possible to rebind them to another `BibEntry`. For the details, take a look at the section [How to add a new AI feature](## How to add a new AI feature).

## Internal model (v2)

There are 3 core models in the AI features:

1. Chat history.
2. Summaries.
3. Embeddings.
4. Fully ingested documents.

The code strictly follows the repository pattern, where an interface is created to access the internal storage for the purpose of abstraction. At the moment of writing, all of these models are implemented by using the [`MVStore`](https://www.h2database.com/html/mvstore.html). For the details of this decisions take a look at the [ADR 0033](./../decisions/0033-store-chats-in-mvstore.md). A helper class was made `MVStoreBase` so that it would be possible to use an in-memory `MVStore` in case there are some errors while opening on-disk storage.

A note needs to be made for embeddings: the embeddings storage is also implementing the internal LangChain4j interface for embeddings so that it could be used in LangChain4j algorithms. Additionally, there is a "fully ingested" repository, which simply contains a "list" of files that were fully ingested. This helps with checking if a file needs to be ingested or not, as there is no 1 to 1 correspondense with embeddings to file (which is many to one).

Because JabRef is not build around one global database, but rather it is a `.bib` file editor, a problem of identifying a `BibEntry` arose and it was solved in a somewhat complicated way:

- In order to uniquely identify a library, an "AI library ID" was introduced (as a metadata field), which is just a UUID. An alternative would be to use the library path, but if the library moves, the path changes, but AI library ID is not.
- In order to uniquely identify an entry, the citation key is used, but only if it is non-empty and unique.
- In some cases (that arise potentially often), the conditions above are not met (for example, a library is not saved - it does not have a path, or an entry does not have a citation key), however user is actively working on an entry. In this case the AI features have an *in-memory cache layer*. So whenever a chat or a summary is created for an entry, it is firstly interacted with the in-memory storage layer. The cache is flushed to the on-disk storage at the close of the JabRef.
- In order to uniquely identify a file, we use the file hash. An alternative would be to use the file path, but the file could be moved, or defined by a relative path. This is also useful when several libraries cite the same paper, and instead of ingesting

## [OLD] Internal model (v1)

The model v1 differs from v2 by:

1. Fields of the chat messages and summaries were differently organized in the `MVStore`.
2. A `LinkedFile#getLink()` was used to identify a file.

To migrate from v1 to v2, the classes `ChatHistoryMigrationV1` and `SummariesMigrationV2` were made.

## How to add a new AI feature

This section describes the standard pattern used for AI features. If should follow a similar plan:

1. Define the model of the artifact of your feature (for example, for summarization it is an AI summary, for chatting they are chat messages and chat history).
2. Define a repository interface (e.g. `SummaryRepository`, `ChatHistoryRepository`) and implement an `MVStore` implementation using the [org.jabref.logic.ai.util.MVStoreBase].
3. Define a logic class in the `logic` package: either a task (e.g. `GenerateSummaryTask` or a utility class for performing an AI feature. It is recommended to make it "without side-effects" (it does not change or write anything in the system). Firstly, this will help in testing the class, and, secondly, the storage is typically hanlded in *in-memory cache* layer, that will be discussed next.
4. Make an in-memory cache storage layer for your feature that has a RAM map between a `BibEntry` (or a group, or some other object that your artifact is linked to) and your model. Sometimes this can be omitted (for example, embeddings do not have the in-memory cache and always use a repository), but generally it is made in order to always have access to the AI feature even if some precondition is not satisfied (for example, storing chat history and summmaries requires that there is a database path and a non-empty unique citation key, but in-memory layer allows to work with them as is). At the close of JabRef (or a library) the in-memory cache layer will check the preconditions and only then write the data to the repository.
5. Make a `TaskAggregator` class. This is needed in order to be able to switch a component between entries and to deduplicate the tasks. So whenever you want to generate the artifact of your feature, you need to always communicate to the `TaskAggregator` class which will either create a new task or give you an already running one. The `TaskAggregator` also connects the results to the in-memory cache.

The next points are targeted to the GUI of the feature:

1. Design a component using the MVVM pattern. You need to write the interface in the FXML, then write a controller `Ai<Feature>View` and a view-model `Ai<Feature>ViewModel`.
2. A typical AI component will be a state machine: first and foremost, check if the AI features are enabled in JabRef (which equals to accepting a privacy policy of AI features). If not, then you must ensure that you component does nothing. To show the privacy policy banner, there is a dedicated component [org.jabref.gui.ai.AiPrivacyNoticeView]. The next states typically envolve checking some preconditions (for example, you can not summarize an entry, if it does not have linked files), and the final is the working state. You might find the [org.jabref.gui.util.BindingsHelper#bindEnum] useful.
3. The entry editor tabs are designed to be switchable (rebound to some other `BibEntry`), so you can have an `entryProperty` and whenver it is changed, the state machine of the component is rerun.
4. When you read an artifact for an entry (or a group, or other entity that is linked to your AI feature), the look-up should be made in 3 steps: look into the repository, look in to the in-memory cache, and only then contact the `TaskAggregator` to start a new generation task.

---
nav_order: 86
parent: Decision Records
---

# Offer JabRef to external AI agents via an HTTP endpoint with a skill and via MCP
`adr~external-ai-agent-access~1`

## Context and Problem Statement

JabRef generates embeddings for the PDFs linked to the entries of a library ([ADR-0035](0035-generate-embeddings-online.md), [ADR-0037](0037-rag-architecture-implementation.md), [ADR-0058](0058-use-djl-for-embeddings.md)).
Only JabRef's own AI chat can use them.
An external AI agent (Claude Code, Cursor, Claude Desktop, ...) working on a paper next to the library cannot ask "which of my PDFs say something about X?":
`jabkit` has no AI command, `jabsrv` has no AI resource, and the embeddings store is locked by the running JabRef.

There are two directions, which this ADR keeps apart:

* JabRef as **server**: external agents call JabRef. This is decided here.
* JabRef as **client**: JabRef's AI chat calls tools. This is the view of [issue 1164](https://github.com/JabRef/jabref-issue-melting-pot/issues/1164) and is **not** decided here.

Issue 1164 raises a concern which applies to both directions: MCP tool definitions consume context window, 60k to 300k tokens for large tool libraries.
It proposes progressive disclosure as strategy to save tokens.

How does JabRef offer its functionality - first: semantic search over the embeddings of linked PDFs - to external AI agents?

## Decision Drivers

* Works while the JabRef GUI is running, since that is when the user works with the library.
* The query has to be embedded with the same model (and the same input prefix) as the stored chunks.
* Low context-window cost for the agent (issue 1164).
* Reaches many agents, not only one vendor's.
* Agents do not depend on JabRef's internal storage format.
* Full-text chunks of the user's PDFs do not leave the machine unnoticed.

## Considered Options

* Skill and HTTP endpoint in `jabsrv`
* MCP server in `jabsrv`
* `jabkit` command reading the embeddings store
* External reader of the embeddings store
* Other tool protocols (LTP, TCP)

## Decision Outcome

Chosen option: "Skill and HTTP endpoint in `jabsrv`" together with "MCP server in `jabsrv`", because both run inside the JabRef which already holds the embeddings store and the embedding model, the skill has near-zero context cost, and MCP reaches the agents which cannot use skills.

Both are thin layers over one service, which wraps `EmbeddingsSearchResponseEngine`.
The realization has two steps:

1. HTTP endpoint (`POST libraries/{id}/ai/search`: query, maximum number of results, minimum score → citation key, page, score, chunk text) and a skill in `skills/users/` calling it.
2. MCP server on the same Grizzly server (`/mcp`), in a separate pull request, closing [issue 931](https://github.com/JabRef/jabref-issue-melting-pot/issues/931).

Constraints for both:

* Available only if the HTTP server and the AI features are enabled.
* A search triggers the ingestion of the linked files which are not yet ingested. It does not wait for it, since ingestion can take minutes: the response contains the results from what is ingested and reports the linked files which are still pending.
* The MCP server offers a small, fixed set of tools (semantic search, library search, get entry) and uses the official [MCP Java SDK](https://modelcontextprotocol.io/sdk/java/mcp-server). This is the answer to the context-window concern of issue 1164: the cost grows with number and size of the tool definitions, so the set is kept small.
* MCP is spoken over HTTP inside the running GUI, not over stdio.

### Consequences

* Good, because no second process has to open the locked store or load the embedding model.
* Good, because the skill loads only its name and description until it is used.
* Good, because MCP clients discover the tools without the user installing a skill.
* Good, because endpoint and MCP tool cannot drift apart: they share one service.
* Bad, because nothing works headless: the GUI has to run with the HTTP server enabled.
* Bad, because full-text chunks of PDFs become readable for every local process, and - as long as `CORSFilter` answers `Access-Control-Allow-Origin: *` - for every web page open in the user's browser.
* Bad, because two interfaces have to be documented and kept stable.

### Confirmation

A `jabsrv` test calls the endpoint with a filled embeddings store and checks citation key, page and chunk text of the result.
The service is linked to this decision by the OpenFastTrace tag `[impl->adr~external-ai-agent-access~1]`; `Needs: impl` is added to this ADR together with step 1.

## Pros and Cons of the Options

### Skill and HTTP endpoint in `jabsrv`

* Good, because skills are progressive disclosure: only name and description are in the context until the skill is used.
* Good, because the endpoint is usable by anything speaking HTTP (`curl`, scripts, the browser extension).
* Good, because it extends the existing resource tree (`libraries/{id}/...`).
* Bad, because the user has to install the skill.
* Bad, because skills are not supported by all agents.
* Bad, because it needs the running GUI.

### MCP server in `jabsrv`

* Good, because it reaches Claude Desktop, Cursor, VS Code, and other MCP clients.
* Good, because tools are discovered by the client; nothing to install besides the server URL.
* Good, because MCP also offers "resources" and "prompts", which JabRef can use later.
* Neutral, because some clients load tool schemas on demand, which removes the context cost there.
* Bad, because tool definitions cost context in the other clients. Kept low by the small tool set.
* Bad, because it adds a dependency (MCP Java SDK).
* Bad, because it needs the running GUI.

### `jabkit` command reading the embeddings store

* Good, because it works headless and fits the existing skills, which all build on `jabkit`.
* Bad, because the store is exclusively locked while the GUI runs.
* Bad, because each call loads the embedding model in a new process.

Deferred; it can be added on top of the same service once there is a need for headless use.

### External reader of the embeddings store

* Good, because JabRef does not need to be changed.
* Bad, because of the same lock and model problems as the `jabkit` command.
* Bad, because it couples agents to the internal storage format ([ADR-0033](0033-store-chats-in-mvstore.md) for the MVStore choice), which then cannot change freely.

### Other tool protocols (LTP, TCP)

Issue 1164 names the Lazy Tool Protocol and the Tool Call Protocol as alternatives to MCP.

* Good, because they are designed to save context.
* Bad, because no support in the targeted agents is known. This was not verified in depth.

The other alternatives of issue 1164 (tool calling via the chat template or the inference framework) concern JabRef as client and are out of scope.

## More Information

* Whether JabRef's AI chat uses MCP as a client is a separate decision.
* `jabsrv` listens on localhost only. Restricting the allowed origins of the AI resource is to be settled in step 1.
* Related: [ADR-0028](0028-http-return-bibtex-string.md), [ADR-0062](0062-separate-jabkit-commands-and-services.md).

<!-- markdownlint-disable-file MD022 -->

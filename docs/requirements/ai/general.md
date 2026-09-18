---
parent: AI
grand_parent: Requirements
---

# General requirements for AI features

## Restart is required when AI features are enabled or disabled
`req~ai.general.enabling.restart~1`

Rationale: AI features are tightly coupled to the library data model and require initialization of multiple subsystems (e.g., embedding models, indexing pipelines, and migration of existing data). Because enabling or disabling AI changes global application state and these components cannot be safely reconfigured during runtime, requiring a restart is the simplest and most reliable way to ensure all subsystems are initialized or shut down correctly.

Needs: impl

<!-- markdownlint-disable-file MD022 -->

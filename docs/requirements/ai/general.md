---
parent: AI
grand_parent: Requirements
---

# General requirements for AI features

## JabRef must require restart when AI features are enabled or disabled
`req~ai.general.enabling.restart~1`

JabRef prompts the user to restart the application when enabling or disabling AI features in preferences.

Rationale:

AI features are tightly coupled to the library data model and require initialization of multiple subsystems (such as embedding models, indexing pipelines, and migration of existing data). Because enabling or disabling AI changes global application state and these components cannot be safely reconfigured during runtime, requiring a restart ensures that all subsystems are initialized or shut down reliably.

Needs: impl

<!-- markdownlint-disable-file MD022 -->

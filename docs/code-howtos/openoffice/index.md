---
parent: Code Howtos
nav_order: 12
has_children: true
---
# The LibreOffice Panel

Start with the current [architecture overview](overview.md) and [citation ordering guide](order-of-appearance.md).
The [compatibility requirements](../../requirements/openoffice.md) and [ADR-0084](../../decisions/0084-canonical-writer-citations.md) describe canonical marks, next-CSL-insertion canonical/Zotero conversion, one-way legacy JStyle migration on insertion outside native Zotero mode, and the unsupported legacy-format boundary.
Bibliography synchronization remains separate: disabled synchronization leaves all bibliography sections unchanged, while enabled synchronization updates only the target mode's managed bibliography.
The [package layout and ownership guide](code-reorganization.md) maps the current source packages and entry points.
The [problems page](problems.md) records historical behavior rather than the current storage contract.

---
parent: Code Howtos
nav_order: 12
has_children: true
---
# The LibreOffice Panel

Start with the [architecture overview](overview.md) and [citation ordering guide](order-of-appearance.md).
JStyle and BST use canonical JabRef citations; CSL follows the saved Zotero compatibility preference, which is preserved across family switches.
The [compatibility requirements](../../requirements/openoffice.md) and [ADR-0084](../../decisions/0084-canonical-writer-citations.md) describe canonical marks, next-CSL-insertion canonical/Zotero conversion, one-way legacy JStyle migration on insertion outside native Zotero mode, and the unsupported legacy-format boundary.
Conversion supports homogeneous canonical `JABREF_<keys> <TYPE> <uid>`, supported legacy `JR_cite`, or native Zotero source documents, not mixed-format repair.
On each insertion, fresh detection uses only the first recognized reference-mark name returned by Writer, skipping unrelated marks and unsupported `CID_n` marks without scanning for later mismatches; no recognized name means no conversion.
UNO name enumeration is not guaranteed sorted or in physical document order; enumeration order is immaterial for homogeneous documents.
After detection triggers conversion, preparation reads the citation groups and renders the proposed set before mutation.
Canonical/native conversion is lossy and intended for the final format, not a lossless round trip; see [Limitations](overview.md#limitations) and [Undo/rollback limits](overview.md#compatibility-boundary).
Native Zotero mode does not migrate legacy JStyle input, and JStyle/BST insertion does not convert native Zotero input.
Bibliography synchronization is separate: disabled synchronization leaves all bibliography sections unchanged, while enabled synchronization updates only the target mode's managed bibliography.
The [package layout and ownership guide](code-reorganization.md) maps the current source packages and entry points.
The [problems page](problems.md) records historical behavior rather than the current storage contract.

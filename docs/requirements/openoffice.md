---
parent: Requirements
---
# OpenOffice/LibreOffice integration

## Canonical Writer citations must remain reusable across JStyle, CSL, and BST styles
`req~openoffice.cross-family-citations~1`

Outside effective native Zotero mode, a document containing canonical JabRef citations can change style families through a refresh.
Native mode is effective only when a CSL style is selected and the saved Zotero compatibility flag is enabled.
JStyle and BST always use canonical mode without clearing that flag; returning to CSL restores the saved choice.
Citation keys, mark identities, stored citation types, and per-citation page information survive the change, including save/reopen between changes.
A style can approximate an unsupported stored citation type without replacing that type; returning to the original family restores its presentation.

Related issue: <https://github.com/JabRef/jabref/issues/14962>.

Needs: impl, utest

## The canonical Writer integration must leave legacy JabRef citations unmanaged outside supported JStyle migration
`req~openoffice.legacy-marks-unmanaged~1`

Legacy JStyle marks named `JR_cite*` and legacy CSL/BST marks containing `CID_n` are excluded from managed citations and export-cited results.
Their existing citation text, mark names, and page-info properties are not converted or rewritten except during the insertion-triggered JStyle migration specified below.
Legacy `CID_n` marks remain unmanaged in every mode.
Zotero's own mark format belongs to a separate CSL-only compatibility strategy, not to this legacy JabRef format policy.

Needs: impl, utest

## The next CSL citation insertion must convert managed citations to the selected compatibility format
`req~openoffice.next-citation-format-conversion~1`

When the first recognized citation reference-mark name returned by Writer identifies canonical JabRef or native Zotero format that differs from the selected Zotero compatibility mode, the next CSL citation insertion shows the existing "Reference mark format" / "Converting references to selected format" warning before conversion.
Opening a document, toggling the preference, selecting a style, refreshing, exporting cited entries, and inserting with JStyle or BST do not trigger canonical ↔ native Zotero conversion.
On each insertion, detection reads fresh reference-mark names, including after reconnecting, and uses only the first recognized name, skipping unrelated marks and unsupported `CID_n` marks.
Recognized formats are canonical `JABREF_<keys> <TYPE> <uid>`, supported legacy `JR_cite` names, and native Zotero citation names.
It does not scan for later mismatching formats; no recognized name means no conversion.
Conversion is supported for homogeneous source documents; mixed-source conversion and mixed-format repair are unsupported.
UNO does not guarantee sorted name enumeration, so the first returned recognized name is not necessarily first in physical document order; enumeration order is immaterial for homogeneous documents.
Legacy `JR_cite*` marks participate only in the separate one-way JStyle migration below; conversion targeting native Zotero format leaves them unchanged.
Legacy `CID_n` marks remain unmanaged and unchanged.

After detection triggers conversion, preparation reads citation groups and validates the proposed write.
The converted groups, pending citation, and any target-mode bibliography requested by normal synchronization are rendered once before document mutation.
Native-to-canonical conversion and native generation require resolved sources; only legacy-only canonical migration permits unresolved entries as specified below.
Preparation failures, including invalid metadata, required but unresolved sources, protected ranges, unsuitable custom-property types or attributes, and rendering failures, leave marks, properties, text, and undo history unchanged.
Conversion requires an available, unlocked Writer undo manager and its own root undo context; nesting inside another undo operation is rejected before document writes.
Successful conversion and insertion form one Undo/Redo step covering identities, formatted text, any normally synchronized target-mode bibliography, page-info changes, and prepared applied-style properties.
Only conversion targeting native mode prepares Zotero preference changes; canonical CSL operations and conversions leave `ZOTERO_PREF` XML untouched while still updating JabRef's applied style.
Native-mode insertion or refresh updates those native preferences.
An ordinary application failure is rolled back only after closing and verifying ownership of that context, without undoing earlier user actions.
Beginning the write transaction can clear pre-existing redo history even when document state is subsequently restored by rollback.
Global disconnects, lost ownership, and failed rollbacks produce a distinct recovery error; this is not a universal rollback guarantee.
Ordinary updates retain raw style-property setters outside the conversion-specific undo contract.

Canonical/native conversion is lossy and intended to select a final format, not provide a lossless round trip.
Only target-format citation state is retained; no persistent source-format record supports restoration on reverse conversion.
Canonical → native conversion drops the original canonical uid and per-citation page information, mapping `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY` to normal visible citations.
Native → canonical conversion generates fresh canonical uids and only normal or narrative types.
Native JSON locators, labels, prefixes, suffixes, author-only flags, unknown fields, item IDs, and native Zotero URI links are not carried into canonical storage or restored by reverse conversion; native output rebuilds JSON from the JabRef library.
Native modifiers do not by themselves prevent conversion; the group-level mapping is specified below.
Immediate Writer Undo/Redo and owned failure rollback can restore the transaction while undo history is available, subject to the recovery limits above; there is no restoration guarantee after reopening.
These conversion losses do not apply to same-format canonical style changes or ordinary native insertion and refresh.

Conversion changes citation marks only; it does not convert, rename, consolidate, or remove bibliography sections, or reject citations based on bibliography settings.
With synchronization on insertion disabled, every existing bibliography section's name, text, and properties stay unchanged, and no bibliography is created.
With synchronization enabled and a style that has a bibliography, normal handling updates or creates only the target mode's managed bibliography; other-format sections stay unchanged and may coexist with it.

Needs: impl, utest

## The next citation insertion outside native Zotero mode must migrate supported legacy JStyle citations to canonical marks
`req~openoffice.legacy-jstyle-migration~1`

On the next insertion, migrate a homogeneous supported legacy JStyle document when the first recognized reference-mark name returned by Writer identifies `JR_cite` format and the effective mode is canonical: always for JStyle and BST, and for CSL when the saved Zotero compatibility setting is off.
Use the fresh first-recognized-name detection policy above, including after reconnecting, and show the existing "Reference mark format" / "Converting references to selected format" warning before mutation.
Connecting, refreshing, exporting, selecting a style, or changing the compatibility preference alone does not migrate legacy marks.
Insertion or conversion targeting native Zotero format leaves `JR_cite` marks unchanged; `CID_n` remains unmanaged in every mode.

Migration preparation reads legacy groups and includes the pending citation.
Mixed-source conversion is unsupported.
Legacy-only canonical migration permits unresolved entries with ordinary unresolved-key placeholders and the successful-insertion warning.
Opening the missing libraries and refreshing restores resolved output without reinserting citations.
Supported names have the form `JR_cite<digits>_<type>_<comma-separated keys>`, with an optional empty digit sequence and type `1` through `6`; keys must satisfy canonical validation.
Map legacy numeric types `1` through `6` to `NORMAL`, `IN_TEXT`, `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY`, respectively.
Preserve storage-order keys and assign each migrated group a fresh stable uid.
Move the legacy page property, whose name is the whole legacy mark name, to `JabRef_page_info:<uid>:<lastStorageIndex>`, belonging to the group's last citation in storage order.
Replace legacy marks with canonical marks using invisible boundaries; migration is one-way, not a legacy output format.

Render the combined proposed citation set and any normally synchronized target bibliography once before document writes; application does not render again.
Use the existing conversion transaction, including preflight checks, root undo ownership, ordinary-failure rollback, and its documented recovery limits.
Successful insertion and migration form one Undo/Redo step covering citation text, old and new marks, identities, legacy and canonical page properties, prepared conversion properties, and any normally synchronized target bibliography.
Undo restores the original legacy marks and page properties; redo restores the prepared canonical identities and page slots.

Related issue: <https://github.com/JabRef/jabref/issues/14962>.
Related pull request: <https://github.com/JabRef/jabref/pull/17673>.

Needs: impl, utest

## Native Zotero citation updates must retain existing modifier compatibility
`req~openoffice.zotero-modifier-compatibility~1`

Ordinary native insertion and refresh must not introduce a rejection solely because existing citations contain locators, labels, prefixes, suffixes, author-only items, or mixed per-item author suppression.
These same-format operations preserve existing native JSON while updating URI links: locators and affixes are not rendered, author-only is not implemented, and any author suppression selects JabRef's group-level narrative rendering.
Native insertion supports only normal and narrative citations; native JSON determines the type, not empty or zero-width mark text.
Canonical JabRef invisible citations remain unchanged.
This is compatibility with existing behavior, not complete support for native Zotero modifier semantics.
Format conversion accepts these modifiers but has the separate data-loss contract above.

Needs: impl, utest

## Native Zotero format conversion must accept citation modifiers
`req~openoffice.zotero-conversion-modifier-compatibility~1`

Locators, labels, prefixes, suffixes, author-only items, and mixed per-item author suppression must not by themselves prevent conversion.
Any item with `suppress-author: true` maps its group to JabRef's in-text citation type, including mixed groups; otherwise the group is normal.
Other native modifiers do not add rendering semantics.
Acceptance does not imply archiving: native JSON modifiers, unknown fields, item IDs, and native Zotero URI links are not carried into canonical storage or restored on reverse conversion.
Native output rebuilds JSON from the JabRef library.

Needs: impl, utest

## Canonical CSL citations must render JabRef-defined partial citations
`req~openoffice.csl-partial-citations~1`

With Zotero compatibility mode disabled, Cite special offers the existing author-only and year-only types for CSL styles.
Author-only uses JabRef's short-author formatting, falling back to editor names and then the citation key.
Year-only uses the resolved year, falling back to the parsed date's year and then the citation key; it is a bare-year approximation, not general CSL author suppression.
These fixed formats apply to numeric, author-year, and alphanumeric styles, preserve group order and repeated entries, and escape user-provided text.
Explicit suffixes in the year field survive, but the approximation does not reproduce processor-generated disambiguation suffixes or CSL name-formatting rules.
All resolved sources still participate in document numbering and bibliography output, and stored citation types and page information remain unchanged by rendering.
Unsupported dialog choices and page information are disabled; JStyle and native Zotero capabilities remain unchanged.

Needs: impl, utest

## Writer panel must open OpenOffice/LibreOffice preferences from More settings
`req~openoffice.preferences-shortcut~1`

The Settings menu offers a localized More settings item that opens JabRef preferences with the OpenOffice/LibreOffice tab selected.
This item remains available without a Writer connection.

Needs: impl, utest

## Successful citation insertion must warn about unresolved existing citation keys
`req~openoffice.insertion-unresolved-warning~1`

After successful insertion, the controller shows an "Unresolved citations" warning listing every unresolved key returned by the engine and advising the user to open the corresponding libraries and refresh.
This applies with bibliography synchronization enabled or disabled, including legacy-only canonical migration, since insertion always refreshes citation markers.
An empty unresolved-key list produces no warning; an insertion failure retains its error dialog rather than reporting success.

Needs: impl, utest

## Canonical citation merge or separation must be undoable in one complete step
`req~openoffice.single-step-group-undo~1`

Outside effective native Zotero mode and with an available, unlocked Writer undo manager, each supported merge or separation uses one undo context for structural replacements, their final formatted output, and page-info property changes.
Undo restores the original citation-group identities and per-citation page information; redo restores the edited groups and their page information.
The proposed post-edit groups and bibliography are rendered before document mutation, so a rendering failure leaves marks, properties, text, and undo history unchanged.
Only explicit refresh performs a global orphan page-info sweep; merge, separation, insertion, and conversion still remove properties belonging to replaced or removed citations immediately.
This group-edit undo contract does not cover raw applied-style property writes in ordinary updates; the conversion-specific contract above includes its prepared property deltas.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->

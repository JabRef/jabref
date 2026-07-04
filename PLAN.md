# Plan: Re-implement the entry editor as a single scrollable field list

Issue: https://github.com/JabRef/jabref/issues/12711
Design reference: https://github.com/JabRef/jabref/issues/12711#issuecomment-2717427364
(Google-Contacts-style edit view: one long scrollable form; fields grouped with a small
icon/heading per group; "+ add <x>" buttons inline; "Show more" reveals rarely-used fields.)

Branch: `new-entry-editor` (based on `main`).

## Target UX

- **One field tab** (working name: **"Fields"** — see open questions) replaces the tabs
  "Required fields", "Optional fields", "Optional fields 2", "Deprecated fields",
  "Other fields", "Comments". Non-field tabs (Source, LaTeX citations, AI, Related articles,
  File annotations, Citations, Fulltext search, custom user tabs) stay unchanged.
  Preview: ignore here — will become a separate pane (separate work). MathSciNet: to be removed
  (separate step at the end).
- Content = **all set fields** + **all required fields (even when empty)**, in one **vertically
  scrolling list** (natural heights, NOT the current stretch-to-tab-height GridPane).
- **Grouping** within the list (section header + thin separator, Google-Contacts style):
  1. *(no header — main)*: citation key, then required fields (entry-type order), then set
     optional fields (important → detail order), then remaining set fields (incl. deprecated)
  2. **Identifiers**: set ∩ {DOI, ISBN, ISSN, EPRINT(+CLASS/TYPE), PMID, MR_NUMBER, …}
     (explicit list; `FieldProperty.IDENTIFIER` only marks DOI/EPRINT, so don't rely on it alone)
  3. **Files & Links**: set ∩ {FILE, URL, URLDATE}
  4. **Comments**: COMMENT + set `UserSpecificCommentField`s
- **Add fields** (bottom of the list, Google-style):
  - Chips/buttons for each *unset important-optional* field → click shows an empty editor
    in the list and focuses it.
  - "Show more" toggle → chips for *secondary/detail optional* fields
    (`BibEntryType#getSecondaryOptionalFields`).
  - Free form: editable ComboBox (known field names) + "Add" button; use
    `FieldFactory.parseField(String)` (issue variant 1).
  - Newly added, still-empty fields must stay visible until the user leaves the entry
    (keep a transient `userAddedFields` set in the tab; cleared on entry switch).

## Architecture notes (state on main, 2026-07-05)

All in `jabgui/src/main/java/org/jabref/gui/entryeditor/` unless noted:

- `EntryEditor.java` — BorderPane around a `TabPane`; binds `viewModel.visibleTabs()`;
  key bindings; instanceof-checks on `FieldsEditorTab` for focus/navigation (lines ~173, ~328).
- `EntryEditorTabModel.java` — sealed model; `enum BuiltIn` lists all built-in tabs
  (incl. `REQUIRED_FIELDS`, … `MATH_SCI_NET`) + `displayName()` + `FIELD_SETS` set.
- `EntryEditorTabFactory.java` — maps `BuiltIn` → tab instance (`createBuiltInTab`).
- `FieldsEditorTab.java` — abstract base: `editors` map, `createLabelAndEditor(...)`
  (uses `FieldEditors.getForField`), `setupPanel(...)` builds a GridPane (percent-height rows,
  ScrollPane `fitToHeight=true` — WRONG for a long list), private `initPanel()` builds
  GridPane→ScrollPane→SplitPane (SplitPane needed for preview-in-tab), preview handling,
  `requestFocus(Field)`, `getShownFields()`; content-driven visibility via
  `determineFieldsToShow(entry).isEmpty()`.
- Subclasses: `RequiredFieldsTab` (required = `BibEntryType.getRequiredFields()` OrFields
  flattened + `InternalField.KEY_FIELD`), `OptionalFieldsTabBase` (`getImportantOptionalFields()`
  / `getDetailOptionalNotDeprecatedFields(mode)`), `OtherFieldsTab` (set fields minus known minus
  custom-tab fields minus comments), `CommentsTab`, `UserDefinedFieldsTab`, `DeprecatedFieldsTab`.
- `EntryEditorTab.java` — base Tab; lazy rebuild in `notifyAboutFocus` only when entry/type
  changed; visibility = preference-driven && content-driven.
- Preferences: `jabgui/.../preferences/JabRefGuiPreferences.java` `getEntryEditorTabs(...)`
  (~line 400) builds the tab-model list; per-tab visibility persisted as one boolean key each
  (`SHOW_REQUIRED_FIELDS`, …) in `storeTabConfigs(...)` (~line 460). Defaults come from an
  `EntryEditorPreferences defaults` object. `EntryEditorPreferences.java` holds
  `tabVisibleProperty(BuiltIn)`.
- Field editors: `jabgui/.../gui/fieldeditors/FieldEditors.getForField(...)`; editors expose
  `getNode()`, `getWeight()` (multiline weight), `focus()`.
- L10n: new `Localization.lang("...")` keys need entries in
  `jablib/src/main/resources/l10n/JabRef_en.properties`.

## Steps (check off as done; commit after each step)

### Phase 1 — MVP: single tab listing required + set fields

- [x] **1. Model + prefs plumbing**: add `BuiltIn.ALL_FIELDS` ("Fields") to
  `EntryEditorTabModel` (incl. `FIELD_SETS`, `displayName()`); add pref key
  `showAllFieldsTab` (default **true**) in `JabRefGuiPreferences.getEntryEditorTabs`/
  `storeTabConfigs` + `EntryEditorPreferences` defaults; place it as first field tab.
  Flip defaults of REQUIRED/IMPORTANT_OPTIONAL/DETAIL_OPTIONAL/DEPRECATED/OTHER/COMMENTS
  to **false** (classes stay; users can re-enable).
  *Check: `./gradlew :jabgui:compileJava` + existing prefs tests still compile.*
- [x] **2. `AllFieldsTab` skeleton** extending `FieldsEditorTab`:
  `determineFieldsToShow` = citation key + required + set fields (ordered as in Target UX,
  flat, no groups yet). Wire into `EntryEditorTabFactory`. Add l10n key(s).
  *Check: compile + start app (`./gradlew :jabgui:run`), open entry: new first tab shows all
  set + required fields.*
- [x] **3. Natural-height scroll layout**: refactor `FieldsEditorTab` minimally so a subclass
  can opt out of stretch layout (e.g. make `initPanel` overridable / extract ScrollPane
  creation, keep SplitPane wrapper for preview). In `AllFieldsTab`: single column,
  label-above-editor or label-left rows with computed heights, ScrollPane
  `fitToHeight(false)`, `fitToWidth(true)`; multiline editors (abstract, comment, file) get
  taller min height via `FieldEditorFX.getWeight()`. Add CSS class in `Base.css`.
  *Check: entry with many fields scrolls; abstract editor is multi-row; window resize OK.*

### Phase 2 — Grouping

- [x] **4. Sections**: partition fields into Main / Identifiers / Files & Links / Comments
  (explicit field sets, see Target UX). Extract partition logic into a plain-Java helper
  (e.g. `FieldListSections`) — testable without JavaFX.
  Render section headers (localized label + separator; optionally JabRefIcons).
  *Check: unit test for partition logic; manual check with an entry having doi/file/comment.*

### Phase 3 — Adding fields

- [ ] **5. Add-field chips**: bottom area with chips for unset important-optional fields;
  "Show more" toggle reveals secondary-optional chips. Transient `userAddedFields`
  (cleared on entry change) so empty added fields remain visible; clicking focuses editor.
- [ ] **6. Free-form add**: editable ComboBox + "Add" button using `FieldFactory.parseField`;
  reject/normalize invalid names; focus the new editor.
  *Check for 5+6: add optional field via chip, type value, value lands in entry (verify in
  Source tab); add arbitrary field name; unit tests for chip-list computation.*
- [ ] **7. Live refresh**: rebuild the shown-field set when fields get set/unset from outside
  (e.g. edits in Source tab, undo, fetchers). Listen to entry field changes (BibEntry event
  bus, cf. `SourceTab`); rebuild ONLY when the computed shown set differs from what is shown
  (typing the first char into a visible empty editor must NOT trigger rebuild/focus loss).
  *Check: edit source → switch back, new field appears; undo removes it; typing keeps focus.*

### Phase 4 — Integration & cleanup

- [ ] **8. Focus & navigation**: `requestFocus(field)`, Jump-to-field dialog
  (`JumpToFieldDialog`), `EntryEditorFocusUtils` traversal, ENTRY_EDITOR_NEXT/PREVIOUS_PANEL
  keybindings work with the new tab; preview-in-tab (SplitPane) + divider persistence OK.
- [ ] **9. Preferences migration**: users with stored prefs get the new tab
  (set `showAllFieldsTab=true`, old field tabs off) — one-shot migration in
  `PreferencesMigrations` (or accept old prefs and only change defaults — decide; document).
- [ ] **10. Remove MathSciNet tab** (`MathSciNetTab`, `BuiltIn.MATH_SCI_NET`, pref key,
  l10n) — ONLY after confirming scope with Oliver.
- [ ] **11. Polish & housekeeping**: CHANGELOG.md entry, l10n keys complete, remove
  dead code paths if old tabs get dropped entirely (decision from step 9), screenshots
  for the PR.
- [ ] **12. Full verification**: `./gradlew :jabgui:test :jabgui:checkstyleMain`
  (plus l10n consistency tests in jablib).

## Open questions (ask Oliver / decide before the relevant step)

1. Tab display name: "Fields"? ("BibTeX" per issue title, but wrong in biblatex mode;
   "Entry"?) — needed in step 1/2. → **Interim: "Fields"**.
2. Do the old category tabs stay as opt-in (visibility=false) or get deleted?
   → **Interim: keep classes, default-off** (step 1), delete later in a follow-up PR.
3. Exact group membership (identifiers list; does ABSTRACT belong to Comments group or main?)
   → **Interim: ABSTRACT stays in main group.**
4. Custom user-defined tabs: their fields also appear in the new tab (no exclusion) — OK?
   → **Interim: yes, show everything.**
5. MathSciNet removal in this PR or separate? (step 10)

## Build / verify commands

```
./gradlew :jabgui:compileJava          # fast compile check
./gradlew :jabgui:run                  # manual smoke test
./gradlew :jabgui:test --tests "org.jabref.gui.entryeditor.*"
./gradlew :jabgui:checkstyleMain
```

## Status log (update when stopping!)

- 2026-07-05: Plan written. Architecture explored (files listed in Architecture notes).
- 2026-07-05: Steps 1+2 done (commit afa25ae626): `BuiltIn.ALL_FIELDS` + prefs plumbing
  (`showAllFieldsTab`), old field tabs + Comments default-off, `AllFieldsTab` (flat list),
  l10n key "Show all fields". Compile OK; `:jabgui:test --tests "org.jabref.gui.entryeditor.*"`
  green.
- 2026-07-05: Step 3 done: `FieldsEditorTab` got hooks `layoutEditors(labels, compressed)` +
  `stretchContentToTabHeight()`; `AllFieldsTab` renders single-column natural-height rows
  (TextAreas capped at 4 rows, weight>1 editors get 60px/weight), ScrollPane fitToHeight=false,
  CSS `.all-fields-list` in jabref-theme.css. Compile OK.
  NOTE: manual GUI smoke test (`./gradlew :jabgui:run`) still pending — headless session.
- 2026-07-05: Step 4 done: `FieldListSections` (partition + `sectionOf`, explicit
  identifier/file/comment field sets) + section headers in `AllFieldsTab.layoutEditors`,
  CSS `.all-fields-section(-header)`, l10n keys "Identifiers"/"Files and links" added,
  `FieldListSectionsTest` green. Next: steps 5+6 (add-field chips + free-form add).

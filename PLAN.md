# PLAN: MathSciNet browser sync

Working plan for `req~mathscinet.sync.*~1` (see `docs/requirements/mathscinet.md`). Spans two repos:

- **jabref** (this repo, branch `switch-to-html-to-node`, already merged with `main` as of commit `dec533bef6`)
- **JabRef-Browser-Extension-experimental** (`../JabRef-Browser-Extension-experimental`), two new branches stacked off `origin/add-fulltext-bridge` (PR #81)

## Status

- [x] Merge `switch-to-html-to-node` with `main`, resolve the `EntryEditorTabModel`/`EntryEditorTabFactory`/`JabRefGuiPreferences` conflicts (COMMENTS vs MATH_SCI_NET both dropped — Comments folded into `AllFieldsTab` on main, MathSciNet WebView tab dropped on this branch), pushed.
- [x] `docs/requirements/mathscinet.md` written.
- [x] Extension: split into two stacked branches, pushed, draft PRs open:
  - `shared-native-bridge` (off `origin/add-fulltext-bridge`, 1 commit): extracts native-messaging connection ownership into `nativeBridge.js` so features register a handler instead of each calling `connectNative` — a general fix/cleanup to PR #81 itself, independent of MathSciNet, so it can be reviewed/merged on its own. [PR #82](https://github.com/JabRef/JabRef-Browser-Extension-experimental/pull/82) (draft, base `add-fulltext-bridge`).
  - `mathscinet-browser-open` (off `shared-native-bridge`, 1 commit): the actual feature — `mathscinetBridge.js` (tab tracking) + `bridge/JabExtBridge.java`/`bridge/Json.java` (new `/v1/mathscinet/open` route + NM command). [PR #83](https://github.com/JabRef/JabRef-Browser-Extension-experimental/pull/83) (draft, base `shared-native-bridge`).
  - Verified via `jbang build`, eslint, `node --check` on both branches individually; full `npm test` blocked by an uninitialized `translators/zotero` submodule, unrelated to this change.
- [x] JabRef: `BrowserExtensionBridgeClient` (jablib, discovery/HTTP client, unit-tested against a real loopback `HttpServer`), `MathSciNetPreferences` + `GuiPreferences`/`JabRefGuiPreferences` wiring, `canSyncWithBrowser` capability flag on `BaseIdentifierEditorViewModel`, toggle button in `IdentifierEditor.fxml`/`.java`, sync logic in `MathSciNetIdentifierEditorViewModel`. `req~mathscinet.sync.*~1` tracing comments added; `./gradlew traceRequirements` passes (227/227). Not committed — left in the working tree for review.

## Remaining follow-up

- Manual end-to-end check (real JabRef + real unpacked extension) from step 5 of Sequencing below — not done, this environment has no display/browser to drive it.
- Mark PR #82 and #83 ready-for-review once the manual end-to-end check passes (currently drafts).
- Commit the jabref-repo working-tree changes (docs, PLAN.md, view model, preferences, bridge client, tests) once reviewed — not yet committed here, only pushed/PR'd on the extension side.
- `IdentifierEditorTest` (TestFX) can't run in this sandbox at all (no X display) — confirmed pre-existing/unrelated by reproducing the same failure on the pre-change tree.

## Architecture recap

```text
JabRef (jabgui)  --HTTP+bearer-->  jabext-experimental (bridge/JabExtBridge.java)  --native messaging-->  extension background --> tab
```

One bridge process already exists (PR #81, unmerged) serving `/v1/health` and `/v1/fulltext` for the fulltext-fetch feature. We are **not** standing up a second bridge/discovery file — we add one more HTTP route + one more native-messaging command pair to the *same* process, and one more command handler in the extension.

**Important gotcha found during research:** `fulltextBridge.js` currently owns the *only* `browser.runtime.connectNative(...)` call (module-level `port` variable in `fulltextBridge.js`). If the new MathSciNet command handler opens its *own* `connectNative` call, the browser spawns a **second** `jabext-experimental` OS process, which independently binds its own port and overwrites the same discovery file the first process wrote — a race that will intermittently break fulltext fetch. The extension-side work must first extract native-messaging connection ownership into one shared module and have both feature handlers dispatch off it.

## Part A — Browser extension repo

Branch: `mathscinet-browser-open`, based on `origin/add-fulltext-bridge`.

### A1. Extract shared native-messaging connection

- New `nativeBridge.js`: owns `connectNative("jabext_experimental")`, exposes `registerHandler(type, fn)` and `reply(msg)`, handles `onDisconnect`/reconnect.
- Refactor `fulltextBridge.js` to call `registerHandler("fetchFulltext", handleFetch)` instead of owning `connect()`/`port` itself.
- `background.js`: replace `startFulltextBridge()` call with a single `startNativeBridge()` (from `nativeBridge.js`) that both `fulltextBridge.js` and the new `mathscinetBridge.js` register against on module load.

### A2. Extension-side tab tracking (`mathscinetBridge.js`, new file)

Mirrors `fulltextBridge.js`'s structure. State: single module-level `let trackedTabId = null;`.

On `{ type: "openMathSciNet", requestId, mrNumber }`:

1. Build `target = "https://www.ams.org/mathscinet-getitem?mr=" + encodeURIComponent(mrNumber)` (must match `MathSciNetId#getExternalURI` in the jabref repo exactly).
2. If `trackedTabId` is set, try `browser.tabs.get(trackedTabId)`:
   - success → `browser.tabs.update(trackedTabId, {url: target, active: true})`, then `browser.windows.update(tab.windowId, {focused: true})`. Reply `{requestId, action: "focused", tabId: trackedTabId}`.
   - failure (tab gone) → fall through to step 3.
3. `browser.tabs.create({url: target, active: true})`; store `trackedTabId = tab.id`. Register a **one-time** `browser.tabs.onRemoved` listener (or a persistent listener that checks `if (tabId === trackedTabId) trackedTabId = null;`) so a user-closed tab doesn't leave a stale id around (`req~mathscinet.sync.own-tabs-only~1`). Reply `{requestId, action: "opened", tabId: trackedTabId}`.

Never call `browser.tabs.query(...)` over all tabs / never match by URL — only ever act on `trackedTabId`, satisfying "don't touch tabs it didn't open."

### A3. Java bridge (`bridge/JabExtBridge.java`)

- New route in `startHttpServer()`: `server.createContext("/v1/mathscinet/open", this::handleMathSciNetOpen);`
- `handleMathSciNetOpen`: same `rejectByOrigin`/`rejectByBearer` guards as `handleFulltext`. Parse `{ "mrNumber": "..." }` POST body (add `Json.readMathSciNetOpenRequest`/`Json.writeMathSciNetOpenResponse` alongside the existing fulltext read/writers in `Json.java`, following the same StringJoiner/json-tree style already used there).
- Send NM message `{"type": "openMathSciNet", "requestId": ..., "mrNumber": ...}` (new `sendNmMathSciNetOpen`, parallel to `sendNmFetch`), await the correlated reply via the existing `pending` map (already generic over `Json.NmReply`; check whether `NmReply` needs an `action`/`tabId` field added, or a second reply record type — simplest is extending the existing one with two more optional fields).
- Return `{action, tabId}` as the HTTP response body on success; map errors the same way `httpStatusForError` does (a `tab-error` or similar code if the extension reports failure).
- No changes to discovery-file writing, token bootstrap, or NM framing — those are already command-agnostic.

### A4. Verification

- `make bridge-build-jvm` (fast path, no native-image) + manual smoke test: load extension unpacked, confirm `/v1/health` still works, then `curl -X POST http://127.0.0.1:<port>/v1/mathscinet/open -H "Authorization: Bearer <token>" -d '{"mrNumber":"619693"}'` opens a tab; repeat with a different `mrNumber` and confirm it reuses/focuses the same tab; close the tab manually and repeat, confirm a new tab opens.
- Existing fulltext-fetch flow still works after the A1 refactor (regression check on the thing we just touched).

## Part B — jabref repo (this repo, `switch-to-html-to-node`)

### B1. Discovery + bridge HTTP client

New class, e.g. `org.jabref.logic.browserext.BrowserExtensionBridgeClient` (name TBD at implementation time) in `jablib` (or `jabgui` if it must depend on GUI-only bits — prefer `jablib` since it's pure I/O/HTTP):

- Path resolution **must byte-for-byte match** `bridge/JabExtBridge.java`'s private `JabRefPaths.jabrefConfigBase()` (read during research, not currently mirrored anywhere in this repo — `org.jabref.logic.util.Directories` uses `AppDirsFactory.getUserDataDir(...)`, a *different* base than the bridge's hand-rolled config dir, so it cannot be reused as-is):
  - Windows: `%APPDATA%` (fallback `~/AppData/Roaming`) `/JabRef`
  - macOS: `~/Library/Application Support/JabRef`
  - else (Linux/BSD/…): `$XDG_CONFIG_HOME` (fallback `~/.config`) `/jabref`
  - then `.resolve("fulltext-providers").resolve("jabext-experimental.json")`
- Read+parse that JSON (`port`, `tokenFile`, protocol `version`); read the bearer token from `tokenFile`.
- `Optional<OpenResult> openMathSciNet(String mrNumber)`: POST to `http://127.0.0.1:<port>/v1/mathscinet/open` with `Authorization: Bearer <token>` using `java.net.http.HttpClient` (JDK built-in, no new dependency). Treat "discovery file missing" / connection refused / non-2xx as an empty `Optional` + logged warning, never throw out to the caller — satisfies `req~mathscinet.sync.unavailable~1`.
- Unit tests: JSON parsing of a fixture discovery file; path resolution per OS (parameterize / mock `os.name` the way existing OS-dependent tests in this codebase already do — check `OS.java`'s test usage for the pattern).

### B2. Preference + toggle state

- Add a boolean preference (default `false`), e.g. `SHOW_MATHSCINET_BROWSER_SYNC` / `mathSciNetSyncWithBrowser`, following the exact pattern of `JabRefGuiPreferences.getCopyToPreferences()` (smallest existing example of a tiny standalone preferences bucket: constant, `bindBoolean`, default from a `getDefault()`).
- Expose it as a `BooleanProperty` reachable from `MathSciNetIdentifierEditorViewModel`.

### B3. View model + UI wiring

- `BaseIdentifierEditorViewModel.configure(...)` currently takes 3 capability flags (`canFetchBibliographyInformationById`, `canLookupIdentifier`, `canShortenIdentifier`). Add a 4th, `canSyncWithBrowser`, following the same pattern; only `MathSciNetIdentifierEditorViewModel` passes `true`.
- `IdentifierEditor.fxml`/`IdentifierEditor.java`: add a toggle control bound to `canSyncWithBrowser` for visibility and to the preference `BooleanProperty` for state (same visibility-binding style already used for `shortenDOIButton`/`fetchInformationByIdentifierButton`/`lookupIdentifierButton`).
- `MathSciNetIdentifierEditorViewModel`:
  - On construction/`bindToEntry`, if sync-preference is on and `identifier` resolves to a valid `MathSciNetId`, call `BrowserExtensionBridgeClient.openMathSciNet(id)` (off the FX thread — reuse `taskExecutor`, same pattern as other async calls in this package).
  - Subscribe to the `identifier` `ObjectProperty` (already updated via `EasyBind.subscribe(textProperty(), ...)` in the base class) so edits to the field while sync is on also trigger a call — matches `req~mathscinet.sync.triggers~1`.
  - Toggling the preference on with a valid identifier already shown triggers one immediate call; toggling off does nothing to the tab (no explicit close call exists in the protocol, matching the requirement).
- No dialog on failure — `dialogService.notify(...)` (non-blocking) at most, matching `req~mathscinet.sync.unavailable~1`.

### B4. Requirement tracing

Add `// [impl->req~mathscinet.sync.xxx~1]` comments at the concrete implementation points once B1–B3 land, so `./gradlew traceRequirements` (mentioned in `docs/requirements/index.md`) doesn't flag these as uncovered.

### B5. Tests

- `MathSciNetIdentifierEditorViewModelTest` (extend existing `IdentifierEditorTest` patterns) covering: no call when sync off; call on toggle-on with valid id; call on entry switch; no call on entry switch to an entry without `MR_NUMBER`.
- `BrowserExtensionBridgeClientTest`: discovery file parsing, missing-file → empty Optional, bad token → no crash.

## Sequencing

1. Part A1 (shared NM connection) first — required before A2/A3 can be tested without breaking the existing fulltext feature.
2. A2 + A3 together (extension bridge command), verified with `curl` per A4.
3. B1 (discovery client) independently, unit-testable without the extension running.
4. B2 + B3 (preference + UI + wiring) on top of B1.
5. B4/B5 pass, then manual end-to-end check: real JabRef + real unpacked extension, toggle on, switch between two entries with different `MR_NUMBER`s, confirm one tab is reused and jumps; close the tab manually, switch entries again, confirm a fresh tab opens; open MathSciNet manually in an unrelated tab first and confirm sync never touches it.

## Explicit non-goals (v1)

- Multiple JabRef windows/instances sharing one bridge tab — undefined, not handled.
- Closing the tracked tab when the toggle is switched off, or when JabRef exits.
- PMID or other identifier types getting the same treatment (only `MR_NUMBER`/MathSciNet, per the explicit ask).

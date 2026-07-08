---
parent: Requirements
---
# MathSciNet

## Background

Historically, JabRef embedded the live MathSciNet website in an entry editor tab (`MathSciNetTab`, `javafx.scene.web.WebView`). This was the last GUI feature depending on `javafx-web`, which bundles a full WebKit, bloats the distribution, and blocks native-image builds. `MathSciNetTab` and its preference plumbing were removed; the `MR_NUMBER` field now uses the same `IdentifierEditor` as DOI/ISBN/ISSN/eprint, with an "open in external browser" button (`MathSciNetId#getExternalURI`, `https://www.ams.org/mathscinet-getitem?mr=<id>`).

Embedding a browser is not coming back: `CommonArchitectureTest.doNotUseJavaFXWeb` fails the build on any `javafx.scene.web` dependency. Opening a plain external-browser tab per click is a UX regression from the old tab: users lose the "keep one tab open, click through entries, stay on MathSciNet" workflow.

The [JabRef Browser Extension (experimental)][ext] already implements a native-messaging bridge (`bridge/JabExtBridge.java`, PR [#81][pr81], branch `add-fulltext-bridge`) so JabRef can reach the browser extension over a loopback HTTP+bearer-token channel, because MV3 service workers cannot bind TCP ports themselves. That bridge currently serves one purpose (fulltext PDF fetch); this requirement extends the same bridge process with a second command so JabRef can drive one browser tab for MathSciNet, recovering the old tab's UX without an embedded WebView.

[ext]: https://github.com/JabRef/JabRef-Browser-Extension-experimental
[pr81]: https://github.com/JabRef/JabRef-Browser-Extension-experimental/pull/81

## Sync toggle on the MathSciNet identifier editor
`req~mathscinet.sync.toggle~1`

The `MR_NUMBER` field's `IdentifierEditor` shows a "Sync with browser" toggle, visible only for this field (not for DOI/ISBN/ISSN/eprint). The toggle's state is a persisted GUI preference, off by default.

Needs: impl

## Sync opens or focuses a JabRef-owned tab
`req~mathscinet.sync.open-or-focus~1`

While the toggle is on and the shown entry has a parseable `MR_NUMBER`, JabRef asks the browser extension (via the bridge) to show that entry's MathSciNet page:

- If the extension has no live tab it previously opened for this purpose, it opens a new tab navigated to `https://www.ams.org/mathscinet-getitem?mr=<id>` and remembers that tab's id.
- If it already has one (from an earlier sync in the same browser session), it navigates that same tab to the new id's URL and brings it to the front (tab focus + window focus) instead of opening another tab.

Implemented on the JabRef side by sending the request; opening/focusing the tab itself is implemented in the browser extension repo (see `req~mathscinet.sync.own-tabs-only~1`), so `impl` coverage here only spans the request side.

Needs: impl

## Sync fires on entry/field changes, not continuously
`req~mathscinet.sync.triggers~1`

A sync request is sent when any of the following happens while the toggle is on:

- the toggle is switched on and the current entry already has a valid `MR_NUMBER`,
- the entry shown in the entry editor changes and the new entry has a valid `MR_NUMBER`,
- the `MR_NUMBER` field's value is edited into a new valid identifier for the current entry.

No request is sent when the toggle is off, when the shown entry has no parseable `MR_NUMBER`, or merely because the identifier text field re-renders without a value change. Switching the toggle off does not close or otherwise touch the browser tab.

Needs: impl

## Extension only ever commands tabs it opened itself
`req~mathscinet.sync.own-tabs-only~1`

The extension must not enumerate or adopt pre-existing MathSciNet tabs (e.g., ones the user opened manually by clicking the identifier editor's external-link button, or by browsing there directly). It tracks at most one tab id that *it* created via this feature. If that tracked tab is later closed by the user, the next sync request opens a fresh tab rather than failing or reusing an unrelated tab. If the user has since navigated the tracked tab elsewhere, the next sync still navigates that same tab id back to the requested MathSciNet page (identity of the tab is what's tracked, not its current URL).

Entirely implemented in the `JabRef-Browser-Extension-experimental` repo (`mathscinetBridge.js`), not this one, so it is intentionally not linked to an `impl` artifact here — this repo's `traceRequirements` cannot and should not cover it.

## Sync reuses the existing bridge transport
`req~mathscinet.sync.transport~1`

The feature adds one HTTP endpoint (e.g. `POST /v1/mathscinet/open`) and one native-messaging command to the *same* bridge process and discovery file (`jabext-experimental`) used by the fulltext-fetch protocol — it does not stand up a second bridge, discovery file, or native-messaging host. Requests carry the same bearer-token and loopback-origin checks as `/v1/fulltext`.

The endpoint/command itself is implemented in the `JabRef-Browser-Extension-experimental` repo; on the JabRef side, `impl` here covers only reusing the same discovery file/provider name rather than inventing a second one.

Needs: impl

## Sync fails silently when the extension/bridge is unavailable
`req~mathscinet.sync.unavailable~1`

If no discovery file is present, the bridge is unreachable, or the request errors, JabRef does not block or interrupt entry editing with a modal dialog. It logs the failure and may show a single non-blocking notification; the identifier text field and its "open in external browser" button keep working regardless of sync state.

Needs: impl

<!-- markdownlint-disable-file MD022 -->

/// The form DSL of the preferences dialog: code-first replacements for the former FXML +
/// controller pairs.
///
/// ## The builder
///
/// [org.jabref.gui.preferences.forms.PreferencesFormBuilder] assembles a tab's node tree.
/// Its contract:
///
/// - **The chain only adds elements.** Anything that configures an element happens inside that
///   element's trailing config lambda, so no call can land on the wrong node.
/// - **The handle a lambda receives states what its element is.** An `InputElement` is a
///   `Control`: it can carry a tooltip, validation and *attachments*; a `NodeElement` is not a
///   `Control` and offers none of that. Asking for the wrong capability does not compile.
/// - **Secondary nodes are attachments, not separate builder methods.** A help button, a browse
///   button or an inline value field is attached to its primary control via the handle
///   (`help(...)`, `browse(...)`, `attachField(...)`, `attach(...)`), which places it beside the
///   control (right after it; left of it for a switch) and couples its state to it. The
///   attachment's own config lambda makes it fully addressable. `checkWithField` remains as sugar over `checkbox` + `attachField` because the
///   "Enable X on port [..]" idiom reads better as one call.
/// - **Regions nest via lambdas** (`section`, `group`, `columns`, `flow`), so a region
///   cannot be left unclosed and can be configured or disabled as a unit. Their handles mirror the
///   element handles: a `SectionRegion` has a header and therefore takes `help(...)`, a plain
///   `FormRegion` has none and does not — again a compile error rather than an exception.
///
/// ## Texts and settings that depend on a switch
///
/// - `label(...)` is a line of its own that introduces what follows; `info(...)` an indented,
///   italic remark of its own; `description(...)` a line inside a setting tile, below its caption,
///   that explains that one setting. Prefer `description` wherever the text belongs to a setting.
/// - A setting that only applies while a switch is on goes into that switch's `whenOn(...)` (shown
///   only while on) or `enabledWhenOn(...)` (always shown, disabled while off), not into a separate
///   row disabled with `disableWhen(switch.not())`. Short values of the switch itself (a port, a
///   size) are attached to it instead.
/// - Inside builder-placed elements, disable and hide via `disableWhen`/`visibleWhen`: the builder
///   combines its own conditions with them, and replaces a binding made on the node directly.
///
/// ## Misuse
///
/// Calls that do not fit the element (a radio outside a `radioGroup`, `whenOn` on something that
/// is no switch, ...) throw when the tab is built. They are programming errors, not runtime states:
/// `PreferencesSearchCoverageTest` builds every tab, so such a mistake fails the build rather than
/// reaching a user.
///
/// ## The editors
///
/// [PasswordFieldEditor] and [TagsFieldEditor] encapsulate control patterns that recur across
/// tabs. Their conventions:
///
/// - **An editor binds its own value property**; builder methods and tabs only place the node.
/// - **Shape follows option count**: an editor without options is a static `create(...)` factory;
///   one with optional parts (the password field's buttons) is a small fluent builder. Its terminal
///   call is named for what it actually does — `PasswordFieldEditor.field()` just hands back the
///   field, so it is not called `build()`.
/// - Editors return their control, so a tab can pass them to the builder's `field`/`custom`
///   methods or dedicated hooks such as `tagsField`. The password editor deliberately has no
///   builder method: its variants differ per call site, and `field(label, editor)` places it just
///   as well.
///
/// The tabs using this DSL live in the sibling packages of `org.jabref.gui.preferences`.
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html#preferences">Preferences component</a>
/// @see <a href="https://devdocs.jabref.org/code-howtos/ui-recommendations.html#form-validation">Form validation</a>
/// @see <a href="https://devdocs.jabref.org/decisions/0069-preferences-search-registration-vs-scene-graph-scan.html">ADR 0069: preferences search registration</a>
package org.jabref.gui.preferences.forms;

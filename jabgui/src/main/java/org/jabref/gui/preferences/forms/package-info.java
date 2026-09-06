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
///   (`help(...)`, `browse(...)`, `attachField(...)`, `attach(...)`), which appends it right after
///   the control and couples its state to it. The attachment's own config lambda makes it fully
///   addressable. `checkWithField` remains as sugar over `checkbox` + `attachField` because the
///   "Enable X on port [..]" idiom reads better as one call.
/// - **Regions nest via lambdas** (`section`, `group`, `columns`, `flow`), so a region
///   cannot be left unclosed and can be configured or disabled as a unit. Their handles mirror the
///   element handles: a `SectionRegion` has a header and therefore takes `help(...)`, a plain
///   `FormRegion` has none and does not — again a compile error rather than an exception.
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

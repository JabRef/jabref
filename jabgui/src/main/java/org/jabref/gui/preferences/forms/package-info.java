/// The form DSL of the preferences dialog: code-first replacements for the former FXML +
/// controller pairs.
///
/// ## The builder
///
/// {@link org.jabref.gui.preferences.forms.PreferencesFormBuilder} assembles a tab's node tree.
/// Its contract:
///
/// - **The chain only adds elements.** Anything that configures an element happens inside that
///   element's trailing config lambda, so no call can land on the wrong node.
/// - **The handle a lambda receives states what its element is.** A `RowElement` sits in a row of
///   its own and can therefore carry a help button; an `InputElement` cannot; a `NodeElement` is
///   not a `Control` and offers neither tooltip nor validation. Asking for the wrong capability
///   does not compile.
/// - **Regions nest via lambdas** (`section`, `group`, `columns`, `flow`, `fields`), so a region
///   cannot be left unclosed and can be configured or disabled as a unit.
///
/// ## The editors
///
/// `BrowseFileEditor`, `PasswordFieldEditor` and `TagsFieldEditor` encapsulate control patterns
/// that recur across tabs. Their conventions:
///
/// - **An editor binds its own value property**; builder methods and tabs only place the node.
/// - **Shape follows option count**: an editor without options is a static `create(...)` factory;
///   one with optional parts (the password field's buttons) is a small fluent builder ending in
///   `build()`.
/// - Editors return their control (or a `row`/`field` pair where the caller needs both), so a tab
///   can pass them to the builder's `field`/`custom` methods or dedicated hooks such as
///   `browseField` and `tagsField`. The password editor deliberately has no builder method: its
///   variants differ per call site, and `field(label, editor)` places it just as well.
package org.jabref.gui.preferences.forms;

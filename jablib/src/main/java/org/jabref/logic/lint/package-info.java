/// JabFix, the linter and formatter for BibTeX libraries.
///
/// - [org.jabref.logic.lint.JabFix] applies a [org.jabref.logic.lint.rule.RuleSet] to the entries of a library.
/// - [org.jabref.logic.lint.JabFixConfiguration] combines a mode, the library's Save Actions, its configuration and the options of a run into the rules a run applies.
/// - [org.jabref.logic.lint.JabFixMode] lists the modes JabFix ships.
///
/// The rule API is in [org.jabref.logic.lint.rule], the standalone built-in rules are in [org.jabref.logic.lint.rules], and the configuration model is [org.jabref.model.metadata.LintSettings].
/// The rules run when [org.jabref.logic.exporter.BibDatabaseWriter] saves a library that carries a configuration, and in `jabkit fix`.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/jabfix.html">JabFix code howto</a>
/// @see <a href="https://devdocs.jabref.org/decisions/0086-per-rule-modes-in-the-formatting-configuration.html">ADR-0086: Per-rule modes in the formatting configuration</a>

package org.jabref.logic.lint;

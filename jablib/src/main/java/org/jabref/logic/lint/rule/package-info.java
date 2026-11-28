/// The rule API of JabFix: a [org.jabref.logic.lint.rule.Rule] scans an entry and reports [org.jabref.logic.lint.rule.Finding]s, each of which may carry a [org.jabref.logic.lint.rule.Fix].
///
/// - [org.jabref.logic.lint.rule.RuleSet] holds the rules of a run, either the built-in ones or those a configuration names.
/// - [org.jabref.logic.lint.rule.FieldValueRule] and [org.jabref.logic.lint.rule.CleanupRule] are bases for rules; [org.jabref.logic.lint.rule.SaveActionRule] runs a Save Action formatter.
/// - [org.jabref.logic.lint.rule.Suppressions] reads the magic comments that switch rules off for one entry, and [org.jabref.logic.lint.rule.MagicCommentRule] reports those that switch nothing off.
///
/// The standalone built-in rules are in [org.jabref.logic.lint.rules].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/jabfix.html">JabFix code howto</a>
@NullMarked
package org.jabref.logic.lint.rule;

import org.jspecify.annotations.NullMarked;

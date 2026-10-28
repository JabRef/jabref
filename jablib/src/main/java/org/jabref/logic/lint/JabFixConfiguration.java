package org.jabref.logic.lint;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.lint.rule.SaveActionIds;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Where the rules of a run come from, and how a library's configuration is set.
///
/// A run applies four layers, each outranking the ones before it:
///
/// 1. a [JabFixMode] -- the run's where it names one, and otherwise the library's,
/// 2. the library's Save Actions, while it still has its `saveActions` item: each repairs on its
///    field, where the item switches them on,
/// 3. the rules the library's configuration states,
/// 4. the rules the run states.
///
/// A rule none of them names does not run. How a layer outranks another is
/// [LintSettings#overriddenBy].
///
/// The Save Actions rank right below the configuration's rules, so moving them into it -- which
/// configuring a library does -- leaves what a run applies unchanged.
@NullMarked
public final class JabFixConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabFixConfiguration.class);

    private JabFixConfiguration() {
    }

    /// What a run of the library applies, with the mode expanded into the rules it names.
    ///
    /// @param run what this run asks for on top of the library -- [LintSettings#NONE] for a save
    ///            that asks for nothing of its own
    public static LintSettings applied(MetaData metaData, LintSettings run) {
        LintSettings stated = saveActionsOf(metaData)
                .overriddenBy(metaData.getFormatting().orElse(LintSettings.NONE))
                .overriddenBy(run);
        return stated.mode()
                     .map(JabFixConfiguration::rulesOf)
                     .orElse(LintSettings.NONE)
                     .overriddenBy(stated);
    }

    /// A mode this JabFix does not know is passed over, like a rule it does not know, so that a
    /// library configured by a newer JabFix still saves.
    private static LintSettings rulesOf(String modeKey) {
        return JabFixMode.fromKey(modeKey)
                         .map(JabFixMode::getSettings)
                         .orElseGet(() -> {
                             LOGGER.warn("Passing over the JabFix mode '{}', which this JabRef does not know", modeKey);
                             return LintSettings.NONE;
                         });
    }

    /// Sets what the library asks JabFix to do, and moves its Save Actions into it: they become
    /// entries of the configuration, which `settings` outrank, and the `saveActions` item is dropped.
    /// Configuring a library with what it says already therefore only moves its Save Actions.
    ///
    /// A library that has no configuration is given one only where there is something to state, so
    /// that it is not handed over to JabFix for nothing; an item that switches the Save Actions off
    /// is dropped either way, since it has no part in a run.
    public static void configure(MetaData metaData, LintSettings settings) {
        LintSettings configured = saveActionsOf(metaData).overriddenBy(settings);
        if (!configured.isEmpty() || metaData.getFormatting().isPresent()) {
            metaData.setFormatting(configured);
        }
        if (metaData.getSaveActions().isPresent()) {
            metaData.clearSaveActions();
        }
    }

    /// The library's Save Actions as entries of a configuration, each repairing on its field.
    ///
    /// An item switching them off states nothing rather than switching them off, so that it does not
    /// switch off what a mode does on the same fields -- something the item never had a say in.
    static LintSettings saveActionsOf(MetaData metaData) {
        SequencedMap<RuleSelector, RuleMode> stated = new LinkedHashMap<>();
        metaData.getSaveActions()
                .filter(FieldFormatterCleanupActions::isEnabled)
                .ifPresent(item -> item.getConfiguredActions()
                                       .forEach(saveAction -> stated.put(SaveActionIds.selectorOf(saveAction), RuleMode.FIX)));
        return new LintSettings(stated);
    }
}

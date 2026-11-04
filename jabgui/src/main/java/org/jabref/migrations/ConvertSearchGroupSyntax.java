package org.jabref.migrations;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.logic.util.Version;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/// Converts search groups of JabRef 5 and older, which stored "regular expression" and "case sensitive" as flags next to
/// the query, to the current syntax, which expresses them as operators inside the query.
public class ConvertSearchGroupSyntax implements PostOpenMigration {

    // We cannot have this constant in `Version.java` because of recursion errors
    // Thus, we keep it here, because it is (currently) used only in the context of groups migration.
    public static final Version VERSION_6_0_ALPHA_1 = Version.parse("6.0-alpha_1");

    private final DialogService dialogService;

    public ConvertSearchGroupSyntax(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @Override
    public boolean isMigrationNecessary(ParserResult parserResult) {
        return parserResult.getMetaData().getGroupSearchSyntaxVersion().isEmpty()
                && parserResult.getMetaData().containsSearchGroups();
    }

    @Override
    public String getId() {
        return "searchGroupSyntax";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Search groups use the query syntax of JabRef 5 and older. Convert them to the current syntax.");
    }

    @Override
    public void performMigration(ParserResult parserResult, List<BibEntry> entries) {
        parserResult.getMetaData().getGroups().ifPresent(this::migrateGroups);
        parserResult.getMetaData().setGroupSearchSyntaxVersion(VERSION_6_0_ALPHA_1);
    }

    private void migrateGroups(GroupTreeNode node) {
        if (node.getGroup() instanceof SearchGroup searchGroup) {
            try {
                if (searchGroup.getSearchQuery().isValid()) {
                    searchGroup.setSearchExpression(SearchQueryConversion.flagsToSearchExpression(searchGroup.getSearchQuery()));
                } else {
                    askForNewSearchExpression(searchGroup);
                }
            } catch (ParseCancellationException _) {
                askForNewSearchExpression(searchGroup);
            }
        }
        for (GroupTreeNode child : node.getChildren()) {
            migrateGroups(child);
        }
    }

    private void askForNewSearchExpression(SearchGroup searchGroup) {
        Optional<String> newSearchExpression = dialogService.showInputDialogWithDefaultAndWait(
                Localization.lang("Search group migration failed"),
                Localization.lang("The search group '%0' could not be migrated. Please enter the new search expression.",
                        searchGroup.getName()),
                searchGroup.getSearchExpression());
        newSearchExpression.ifPresent(searchGroup::setSearchExpression);
    }
}

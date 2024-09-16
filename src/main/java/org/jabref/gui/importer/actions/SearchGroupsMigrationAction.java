package org.jabref.gui.importer.actions;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.Version;
import org.jabref.migrations.SearchToLuceneMigration;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * This action checks whether the syntax for SearchGroups is the new one.
 * If not we ask the user whether to migrate.
 */
public class SearchGroupsMigrationAction implements GUIPostOpenAction {

    // We cannot have this constant in `Version.java` because of recursion errors
    // Thus, we keep it here, because it is (currently) used only in the context of groups migration.
    public static final Version VERSION_6_0_ALPHA = Version.parse("6.0-alpha");

    @Override
    public boolean isActionNecessary(ParserResult parserResult, CliPreferences preferences) {
        if (parserResult.getMetaData().getGroupSearchSyntaxVersion().isPresent()) {
            // Currently the presence of any version is enough to know that no migration is necessary
            return false;
        }

        Optional<GroupTreeNode> groups = parserResult.getMetaData().getGroups();
        return groups.filter(this::groupOrSubgroupIsSearchGroup).isPresent();
    }

    private boolean groupOrSubgroupIsSearchGroup(GroupTreeNode groupTreeNode) {
        if (groupTreeNode.getGroup() instanceof SearchGroup) {
            return true;
        }
        for (GroupTreeNode child : groupTreeNode.getChildren()) {
            if (groupOrSubgroupIsSearchGroup(child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        if (!dialogService.showConfirmationDialogAndWait(Localization.lang("Search groups migration of %0", parserResult.getPath().map(Path::toString).orElse("")),
                Localization.lang("The search groups syntax is outdated. Do you want to migrate to the new syntax?"),
                Localization.lang("Migrate"), Localization.lang("Keep as is"))) {
            return;
        }

        parserResult.getMetaData().getGroups().ifPresent(groupTreeNode -> migrateGroups(groupTreeNode, dialogService));
        parserResult.getMetaData().setGroupSearchSyntaxVersion(VERSION_6_0_ALPHA);
        parserResult.setChangedOnMigration(true);
    }

    private void migrateGroups(GroupTreeNode node, DialogService dialogService) {
        if (node.getGroup() instanceof SearchGroup searchGroup) {
            try {
                String luceneSearchExpression = SearchToLuceneMigration.migrateToLuceneSyntax(searchGroup.getSearchExpression(), searchGroup.getSearchFlags().contains(SearchFlags.REGULAR_EXPRESSION));
                searchGroup.setSearchExpression(luceneSearchExpression);
            } catch (ParseCancellationException e) {
                Optional<String> luceneSearchExpression = dialogService.showInputDialogWithDefaultAndWait(
                        Localization.lang("Search group migration failed"),
                        Localization.lang("The search group '%0' could not be migrated. Please enter the new search expression.", searchGroup.getName()),
                        searchGroup.getSearchExpression());
                luceneSearchExpression.ifPresent(searchGroup::setSearchExpression);
            }
        }
        for (GroupTreeNode child : node.getChildren()) {
            migrateGroups(child, dialogService);
        }
    }
}

package org.jabref.gui.importer.actions;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.logic.util.Version;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * This action checks whether the syntax for SearchGroups is the new one.
 * If not we ask the user whether to migrate.
 */
public class SearchGroupsMigrationAction implements GUIPostOpenAction {

    // We cannot have this constant in `Version.java` because of recursion errors
    // Thus, we keep it here, because it is (currently) used only in the context of groups migration.
    public static final Version VERSION_6_0_ALPHA = Version.parse("6.0-alpha");
    public static final Version VERSION_6_0_ALPHA_1 = Version.parse("6.0-alpha_1");

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        Optional<Version> currentVersion = parserResult.getMetaData().getGroupSearchSyntaxVersion();
        if (currentVersion.isPresent()) {
            if (currentVersion.get().equals(VERSION_6_0_ALPHA)) {
                // TODO: This text will only be shown after releasing 6.0-alpha and then removed
                dialogService.showErrorDialogAndWait("Search groups migration of " + parserResult.getPath().map(Path::toString).orElse(""),
                        "The search groups syntax has been reverted to the old one. Please use the backup you made before using 6.0-alpha.");
            }
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
        parserResult.getMetaData().setGroupSearchSyntaxVersion(VERSION_6_0_ALPHA_1);
        parserResult.setChangedOnMigration(true);
    }

    private void migrateGroups(GroupTreeNode node, DialogService dialogService) {
        if (node.getGroup() instanceof SearchGroup searchGroup) {
            try {
                String newSearchExpression = SearchQueryConversion.flagsToSearchExpression(searchGroup.getSearchQuery());
                searchGroup.setSearchExpression(newSearchExpression);
            } catch (ParseCancellationException e) {
                Optional<String> newSearchExpression = dialogService.showInputDialogWithDefaultAndWait(
                        Localization.lang("Search group migration failed"),
                        Localization.lang("The search group '%0' could not be migrated. Please enter the new search expression.",
                        searchGroup.getName()),
                        searchGroup.getSearchExpression());
                newSearchExpression.ifPresent(searchGroup::setSearchExpression);
            }
        }
        for (GroupTreeNode child : node.getChildren()) {
            migrateGroups(child, dialogService);
        }
    }
}

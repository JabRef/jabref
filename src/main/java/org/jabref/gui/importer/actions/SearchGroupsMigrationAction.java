package org.jabref.gui.importer.actions;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.preferences.PreferencesService;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;

/**
 * This action checks whether the syntax for SearchGroups is the new one.
 * If not we ask the user whether to migrate.
 */
public class SearchGroupsMigrationAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, PreferencesService preferencesService) {
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
    public void performAction(ParserResult parserResult, DialogService dialogService, PreferencesService preferencesService) {
        if (!dialogService.showConfirmationDialogAndWait(Localization.lang("Search groups migration of %0", parserResult.getPath().map(Path::toString).orElse("")),
                Localization.lang("The search groups syntax is outdated. Do you want to migrate to the new syntax?"),
                Localization.lang("Migrate"), Localization.lang("Cancel"))) {
            return;
        }

        parserResult.getMetaData().getGroups().ifPresent(this::migrateGroups);

        parserResult.getMetaData().setGroupSearchSyntaxVersion(SearchGroup.VERSION_6_0_ALPHA);
    }

    private void migrateGroups(GroupTreeNode node) {
        if (node.getGroup() instanceof SearchGroup searchGroup) {
            String luceneSearchExpression = migrateToLuceneSyntax(searchGroup.getSearchExpression(), searchGroup.getSearchFlags().contains(SearchFlags.REGULAR_EXPRESSION));
            searchGroup.setSearchExpression(luceneSearchExpression);
        }
        for (GroupTreeNode child : node.getChildren()) {
            migrateGroups(child);
        }
    }

    @VisibleForTesting
    static String migrateToLuceneSyntax(String searchExpression, boolean isRegularExpression) {
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToLuceneVisitor searchToLuceneVisitor = new SearchToLuceneVisitor(isRegularExpression);
        QueryNode luceneQueryNode = searchToLuceneVisitor.visit(context);
        String result = luceneQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();
        if (!searchToLuceneVisitor.isNegation()) {
            // Remove "all:" prefix for some cleaner search queries
            // There is no UnfieldedQueryNode in Lucene, only FieldQueryNode
            return result.replace(SearchFieldConstants.DEFAULT_FIELD + ":", "");
        }
        return result;
    }

    private static SearchParser.StartContext getStartContext(String searchExpression) {
        SearchLexer lexer = new SearchLexer(new ANTLRInputStream(searchExpression));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        return parser.start();
    }
}

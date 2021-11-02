package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibQueryVisitor {
    public static final String NO_EXPLICIT_FIELD = "default";
    private static final Logger LOGGER = LoggerFactory.getLogger(BibQueryVisitor.class);

    private final BibEntry bibEntry;
    private EnumSet<SearchRules.SearchFlags> searchFlags;

    public BibQueryVisitor(BibEntry bibEntry, EnumSet<SearchRules.SearchFlags> searchFlags) {
        this.bibEntry = bibEntry;
        this.searchFlags = searchFlags;
    }

    public boolean matchFound(QueryNode query) {
        if (query instanceof FieldQueryNode) {
            return matchFound((FieldQueryNode) query);
        } else if (query instanceof BooleanQueryNode) {
            return matchFound((BooleanQueryNode) query);
        } else if (query instanceof ModifierQueryNode) {
            return matchFound((ModifierQueryNode) query);
        } else if (query instanceof RegexpQueryNode) {
            return matchFound((RegexpQueryNode) query);
        } else {
            LOGGER.error("Unsupported case when transforming the query:\n {}", query);
            return false;
        }
    }

    private boolean matchFound(String searchField, String searchString, boolean regExMode) {
        Pattern pattern = null;
        if (regExMode) {
            try {
                pattern = Pattern.compile(searchString, searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                LOGGER.error("Could not compile regex {}", searchString, ex);
                return false;
            }
        } else {
            if (!searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
                searchString = searchString.toLowerCase(Locale.ROOT);
            }
        }
        String finalSearchString = searchString;

        Pattern finalPattern = pattern;

        if (searchField.isEmpty()) {
            return bibEntry.getFields().stream()
                           .map(field -> bibEntry.getLatexFreeField(field))
                           .filter(content -> matchFound(finalSearchString, finalPattern, content))
                           .findAny()
                           .isPresent();
        } else {
            Field field = FieldFactory.parseField(searchField);
            return matchFound(finalSearchString, finalPattern, bibEntry.getLatexFreeField(field));
        }
    }

    private boolean matchFound(FieldQueryNode query) {
        if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            LOGGER.info("RegEx search is not supported in lucene search");
        }
        return matchFound(query.getFieldAsString(), query.getTextAsString(), false);
    }

    private boolean matchFound(RegexpQueryNode query) {
        return matchFound(query.getFieldAsString(), query.getText().toString(), true);
    }

    /**
     * @param finalPattern if not null, the RegEx pattern to match for. If null, a "normal" search is executed
     */
    private boolean matchFound(String finalSearchString, Pattern finalPattern, Optional<String> content) {
        return content.map(value -> searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? value : value.toLowerCase(Locale.ROOT))
                      .filter(value -> {
                          if (finalPattern != null) {
                              return finalPattern.matcher(value).find();
                          } else {
                              List<String> unmatchedWords = new SentenceAnalyzer(finalSearchString).getWords();
                              Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
                              while (unmatchedWordsIterator.hasNext()) {
                                  String word = unmatchedWordsIterator.next();
                                  if (value.contains(word)) {
                                      unmatchedWordsIterator.remove();
                                  }
                              }
                              return unmatchedWords.isEmpty();
                          }
                      })
                      .isPresent();
    }

    private boolean matchFound(BooleanQueryNode query) {
        if (query instanceof OrQueryNode) {
            return matchFound((OrQueryNode) query);
        } else {
            // AND is the default
            return matchFoundForAnd(query);
        }
    }

    private boolean matchFoundForAnd(BooleanQueryNode query) {
        for (QueryNode child : query.getChildren()) {
            if (!matchFound(child)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchFound(OrQueryNode query) {
        for (QueryNode child : query.getChildren()) {
            if (matchFound(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchFound(ModifierQueryNode query) {
        ModifierQueryNode.Modifier modifier = query.getModifier();
        if (modifier == ModifierQueryNode.Modifier.MOD_NOT) {
            return !matchFound(query.getChild());
        } else {
            // optional not yet supported
            return matchFound(query.getChild());
        }
    }
}

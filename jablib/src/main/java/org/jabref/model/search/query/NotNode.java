package org.jabref.model.search.query;

public record NotNode(BaseQueryNode negatedNode) implements BaseQueryNode {
}

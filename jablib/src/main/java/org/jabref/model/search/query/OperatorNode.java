package org.jabref.model.search.query;

import java.util.List;

public record OperatorNode(Operator op, List<BaseQueryNode> children) implements BaseQueryNode {
    public enum Operator {
        AND,
        OR
    }
}

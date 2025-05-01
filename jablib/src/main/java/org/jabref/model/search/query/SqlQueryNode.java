package org.jabref.model.search.query;

import java.util.ArrayList;
import java.util.List;

public class SqlQueryNode {
    private final String cte;
    private final List<String> params;

    public SqlQueryNode(String cte, List<String> params) {
        this.cte = cte;
        this.params = new ArrayList<>(params);
    }

    public SqlQueryNode(String cte) {
        this.cte = cte;
        this.params = List.of();
    }

    public String cte() {
        return cte;
    }

    public List<String> params() {
        return params;
    }
}

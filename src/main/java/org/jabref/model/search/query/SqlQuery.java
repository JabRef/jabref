package org.jabref.model.search.query;

import java.util.ArrayList;
import java.util.List;

public class SqlQuery {
    private final String cte;
    private final List<String> params;

    public SqlQuery(String cte, List<String> params) {
        this.cte = cte;
        this.params = new ArrayList<>(params);
    }

    public SqlQuery(String cte) {
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

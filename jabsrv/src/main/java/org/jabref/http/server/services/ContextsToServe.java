package org.jabref.http.server.services;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;

import jakarta.inject.Singleton;

@Singleton
public class ContextsToServe {
    private List<BibDatabaseContext> contextsToServe;

    public void setContextsToServe(List<BibDatabaseContext> contextsToServe) {
        this.contextsToServe = contextsToServe;
    }

    public List<BibDatabaseContext> getContextsToServe() {
        return contextsToServe;
    }
}

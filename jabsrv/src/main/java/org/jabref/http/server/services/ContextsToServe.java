package org.jabref.http.server.services;

import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;

import jakarta.inject.Singleton;

@Singleton
public class ContextsToServe {
    private ObservableList<BibDatabaseContext> contextsToServe;

    public void setContextsToServe(ObservableList<BibDatabaseContext> contextsToServe) {
        this.contextsToServe = contextsToServe;
    }

    public ObservableList<BibDatabaseContext> getContextsToServe() {
        return contextsToServe;
    }

    /// contextsToServe.isEmpty() could be true when no libraries are opened in JabRef
    /// Check filesToServe.isEmpty() to determine whether called from the CLI or GUI
    public boolean isEmpty() {
        return contextsToServe == null || contextsToServe.isEmpty();
    }
}

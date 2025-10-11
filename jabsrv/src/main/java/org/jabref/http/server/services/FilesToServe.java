package org.jabref.http.server.services;

import java.nio.file.Path;
import java.util.List;

import jakarta.inject.Singleton;

@Singleton
public class FilesToServe {
    private List<Path> filesToServe;

    public void setFilesToServe(List<Path> filesToServe) {
        this.filesToServe = filesToServe;
    }

    public List<Path> getFilesToServe() {
        return filesToServe;
    }

    public boolean isEmpty() {
        return filesToServe == null || filesToServe.isEmpty();
    }
}

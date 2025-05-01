package org.jabref.http.server.services;

import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class FilesToServe {
    private List<Path> filesToServe;

    public void setFilesToServe(List<Path> filesToServe) {
        this.filesToServe = filesToServe;
    }

    public List<Path> getFilesToServe() {
        return filesToServe;
    }
}

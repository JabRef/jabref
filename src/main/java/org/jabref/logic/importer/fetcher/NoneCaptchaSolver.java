package org.jabref.logic.importer.fetcher;

public class NoneCaptchaSolver implements CaptchaSolver {
    @Override
    public String solve(String queryURL) {
        return "";
    }
}

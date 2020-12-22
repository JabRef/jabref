package org.jabref.logic.importer.fetcher;

public interface CaptchaSolver {

    /**
     * Instructs the user to solve the captcha given at
     *
     * @param queryURL the URL to query
     * @return html content after solving the captcha
     */
    String solve(String queryURL);
}

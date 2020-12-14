package org.jabref.logic.importer.fetcher;

public interface CaptchaSolver {

    /**
     * Instructes the user to solve the captcha given at
     * @param queryURL
     * @return
     */
    String solve(String queryURL);
}

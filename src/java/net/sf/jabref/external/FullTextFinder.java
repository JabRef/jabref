package net.sf.jabref.external;

import java.net.URL;

/**
 * This interface is used for classes that try to resolve a full-text PDF url from an article
 * web page. Implementing classes should specialize on specific article sites.
 *  */
public interface FullTextFinder {

    /**
     * Report whether this FullTextFinder works for the site providing the given URL.
     *
     * @return true if the site is supported, false otherwise. If the site might be supported,
     *   it is best to return true.
     */
    public boolean supportsSite(URL url);

       /**
     * Take the source HTML for an article page, and try to find the URL to the
     * full text for this article.
     *
     * @param url The URL to the article's web page.
     * @param pageSource The contents of the article's web page.
     * @return The fulltext PDF URL, if found, or null if not found.
     */
    public URL findFullTextURL(URL url, String pageSource);
}

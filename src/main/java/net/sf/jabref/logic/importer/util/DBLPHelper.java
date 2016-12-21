package net.sf.jabref.logic.importer.util;

public class DBLPHelper {

    private final DBLPQueryCleaner cleaner = new DBLPQueryCleaner();

    /*
     * This is a small helper class that cleans the user submitted query. Right
     * now, we cannot search for ":" on dblp.org. So, we remove colons from the
     * user submitted search string. Also, the search is case sensitive if we
     * use capitals. So, we better change the text to lower case.
     */

    static class DBLPQueryCleaner {

        public String cleanQuery(final String query) {
            String cleaned = query;

            cleaned = cleaned.replace("-", " ").replace(" ", "%20").replace(":", "").toLowerCase();

            return cleaned;
        }
    }


    /**
     *
     * @param query
     *            string with the user query
     * @return a string with the user query, but compatible with dblp.org
     */
    public String cleanDBLPQuery(String query) {
        return cleaner.cleanQuery(query);
    }


}

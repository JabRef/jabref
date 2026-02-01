package org.jabref.logic.importer.fetcher.citation.opencitations;

public class CountResponse {
    private String count;

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public int getCountAsInt() {
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

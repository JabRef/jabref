package org.jabref.gui.entryeditor.citationrelationtab;

public class RelatedEntriesComponentConfig {
    private String heading;

    public String getHeading() {
        return heading == null ? "" : heading;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        RelatedEntriesComponentConfig config = new RelatedEntriesComponentConfig();

        public Builder withHeading(String heading) {
            this.config.heading = heading;
            return this;
        }

        public RelatedEntriesComponentConfig build() {
            return config;
        }

    }
}

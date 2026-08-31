package org.jabref.model.study;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public class StudyCatalog {
    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("max-results")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxResults;

    public StudyCatalog(String name, boolean enabled, String reason) {
        this.name = name;
        this.enabled = enabled;
        this.reason = reason != null ? reason : "";
    }

    public StudyCatalog(String name, boolean enabled) {
        this(name, enabled, "");
    }

    /// Used for Jackson deserialization
    public StudyCatalog() {
        this.name = "";
        this.enabled = true;
        this.reason = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public @Nullable Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StudyCatalog that = (StudyCatalog) o;

        if (isEnabled() != that.isEnabled()) {
            return false;
        }
        if (!Objects.equals(getName(), that.getName())) {
            return false;
        }
        if (!Objects.equals(getReason(), that.getReason())) {
            return false;
        }
        return Objects.equals(getMaxResults(), that.getMaxResults());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, reason, maxResults);
    }

    @Override
    public String toString() {
        return "StudyCatalog{" +
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                ", reason='" + reason + '\'' +
                ", maxResults=" + maxResults +
                '}';
    }
}

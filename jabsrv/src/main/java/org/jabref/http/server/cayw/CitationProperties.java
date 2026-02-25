package org.jabref.http.server.cayw;

import java.util.Optional;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

public class CitationProperties {

    private @Nullable LocatorType locatorType;
    private @Nullable String locatorValue;
    private @Nullable String prefix;
    private @Nullable String suffix;
    private boolean omitAuthor;

    public CitationProperties() {
    }

    public void setLocatorType(@Nullable LocatorType locatorType) {
        this.locatorType = locatorType;
    }

    public Optional<LocatorType> getLocatorType() {
        return Optional.ofNullable(locatorType);
    }

    public CitationProperties withLocatorType(@Nullable LocatorType locatorType) {
        this.locatorType = locatorType;
        return this;
    }

    public void setLocatorValue(@Nullable String locatorValue) {
        this.locatorValue = locatorValue;
    }

    public Optional<String> getLocatorValue() {
        return Optional.ofNullable(locatorValue).filter(s -> !s.isBlank());
    }

    public CitationProperties withLocatorValue(@Nullable String locatorValue) {
        this.locatorValue = locatorValue;
        return this;
    }

    public void setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
    }

    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix)
                       .map(String::strip)
                       .filter(s -> !s.isEmpty());
    }

    public CitationProperties withPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        return this;
    }

    public void setSuffix(@Nullable String suffix) {
        this.suffix = suffix;
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix)
                       .map(String::strip)
                       .filter(s -> !s.isEmpty());
    }

    public CitationProperties withSuffix(@Nullable String suffix) {
        this.suffix = suffix;
        return this;
    }

    public boolean isOmitAuthor() {
        return omitAuthor;
    }

    public void setOmitAuthor(boolean omitAuthor) {
        this.omitAuthor = omitAuthor;
    }

    public CitationProperties withOmitAuthor(boolean omitAuthor) {
        this.omitAuthor = omitAuthor;
        return this;
    }

    public Optional<String> getFormattedLocator() {
        if (locatorType == null || getLocatorValue().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("%s %s".formatted(locatorType.getAbbreviation(), locatorValue));
    }

    /// Returns the combined postnote string (locator + suffix)
    public Optional<String> getPostnote() {
        StringJoiner joiner = new StringJoiner(", ");
        getFormattedLocator().ifPresent(joiner::add);
        getSuffix().ifPresent(joiner::add);
        String result = joiner.toString();
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    public boolean hasProperties() {
        return getFormattedLocator().isPresent()
                || getPrefix().isPresent()
                || getSuffix().isPresent()
                || omitAuthor;
    }
}

package org.jabref.support;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

/// Excludes the html-to-node library from architecture analysis: it shares the org.jabref
/// namespace, but it is an external dependency and not part of this repository's sources.
public class DoNotIncludeHtmlToNode implements ImportOption {

    @Override
    public boolean includes(Location location) {
        return !location.contains("/org/jabref/htmltonode/");
    }
}

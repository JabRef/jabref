package org.jabref.preferences;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record WindowPreferences(
        WindowCoordinates screen1coordinates,
        WindowCoordinates screen2coordinates,
        WindowCoordinates screen3coordinates,
        boolean maximized) {
    @RecordBuilder
    public record WindowCoordinates(double x, double y, double width, double height) {
    }
}

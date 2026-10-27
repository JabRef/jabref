package org.jabref.logic.util.io;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AutoLinkPreferences {

    public enum CitationKeyDependency {
        START, // Filenames starting with the citation key
        EXACT, // Filenames exactly matching the citation key
        REGEX // Filenames matching a regular expression pattern
    }

    private static final SimpleObjectProperty<Character> DEFAULT_KEYWORD_SEPARATOR = new SimpleObjectProperty<>(',');

    private final ObjectProperty<CitationKeyDependency> citationKeyDependency;
    private final StringProperty regularExpression;
    private final BooleanProperty askAutoNamingPdfs;
    private final SimpleObjectProperty<Character> keywordSeparator;

    private AutoLinkPreferences() {
        this(
                CitationKeyDependency.START,            // Citation key dependency
                "**/.*[citationkey].*\\\\.[extension]", // Regular expression
                true,                                   // Ask auto naming PDFs
                DEFAULT_KEYWORD_SEPARATOR               // Keyword separator
        );
    }

    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean askAutoNamingPdfs,
                               ReadOnlyObjectProperty<Character> keywordSeparatorProperty) {
        this.citationKeyDependency = new SimpleObjectProperty<>(citationKeyDependency);
        this.regularExpression = new SimpleStringProperty(regularExpression);
        this.askAutoNamingPdfs = new SimpleBooleanProperty(askAutoNamingPdfs);

        this.keywordSeparator = new SimpleObjectProperty<>();
        this.keywordSeparator.bind(keywordSeparatorProperty);
    }

    /// For testing purpose
    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean askAutoNamingPdfs,
                               Character keywordSeparator) {
        this.citationKeyDependency = new SimpleObjectProperty<>(citationKeyDependency);
        this.regularExpression = new SimpleStringProperty(regularExpression);
        this.askAutoNamingPdfs = new SimpleBooleanProperty(askAutoNamingPdfs);
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
    }

    public static AutoLinkPreferences getDefault() {
        return new AutoLinkPreferences();
    }

    public CitationKeyDependency getCitationKeyDependency() {
        return citationKeyDependency.getValue();
    }

    public ObjectProperty<CitationKeyDependency> citationKeyDependencyProperty() {
        return citationKeyDependency;
    }

    public void setCitationKeyDependency(CitationKeyDependency citationKeyDependency) {
        this.citationKeyDependency.set(citationKeyDependency);
    }

    public String getRegularExpression() {
        return regularExpression.getValue();
    }

    public StringProperty regularExpressionProperty() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) {
        this.regularExpression.set(regularExpression);
    }

    public boolean shouldAskAutoNamingPdfs() {
        return askAutoNamingPdfs.getValue();
    }

    public BooleanProperty askAutoNamingPdfsProperty() {
        return askAutoNamingPdfs;
    }

    public void setAskAutoNamingPdfs(boolean askAutoNamingPdfs) {
        this.askAutoNamingPdfs.set(askAutoNamingPdfs);
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.getValue();
    }
}

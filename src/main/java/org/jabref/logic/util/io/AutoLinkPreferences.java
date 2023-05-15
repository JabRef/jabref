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

    private final ObjectProperty<CitationKeyDependency> citationKeyDependency;
    private final StringProperty regularExpression;
    private final BooleanProperty askAutoNamingPdfs;
    private final ReadOnlyObjectProperty<Character> keywordSeparator;
    private final BooleanProperty autoLinkFiles;

    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean askAutoNamingPdfs,
                               ObjectProperty<Character> keywordSeparatorProperty,
                               boolean autoLinkFiles) {
        this.citationKeyDependency = new SimpleObjectProperty<>(citationKeyDependency);
        this.regularExpression = new SimpleStringProperty(regularExpression);
        this.askAutoNamingPdfs = new SimpleBooleanProperty(askAutoNamingPdfs);
        this.keywordSeparator = keywordSeparatorProperty;
        this.autoLinkFiles = new SimpleBooleanProperty(autoLinkFiles);
    }

    /**
     * For testing purpose
     */
    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean askAutoNamingPdfs,
                               Character keywordSeparator,
                               boolean autoLinkFiles) {
        this.citationKeyDependency = new SimpleObjectProperty<>(citationKeyDependency);
        this.regularExpression = new SimpleStringProperty(regularExpression);
        this.askAutoNamingPdfs = new SimpleBooleanProperty(askAutoNamingPdfs);
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
        this.autoLinkFiles = new SimpleBooleanProperty(autoLinkFiles);
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

    public boolean autoLinkFilesEnabled() {
        return this.autoLinkFiles.getValue();
    }

    public BooleanProperty autoLinkEnabledProperty() {
        return this.autoLinkFiles;
    }

    public void setAutoLinkFilesEnabled(boolean enabled) {
        this.autoLinkFiles.setValue(enabled);
    }
}

package org.jabref.gui.sharelatex;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.sharelatex.ShareLatexProject;

/**
 * Data class
 * @author CS
 *
 */
public class ShareLatexProjectViewModel {

    private final SimpleBooleanProperty active = new SimpleBooleanProperty(false);
    private final String projectId;
    private final StringProperty projectTitle;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty lastUpdated;

    public ShareLatexProjectViewModel(ShareLatexProject project) {
        this.projectId = project.getProjectId();
        this.projectTitle = new SimpleStringProperty(project.getProjectTitle());
        this.firstName = new SimpleStringProperty(project.getFirstName());
        this.lastName = new SimpleStringProperty(project.getLastName());
        this.lastUpdated = new SimpleStringProperty(project.getLastUpdated());
    }

    public String getProjectId() {
        return projectId;
    }

    public StringProperty getProjectTitle() {
        return projectTitle;
    }

    public StringProperty getFirstName() {
        return firstName;
    }

    public StringProperty getLastName() {
        return lastName;
    }

    public StringProperty getLastUpdated() {
        return lastUpdated;
    }

    public Boolean isActive() {
        return active.getValue();
    }

    public BooleanProperty isActiveProperty() {
        return active;
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }
}

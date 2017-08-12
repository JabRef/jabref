package org.jabref.model.sharelatex;

import java.util.Objects;

public class ShareLatexProject {

    private final String projectId;
    private final String projectTitle;
    private final String firstName;
    private final String lastName;
    private final String lastUpdated;

    public ShareLatexProject(String projectId, String projectTitle, String firstName, String lastName, String lastUpdated) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.lastUpdated = lastUpdated;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return "[projectId =" + projectId + ", "
                + "projectTitle = " + projectTitle + ", "
                + "firstName = " + firstName + ", "
                + "lastName = " + lastName + ", "
                + "lastUpdated = " + lastUpdated + "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ShareLatexProject other = (ShareLatexProject) obj;

        return Objects.equals(projectId, other.projectId) && Objects.equals(projectTitle, other.projectTitle) && Objects.equals(lastUpdated, other.lastUpdated) && Objects.equals(lastName, other.lastName) && Objects.equals(firstName, other.firstName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, projectTitle, lastUpdated, lastName, firstName);
    }

}

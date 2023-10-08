package org.jabref.gui.git;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GitPreferences {
    private StringProperty username;
    private StringProperty password;

    public GitPreferences(String username, String password) {
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
    }

    public GitPreferences(StringProperty username, StringProperty password) {
        this.username = username;
        this.password = password;
    }

    public StringProperty getUsernameProperty() {
        return this.username;
    }

    public String getUsername() {
        return this.username.get();
    }

    public StringProperty getPasswordProperty() {
        return this.password;
    }

    public String getPassword() {
        return this.password.get();
    }

    public void setPassword(StringProperty password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = new SimpleStringProperty(password);
    }

    public void setUsername(StringProperty username) {
        this.username = username;
    }

    public void setUsername(String username) {
        this.username = new SimpleStringProperty(username);
    }
}

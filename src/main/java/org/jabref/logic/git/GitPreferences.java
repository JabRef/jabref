package org.jabref.logic.git;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GitPreferences {
    private StringProperty username = new SimpleStringProperty();
    private StringProperty password = new SimpleStringProperty();

    public GitPreferences(String username, String password) {
        //this.username.setValue(username);
        //this.password.set(password);
    }

    public String getUsername() {
        return username.toString();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public final String getPassword() {
        return password.getValue();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }
}

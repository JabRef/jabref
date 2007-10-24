/*
 * DBStrings.java
 *
 * Created on October 1, 2007, 6:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jabref.sql;

import net.sf.jabref.Globals;

/**
 *
 * @author pattonlk
 */
public class DBStrings {
   
    private String serverType;
    private String serverHostname;
    private String database;
    private String username;
    private String password;

    private String[] serverTypes;
    private boolean isInitialized;
    private boolean configValid;

    /** Creates a new instance of DBStrings */
    public DBStrings() {
        this.setServerType(null);
        this.setServerHostname(null);
        this.setDatabase(null);
        this.setUsername(null);
        this.setPassword(null);
        this.isInitialized(false);
        this.isConfigValid(false);
    }

    public void initialize() {
        //String [] servers = {Globals.lang("MySQL"), Globals.lang("Derby")};
        String [] servers = {Globals.lang("MySQL")};
        setServerTypes(servers);
        setServerType(Globals.lang("MySQL"));
        setServerHostname(Globals.lang("localhost"));
        setDatabase(Globals.lang("jabref"));
        setUsername(Globals.lang("root"));
        setPassword("");
        isInitialized(true);
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public void setServerHostname(String serverHostname) {
        this.serverHostname = serverHostname;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServerType() {
        return serverType;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String[] getServerTypes() {
        return serverTypes;
    }

    public void setServerTypes(String[] serverTypes) {
        this.serverTypes = serverTypes;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void isInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    public boolean isConfigValid() {
        return configValid;
    }

    public void isConfigValid(boolean configValid) {
        this.configValid = configValid;
    }

}

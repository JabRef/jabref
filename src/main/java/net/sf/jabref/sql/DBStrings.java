/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

   /**
    * Initializes the variables needed with defaults
    */
    public void initialize() {
        String [] servers = {Globals.lang("MySQL"), Globals.lang("PostgreSQL")};
        setServerTypes(servers);
        setServerType(Globals.prefs.get("dbConnectServerType"));
        setServerHostname(Globals.prefs.get("dbConnectHostname"));
        setDatabase(Globals.prefs.get("dbConnectDatabase"));
        setUsername(Globals.prefs.get("dbConnectUsername"));
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

    /**
     * Store these db strings into JabRef preferences.
     */
    public void storeToPreferences() {
        Globals.prefs.put("dbConnectServerType", getServerType());
        Globals.prefs.put("dbConnectHostname", getServerHostname());
        Globals.prefs.put("dbConnectDatabase", getDatabase());
        Globals.prefs.put("dbConnectUsername", getUsername());
    }
}

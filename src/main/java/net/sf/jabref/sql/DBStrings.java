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
import net.sf.jabref.JabRefPreferences;

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
        String[] servers = {"MySQL", "PostgreSQL"};
        setServerTypes(servers);
        setServerType(Globals.prefs.get(JabRefPreferences.DB_CONNECT_SERVER_TYPE));
        setServerHostname(Globals.prefs.get(JabRefPreferences.DB_CONNECT_HOSTNAME));
        setDatabase(Globals.prefs.get(JabRefPreferences.DB_CONNECT_DATABASE));
        setUsername(Globals.prefs.get(JabRefPreferences.DB_CONNECT_USERNAME));
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

    private void setServerTypes(String[] serverTypes) {
        this.serverTypes = serverTypes;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void isInitialized(boolean isInit) {
        this.isInitialized = isInit;
    }

    public boolean isConfigValid() {
        return configValid;
    }

    public void isConfigValid(boolean confValid) {
        this.configValid = confValid;
    }

    /**
     * Store these db strings into JabRef preferences.
     */
    public void storeToPreferences() {
        Globals.prefs.put(JabRefPreferences.DB_CONNECT_SERVER_TYPE, getServerType());
        Globals.prefs.put(JabRefPreferences.DB_CONNECT_HOSTNAME, getServerHostname());
        Globals.prefs.put(JabRefPreferences.DB_CONNECT_DATABASE, getDatabase());
        Globals.prefs.put(JabRefPreferences.DB_CONNECT_USERNAME, getUsername());
    }
}

package net.sf.jabref.sql;

import net.sf.jabref.JabRefPreferences;

import java.util.Arrays;
import java.util.List;

public final class DBStringsPreferences {

    private final String serverType;
    private final String serverHostname;
    private final String username;
    private final String database;

    public DBStringsPreferences(String serverType, String serverHostname, String username, String database) {
        this.serverType = serverType;
        this.serverHostname = serverHostname;
        this.username = username;
        this.database = database;
    }

    public String getServerType() {
        return serverType;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public String getUsername() {
        return username;
    }

    public String getDatabase() {
        return database;
    }

    public static DBStringsPreferences loadFromPreferences(JabRefPreferences preferences) {
        return new DBStringsPreferences(
                preferences.get(JabRefPreferences.DB_CONNECT_SERVER_TYPE),
                preferences.get(JabRefPreferences.DB_CONNECT_HOSTNAME),
                preferences.get(JabRefPreferences.DB_CONNECT_USERNAME),
                preferences.get(JabRefPreferences.DB_CONNECT_DATABASE));
    }

    /**
     * Store these db strings into JabRef preferences.
     */
    public void storeToPreferences(JabRefPreferences preferences) {
        preferences.put(JabRefPreferences.DB_CONNECT_SERVER_TYPE, getServerType());
        preferences.put(JabRefPreferences.DB_CONNECT_HOSTNAME, getServerHostname());
        preferences.put(JabRefPreferences.DB_CONNECT_DATABASE, getDatabase());
        preferences.put(JabRefPreferences.DB_CONNECT_USERNAME, getUsername());
    }
}
